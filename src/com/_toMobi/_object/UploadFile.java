package com._toMobi._object;

import java.io.Serializable;

/**
 * @author Mandela aka puumInc
 */
public class UploadFile implements Serializable {

    public static final long serialVersionUID = 1L;

    private String name;
    private String filePath;
    private double sourceSize;
    private double byteSent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public double getSourceSize() {
        return sourceSize;
    }

    public void setSourceSize(double sourceSize) {
        this.sourceSize = sourceSize;
    }

    public double getByteSent() {
        return byteSent;
    }

    public void setByteSent(double byteSent) {
        this.byteSent = byteSent;
    }
}
