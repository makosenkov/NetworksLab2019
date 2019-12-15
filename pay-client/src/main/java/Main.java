import client.Client;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try {
            if (args[0] != null && args[1] != null) {
                new Client(Integer.parseInt(args[0]), args[1]).run();
            } else {
                System.out.println("Wrong arguments");
            }
        } catch (Exception e) {
            System.out.println("Something gone wrong");
        }

    }

}
