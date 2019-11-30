public class Main {

    public static void main(String[] args) throws Exception {
        String address = null;
        if (args[0] != null) {
            address = args[0];
        } else {
            System.out.println("Wrong arguments: write host address");
        }
        String[] files = new String[args.length - 1];
        System.arraycopy(args, 1, files, 0, args.length - 1);
        TftpClient tFTPClient = new TftpClient(address, 69);
        tFTPClient.getFiles(files);
    }

}
