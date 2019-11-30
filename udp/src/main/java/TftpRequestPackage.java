import java.nio.ByteBuffer;

public class TftpRequestPackage {

    private byte opCode;

    private String fileName;

    private String mode;

    public TftpRequestPackage(byte opCode, String fileName, String mode) {
        this.opCode = opCode;
        this.fileName = fileName;
        this.mode = mode;
    }

    public byte getOpCode() {
        return opCode;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMode() {
        return mode;
    }

    /*
     * Сборка запроса
     */
    public ByteBuffer build() {
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
        return ByteBuffer.wrap(rrqByteArray);
    }
}
