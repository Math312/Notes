package com.hand.domain.entity;

import java.util.Date;
import javax.persistence.*;

public class Staff {
    @Id
    @Column(name = "staff_id")
    private Byte staffId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "address_id")
    private Short addressId;

    private String email;

    @Column(name = "store_id")
    private Byte storeId;

    private Boolean active;

    private String username;

    private String password;

    @Column(name = "last_update")
    private Date lastUpdate;

    private byte[] picture;

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
     * @return first_name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * @param firstName
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName == null ? null : firstName.trim();
    }

    /**
     * @return last_name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * @param lastName
     */
    public void setLastName(String lastName) {
        this.lastName = lastName == null ? null : lastName.trim();
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
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email
     */
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
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
     * @return active
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * @param active
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username
     */
    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    /**
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password
     */
    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
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

    /**
     * @return picture
     */
    public byte[] getPicture() {
        return picture;
    }

    /**
     * @param picture
     */
    public void setPicture(byte[] picture) {
        this.picture = picture;
    }
}