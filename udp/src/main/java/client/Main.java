package client;

public class Main {

    public static void main(String[] args) throws Exception {
        String address = null;
        if (args[0] != null) {
            address = args[0];
        } else {
            System.out.println("Wrong arguments: write host address");
        }
        if (args[1] != null) {
            if (args[1].equals("get")) {
                String[] files = new String[args.length - 2];
                System.arraycopy(args, 2, files, 0, args.length - 2);
                TftpClient tFTPClient = new TftpClient(address, 69);
                tFTPClient.getFiles(files);
            } else if (args[1].equals("send")) {
                String[] files = new String[args.length - 2];
                System.arraycopy(args, 2, files, 0, args.length - 2);
                TftpClient tFTPClient = new TftpClient(address, 69);
                tFTPClient.sendFiles(files);
            }
        }

    }

}
