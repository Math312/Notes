package com.hand.domain.entity;

import java.util.Date;
import javax.persistence.*;

public class Inventory {
    @Id
    @Column(name = "inventory_id")
    private Integer inventoryId;

    @Column(name = "film_id")
    private Short filmId;

    @Column(name = "store_id")
    private Byte storeId;

    @Column(name = "last_update")
    private Date lastUpdate;

    /**
     * @return inventory_id
     */
    public Integer getInventoryId() {
        return inventoryId;
    }

    /**
     * @param inventoryId
     */
    public void setInventoryId(Integer inventoryId) {
        this.inventoryId = inventoryId;
    }

    /**
     * @return film_id
     */
    public Short getFilmId() {
        return filmId;
    }

    /**
     * @param filmId
     */
    public void setFilmId(Short filmId) {
        this.filmId = filmId;
    }

    /**
     * @return store_id
     */
    public Byte getStoreId() {
        return storeId;
    }

    /**
     * @param storeId
     */
    public void setStoreId(Byte storeId) {
        this.storeId = storeId;
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