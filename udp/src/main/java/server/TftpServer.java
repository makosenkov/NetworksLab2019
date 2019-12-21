package server;

import utils.TftpDataPacket;
import utils.TftpUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class TftpServer implements Runnable {

    private DatagramSocket datagramSocket;
    private InetAddress inetAddress = null;
    private DatagramPacket incomingDatagramPacket;
    private String dir;
    private static final byte OP_RRQ = 1;
    private static final byte OP_WRQ = 2;
    private static final byte OP_DATAPACKET = 3;
    private static final byte OP_ACK = 4;
    private static final byte OP_ERROR = 5;
    private static final int DATA_PACKET_SIZE = 516;
    private static final int ACK_PACKET_SIZE = 4;

    public TftpServer(String address, int port, String dir) throws SocketException {
        datagramSocket = new DatagramSocket(null);
        SocketAddress socketAddress = new InetSocketAddress(address, port);
        datagramSocket.bind(socketAddress);
        datagramSocket.setReuseAddress(true);
        this.dir = dir + "/";
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

    /*
     * Принимаем пакеты
     */
    private void receivePackage(String filename) throws IOException {
        Files.deleteIfExists(Paths.get(dir + filename));
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
                        TftpUtils.writeToFile(byteArrayOutputStream.toByteArray(), dir + filename);
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

    @Override
    public void run() {
        while (true) {
            byte[] bufferByteArray = new byte[DATA_PACKET_SIZE];
            incomingDatagramPacket = new DatagramPacket(
                bufferByteArray,
                bufferByteArray.length
            );
            try {
                System.out.println("Ожидание запроса");
                datagramSocket.receive(incomingDatagramPacket);
            } catch (IOException e) {
                System.out.println("Ошибка при приеме запроса");
            }
            byte opCode = bufferByteArray[1];

            switch (opCode) {
                case OP_ERROR:
                    System.out.println("Принят пакет с флагом ошибки");

                case OP_WRQ:

                    try {
                        String filename = TftpUtils.parseFilenameFromBytes(bufferByteArray);
                        sendAcknowledgment(new byte[] {0, 0}, incomingDatagramPacket.getAddress());
                        System.out.println("Принят запрос на запись");

                        receivePackage(filename);
                        continue;
                    } catch (IOException e) {
                        System.out.println("Не удалось отправить ACK на запись");
                    }
                case OP_RRQ:
                    try {
                        String filename = TftpUtils.parseFilenameFromBytes(bufferByteArray);
                        System.out.println("Принят запрос на чтение");
                        sendFilePackets(dir + filename, incomingDatagramPacket.getSocketAddress());
                    } catch (IOException e) {
                        System.out.println("Не удалось отправить ACK на чтение");
                    }
            }
        }
    }


    public static void main(String[] args) {
        String address = null;
        String port = null;
        String dir; //абочая директория
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
            dir = args[2];
        } else {
            dir = "/home";
        }

        TftpServer server = null;
        try {
            server = new TftpServer(address, Integer.parseInt(Objects.requireNonNull(port)), dir);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (server != null) {
            server.run();
        } else {
            System.out.println("не удалось запустить сервер");
        }
    }
}
