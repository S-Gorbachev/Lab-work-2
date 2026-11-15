import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AST-узел разобранного SQL-запроса.
 * Неизменяем после создания; коллекции возвращаются как unmodifiable.
 */
public abstract class Statement {

    /**
     * {@code INSERT INTO table [(cols)] VALUES (...), ...}.
     */
    public static final class Insert extends Statement {
        private final String table;
        private final List<String> cols;
        private final List<List<Object>> rows;

        /**
         * @param table имя таблицы
         * @param cols  список колонок (может быть пустым)
         * @param rows  список строк значений
         */
        public Insert(String table, List<String> cols, List<List<Object>> rows) {
            this.table = table;
            this.cols = (cols == null) ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(cols));
            List<List<Object>> copy = new ArrayList<List<Object>>();
            if (rows != null) {
                for (List<Object> r : rows) {
                    copy.add(Collections.unmodifiableList(new ArrayList<Object>(r)));
                }
            }
            this.rows = Collections.unmodifiableList(copy);
        }

        public String getTable() { return table; }
        public List<String> getCols() { return cols; }
        public List<List<Object>> getRows() { return rows; }

        @Override
        public String toString() {
            return "Insert{table=" + table + ", cols=" + cols + ", rows=" + rows + "}";
        }
    }

    /**
     * {@code SELECT cols|* FROM table [WHERE cond]}.
     */
    public static final class Select extends Statement {
        private final List<String> cols;
        private final String table;
        private final Condition cond;

        public Select(List<String> cols, String table, Condition cond) {
            this.cols = (cols == null) ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(cols));
            this.table = table;
            this.cond = cond;
        }

        public List<String> getCols() { return cols; }
        public String getTable() { return table; }
        public Condition getCond() { return cond; }

        @Override
        public String toString() {
            return "Select{cols=" + cols + ", table=" + table +
                    (cond != null ? ", where=" + cond : "") + "}";
        }
    }

    /**
     * {@code UPDATE table SET col=val[, ...] [WHERE cond]}.
     */
    public static final class Update extends Statement {
        private final String table;
        private final Map<String, Object> sets;
        private final Condition cond;

        public Update(String table, Map<String, Object> sets, Condition cond) {
            this.table = table;
            this.sets = (sets == null) ? Collections.<String, Object>emptyMap()
                    : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(sets));
            this.cond = cond;
        }

        public String getTable() { return table; }
        public Map<String, Object> getSets() { return sets; }
        public Condition getCond() { return cond; }

        @Override
        public String toString() {
            return "Update{table=" + table + ", set=" + sets +
                    (cond != null ? ", where=" + cond : "") + "}";
        }
    }

    /**
     * {@code DELETE FROM table [WHERE cond]}.
     */
    public static final class Delete extends Statement {
        private final String table;
        private final Condition cond;

        public Delete(String table, Condition cond) {
            this.table = table;
            this.cond = cond;
        }

        public String getTable() { return table; }
        public Condition getCond() { return cond; }

        @Override
        public String toString() {
            return "Delete{table=" + table +
                    (cond != null ? ", where=" + cond : "") + "}";
        }
    }

    /**
     * {@code CREATE TABLE name (field type [PRIMARY KEY], ...)}.
     */
    public static final class CreateTable extends Statement {
        private final String table;
        private final List<Field> fields;

        public CreateTable(String table, List<Field> fields) {
            this.table = table;
            this.fields = (fields == null) ? Collections.<Field>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Field>(fields));
        }

        public String getTable() { return table; }
        public List<Field> getFields() { return fields; }

        @Override
        public String toString() {
            return "CreateTable{table=" + table + ", fields=" + fields + "}";
        }
    }

    /**
     * {@code DROP TABLE name}.
     */
    public static final class DropTable extends Statement {
        private final String table;

        public DropTable(String table) { this.table = table; }

        public String getTable() { return table; }

        @Override
        public String toString() { return "DropTable{table=" + table + "}"; }
    }

    /**
     * {@code CREATE INDEX name ON table (column)}.
     */
    public static final class CreateIndex extends Statement {
        private final String index;
        private final String table;
        private final String column;

        public CreateIndex(String index, String table, String column) {
            this.index = index;
            this.table = table;
            this.column = column;
        }

        public String getIndex() { return index; }
        public String getTable() { return table; }
        public String getColumn() { return column; }

        @Override
        public String toString() {
            return "CreateIndex{name=" + index + ", table=" + table + ", column=" + column + "}";
        }
    }
}