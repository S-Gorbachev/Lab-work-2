import java.io.Serializable;
import java.util.*;

/**
 * Запись таблицы: набор значений по именам столбцов.
 * Порядок столбцов сохраняется ({@link LinkedHashMap}).
 */
public class Row implements Serializable {

    private final Map<String, Object> values = new LinkedHashMap<>();

    /**
     * @param col имя столбца
     * @return значение или {@code null}
     */
    public Object get(String col) {
        return values.get(col);
    }

    /**
     * @param col имя столбца
     * @param v   значение
     */
    public void set(String col, Object v) {
        values.put(col, v);
    }

    /**
     * @return карта значений
     */
    public Map<String, Object> getValues() {
        return values;
    }

    /**
     * @return неполная копия строки (значения — те же объекты)
     */
    public Row copy() {
        Row r = new Row();
        r.values.putAll(this.values);
        return r;
    }
}