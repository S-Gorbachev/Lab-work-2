import java.util.ArrayList;
import java.util.List;

/**
 * Лексер SQL-запроса: превращает строку в список токенов.
 */
public class Lexer {

    private final String s;
    private int i = 0;

    /**
     * @param s SQL-запрос
     */
    Lexer(String s) {
        this.s = s;
    }

    /**
     * Разбивает строку на токены.
     *
     * @return список токенов, последний — {@link Tk#EOF}
     * @throws RuntimeException при недопустимом символе или незакрытой строке
     */
    List<Token> tokenize() {
        List<Token> out = new ArrayList<>();
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '\'') {
                int j = ++i;
                StringBuilder sb = new StringBuilder();
                while (i < s.length() && s.charAt(i) != '\'')
                    sb.append(s.charAt(i++));
                if (i >= s.length())
                    throw new RuntimeException("Unterminated string");
                i++;
                out.add(new Token(Tk.STRING, sb.toString(), j));
                continue;
            }
            if (Character.isDigit(c) || c == '-') {
                int j = i;
                if (c == '-') i++;
                while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.'))
                    i++;
                out.add(new Token(Tk.NUMBER, s.substring(j, i), j));
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int j = i;
                while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) i++;
                out.add(new Token(Tk.WORD, s.substring(j, i), j));
                continue;
            }
            if ("(),;=<>*".indexOf(c) >= 0) {
                out.add(new Token(Tk.PUNC, "" + c, i));
                i++;
                continue;
            }
            throw new RuntimeException("Bad char: " + c);
        }
        out.add(new Token(Tk.EOF, "", i));
        return out;
    }
}