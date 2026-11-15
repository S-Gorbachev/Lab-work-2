/**
 * Прикладное исключение СУБД: дубликат PK, отсутствие таблицы,
 * несовпадение типов и т.п.
 */
public class DBException extends Exception {

    /**
     * @param message описание ошибки
     */
    public DBException(String message) {
        super(message);
    }

    /**
     * @param message описание ошибки
     * @param cause   причина
     */
    public DBException(String message, Throwable cause) {
        super(message, cause);
    }
}