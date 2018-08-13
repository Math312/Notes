package com.hand.domain.entity;

import java.util.Date;
import javax.persistence.*;

@Table(name = "film_actor")
public class FilmActor {
    @Id
    @Column(name = "actor_id")
    private Short actorId;

    @Id
    @Column(name = "film_id")
    private Short filmId;

    @Column(name = "last_update")
    private Date lastUpdate;

    /**
     * @return actor_id
     */
    public Short getActorId() {
        return actorId;
    }

    /**
     * @param actorId
     */
    public void setActorId(Short actorId) {
        this.actorId = actorId;
    }

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