import java.net.ConnectException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        System.out.println("== Добро пожаловать, Агент 62349А ==");
        System.out.println("=====      Помощь - /help      =====");
        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Введите имя хоста:");
            String host = scanner.nextLine();
            switch (host) {
                case "/help":
                    System.out.println("/exit - выход");
                    return;
                case "/exit":
                    System.out.println("Выход из системы...");
                    System.out.println("Удачного дня!");
                    return;
            }
            System.out.println("Введите порт:");
            String port = scanner.nextLine();
            switch (port) {
                case "/help":
                    System.out.println("/exit - выход");
                    continue;
                case "/exit":
                    System.out.println("Выход из системы...");
                    System.out.println("Удачного дня!");
                    return;
            }
            Client client;
            try {
                client = new Client(host, port);
                client.runClient();
            } catch (ConnectException e) {
                System.out.println("сервер не доступен по данному адресу");
            } catch (Exception e) {
                System.out.println("шото пошло не так");
                e.printStackTrace();
            }
        }
    }

}
