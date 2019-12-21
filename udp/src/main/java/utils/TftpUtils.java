package utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class TftpUtils {
    private static final int DATA_PACKET_SIZE = 516;
    private static final int DATA_PACKET_CONTENT_SIZE = 512;

    public static List<TftpDataPacket> getPacketListFromFile(String filename) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(filename));
        List<TftpDataPacket> packetList = new ArrayList<>();
        int offset = 0;
        int blockNumber = 1 ;
        while (offset < fileBytes.length) {
            byte[] packet;
            int j = 0;
            if (offset + DATA_PACKET_CONTENT_SIZE >= fileBytes.length) {
                packet = new byte[fileBytes.length - offset];
                for (int i = offset; i < fileBytes.length; i++) {
                    packet[j] = fileBytes[i];
                    j++;
                }
            } else {
                packet = new byte[DATA_PACKET_CONTENT_SIZE];
                for (int i = offset; i < offset + DATA_PACKET_CONTENT_SIZE; i++) {
                    packet[j] = fileBytes[i];
                    j++;
                }
            }
            TftpDataPacket tftpDataPacket = new TftpDataPacket(packet, blockNumber);
            packetList.add(tftpDataPacket);
            offset += DATA_PACKET_CONTENT_SIZE;
            blockNumber++;
        }
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
        int length =  packet.getData().length;
        return length >= DATA_PACKET_SIZE;
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
