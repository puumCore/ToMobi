package com._toMobi._object;

import java.io.Serializable;
import java.util.List;

/**
 * @author Mandela aka puumInc
 * @version 1.1.2
 */
public class Job implements Serializable {

    public static final long serialVersionUID = 1L;

    private String jobName;
    private String filePath;
    private double sourceSize;
    private double byteSent;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
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
