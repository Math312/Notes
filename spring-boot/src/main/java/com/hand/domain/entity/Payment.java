package com.hand.domain.entity;

import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.*;

public class Payment {
    @Id
    @Column(name = "payment_id")
    private Short paymentId;

    @Column(name = "customer_id")
    private Short customerId;

    @Column(name = "staff_id")
    private Byte staffId;

    @Column(name = "rental_id")
    private Integer rentalId;

    private BigDecimal amount;

    @Column(name = "payment_date")
    private Date paymentDate;

    @Column(name = "last_update")
    private Date lastUpdate;

    /**
     * @return payment_id
     */
    public Short getPaymentId() {
        return paymentId;
    }

    /**
     * @param paymentId
     */
    public void setPaymentId(Short paymentId) {
        this.paymentId = paymentId;
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
     * @return amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * @param amount
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * @return payment_date
     */
    public Date getPaymentDate() {
        return paymentDate;
    }

    /**
     * @param paymentDate
     */
    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
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