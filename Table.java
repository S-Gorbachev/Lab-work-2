import java.io.*;
import java.util.*;

/**
 * Таблица: схема, строки, индексы и счётчик идентификаторов.
 * Сериализуется целиком в файл {@code <name>.db}.
 */
public class Table implements Serializable, Iterable<Row> {

    private final String name;
    private final List<Field> schema = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, Index> indexes = new HashMap<>();
    private int nextRowId = 0;

    /**
     * @param name имя таблицы
     */
    public Table(String name) { this.name = name; }

    public String getName() { return name; }
    public List<Field> getSchema() { return schema; }
    public List<Row> getRows() { return rows; }
    public Map<String, Index> getIndexes() { return indexes; }

    /**
     * @param f поле схемы
     */
    public void addField(Field f) { schema.add(f); }

    /**
     * @param name имя столбца
     * @return поле или {@code null}
     */
    public Field field(String name) {
        for (Field f : schema) if (f.getName().equalsIgnoreCase(name)) return f;
        return null;
    }

    /**
     * @return поле-первичный ключ или {@code null}
     */
    public Field primaryKey() {
        for (Field f : schema) if (f.isPrimaryKey()) return f;
        return null;
    }

    /**
     * @param r строка
     * @return позиция строки в таблице
     */
    public int rowIdOf(Row r) { return rows.indexOf(r); }

    /**
     * Вставляет строку с проверкой уникальности первичного ключа.
     *
     * @param r строка
     * @throws DBException если PK пуст или не уникален
     */
    public void insert(Row r) throws DBException {
        Field pk = primaryKey();
        if (pk != null) {
            Object v = r.get(pk.getName());
            if (v == null) throw new DBException("PK is null: " + pk.getName());
            for (Row exist : rows)
                if (Objects.equals(exist.get(pk.getName()), v))
                    throw new DBException("Duplicate PK: " + v);
        }
        int id = nextRowId++;
        rows.add(r);
        for (Index idx : indexes.values())
            idx.add(r.get(idx.getColumn()), id);
    }

    /**
     * Обновляет строки, удовлетворяющие предикату, и поддерживает индексы.
     *
     * @param cond предикат отбора
     * @param sets новые значения по столбцам
     * @return число обновлённых строк
     */
    public int update(java.util.function.Predicate<Row> cond,
                      Map<String, Object> sets) {
        int n = 0;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (!cond.test(r)) continue;
            for (Index idx : indexes.values())
                idx.remove(r.get(idx.getColumn()), i);
            for (Map.Entry<String, Object> e : sets.entrySet())
                r.set(e.getKey(), e.getValue());
            for (Index idx : indexes.values())
                idx.add(r.get(idx.getColumn()), i);
            n++;
        }
        return n;
    }

    /**
     * Удаляет строки, удовлетворяющие предикату, и перестраивает индексы.
     *
     * @param cond предикат
     * @return число удалённых строк
     */
    public int delete(java.util.function.Predicate<Row> cond) {
        int n = 0;
        for (int i = rows.size() - 1; i >= 0; i--) {
            Row r = rows.get(i);
            if (cond.test(r)) { rows.remove(i); n++; }
        }
        if (n > 0) rebuildIndexes();
        return n;
    }

    /**
     * Перестраивает все индексы по текущим строкам.
     */
    public void rebuildIndexes() {
        for (String col : new ArrayList<String>(indexes.keySet())) {
            Index fresh = new Index(name, col);
            for (int i = 0; i < rows.size(); i++) {
                fresh.add(rows.get(i).get(col), i);
            }
            indexes.put(col, fresh);
        }
    }

    @Override
    public Iterator<Row> iterator() {
        return rows.iterator();
    }
}