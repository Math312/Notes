package com.hand.domain.entity;

import javax.persistence.*;

@Table(name = "actor_info")
public class ActorInfo {
    @Column(name = "actor_id")
    private Short actorId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "film_info")
    private String filmInfo;

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
     * @return film_info
     */
    public String getFilmInfo() {
        return filmInfo;
    }

    /**
     * @param filmInfo
     */
    public void setFilmInfo(String filmInfo) {
        this.filmInfo = filmInfo == null ? null : filmInfo.trim();
    }
}