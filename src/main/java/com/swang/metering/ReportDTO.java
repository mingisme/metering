package com.swang.metering;

import java.util.ArrayList;
import java.util.Date;

public class ReportDTO implements JSONSerdeCompatible{
    private Integer productId;
    private Integer suitId;
    private Date statisticTime;
    private ArrayList<InstanceDTO> reportList = new ArrayList();

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getSuitId() {
        return suitId;
    }

    public void setSuitId(Integer suitId) {
        this.suitId = suitId;
    }

    public Date getStatisticTime() {
        return statisticTime;
    }

    public void setStatisticTime(Date statisticTime) {
        this.statisticTime = statisticTime;
    }

    public ArrayList<InstanceDTO> getReportList() {
        return reportList;
    }

    public void setReportList(ArrayList<InstanceDTO> reportList) {
        this.reportList = reportList;
    }

    @Override
    public String toString() {
        return "ReportDTO{" +
                "productId=" + productId +
                ", statisticTime=" + statisticTime +
                ", reportList=" + reportList +
                '}';
    }
}
