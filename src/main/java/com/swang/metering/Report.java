package com.swang.metering;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Report implements JSONSerdeCompatible {

    private String orgId;
    private String project;

    public Long timestamp;
    private long recordNumber;
    private long bytesNumber;

    public Report() {
    }

    public Report(String orgId, String project, Long timestamp, long recordNumber, long bytesNumber) {
        this.orgId = orgId;
        this.project = project;
        this.timestamp = timestamp;
        this.recordNumber = recordNumber;
        this.bytesNumber = bytesNumber;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public long getRecordNumber() {
        return recordNumber;
    }

    public void setRecordNumber(long recordNumber) {
        this.recordNumber = recordNumber;
    }

    public long getBytesNumber() {
        return bytesNumber;
    }

    public void setBytesNumber(long bytesNumber) {
        this.bytesNumber = bytesNumber;
    }

    @JsonIgnore
    public String getReportKey() {
        return orgId + "__" + project;
    }

    @Override
    public String toString() {
        return "Report{" +
                "orgId='" + orgId + '\'' +
                ", project='" + project + '\'' +
                ", timestamp=" + timestamp +
                ", recordNumber=" + recordNumber +
                ", bytesNumber=" + bytesNumber +
                '}';
    }
}
