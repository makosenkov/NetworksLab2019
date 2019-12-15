import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


public class ClientHandler {
    private static final String WRONG_FORMAT = "Wrong format";
    private static final String COMPLETE = "Complete";
    private static final String WRONG_ID = "Wrong currency id";
    private static final String ERR_SIGN = "Wrong login";
    private Socket socket;
    private Server server;
    private BufferedReader in;
    private BufferedWriter out;
    private boolean connect;
    private String login;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        connect = true;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        run();
    }


    public void run() {
        new Thread(() -> {
            while (connect) {
                try {
                    String msg = in.readLine();
                    if (msg == null) {
                        disconnect();
                        return;
                    }
                    if (login != null) System.out.println("from " + login + ": " + msg);
                    msgHandler(msg);
                } catch (IOException e) {
                    this.downService();
                }
            }

        }).start();
    }

    private void send(String msg) {
        try {
            System.out.println("to " + login + ": " + msg);
            out.write(msg + "\n");
            out.flush();
        } catch (IOException ignored) {
        }
    }

    private void msgHandler(String msg) throws IOException, NumberFormatException {
        String[] strings = msg.split("\\s+");
        switch (strings[0]) {
            case "signin":
                signin(strings[1]);
                break;
            case "signup":
                signup(strings[1]);
                break;
            case "list":
                send(Utilities.getListRates());
                break;
            case "add":
                if (strings.length != 2) {
                    send(WRONG_FORMAT);
                    break;
                }
                Utilities.addNewCurrency(Integer.parseInt(strings[1]));
                send(COMPLETE);
                break;
            case "addRate":
                if (strings.length != 3) {
                    send(WRONG_FORMAT);
                    break;
                }
                if(Utilities.addRate(Integer.parseInt(strings[1]), Double.parseDouble(strings[2]))) send(COMPLETE);
                else send(WRONG_ID);
                break;
            case "delete":
                if (strings.length != 2) {
                    send(WRONG_FORMAT);
                    break;
                }
                if(Utilities.deleteCurrency(Integer.parseInt(strings[1]))) send(COMPLETE);
                else send(WRONG_ID);
                break;
            case "help":
                send("list - get list of currencies rates\n" +
                        "add <id> - add new currency(id)\n" +
                        "delete <id> - delete currency(id)\n" +
                        "addRate <id> <r> - add new rate(r) to currency(id)\n" +
                        "/exit - to exit");
            case "/exit":
                send("exit");
                disconnect();
                break;
            default:
                send("Unknown command");
        }
    }

    private void signin(String login) throws IOException {
        if (!Utilities.checkLogin(login)) {
            send(ERR_SIGN);
        } else {
            this.login = login;
            System.out.println(login + " is connected");
            send("Enter complete");
        }
    }

    private void signup(String login) throws IOException {
        if (Utilities.checkLogin(login)) send("This login is already used");
        else {
            this.login = login;
            Utilities.addNewClient(login);
            System.out.println(login + " is connected");
            send("Enter complete");
        }
    }

    private void downService() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
                for (ClientHandler vr : server.getClientList()) {
                    if (vr.equals(this))
                        server.removeClient(this);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void disconnect() throws IOException {
        socket.close();
        server.removeClient(this);
        connect = false;
    }
}