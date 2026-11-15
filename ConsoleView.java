import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Слой представления: печать в консоль.
 * <p>
 * Поддерживает включение/отключение вывода через {@link #setText(boolean)}
 * — полезно в тестах для пакетной загрузки данных.
 */
public class ConsoleView {

    private boolean isText = true;

    /**
     * Включает или отключает вывод в консоль.
     *
     * @param i {@code true} — печатать, {@code false} — молчать
     */
    public void setText(boolean i) {
        isText = i;
    }

    /**
     * @return текущее состояние вывода
     */
    public boolean getText() {
        return isText;
    }

    /**
     * Печатает набор строк в табличном виде с рамкой.
     *
     * @param cols список колонок для вывода
     * @param rows строки таблицы
     */
    public void printTable(List<String> cols, List<Row> rows) {
        Map<String, Integer> w = new LinkedHashMap<String, Integer>();
        for (String c : cols) w.put(c, c.length());
        for (Row r : rows) {
            for (String c : cols) {
                String s = fmt(r.get(c));
                if (s.length() > w.get(c)) w.put(c, s.length());
            }
        }

        StringBuilder sepB = new StringBuilder("+");
        for (int width : w.values()) {
            for (int i = 0; i < width + 2; i++) sepB.append('-');
            sepB.append('+');
        }
        String sep = sepB.toString();

        System.out.println(sep);
        StringBuilder head = new StringBuilder("|");
        for (Map.Entry<String, Integer> e : w.entrySet()) {
            head.append(' ').append(padRight(e.getKey(), e.getValue())).append(" |");
        }
        System.out.println(head);
        System.out.println(sep);

        for (Row r : rows) {
            StringBuilder sb = new StringBuilder("|");
            for (Map.Entry<String, Integer> e : w.entrySet()) {
                sb.append(' ').append(padRight(fmt(r.get(e.getKey())), e.getValue())).append(" |");
            }
            System.out.println(sb);
        }
        System.out.println(sep);
    }

    /**
     * Печатает сообщение (если вывод включён).
     *
     * @param msg текст сообщения
     */
    public void info(String msg) {
        if (isText) System.out.println(msg);
    }

    /**
     * Печатает отформатированное сообщение.
     *
     * @param msg  формат в стиле {@link String#format}
     * @param args аргументы формата
     */
    public void infoF(String msg, Object... args) {
        if (isText) System.out.println(String.format(msg, args));
    }

    /**
     * Печатает сообщение об ошибке с префиксом {@code ERROR:}.
     *
     * @param msg текст ошибки
     */
    public void error(String msg) {
        if (isText) System.out.println("ERROR: " + msg);
    }

    /**
     * Печатает отформатированное сообщение об ошибке.
     *
     * @param msg  формат
     * @param args аргументы формата
     */
    public void errorF(String msg, Object... args) {
        if (isText) System.out.println("ERROR: " + String.format(msg, args));
    }

    /**
     * Печатает приглашение {@code db> } без перевода строки.
     */
    public void prompt() {
        System.out.print("db> ");
        System.out.flush();
    }

    private static String padRight(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    private static String fmt(Object o) {
        if (o == null) return "NULL";
        if (o instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd").format((Date) o);
        }
        return String.valueOf(o);
    }
}