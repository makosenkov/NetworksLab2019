package utils;

public class TftpDataPacket {

    private int blockNumber;
    private byte[] packet;

    public TftpDataPacket(byte[] data, int blockNumber)
    {
        if (data.length > 4098)
        {
            throw new IllegalArgumentException("Длина пакета не может превышать 4098 байт");
        }
        this.blockNumber = blockNumber;
        packet = new byte[data.length + 4];
        packet[0] = 0;
        packet[1] = 3;   //opcode
        packet[2] = (byte) (blockNumber / 256);
        packet[3] = (byte) (blockNumber % 256);
        System.arraycopy(data, 0, packet, 4, packet.length - 4);
    }

    public int getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(int blockNumber) {
        this.blockNumber = blockNumber;
    }

    public byte[] getPacket() {
        return packet;
    }

    public void setPacket(byte[] packet) {
        this.packet = packet;
    }
}
