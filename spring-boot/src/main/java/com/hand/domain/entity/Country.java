package com.hand.domain.entity;

import java.util.Date;
import javax.persistence.*;

public class Country {
    @Id
    @Column(name = "country_id")
    private Short countryId;

    private String country;

    @Column(name = "last_update")
    private Date lastUpdate;

    /**
     * @return country_id
     */
    public Short getCountryId() {
        return countryId;
    }

    /**
     * @param countryId
     */
    public void setCountryId(Short countryId) {
        this.countryId = countryId;
    }

    /**
     * @return country
     */
    public String getCountry() {
        return country;
    }

    /**
     * @param country
     */
    public void setCountry(String country) {
        this.country = country == null ? null : country.trim();
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