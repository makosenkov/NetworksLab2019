import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Set;

public class TftpClient {

    private static String TFTP_SERVER_ADDRESS;
    private static int TFTP_SERVER_PORT;

    private Selector selector;
    private SocketAddress socketAddress;
    private int fileCount;

    private static final byte OP_RRQ = 1;
    private static final byte OP_DATAPACKET = 3;
    private static final byte OP_ACK = 4;
    private static final byte OP_ERROR = 5;
    private static final int PACKET_SIZE = 512;

    public TftpClient(String address, int port) {
        TFTP_SERVER_ADDRESS = address;
        TFTP_SERVER_PORT = port;
    }

    public void getFiles(String[] files) throws IOException {
        registerAndRequest(files);
        readFiles();
    }

    /*
     * Создаем каналы и отправляем запрос по каждому файлу
     */
    private void registerAndRequest(String[] files) throws IOException {
        fileCount = files.length;
        selector = Selector.open();
        socketAddress = new InetSocketAddress(TFTP_SERVER_ADDRESS, TFTP_SERVER_PORT);
        for (String fileName : files) {
            DatagramChannel dc = DatagramChannel.open();
            dc.configureBlocking(false);
            SelectionKey selectionKey = dc.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(fileName);
            sendRequest(fileName, dc);
        }
    }

    /*
     * Отправляем запрос
     */
    private void sendRequest(String fileName, DatagramChannel dChannel)
            throws IOException {
        String mode = "octet";
        TftpRequestPackage tftpRequestPackage = new TftpRequestPackage(OP_RRQ, fileName, mode);
        ByteBuffer rrqByteBuffer = tftpRequestPackage.build();
        System.out.println("Sending Request to TFTP Server.");
        dChannel.send(rrqByteBuffer, socketAddress);
        System.out.println("Request Sent Success");
    }

    private void readFiles() throws IOException {
        int counter = 0;
        while (counter < fileCount) {
            counter++;
            int readyChannels = selector.select();
            if (readyChannels == 0) {
                continue;
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if (key.isAcceptable()) {
                    System.out.println("connection accepted: " + key.channel());
                } else if (key.isConnectable()) {
                    System.out.println("connection established: " + key.channel());
                } else if (key.isReadable()) {
                    System.out.println("Channel Readable: " + key.channel());
                    receivePackage((DatagramChannel) key.channel(), (String) key.attachment());
                    System.out.println("File received.");
                } else if (key.isWritable()) {
                    System.out.println("writable: " + key.channel());
                }
                keyIterator.remove();
            }
        }
    }

    /*
     * Принимаем файл
     */
    private void receivePackage(DatagramChannel dc, String fileName)
            throws IOException {
        ByteBuffer dst;
        do {
            dst = ByteBuffer.allocateDirect(PACKET_SIZE);
            SocketAddress remoteSocketAddress = dc.receive(dst);

            byte opCode = dst.get(1);

            switch (opCode) {
                case OP_ERROR:
                    System.out.println("Error package received");

                case OP_DATAPACKET:

                    byte[] packetBlockNumber = {dst.get(2), dst.get(3)};

                    readPackageData(dst, fileName);
                    sendAcknowledgment(packetBlockNumber, remoteSocketAddress, dc);
            }
        } while ((isLastPacket(dst)));
    }

    /*
     * Читаем содержимое пакета
     */
    private void readPackageData(ByteBuffer dst, String fileName)
            throws IOException {
        byte[] fileContent = new byte[PACKET_SIZE];
        dst.flip();

        int m = 0, counter = 0;
        while (dst.hasRemaining()) {
            if (counter > 3) {
                fileContent[m] = dst.get();
                m++;
            } else {
                dst.get();
            }
            counter++;
        }

        Path filePath = Paths.get(fileName);

        byte[] toWrite = new byte[m];
        System.arraycopy(fileContent, 0, toWrite, 0, m);

        writeToFile(filePath, toWrite);

    }

    private void writeToFile(Path filePath, byte[] toWrite) throws IOException {
        if (Files.exists(filePath)) {
            Files.write(filePath, toWrite, StandardOpenOption.APPEND);
        } else {
            Files.write(filePath, toWrite, StandardOpenOption.CREATE);
        }
    }

    private boolean isLastPacket(ByteBuffer buffer) {
        return buffer.limit() < 512;
    }

    /*
     * Отправляем ACK после прочтения
     */
    private void sendAcknowledgment(byte[] blockNumber,
                                    SocketAddress socketAddress, DatagramChannel dc) throws IOException {
        byte[] ACK = {0, OP_ACK, blockNumber[0], blockNumber[1]};
        dc.send(ByteBuffer.wrap(ACK), socketAddress);
    }

}