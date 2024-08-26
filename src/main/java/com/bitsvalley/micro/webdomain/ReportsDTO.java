package com.bitsvalley.micro.webdomain;

import java.util.ArrayList;
import java.util.List;

public class ReportsDTO {

    long customerId;
    String customerName;
    List<ReportDTO> reportDTOs = new ArrayList<ReportDTO>();

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public List<ReportDTO> getReportDTOs() {
        return reportDTOs;
    }

    public void setReportDTOs(List<ReportDTO> reportDTOs) {
        this.reportDTOs = reportDTOs;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

}
