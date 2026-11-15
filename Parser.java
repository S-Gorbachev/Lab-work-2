import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Рекурсивный парсер минимального подмножества SQL.
 * Возвращает {@link Statement} — неизменяемое AST.
 */
public class Parser {

    private final List<Token> tokens;
    private int pos = 0;

    /**
     * @param sql SQL-запрос
     */
    public Parser(String sql) {
        this.tokens = new Lexer(sql).tokenize();
    }

    private Token peek() { return tokens.get(pos); }
    private Token next() { return tokens.get(pos++); }

    private boolean isWord(String w) {
        Token t = peek();
        return t.type() == Tk.WORD && t.text().equalsIgnoreCase(w);
    }

    private boolean isPunc(String s) {
        Token t = peek();
        return t.type() == Tk.PUNC && t.text().equals(s);
    }

    private void expectWord(String w) {
        Token t = peek();
        if (t.type() != Tk.WORD || !t.text().equalsIgnoreCase(w)) {
            throw new RuntimeException("Expected '" + w + "' at pos " + t.pos() + ", got '" + t.text() + "'");
        }
        pos++;
    }

    private void expectPunc(String s) {
        Token t = peek();
        if (t.type() != Tk.PUNC || !t.text().equals(s)) {
            throw new RuntimeException("Expected '" + s + "' at pos " + t.pos() + ", got '" + t.text() + "'");
        }
        pos++;
    }

    private boolean consumeIf(String s) {
        if (isPunc(s)) { pos++; return true; }
        return false;
    }

    private boolean consumeWordIf(String w) {
        if (isWord(w)) { pos++; return true; }
        return false;
    }

    /**
     * Разбирает запрос.
     *
     * @return AST-узел запроса
     * @throws RuntimeException при синтаксической ошибке
     */
    public Statement parse() {
        if (isWord("INSERT")) return parseInsert();
        if (isWord("SELECT")) return parseSelect();
        if (isWord("UPDATE")) return parseUpdate();
        if (isWord("DELETE")) return parseDelete();
        if (isWord("CREATE")) return parseCreate();
        if (isWord("DROP")) return parseDrop();
        Token t = peek();
        throw new RuntimeException("Unknown statement at pos " + t.pos() + ": '" + t.text() + "'");
    }

    private Statement parseInsert() {
        expectWord("INSERT");
        expectWord("INTO");
        String table = next().text();

        List<String> cols = new ArrayList<String>();
        if (isPunc("(")) {
            pos++;
            cols.add(next().text());
            while (consumeIf(",")) cols.add(next().text());
            expectPunc(")");
        }

        expectWord("VALUES");
        List<List<Object>> rows = new ArrayList<List<Object>>();
        do {
            expectPunc("(");
            List<Object> vals = new ArrayList<Object>();
            vals.add(literal());
            while (consumeIf(",")) vals.add(literal());
            expectPunc(")");
            rows.add(vals);
        } while (consumeIf(","));

        consumeIf(";");
        return new Statement.Insert(table, cols, rows);
    }

    private Statement parseSelect() {
        expectWord("SELECT");
        List<String> cols = new ArrayList<String>();
        if (isPunc("*")) {
            pos++;
            cols.add("*");
        } else {
            cols.add(next().text());
            while (consumeIf(",")) cols.add(next().text());
        }
        expectWord("FROM");
        String table = next().text();

        Condition cond = consumeWordIf("WHERE") ? parseCond() : null;
        consumeIf(";");
        return new Statement.Select(cols, table, cond);
    }

    private Statement parseUpdate() {
        expectWord("UPDATE");
        String table = next().text();
        expectWord("SET");

        Map<String, Object> sets = new LinkedHashMap<String, Object>();
        do {
            String col = next().text();
            expectPunc("=");
            sets.put(col, literal());
        } while (consumeIf(","));

        Condition cond = consumeWordIf("WHERE") ? parseCond() : null;
        consumeIf(";");
        return new Statement.Update(table, sets, cond);
    }

    private Statement parseDelete() {
        expectWord("DELETE");
        expectWord("FROM");
        String table = next().text();
        Condition cond = consumeWordIf("WHERE") ? parseCond() : null;
        consumeIf(";");
        return new Statement.Delete(table, cond);
    }

    private Statement parseCreate() {
        expectWord("CREATE");

        if (consumeWordIf("INDEX")) {
            String idxName = next().text();
            expectWord("ON");
            String table = next().text();
            expectPunc("(");
            String col = next().text();
            expectPunc(")");
            consumeIf(";");
            return new Statement.CreateIndex(idxName, table, col);
        }

        expectWord("TABLE");
        String table = next().text();
        expectPunc("(");

        List<Field> fields = new ArrayList<Field>();
        do {
            String name = next().text();
            String type = next().text().toUpperCase();
            boolean pk = false;
            if (consumeWordIf("PRIMARY")) {
                expectWord("KEY");
                pk = true;
            }
            fields.add(new Field(name, FieldType.valueOf(type), pk));
        } while (consumeIf(","));

        expectPunc(")");
        consumeIf(";");
        return new Statement.CreateTable(table, fields);
    }

    private Statement parseDrop() {
        expectWord("DROP");
        expectWord("TABLE");
        String table = next().text();
        consumeIf(";");
        return new Statement.DropTable(table);
    }

    private Condition parseCond() {
        Condition left = parseAtom();
        while (isWord("AND") || isWord("OR")) {
            String op = next().text().toUpperCase();
            Condition right = parseAtom();
            left = new Condition.Binary(left, op, right);
        }
        return left;
    }

    private Condition parseAtom() {
        if (consumeIf("(")) {
            Condition c = parseCond();
            expectPunc(")");
            return c;
        }
        String col = next().text();
        String op = next().text();
        Object val = literal();
        return new Condition.Compare(col, op, val);
    }

    private Object literal() {
        Token tk = next();
        switch (tk.type()) {
            case NUMBER:
                if (tk.text().indexOf('.') >= 0) return Double.valueOf(Double.parseDouble(tk.text()));
                return Integer.valueOf(Integer.parseInt(tk.text()));
            case STRING:
                return tk.text();
            case WORD:
                if (tk.text().equalsIgnoreCase("TRUE")) return Boolean.TRUE;
                if (tk.text().equalsIgnoreCase("FALSE")) return Boolean.FALSE;
                throw new RuntimeException("Bad literal '" + tk.text() + "' at pos " + tk.pos());
            default:
                throw new RuntimeException("Bad literal '" + tk.text() + "' at pos " + tk.pos());
        }
    }
}