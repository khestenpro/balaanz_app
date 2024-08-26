package com.bitsvalley.micro.domain;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Fru Chifen
 * 17.08.2022
 */

@Entity
@Table(name = "taskcomment")
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String createdBy;
    private Date createdDate;
    private String imageUrl;
    private String notes;

    @ManyToOne
    private Task task;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }
}
