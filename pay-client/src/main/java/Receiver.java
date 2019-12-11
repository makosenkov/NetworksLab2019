import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Receiver implements Runnable {

    private Socket socket;

    public Receiver(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String received;
        while (true) {
            try(BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                received = inFromServer.readLine();
                System.out.println(received);
            } catch (IOException e) {
                System.out.println("Ошибка ввода-вывода при приеме сообщения");
            }
        }
    }

}
