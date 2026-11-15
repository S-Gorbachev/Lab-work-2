import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Исполнитель AST-узлов SQL-запросов.
 * <p>
 * Метод {@link #execute(Statement)} выполняет запрос и сохраняет БД на диск;
 * {@link #executeForInstance(Statement)} — только выполняет, без сохранения
 * (используется в тестах и пакетной загрузке).
 */
public class QueryExecutor {

    private final Database db;
    private final ConsoleView view;
    private boolean isTimer = false;

    /**
     * @param db   база данных
     * @param view вывод
     */
    public QueryExecutor(Database db, ConsoleView view) {
        this.db = db;
        this.view = view;
    }

    /**
     * @param t включать ли замер времени
     */
    public void setTimer(boolean t) {
        isTimer = t;
    }

    /**
     * @return {@code true}, если замер времени включён
     */
    public boolean getTimer() {
        return isTimer;
    }

    /**
     * Выполняет запрос и сохраняет БД на диск.
     *
     * @param s AST-узел
     * @throws Exception при ошибке исполнения
     */
    public void execute(Statement s) throws Exception {
        long t0 = System.nanoTime();
        executeProcess(s);
        long t1 = System.nanoTime();
        db.save();
        long t2 = System.nanoTime();
        if (isTimer) {
            view.infoF("Execution: %s\nSaving: %s\nTotal: %s",
                    Timer.fmt(t1 - t0), Timer.fmt(t2 - t1), Timer.fmt(t2 - t0));
        }
    }

    /**
     * Выполняет запрос без сохранения на диск.
     *
     * @param s AST-узел
     * @throws Exception при ошибке исполнения
     */
    public void executeForInstance(Statement s) throws Exception {
        long t0 = System.nanoTime();
        executeProcess(s);
        long t1 = System.nanoTime();
        if (isTimer) {
            view.infoF("Execution: %s", Timer.fmt(t1 - t0));
        }
    }

    /**
     * Диспетчеризация AST-узла в конкретный обработчик.
     *
     * @param s AST-узел
     * @throws Exception при ошибке
     */
    public void executeProcess(Statement s) throws Exception {
        if (s instanceof Statement.Insert i) execInsert(i);
        else if (s instanceof Statement.Select i) execSelect(i);
        else if (s instanceof Statement.Update i) execUpdate(i);
        else if (s instanceof Statement.Delete i) execDelete(i);
        else if (s instanceof Statement.CreateTable i) {
            try {
                db.createTable(i.getTable(), i.getFields());
                view.infoF("Table %s created", i.getTable());
            } catch (DBException e) {
                view.errorF("Table %s already exists", i.getTable());
            }
        } else if (s instanceof Statement.DropTable i) {
            try {
                db.dropTable(i.getTable());
                view.infoF("Table %s dropped", i.getTable());
            } catch (DBException e) {
                view.errorF("Table %s doesn't exists", i.getTable());
            }
        } else if (s instanceof Statement.CreateIndex i) {
            execCreateIndex(i);
        } else {
            throw new DBException("Unknown statement: " + s);
        }
    }

    private void execInsert(Statement.Insert ins) throws DBException {
        Table t = db.table(ins.getTable());
        List<String> cols;
        if (ins.getCols().isEmpty()) {
            cols = new ArrayList<String>();
            for (Field f : t.getSchema()) cols.add(f.getName());
        } else {
            cols = ins.getCols();
        }
        for (List<Object> vals : ins.getRows()) {
            if (vals.size() != cols.size())
                throw new DBException("Column count mismatch");
            Row r = new Row();
            for (int i = 0; i < cols.size(); i++) {
                Field f = t.field(cols.get(i));
                r.set(f.getName(), coerce(vals.get(i), f.getType()));
            }
            t.insert(r);
            view.info("1 row inserted");
        }
    }

    private void execSelect(Statement.Select s) throws DBException {
        Table t = db.table(s.getTable());
        Condition cond = s.getCond();

        List<Row> data = new ArrayList<Row>();

        Index idx = null;
        if (cond != null) {
            String col = cond.indexedColumn();
            if (col != null) idx = t.getIndexes().get(col);
        }
        if (idx != null && cond instanceof Condition.Compare) {
            Condition.Compare c = (Condition.Compare) cond;
            Object key = coerce(c.val, t.field(c.col).getType());
            for (Integer id : idx.lookupEq(key)) {
                if (id >= 0 && id < t.getRows().size()) {
                    Row r = t.getRows().get(id);
                    if (cond.test(r)) data.add(r);
                }
            }
        } else {
            for (Row r : t) {
                if (cond == null || cond.test(r)) data.add(r);
            }
        }

        List<String> cols;
        if (s.getCols().size() == 1 && "*".equals(s.getCols().get(0))) {
            cols = new ArrayList<String>();
            for (Field f : t.getSchema()) cols.add(f.getName());
        } else {
            cols = s.getCols();
        }

        view.printTable(cols, data);
        view.infoF("(%d rows)", data.size());
    }

    private void execUpdate(Statement.Update u) throws DBException {
        Table t = db.table(u.getTable());
        Condition cond = u.getCond();
        int n = t.update(r -> cond == null || cond.test(r), u.getSets());
        view.infoF("%d rows updated", n);
    }

    private void execDelete(Statement.Delete d) throws DBException {
        Table t = db.table(d.getTable());
        Condition cond = d.getCond();
        int n = t.delete(r -> cond == null || cond.test(r));
        view.infoF("%d rows deleted", n);
    }

    private void execCreateIndex(Statement.CreateIndex ci) throws DBException {
        Table t = db.table(ci.getTable());
        Index idx = new Index(t.getName(), ci.getColumn());
        for (int i = 0; i < t.getRows().size(); i++) {
            idx.add(t.getRows().get(i).get(ci.getColumn()), i);
        }
        t.getIndexes().put(ci.getColumn(), idx);
        view.infoF("Index %s created on %s(%s)", ci.getIndex(), ci.getTable(), ci.getColumn());
    }

    private Object coerce(Object v, FieldType type) {
        switch (type) {
            case INT: return Integer.valueOf(((Number) v).intValue());
            case DOUBLE: return Double.valueOf(((Number) v).doubleValue());
            case BOOLEAN: return (v instanceof Boolean) ? v : Boolean.valueOf(v.toString());
            case STRING: return v.toString();
            case DATE:
                if (v instanceof Date) return v;
                try {
                    return new SimpleDateFormat("yyyy-MM-dd").parse(v.toString());
                } catch (Exception e) {
                    throw new RuntimeException("Bad date: " + v);
                }
        }
        return v;
    }
}