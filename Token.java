/**
 * Токен SQL-запроса.
 *
 * @param type тип токена
 * @param text текст
 * @param pos  позиция в исходной строке
 */
public record Token(Tk type, String text, int pos) {}