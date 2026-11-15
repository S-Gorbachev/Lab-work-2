/**
 * Замер времени в наносекундах с форматированным выводом.
 */
public class Timer implements AutoCloseable {

    private final String label;
    private final long t0;
    private final ConsoleView view;

    private Timer(String label, ConsoleView view) {
        this.label = label;
        this.view = view;
        this.t0 = System.nanoTime();
    }

    /**
     * @param label метка для вывода
     * @param view  вывод
     * @return запущенный таймер
     */
    public static Timer start(String label, ConsoleView view) {
        return new Timer(label, view);
    }

    @Override
    public void close() {
        long ns = System.nanoTime() - t0;
        view.infoF("%s = %s", label, fmt(ns));
    }

    /**
     * Форматирует наносекунды в человекочитаемую строку.
     *
     * @param ns время в наносекундах
     * @return строка вида {@code "12.3 µs"}, {@code "1.42 ms"}, {@code "2.100 s"}
     */
    public static String fmt(long ns) {
        if (ns < 1_000_000L) return String.format("%.1f µs", ns / 1_000.0);
        if (ns < 1_000_000_000L) return String.format("%.2f ms", ns / 1_000_000.0);
        return String.format("%.3f s", ns / 1_000_000_000.0);
    }
}