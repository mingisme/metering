package com.swang.metering;

public class InstanceDTO {
    private String ouId;
    private String instanceId;
    private String propertyName;
    private Double usage;
    private Double quota;

    public String getOuId() {
        return ouId;
    }

    public void setOuId(String ouId) {
        this.ouId = ouId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Double getUsage() {
        return usage;
    }

    public void setUsage(Double usage) {
        this.usage = usage;
    }

    public Double getQuota() {
        return quota;
    }

    public void setQuota(Double quota) {
        this.quota = quota;
    }
}
