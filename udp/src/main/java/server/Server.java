package server;

import client.TftpDataPacket;
import client.TftpRequestPackage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Server implements Runnable {

    private static String TFTP_SERVER_ADDRESS;
    private static int TFTP_SERVER_PORT;

    private DatagramSocket datagramSocket = null;
    private InetAddress inetAddress = null;
    private DatagramPacket incomingDatagramPacket;
    private SocketAddress socketAddress = null;

    private static final byte OP_RRQ = 1;
    private static final byte OP_WRQ = 2;
    private static final byte OP_DATAPACKET = 3;
    private static final byte OP_ACK = 4;
    private static final byte OP_ERROR = 5;
    private static final int DATA_PACKET_SIZE = 516;
    private static final int DATA_PACKET_CONTENT_SIZE = 512;
    private static final int ACK_PACKET_SIZE = 4;

    public Server(String address, int port) throws SocketException {
        TFTP_SERVER_ADDRESS = address;
        TFTP_SERVER_PORT = port;
        datagramSocket = new DatagramSocket(null);
        socketAddress = new InetSocketAddress(address, port);
        datagramSocket.bind(socketAddress);
        datagramSocket.setReuseAddress(true);
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
                datagramSocket.getLocalPort()
        );
        datagramSocket.receive(incomingDatagramPacket);
        byte opCode = bufferByteArray[1];

        switch (opCode) {
            case OP_ERROR:
                System.out.println("Error package received");

            case OP_ACK:

                byte[] packetBlockNumber = {bufferByteArray[2], bufferByteArray[3]};

                if (packetBlockNumber[0] == 0 && packetBlockNumber[1] == 0) {
                    return true;
                }
        }
        return false;
    }

    private void sendFilePackets(String filename, SocketAddress address) throws IOException {
        List<TftpDataPacket> packets = getPacketListFromFile(filename);

        for (int i = 0; i < packets.size(); i++) {
            TftpDataPacket packet = packets.get(i);
            DatagramPacket sendPack = new DatagramPacket(packet.getPacket(), packet.getPacket().length, address);

            datagramSocket.send(sendPack);
            // Ответ от сервера
            byte[] bufferByteArray = new byte[ACK_PACKET_SIZE];
            incomingDatagramPacket = new DatagramPacket(
                    bufferByteArray,
                    bufferByteArray.length,
                    inetAddress,
                    datagramSocket.getLocalPort()
            );
            datagramSocket.receive(incomingDatagramPacket);
            int ackBlockNumber = Byte.toUnsignedInt(bufferByteArray[2]) + Byte.toUnsignedInt(bufferByteArray[3]);
            if (ackBlockNumber == packet.getBlockNumber()) {
                System.out.println("Подтверджена отправка пакета №: " + ackBlockNumber);
            } else {
                System.out.println("Подтвержден пакет " + ackBlockNumber + ", перепосылаю пакет номер " + packet.getBlockNumber());
                i--;
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
        byte[] requestByteArray = buildRequest(opcode, fileName, "octet");
        return new DatagramPacket(requestByteArray,
                requestByteArray.length, inetAddress, TFTP_SERVER_PORT);
    }

    public byte[] buildRequest(byte opCode, String fileName, String mode) {
        byte zeroByte = 0;
        int rrqByteLength = 2 + fileName.length() + 1 + mode.length() + 1;
        byte[] rrqByteArray = new byte[rrqByteLength];

        rrqByteArray[0] = zeroByte;
        rrqByteArray[1] = opCode;
        int position = 2;
        //пишем filename
        for (int i = 0; i < fileName.length(); i++) {
            rrqByteArray[position] = (byte) fileName.charAt(i);
            position++;
        }
        rrqByteArray[position] = zeroByte;
        position++;
        // пишем mode
        for (int i = 0; i < mode.length(); i++) {
            rrqByteArray[position] = (byte) mode.charAt(i);
            position++;
        }
        rrqByteArray[position] = zeroByte;
        return rrqByteArray;
    }

    private List<TftpDataPacket> getPacketListFromFile(String filename) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(filename));
        int numberOfPackets = fileBytes.length / DATA_PACKET_CONTENT_SIZE + 1; //обеспечиваем неполный последний пакет
        List<TftpDataPacket> packetList = new ArrayList<>();
        for (int i = 0; i < numberOfPackets - 1; i++)     //Последний пакет пока не создаем
        {
            byte[] data = new byte[DATA_PACKET_CONTENT_SIZE];
            System.arraycopy(fileBytes, i * 512, data, 0, data.length);
            TftpDataPacket toAdd = new TftpDataPacket(data, (i + 1));
            packetList.add(toAdd);
        }
        int startIOfLastBytes = DATA_PACKET_CONTENT_SIZE * (numberOfPackets - 1);
        int bytesLeft = fileBytes.length - startIOfLastBytes;
        byte[] lastPack = new byte[bytesLeft];
        for (int j = startIOfLastBytes; j < fileBytes.length; j++) {
            lastPack[j % DATA_PACKET_CONTENT_SIZE] = fileBytes[j];
        }
        TftpDataPacket last = new TftpDataPacket(lastPack, numberOfPackets);
        packetList.add(last);
        return packetList;
    }

    /*
     * Принимаем пакеты
     */
    private void receivePackage(String filename) throws IOException {
        do {
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
                    System.out.println("Error package received");

                case OP_DATAPACKET:

                    byte[] packetBlockNumber = {bufferByteArray[2], bufferByteArray[3]};

                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                         DataOutputStream dos = new DataOutputStream(byteArrayOutputStream)) {
                        dos.write(incomingDatagramPacket.getData(), 4, incomingDatagramPacket.getLength() - 4);
                        sendAcknowledgment(packetBlockNumber, incomingDatagramPacket.getAddress());
                        writeToFile(byteArrayOutputStream.toByteArray(), filename);
                    }
            }
        } while (!isLastPacket(incomingDatagramPacket));

    }

    private void writeToFile(byte[] bytes, String filename) {
        if (Paths.get(filename).toFile().exists()) {
            System.out.println(bytes.length);
            try {
                Files.write(Paths.get(filename), bytes, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.out.println("Не удалось дописать данные к файлу");
            }
        } else {
            try {
                Files.write(Paths.get(filename), bytes, StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                System.out.println("Не удалось создать файл");
            }
        }
    }

    private boolean isLastPacket(DatagramPacket packet) {
        return packet.getLength() < 512;
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
                System.out.println("server started");
                datagramSocket.receive(incomingDatagramPacket);
            } catch (IOException e) {
                System.out.println("something gone wrong");
            }
            byte opCode = bufferByteArray[1];

            switch (opCode) {
                case OP_ERROR:
                    System.out.println("Error package received");

                case OP_WRQ:

                    try {
                        StringBuilder builder = new StringBuilder();
                        int position = 2;
                        while (bufferByteArray[position] != 0) {
                            char letter = (char) bufferByteArray[position];
                            builder.append(letter);
                            if (letter == '/') {
                                builder.setLength(0);
                            }
                            position++;
                        }
                        String filename = builder.toString();
                        sendAcknowledgment(new byte[]{0, 0}, incomingDatagramPacket.getAddress());
                        System.out.println("принят запрос на запись");
                        receivePackage(filename);
                    } catch (IOException e) {
                        System.out.println("Не удалось отправить ACK на запись");
                    }

            }
        }
    }

    public static void main(String[] args) {
        Server server = null;
        try {
            server = new Server("192.168.0.38", 8080);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        server.run();
    }
}
