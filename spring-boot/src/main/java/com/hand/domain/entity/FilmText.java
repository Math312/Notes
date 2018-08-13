package com.hand.domain.entity;

import javax.persistence.*;

@Table(name = "film_text")
public class FilmText {
    @Id
    @Column(name = "film_id")
    private Short filmId;

    private String title;

    private String description;

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
}