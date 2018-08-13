package com.hand.domain.entity;

import java.math.BigDecimal;
import javax.persistence.*;

@Table(name = "film_list")
public class FilmList {
    @Column(name = "FID")
    private Short fid;

    private String title;

    private String category;

    private BigDecimal price;

    private Short length;

    private String rating;

    private String description;

    private String actors;

    /**
     * @return FID
     */
    public Short getFid() {
        return fid;
    }

    /**
     * @param fid
     */
    public void setFid(Short fid) {
        this.fid = fid;
    }

    /**
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title
     */
    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

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
     * @return price
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * @param price
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * @return length
     */
    public Short getLength() {
        return length;
    }

    /**
     * @param length
     */
    public void setLength(Short length) {
        this.length = length;
    }

    /**
     * @return rating
     */
    public String getRating() {
        return rating;
    }

    /**
     * @param rating
     */
    public void setRating(String rating) {
        this.rating = rating == null ? null : rating.trim();
    }

    /**
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     */
    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    /**
     * @return actors
     */
    public String getActors() {
        return actors;
    }

    /**
     * @param actors
     */
    public void setActors(String actors) {
        this.actors = actors == null ? null : actors.trim();
    }
}