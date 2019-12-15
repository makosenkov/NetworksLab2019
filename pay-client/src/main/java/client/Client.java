package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private int port;
    private String host;
    private static BufferedReader con_br = new BufferedReader(new InputStreamReader(System.in));
    private PrintWriter sock_pw;
    private BufferedReader sock_br;

    public Client(int port, String host) {
        this.host = host;
        this.port = port;
    }

    public void run() throws IOException {
        Socket sock = new Socket(host, port);
        sock_br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        sock_pw = new PrintWriter(sock.getOutputStream(), true);
        System.out.println("Connection established");
        String s;

        enter();

        Thread clientWriter = new ClientWriter(sock_pw, con_br);
        clientWriter.start();

        while ((s = sock_br.readLine()) != null) {
            if (s.equals("exit")) return;
            System.out.println("\r" + s);
            System.out.print("> ");
        }
        con_br.close();
        clientWriter.interrupt();
        sock.close();
    }

    private void enter() throws IOException {
        String s;
        while (true) {
            System.out.println("Sign in (1) or sign up(2)?");
            s = con_br.readLine();
            switch (s) {
                case "1":
                    System.out.print("Login: ");
                    s = con_br.readLine();
                    sock_pw.println("signin " + s);
                    s = sock_br.readLine();
                    System.out.println(s);
                    if(s.equals("Enter complete")) {
                        System.out.println("Write \"help\" for help");
                        return;
                    }
                    else break;
                case "2":
                    System.out.print("Create login: ");
                    s = con_br.readLine();
                    sock_pw.println("signup " + s);
                    s = sock_br.readLine();
                    System.out.println(s);
                    if(s.equals("Enter complete")) {
                        System.out.println("Write \"help\" for help");
                        return;
                    }
                    else break;
                default:
                    break;
            }
        }
    }

}