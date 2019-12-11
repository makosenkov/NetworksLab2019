import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Sender implements Runnable {

    private Socket socket;

    public Sender(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        String command;
        while (true) {
            System.out.print(">: ");
            command = scanner.nextLine();

            try (DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream())){
                if (command.equals("/exit")) {
                    try {
                        socket.close();
                        outToServer.writeBytes(command);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Выход из системы...");
                    System.out.println("Удачного дня!");
                    return;
                }
                outToServer.writeBytes(command);
            } catch (IOException e) {
                System.out.println("Ошибка ввода-вывода при отправке сообщения");
            }
        }
    }
}
