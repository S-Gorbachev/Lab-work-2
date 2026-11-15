import java.util.*;

/**
 * Условие WHERE в SQL-запросе.
 * <p>
 * Представлено двумя видами: {@link Compare} — сравнение одного столбца со значением,
 * {@link Binary} — объединение двух условий через AND/OR.
 */
public abstract class Condition {

    /**
     * Проверяет, удовлетворяет ли строка условию.
     *
     * @param r строка таблицы
     * @return {@code true}, если строка подходит под условие
     */
    public abstract boolean test(Row r);

    /**
     * Возвращает имя столбца, по которому можно использовать индекс,
     * или {@code null}, если индекс неприменим.
     *
     * @return имя индексируемого столбца или {@code null}
     */
    public String indexedColumn() {
        return null;
    }

    /**
     * Сравнение одного столбца со значением: {@code col op val}.
     */
    public static class Compare extends Condition {

        final String col;
        final String op;
        final Object val;

        /**
         * @param c имя столбца
         * @param o оператор сравнения ({@code =, <>, <, >, <=, >=})
         * @param v значение для сравнения
         */
        public Compare(String c, String o, Object v) {
            col = c;
            op = o;
            val = v;
        }

        /**
         * Индекс применим только для оператора {@code =}.
         *
         * @return имя столбца для {@code =} или {@code null} иначе
         */
        @Override
        public String indexedColumn() {
            return op.equals("=") ? col : null;
        }

        /**
         * Проверяет сравнение с учётом типа: число, строка, дата.
         *
         * @param r строка таблицы
         * @return {@code true}, если условие выполнено
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public boolean test(Row r) {
            Object a = r.get(col);
            if (a == null || val == null) return false;
            int cmp;
            if (a instanceof Number && val instanceof Number)
                cmp = Double.compare(((Number) a).doubleValue(), ((Number) val).doubleValue());
            else if (a instanceof Comparable && a.getClass().equals(val.getClass()))
                cmp = ((Comparable) a).compareTo(val);
            else if (a instanceof java.util.Date && val instanceof java.util.Date)
                cmp = ((java.util.Date) a).compareTo((java.util.Date) val);
            else return false;
            switch (op) {
                case "=": return cmp == 0;
                case "<>": return cmp != 0;
                case "<": return cmp < 0;
                case ">": return cmp > 0;
                case "<=": return cmp <= 0;
                case ">=": return cmp >= 0;
            }
            return false;
        }
    }

    /**
     * Объединение двух условий через {@code AND} или {@code OR}.
     */
    public static class Binary extends Condition {

        final Condition l;
        final Condition r;
        final String op;

        /**
         * @param l левое условие
         * @param op {@code AND} или {@code OR}
         * @param r правое условие
         */
        public Binary(Condition l, String op, Condition r) {
            this.l = l;
            this.op = op;
            this.r = r;
        }

        @Override
        public boolean test(Row row) {
            return op.equals("AND") ? l.test(row) && r.test(row) : l.test(row) || r.test(row);
        }

        /**
         * Берёт индексируемый столбец из левой или правой части.
         *
         * @return имя столбца или {@code null}
         */
        @Override
        public String indexedColumn() {
            return l.indexedColumn() != null ? l.indexedColumn() : r.indexedColumn();
        }
    }
}