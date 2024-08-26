package com.bitsvalley.micro.domain;

import javax.persistence.*;

/**
 * @author Fru Chifen
 * 22.02.2023
 */
@Entity
@Table(name = "warehouselocation")
public class WarehouseLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private long orgId;
    private String name;
    private String binNumber;
    private String description;
    private String parentID;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParentID() {
        return parentID;
    }

    public void setParentID(String parentID) {
        this.parentID = parentID;
    }

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    public String getBinNumber() {
        return binNumber;
    }

    public void setBinNumber(String binNumber) {
        this.binNumber = binNumber;
    }

    public long getId() {
        return id;
    }

}
