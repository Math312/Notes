package com.hand.domain.entity;

import java.util.Date;
import javax.persistence.*;

public class Store {
    @Id
    @Column(name = "store_id")
    private Byte storeId;

    @Column(name = "manager_staff_id")
    private Byte managerStaffId;

    @Column(name = "address_id")
    private Short addressId;

    @Column(name = "last_update")
    private Date lastUpdate;

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
     * @return manager_staff_id
     */
    public Byte getManagerStaffId() {
        return managerStaffId;
    }

    /**
     * @param managerStaffId
     */
    public void setManagerStaffId(Byte managerStaffId) {
        this.managerStaffId = managerStaffId;
    }

    /**
     * @return address_id
     */
    public Short getAddressId() {
        return addressId;
    }

    /**
     * @param addressId
     */
    public void setAddressId(Short addressId) {
        this.addressId = addressId;
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