package com.example.taybudget.data.model;

import java.io.Serializable;
import java.util.Date;

public class Tag implements Serializable {
    private String user;
    private String tag;
    private Date created;
    private Date lastUsed;
    private int count;

    public Tag(String user, String tag, Date created, Date lastUsed, int count) {
        this.user = user;
        this.tag = tag;
        this.created = created;
        this.lastUsed = lastUsed;
        this.count = count;
    }

    public Tag() {
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
