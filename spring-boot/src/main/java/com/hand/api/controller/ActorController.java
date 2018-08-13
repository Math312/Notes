//package com.hand.api.controller;
//
//import com.hand.api.service.ActorService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.*;
//
//import javax.validation.Valid;
//
//@Controller
//@RequestMapping(value = "/v1/actor")
//public class ActorController
//{
//    @Autowired
//    private ActorService actorService;
//
////    @ModelAttribute
////    public void queryById2(@PathVariable("id") Long id,Map<String,Object> data)
////    {
////        Actor actor = actorService.selectById(id);
////        data.put("actor",actor);
////    }
//
//    @RequestMapping(value = "/actors/{id}",method = RequestMethod.GET)
//    public ResponseEntity<Actor>  queryById(@PathVariable("id") Long id)
//    {
//        Actor actor = actorService.selectById(id);
//        System.out.println(actor);
//        return new ResponseEntity<Actor>(actor, HttpStatus.OK);
//    }
//
//    @RequestMapping(value = "/{id}",method = RequestMethod.PUT)
//    public ResponseEntity<Actor> updateActor(@PathVariable("id") Long id, @Valid Actor actor) throws Exception {
//
//        System.out.println(actor);
//        actor.setActor_id(id);
//        Actor actor1 = actorService.updateActor(actor);
//        System.out.println(actor1);
//        return new ResponseEntity<Actor>(actor1,HttpStatus.OK);
//    }
//
//    @RequestMapping(value = "",method = RequestMethod.GET)
//    public ResponseEntity list(@RequestParam int page,@RequestParam int num)
//    {
//        return new ResponseEntity(actorService.list(page,num),HttpStatus.OK);
//    }
//}
