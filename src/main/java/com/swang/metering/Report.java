package com.swang.metering;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Report implements JSONSerdeCompatible{

    private String orgId;
    private String object;

    public Long timestamp;
    private long number;
    private long bytes;

    public Report() {
    }

    public Report(String orgId, String object, Long timestamp, long number, long bytes) {
        this.orgId = orgId;
        this.object = object;
        this.timestamp = timestamp;
        this.number = number;
        this.bytes = bytes;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    @JsonIgnore
    public String getReportKey() {
        return orgId + "__" + object;
    }

    @Override
    public String toString() {
        return "Report{" +
                "orgId='" + orgId + '\'' +
                ", object='" + object + '\'' +
                ", timestamp=" + timestamp +
                ", number=" + number +
                ", bytes=" + bytes +
                '}';
    }
}
