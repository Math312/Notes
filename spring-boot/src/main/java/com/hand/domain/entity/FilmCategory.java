package com.hand.domain.entity;

import java.util.Date;
import javax.persistence.*;

@Table(name = "film_category")
public class FilmCategory {
    @Id
    @Column(name = "film_id")
    private Short filmId;

    @Id
    @Column(name = "category_id")
    private Byte categoryId;

    @Column(name = "last_update")
    private Date lastUpdate;

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
     * @return category_id
     */
    public Byte getCategoryId() {
        return categoryId;
    }

    /**
     * @param categoryId
     */
    public void setCategoryId(Byte categoryId) {
        this.categoryId = categoryId;
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