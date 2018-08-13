package com.hand.domain.entity;

import java.math.BigDecimal;
import javax.persistence.*;

@Table(name = "sales_by_film_category")
public class SalesByFilmCategory {
    private String category;

    @Column(name = "total_sales")
    private BigDecimal totalSales;

    /**
     * @return category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @param category
     */
    public void setCategory(String category) {
        this.category = category == null ? null : category.trim();
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