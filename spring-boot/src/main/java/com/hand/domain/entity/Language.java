package com.hand.domain.entity;

import java.util.Date;
import javax.persistence.*;

public class Language {
    @Id
    @Column(name = "language_id")
    private Byte languageId;

    private String name;

    @Column(name = "last_update")
    private Date lastUpdate;

    /**
     * @return language_id
     */
    public Byte getLanguageId() {
        return languageId;
    }

    /**
     * @param languageId
     */
    public void setLanguageId(Byte languageId) {
        this.languageId = languageId;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * @return last_update
     */
    public Date getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @param lastUpdate
     */
    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}