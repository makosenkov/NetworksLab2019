package client;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class ClientWriter extends Thread {
    private BufferedReader con_br;
    private PrintWriter sock_pw;

    public ClientWriter(PrintWriter sock_pw, BufferedReader con_br) {
        this.sock_pw = sock_pw;
        this.con_br = con_br;
    }

    @Override
    public void run() {
        String s;
        try {
            while (true) {
                System.out.print("> ");
                s = con_br.readLine();
                if (s.equals("/exit")) return;
                if (s != null)
                    sock_pw.println(s);
                else
                    break;
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}