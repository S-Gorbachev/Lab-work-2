import java.io.*;
import java.util.*;

/**
 * Индекс по одному столбцу: {@code TreeMap<значение, множество rowId>}.
 * <p>
 * Ускоряет поиск по равенству через {@link #lookupEq(Object)}.
 * Сериализуется в отдельный файл {@code <table>.<column>.idx}.
 */
public class Index implements Serializable {

    private final String tableName;
    private final String column;
    private final TreeMap<Comparable, Set<Integer>> map = new TreeMap<>();

    /**
     * @param tableName имя таблицы
     * @param column    имя индексируемого столбца
     */
    public Index(String tableName, String column) {
        this.tableName = tableName;
        this.column = column;
    }

    /**
     * @return имя таблицы
     */
    public String getTableName() { return tableName; }

    /**
     * @return имя индексируемого столбца
     */
    public String getColumn() { return column; }

    /**
     * Добавляет связь значение → rowId.
     *
     * @param value значение столбца
     * @param rowId идентификатор строки
     */
    @SuppressWarnings("unchecked")
    public void add(Object value, int rowId) {
        map.computeIfAbsent((Comparable) value, k -> new HashSet<>()).add(rowId);
    }

    /**
     * Удаляет связь значение → rowId.
     *
     * @param value значение столбца
     * @param rowId идентификатор строки
     */
    public void remove(Object value, int rowId) {
        Set<Integer> s = map.get(value);
        if (s != null) {
            s.remove(rowId);
            if (s.isEmpty()) map.remove(value);
        }
    }

    /**
     * Точное совпадение по значению.
     *
     * @param value значение
     * @return множество rowId или пустое множество
     */
    public Set<Integer> lookupEq(Object value) {
        return map.getOrDefault(value, Collections.emptySet());
    }

    /**
     * Диапазонный поиск (включительно).
     *
     * @param from нижняя граница или {@code null}
     * @param to   верхняя граница или {@code null}
     * @return множество rowId
     */
    public Set<Integer> lookupRange(Object from, Object to) {
        Set<Integer> res = new HashSet<>();
        NavigableMap<Comparable, Set<Integer>> sub =
                map.subMap(from == null ? map.firstKey() : (Comparable) from, true,
                        to == null ? map.lastKey() : (Comparable) to, true);
        sub.values().forEach(res::addAll);
        return res;
    }

    /**
     * @return количество уникальных значений в индексе
     */
    public int size() { return map.size(); }

    /**
     * @param dbDir каталог БД
     * @param table имя таблицы
     * @param column имя столбца
     * @return файл индекса
     */
    public static File fileFor(String dbDir, String table, String column) {
        return new File(dbDir, table + "." + column + ".idx");
    }

    /**
     * Сохраняет индекс на диск.
     *
     * @param dbDir каталог БД
     * @throws IOException при ошибке записи
     */
    public void save(String dbDir) throws IOException {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(fileFor(dbDir, tableName, column)))) {
            oos.writeObject(this);
        }
    }

    /**
     * Загружает индекс из файла.
     *
     * @param f файл индекса
     * @return индекс
     * @throws IOException            при ошибке чтения
     * @throws ClassNotFoundException если класс не найден
     */
    public static Index load(File f) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            return (Index) ois.readObject();
        }
    }
}