package com.bitsvalley.micro.webdomain;

import com.bitsvalley.micro.utils.BVMicroUtils;

public class SavingReportDTO {

    String descriptionValues;
    String paidValues;
    String dueValues;
    String sumTotalPaid;
    String sumTotalDue;

    public String getDescriptionValues() {
        return descriptionValues;
    }

    public void setDescriptionValues(String descriptionValues) {
        this.descriptionValues = descriptionValues;
    }

    public String getPaidValues() {
        return paidValues;
    }

    public void setPaidValues(String paidValues) {
        this.paidValues = paidValues;
    }

    public String getDueValues() {
        return dueValues;
    }

    public void setDueValues(String dueValues) {
        this.dueValues = dueValues;
    }

    public String getSumTotalPaid() {
        return sumTotalPaid;
    }

    public void setSumTotalPaid(String sumTotalPaid) {
        this.sumTotalPaid = sumTotalPaid;
    }

    public String getSumTotalDue() {
        return sumTotalDue;
    }

    public void setSumTotalDue(String sumTotalDue) {
        this.sumTotalDue = sumTotalDue;
    }

}
