package utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class TftpUtils {
    private static final int DATA_PACKET_CONTENT_SIZE = 2048;

    public static List<TftpDataPacket> getPacketListFromFile(String filename) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(filename));
        int numberOfPackets = fileBytes.length / DATA_PACKET_CONTENT_SIZE + 1; //обеспечиваем неполный последний пакет
        List<TftpDataPacket> packetList = new ArrayList<>();
        for (int i = 0; i < numberOfPackets - 1; i++)     //Последний пакет пока не создаем
        {
            byte[] data = new byte[DATA_PACKET_CONTENT_SIZE];
            System.arraycopy(fileBytes, i * DATA_PACKET_CONTENT_SIZE, data, 0, data.length);
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

    public static void writeToFile(byte[] bytes, String filename) {
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

    public static boolean isNotLastPacket(DatagramPacket packet) {
        return packet.getLength() >= DATA_PACKET_CONTENT_SIZE;
    }

    public static String parseFilenameFromBytes(byte[] bufferByteArray) {
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
        return builder.toString();
    }

    public static byte[] buildRequest(byte opCode, String fileName, String mode) {
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

}
