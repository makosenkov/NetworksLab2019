package client;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
    private static final int DATA_PACKET_CONTENT_SIZE = 512;
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

        for (TftpDataPacket packet : packets) {
            DatagramPacket sendPack = new DatagramPacket(packet.getPacket(), packet.getPacket().length, address);

            datagramSocket.send(sendPack); // Send package
            // Wait for ack from client
            byte[] bufferByteArray = new byte[ACK_PACKET_SIZE];
            incomingDatagramPacket = new DatagramPacket(
                    bufferByteArray,
                    bufferByteArray.length,
                    inetAddress,
                    datagramSocket.getLocalPort()
            );
            datagramSocket.receive(incomingDatagramPacket);
            int ackBlockNumber = Byte.toUnsignedInt(bufferByteArray[2]) + Byte.toUnsignedInt(bufferByteArray[3]);
            if (ackBlockNumber == packet.getBlockNumber()){
                System.out.println("Подтверджена отправка пакета №: "+ ackBlockNumber);
            }
        }

    }

    private void get(String fileName) throws IOException {
        datagramSocket = new DatagramSocket();
        // шлем пакет с запросом
        datagramSocket.send(createDatagramPacketForRequest(OP_RRQ, fileName));

        // получаем поток байт из принятых пакетов
        ByteArrayOutputStream byteOutOS = receivePackage();

        // записываем поток в файл
        writeToFile(byteOutOS, fileName);
    }

    private DatagramPacket createDatagramPacketForRequest(byte opcode, String fileName) throws IOException {
        inetAddress = InetAddress.getByName(TFTP_SERVER_ADDRESS);
        TftpRequestPackage requestPackage = new TftpRequestPackage(opcode, fileName, "octet");
        byte[] requestByteArray = requestPackage.build();
        return new DatagramPacket(requestByteArray,
                requestByteArray.length, inetAddress, TFTP_SERVER_PORT);
    }

    private List<TftpDataPacket> getPacketListFromFile(String filename) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(filename));
        int numberOfPackets = fileBytes.length / DATA_PACKET_CONTENT_SIZE + 1; //обеспечиваем неполный последний пакет
        List<TftpDataPacket> packetList = new ArrayList<>();
        for (int i = 0; i < numberOfPackets - 1; i++)     //Последний пакет пока не создаем
        {
            byte[] data = new byte[DATA_PACKET_CONTENT_SIZE];
            System.arraycopy(fileBytes, i * 512, data, 0, data.length);
            /* i + 1 = the block number.*/
            TftpDataPacket toAdd = new TftpDataPacket(data, (i + 1));
            packetList.add(toAdd);
            int startIOfLastBytes = DATA_PACKET_CONTENT_SIZE * (numberOfPackets-1);
            int bytesLeft = fileBytes.length - startIOfLastBytes;
            byte[] lastPack = new byte[bytesLeft];
            for (int j = startIOfLastBytes; j < fileBytes.length; j++)
            {
                lastPack[j % DATA_PACKET_CONTENT_SIZE] = fileBytes[j];
            }
            TftpDataPacket last = new TftpDataPacket(lastPack, numberOfPackets);
            packetList.add(last);
        }
        return packetList;
    }

    /*
     * Принимаем пакеты
     */
    private ByteArrayOutputStream receivePackage() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
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

                    DataOutputStream dos = new DataOutputStream(byteArrayOutputStream);
                    dos.write(incomingDatagramPacket.getData(), 4, incomingDatagramPacket.getLength() - 4);
                    sendAcknowledgment(packetBlockNumber, incomingDatagramPacket.getAddress());
            }
        } while ((isLastPacket(incomingDatagramPacket)));
        return byteArrayOutputStream;
    }

    private void writeToFile(ByteArrayOutputStream baos, String filename) throws IOException {
        try (OutputStream os = new FileOutputStream(filename)) {
            baos.writeTo(os);
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

}