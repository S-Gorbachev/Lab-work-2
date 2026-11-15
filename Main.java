import java.util.Date;
import java.util.Random;
import java.util.Scanner;
import java.util.random.*;
import java.time.*;

/**
 * Точка входа: консольный интерфейс, встроенные генераторы тестовых данных.
 */
public class Main {

    /**
     * @param args необязательный каталог БД (по умолчанию {@code db})
     */
    public static void main(String[] args) {
        String dir = args.length > 0 ? args[0] : "db";
        Database db = Database.getInstance(dir);
        ConsoleView view = new ConsoleView();
        QueryExecutor exec = new QueryExecutor(db, view);

        System.out.println("---------- ДОБРО ПОЖАЛОВАТЬ! ----------");
        System.out.println("------ ВСТРОЕННЫЙ НАБОР ХЕЛПЕРОВ ------");
        System.out.println("• users - автоматическое создание таблицы \"users\"");
        System.out.println("• phone numbers - автоматическое создание таблицы \"phone_numbers\"");
        System.out.println("• operators - автоматическое создание таблицы \"operators\"");
        System.out.println("• +time - включить замер времени");
        System.out.println("• -time - выключить замер времени (по умолчанию)");
        System.out.println("• exit - выход из системы");

        Scanner sc = new Scanner(System.in);
        while (true) {
            view.prompt();
            if (!sc.hasNextLine()) break;
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("exit")) break;
            if (line.equalsIgnoreCase("+time")) { exec.setTimer(true); continue; }
            if (line.equalsIgnoreCase("-time")) { exec.setTimer(false); continue; }
            if (line.equalsIgnoreCase("phone numbers")) { create_phone_numbers(exec, view); continue; }
            if (line.equalsIgnoreCase("users")) { create_users(exec, view); continue; }
            if (line.equalsIgnoreCase("operators")) { create_operators(exec, view); continue; }
            try {
                Statement s = new Parser(line).parse();
                exec.execute(s);
            } catch (Exception e) {
                view.error(e.getMessage());
            }
        }
        db.save();
        System.out.println("---------- РАБОТА ОКОНЧЕНА -------------");
    }

    /**
     * Генерирует таблицу {@code users} с 10 000 случайными записями.
     *
     * @param exec исполнитель запросов
     * @param view вывод
     */
    private static void create_users(QueryExecutor exec, ConsoleView view) {
        boolean ct = exec.getTimer();
        exec.setTimer(false);
        view.setText(false);
        long t0 = System.nanoTime();

        Random rand = new Random(42);
        char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

        try {
            exec.execute(new Parser("DROP TABLE users;").parse());
        } catch (Exception e) {
            view.error(e.getMessage());
        }

        try {
            String create = "CREATE TABLE users " +
                    "(id INT PRIMARY KEY, initials STRING, phone_numberID INT, operatorID INT);";
            exec.executeForInstance(new Parser(create).parse());
        } catch (Exception e) {
            view.error(e.getMessage());
        }

        for (int i = 0; i < 10000; i++) {
            StringBuilder initials = new StringBuilder();
            initials.append(alphabet[rand.nextInt(alphabet.length)]).append('.');
            initials.append(alphabet[rand.nextInt(alphabet.length)]).append('.');

            StringBuilder phone = new StringBuilder();
            for (int g = 0; g < 3; g++) {
                if (g > 0) phone.append('-');
                for (int d = 0; d < 3; d++) phone.append(rand.nextInt(10));
            }

            int operatorID = rand.nextInt(5);
            int phone_numberID = rand.nextInt(10000);

            String insert = "INSERT INTO users VALUES (" + i
                    + ", '" + initials + "', " + phone_numberID + ", " + operatorID + ");";

            try {
                exec.executeForInstance(new Parser(insert).parse());
            } catch (Exception e) {
                view.error(e.getMessage());
            }
        }

        long t1 = System.nanoTime();
        view.setText(true);
        view.infoF("Execution: %s", Timer.fmt(t1 - t0));
        exec.setTimer(ct);
    }

    /**
     * Генерирует таблицу {@code phone_numbers} с 10 000 случайными записями.
     *
     * @param exec исполнитель запросов
     * @param view вывод
     */
    private static void create_phone_numbers(QueryExecutor exec, ConsoleView view) {
        boolean ct = exec.getTimer();
        exec.setTimer(false);
        view.setText(false);
        long t0 = System.nanoTime();

        String[] countries = new String[]{"Russia", "Ukraine", "Belarus", "USA", "UK", "Greece", "Poland"
                , "France", "Germany", "Australia", "Brazil"};
        Random rand = new Random(42);

        try {
            exec.execute(new Parser("DROP TABLE phone_numbers").parse());
        } catch (Exception e) {
            view.error(e.getMessage());
        }

        try {
            String create = "CREATE TABLE phone_numbers " +
                    "(id INT PRIMARY KEY, number STRING, country STRING, last_use DATE)";
            exec.executeForInstance(new Parser(create).parse());
        } catch (Exception e) {
            view.error(e.getMessage());
        }

        for (int i = 0; i < 10000; i++) {
            StringBuilder phone = new StringBuilder();
            String country = countries[rand.nextInt(countries.length)];
            LocalDate lastUsed = LocalDate.now().minusDays(rand.nextInt(3650));
            for (int t = 0; t < 3; t++) {
                if (t > 0) phone.append("-");
                for (int d = 0; d < 3; d++) {
                    int digit = rand.nextInt(10);
                    phone.append(digit);
                }
            }

            String insert = "INSERT INTO phone_numbers VALUES (" + i + ", '" + phone + "', '" + country + "', '" +
                    lastUsed + "')";

            try {
                exec.executeForInstance(new Parser(insert).parse());
            } catch (Exception e) {
                view.error(e.getMessage());
            }
        }
        long t1 = System.nanoTime();
        view.setText(true);
        view.infoF("Execution: %s", Timer.fmt(t1 - t0));
        exec.setTimer(ct);
    }

    /**
     * Генерирует таблицу {@code operators} с 5 случайными записями.
     *
     * @param exec исполнитель запросов
     * @param view вывод
     */
    private static void create_operators(QueryExecutor exec, ConsoleView view) {
        boolean ct = exec.getTimer();
        exec.setTimer(false);
        view.setText(false);
        long t0 = System.nanoTime();

        Random rand = new Random(42);
        String[] operators = {"MTS", "Beeline", "Megafon", "Tele2", "Yota"};

        try {
            exec.execute(new Parser("DROP TABLE operators;").parse());
        } catch (Exception e) {
            view.error(e.getMessage());
        }

        try {
            String create = "CREATE TABLE operators " +
                    "(id INT PRIMARY KEY, name STRING, cost_per_month DOUBLE, isActive BOOLEAN);";
            exec.executeForInstance(new Parser(create).parse());
        } catch (Exception e) {
            view.error(e.getMessage());
        }

        for (int i = 0; i < operators.length; i++) {
            double cost = Math.round((100.0 + rand.nextDouble() * 1400.0) * 100.0) / 100.0;
            boolean isActive = rand.nextInt(100) < 80;

            String insert = "INSERT INTO operators VALUES (" + i
                    + ", '" + operators[i] + "', " + cost + ", " + (isActive ? "TRUE" : "FALSE") + ");";

            try {
                exec.executeForInstance(new Parser(insert).parse());
            } catch (Exception e) {
                view.error(e.getMessage());
            }
        }

        long t1 = System.nanoTime();
        view.setText(true);
        view.infoF("Execution: %s", Timer.fmt(t1 - t0));
        exec.setTimer(ct);
    }
}