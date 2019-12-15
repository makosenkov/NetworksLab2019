package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class Server {
    private static final int PORT = 8080;
    private List<ClientHandler> clientList;

    public Server() {
        clientList = new LinkedList<>();
    }

    public void run() throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("Server Started");
        try {
            while (true) {
                Socket socket = server.accept();
                try {
                    clientList.add(new ClientHandler(socket, this));
                } catch (IOException e) {
                    socket.close();
                }
            }
        } finally {
            server.close();
        }
    }

    public List<ClientHandler> getClientList() {
        return clientList;
    }

    public void removeClient(ClientHandler session) {
        clientList.remove(session);
    }

    public static void main(String[] args) throws IOException {
        new Server().run();
    }
}