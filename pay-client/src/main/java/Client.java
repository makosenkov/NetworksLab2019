import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    private ExecutorService executor = Executors.newFixedThreadPool(10);
    private Socket socket;

    public Client(String host, String port) throws IOException {
        socket = new Socket(host, Integer.parseInt(port));
    }

    public void runClient() {
        executor.execute(new Receiver(socket));
        executor.execute(new Sender(socket));
    }
}
