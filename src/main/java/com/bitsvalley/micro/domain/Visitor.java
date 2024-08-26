package com.bitsvalley.micro.domain;

import com.bitsvalley.micro.utils.PriorityStatus;
import com.bitsvalley.micro.utils.TicketStatus;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Entity
@Table(name = "visitor")
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String createdBy;
    private Date createdDate;
    private String branchCode;
    private PriorityStatus priority;
    private String title;
    private String name;
    private String telephone;
    private String email;
    private String checkInTime;
    private String checkOutTime;
    private long orgId;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public PriorityStatus getPriority() {
        return priority;
    }

    public void setPriority(PriorityStatus priority) {
        this.priority = priority;
    }

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }
}
