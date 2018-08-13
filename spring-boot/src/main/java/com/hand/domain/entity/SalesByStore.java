package com.hand.domain.entity;

import java.math.BigDecimal;
import javax.persistence.*;

@Table(name = "sales_by_store")
public class SalesByStore {
    private String store;

    private String manager;

    @Column(name = "total_sales")
    private BigDecimal totalSales;

    /**
     * @return store
     */
    public String getStore() {
        return store;
    }

    /**
     * @param store
     */
    public void setStore(String store) {
        this.store = store == null ? null : store.trim();
    }

    /**
     * @return manager
     */
    public String getManager() {
        return manager;
    }

    /**
     * @param manager
     */
    public void setManager(String manager) {
        this.manager = manager == null ? null : manager.trim();
    }

    /**
     * @return total_sales
     */
    public BigDecimal getTotalSales() {
        return totalSales;
    }

    /**
     * @param totalSales
     */
    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }
}