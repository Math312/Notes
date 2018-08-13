package com.hand.domain.entity;

import java.util.Date;
import javax.persistence.*;

public class Rental {
    @Id
    @Column(name = "rental_id")
    private Integer rentalId;

    @Column(name = "rental_date")
    private Date rentalDate;

    @Column(name = "inventory_id")
    private Integer inventoryId;

    @Column(name = "customer_id")
    private Short customerId;

    @Column(name = "return_date")
    private Date returnDate;

    @Column(name = "staff_id")
    private Byte staffId;

    @Column(name = "last_update")
    private Date lastUpdate;

    /**
     * @return rental_id
     */
    public Integer getRentalId() {
        return rentalId;
    }

    /**
     * @param rentalId
     */
    public void setRentalId(Integer rentalId) {
        this.rentalId = rentalId;
    }

    /**
     * @return rental_date
     */
    public Date getRentalDate() {
        return rentalDate;
    }

    /**
     * @param rentalDate
     */
    public void setRentalDate(Date rentalDate) {
        this.rentalDate = rentalDate;
    }

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
     * @return customer_id
     */
    public Short getCustomerId() {
        return customerId;
    }

    /**
     * @param customerId
     */
    public void setCustomerId(Short customerId) {
        this.customerId = customerId;
    }

    /**
     * @return return_date
     */
    public Date getReturnDate() {
        return returnDate;
    }

    /**
     * @param returnDate
     */
    public void setReturnDate(Date returnDate) {
        this.returnDate = returnDate;
    }

    /**
     * @return staff_id
     */
    public Byte getStaffId() {
        return staffId;
    }

    /**
     * @param staffId
     */
    public void setStaffId(Byte staffId) {
        this.staffId = staffId;
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