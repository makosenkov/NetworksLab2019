package client;

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

}
