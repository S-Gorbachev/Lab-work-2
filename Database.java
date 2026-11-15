import java.io.*;
import java.util.*;

/**
 * Singleton-хранилище таблиц. Отвечает за загрузку при старте
 * и сохранение на диск по требованию.
 */
public class Database {

    private static Database instance;
    private final Map<String, Table> tables = new LinkedHashMap<>();
    private final String dir;

    private Database(String dir) {
        this.dir = dir;
    }

    /**
     * Возвращает единственный экземпляр БД, загружая данные с диска при первом вызове.
     *
     * @param dir каталог хранения файлов
     * @return экземпляр БД
     */
    public static Database getInstance(String dir) {
        if (instance == null) {
            instance = new Database(dir);
            new File(dir).mkdirs();
            instance.load();
        }
        return instance;
    }

    /**
     * @return ранее созданный экземпляр БД или {@code null}
     */
    public static Database get() {
        return instance;
    }

    /**
     * @return карта имя-таблицы → таблица
     */
    public Map<String, Table> getTables() {
        return tables;
    }

    /**
     * Возвращает таблицу по имени (регистронезависимо).
     *
     * @param n имя таблицы
     * @return таблица
     * @throws DBException если таблицы нет
     */
    public Table table(String n) throws DBException {
        Table t = tables.get(n.toLowerCase());
        if (t == null) throw new DBException("Table not found: " + n);
        return t;
    }

    /**
     * @param n имя таблицы
     * @return {@code true}, если таблица существует
     */
    public boolean exists(String n) {
        return tables.containsKey(n.toLowerCase());
    }

    /**
     * Создаёт таблицу с заданной схемой.
     *
     * @param name   имя таблицы
     * @param fields список полей
     * @throws DBException если таблица уже существует
     */
    public void createTable(String name, List<Field> fields) throws DBException {
        if (exists(name)) throw new DBException("Table exists: " + name);
        Table t = new Table(name);
        fields.forEach(t::addField);
        tables.put(name.toLowerCase(), t);
    }

    /**
     * Удаляет таблицу и её файл с диска.
     *
     * @param name имя таблицы
     * @throws DBException если таблицы нет
     */
    public void dropTable(String name) throws DBException {
        String table = name.toLowerCase();
        if (!tables.containsKey(table))
            throw new DBException("Table not found");
        tables.remove(name.toLowerCase());
        new File(dir, name + ".db").delete();
    }

    /**
     * Сериализует все таблицы и их индексы на диск.
     */
    public void save() {
        for (Table t : tables.values()) {
            File f = new File(dir, t.getName() + ".db");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
                oos.writeObject(t);
            } catch (IOException e) {
                System.err.println("save error: " + e.getMessage());
            }
            for (Index idx : t.getIndexes().values()) {
                try {
                    idx.save(dir);
                } catch (IOException ignored) {}
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void load() {
        File[] files = new File(dir).listFiles((d, n) -> n.endsWith(".db"));
        if (files == null) return;
        for (File f : files) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                Table t = (Table) ois.readObject();
                for (Index idx : t.getIndexes().values()) {
                    File ixf = Index.fileFor(dir, t.getName(), idx.getColumn());
                    if (ixf.exists()) {
                        Index loaded = Index.load(ixf);
                    }
                }
                tables.put(t.getName().toLowerCase(), t);
            } catch (Exception e) {
                System.err.println("Load failed: " + f + " — " + e.getMessage());
            }
        }
    }
}