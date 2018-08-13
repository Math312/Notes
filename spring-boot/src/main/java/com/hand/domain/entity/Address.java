package com.hand.domain.entity;

import java.util.Date;
import javax.persistence.*;

public class Address {
    @Id
    @Column(name = "address_id")
    private Short addressId;

    private String address;

    private String address2;

    private String district;

    @Column(name = "city_id")
    private Short cityId;

    @Column(name = "postal_code")
    private String postalCode;

    private String phone;

    @Column(name = "last_update")
    private Date lastUpdate;

    private byte[] location;

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
     * @return address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @param address
     */
    public void setAddress(String address) {
        this.address = address == null ? null : address.trim();
    }

    /**
     * @return address2
     */
    public String getAddress2() {
        return address2;
    }

    /**
     * @param address2
     */
    public void setAddress2(String address2) {
        this.address2 = address2 == null ? null : address2.trim();
    }

    /**
     * @return district
     */
    public String getDistrict() {
        return district;
    }

    /**
     * @param district
     */
    public void setDistrict(String district) {
        this.district = district == null ? null : district.trim();
    }

    /**
     * @return city_id
     */
    public Short getCityId() {
        return cityId;
    }

    /**
     * @param cityId
     */
    public void setCityId(Short cityId) {
        this.cityId = cityId;
    }

    /**
     * @return postal_code
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * @param postalCode
     */
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode == null ? null : postalCode.trim();
    }

    /**
     * @return phone
     */
    public String getPhone() {
        return phone;
    }

    /**
     * @param phone
     */
    public void setPhone(String phone) {
        this.phone = phone == null ? null : phone.trim();
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
     * @return location
     */
    public byte[] getLocation() {
        return location;
    }

    /**
     * @param location
     */
    public void setLocation(byte[] location) {
        this.location = location;
    }
}