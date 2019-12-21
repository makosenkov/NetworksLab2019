package client;

import utils.TftpDataPacket;
import utils.TftpUtils;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class TftpClient {

    private static String TFTP_SERVER_ADDRESS;
    private static int TFTP_SERVER_PORT;

    private DatagramSocket datagramSocket = null;
    private InetAddress inetAddress = null;
    private DatagramPacket incomingDatagramPacket;

    private static final byte OP_RRQ = 1;
    private static final byte OP_WRQ = 2;
    private static final byte OP_DATAPACKET = 3;
    private static final byte OP_ACK = 4;
    private static final byte OP_ERROR = 5;
    private static final int DATA_PACKET_SIZE = 516;
    private static final int ACK_PACKET_SIZE = 4;

    public TftpClient(String address, int port) {
        TFTP_SERVER_ADDRESS = address;
        TFTP_SERVER_PORT = port;
    }

    public void getFiles(String[] files) throws IOException {
        for (String file : files) {
            get(file);
        }
    }

    public void sendFiles(String[] files) throws IOException {
        for (String file : files) {
            send(file);
        }
    }

    private void send(String fileName) throws IOException {
        datagramSocket = new DatagramSocket();
        datagramSocket.send(createDatagramPacketForRequest(OP_WRQ, fileName));

        if (receiveAckToWrite()) {
            sendFilePackets(fileName, incomingDatagramPacket.getSocketAddress());
        }
    }

    private boolean receiveAckToWrite() throws IOException {
        byte[] bufferByteArray = new byte[ACK_PACKET_SIZE];
        incomingDatagramPacket = new DatagramPacket(
            bufferByteArray,
            bufferByteArray.length,
            inetAddress,
            TFTP_SERVER_PORT
        );
        datagramSocket.receive(incomingDatagramPacket);
        byte opCode = bufferByteArray[1];

        switch (opCode) {
            case OP_ERROR:
                System.out.println("Принят пакет с кодом ошибки");

            case OP_ACK:

                byte[] packetBlockNumber = {bufferByteArray[2], bufferByteArray[3]};

                if (packetBlockNumber[0] == 0 && packetBlockNumber[1] == 0) {
                    return true;
                }
        }
        return false;
    }

    private void sendFilePackets(String filename, SocketAddress address) throws IOException {
        List<TftpDataPacket> packets = TftpUtils.getPacketListFromFile(filename);
        datagramSocket.setSoTimeout(50000);
        sendPackets:
        for (TftpDataPacket tftpDataPacket : packets) {
            sending:
            while (true) {
                try {
                    DatagramPacket sendPack = new DatagramPacket(tftpDataPacket.getPacket(), tftpDataPacket.getPacket().length, address);

                    Thread th = new Thread(() -> {
                        for (int i = 0; i < 25; i++) {
                            try {
                                if (i % 5 == 0) {
                                    try {
                                        datagramSocket.send(sendPack);
                                        System.out.println("отправлен пакет " + tftpDataPacket.getBlockNumber());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                Thread.sleep(1000);
                                if (Thread.interrupted()) {
                                    break;
                                }
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    });

                    th.start();
                    while (true) {
                        byte[] bufferByteArray = new byte[ACK_PACKET_SIZE];
                        incomingDatagramPacket = new DatagramPacket(
                            bufferByteArray,
                            bufferByteArray.length,
                            address
                        );
                        datagramSocket.receive(incomingDatagramPacket);
                        int ackBlockNumber = Byte.toUnsignedInt(bufferByteArray[2]) * 256 + Byte.toUnsignedInt(bufferByteArray[3]);
                        if (ackBlockNumber == tftpDataPacket.getBlockNumber()) {
                            System.out.println("Подтверджена отправка пакета №: " + ackBlockNumber);
                            th.interrupt();
                            break sending;
                        }
                    }
                    // Ответ от клиента
                } catch (SocketTimeoutException e) {
                    System.out.println("Timeout fail");
                    break sendPackets;
                }
            }
        }
    }

    private void get(String fileName) throws IOException {
        datagramSocket = new DatagramSocket();
        // шлем пакет с запросом
        datagramSocket.send(createDatagramPacketForRequest(OP_RRQ, fileName));

        // получаем поток байт из принятых пакетов
        receivePackage(fileName);

    }

    private DatagramPacket createDatagramPacketForRequest(byte opcode, String fileName) throws IOException {
        inetAddress = InetAddress.getByName(TFTP_SERVER_ADDRESS);
        byte[] requestByteArray = TftpUtils.buildRequest(opcode, fileName, "octet");
        return new DatagramPacket(requestByteArray,
            requestByteArray.length, inetAddress, TFTP_SERVER_PORT);
    }

    /*
     * Принимаем пакеты
     */
    private void receivePackage(String filename) throws IOException {
        Files.deleteIfExists(Paths.get(filename));
        loop:
        while (true) {
            byte[] bufferByteArray = new byte[DATA_PACKET_SIZE];
            incomingDatagramPacket = new DatagramPacket(
                bufferByteArray,
                bufferByteArray.length,
                inetAddress,
                datagramSocket.getLocalPort()
            );
            datagramSocket.receive(incomingDatagramPacket);
            byte opCode = bufferByteArray[1];

            switch (opCode) {
                case OP_ERROR:
                    System.out.println("Принят пакет с кодом ошибки");

                case OP_DATAPACKET:

                    byte[] packetBlockNumber = {bufferByteArray[2], bufferByteArray[3]};
                    if (incomingDatagramPacket.getLength() < 512) {
                        System.out.println("last");
                    }
                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                         DataOutputStream dos = new DataOutputStream(byteArrayOutputStream)) {
                        dos.write(incomingDatagramPacket.getData(), 4, incomingDatagramPacket.getLength() - 4);
                        sendAcknowledgment(packetBlockNumber, incomingDatagramPacket.getAddress());
                        TftpUtils.writeToFile(byteArrayOutputStream.toByteArray(), filename);
                        if (byteArrayOutputStream.toByteArray().length < 512) {
                            break loop;
                        }
                    }
            }
        }
    }

    /*
     * Отправляем ACK после прочтения
     */
    private void sendAcknowledgment(byte[] blockNumber, InetAddress inetAddress) throws IOException {
        byte[] ACK = {0, OP_ACK, blockNumber[0], blockNumber[1]};
        DatagramPacket ack = new DatagramPacket(ACK, ACK.length, inetAddress,
            incomingDatagramPacket.getPort());
        datagramSocket.send(ack);
    }

    public static void main(String[] args) throws Exception {
        String address = null;
        String port = null;
        if (args[0] != null) {
            address = args[0];
        } else {
            System.out.println("Wrong arguments: host address");
        }
        if (args[1] != null) {
            port = args[1];
        } else {
            System.out.println("Wrong arguments: port");
        }
        if (args[2] != null) {

            if (args[2].equals("get")) {
                String[] files = new String[args.length - 3];
                System.arraycopy(args, 3, files, 0, args.length - 3);
                TftpClient tFTPClient = new TftpClient(address, Integer.parseInt(Objects.requireNonNull(port)));
                tFTPClient.getFiles(files);
                System.out.println("received");
            } else if (args[2].equals("send")) {
                String[] files = new String[args.length - 3];
                System.arraycopy(args, 3, files, 0, args.length - 3);
                TftpClient tFTPClient = new TftpClient(address, Integer.parseInt(Objects.requireNonNull(port)));
                tFTPClient.sendFiles(files);
            }
        }

    }

}
