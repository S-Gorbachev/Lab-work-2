import java.io.Serializable;

/**
 * Описание столбца таблицы: имя, тип и признак первичного ключа.
 */
public class Field implements Serializable {

    private final String name;
    private final FieldType type;
    private final boolean primaryKey;

    /**
     * @param name       имя столбца
     * @param type       тип данных
     * @param primaryKey является ли столбец первичным ключом
     */
    public Field(String name, FieldType type, boolean primaryKey) {
        this.name = name;
        this.type = type;
        this.primaryKey = primaryKey;
    }

    /**
     * @return имя столбца
     */
    public String getName() {
        return name;
    }

    /**
     * @return тип данных
     */
    public FieldType getType() {
        return type;
    }

    /**
     * @return {@code true}, если столбец — первичный ключ
     */
    public boolean isPrimaryKey() {
        return primaryKey;
    }
}