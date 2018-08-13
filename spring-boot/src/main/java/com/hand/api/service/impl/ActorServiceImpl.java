package com.hand.api.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.hand.api.service.ActorService;
import com.hand.domain.entity.Actor;
import com.hand.infra.mapper.ActorMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@ComponentScan(basePackages = {"com.hand.infra.mapper"})
@Transactional(rollbackFor = Exception.class)
public class ActorServiceImpl implements ActorService {

    @Autowired
    private ActorMapper actorDao;

    @Override
    public Actor selectById(Long id)
    {
        return actorDao.selectByPrimaryKey(id);
    }

    @Override
    public Actor insertInstance(Actor instance)
    {
        actorDao.insert(instance);
        return actorDao.selectByPrimaryKey(instance.getActorId());
    }

    @Override
    public void deleteById(Long id) {

        actorDao.deleteByPrimaryKey(id);

    }

    @Override
    public PageInfo<Actor> list(int page, int num) {

        PageHelper.startPage(0,10);
        List<Actor> list = actorDao.selectAll();
        PageInfo<Actor> pageInfo = new PageInfo<>(list);
        return pageInfo;
    }

    @Override
    public Actor updateActor(Actor actor) {
        actorDao.updateByPrimaryKey(actor);
        return actorDao.selectByPrimaryKey(actor.getActorId());
    }
}
