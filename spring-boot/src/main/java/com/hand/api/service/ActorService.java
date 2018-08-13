package com.hand.api.service;

import com.hand.domain.entity.Actor;

public interface ActorService extends BasicService<Actor> {

    public Actor updateActor(Actor actor);
}
