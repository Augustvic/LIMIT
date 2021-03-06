package com.limit.user.controller;

import com.limit.common.result.CodeMsg;
import com.limit.common.result.Result;
import com.limit.redis.service.RedisService;
import com.limit.redis.key.user.UserKey;
import com.limit.user.entity.User;
import com.limit.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/demo")
public class SampleController {

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

//    @RequestMapping("/mq/header")
//    @ResponseBody
//    public Result<String> header() {
//        producer.sendHeader("hello,imooc");
//        return Result.success("Hello, world");
//    }
//
//    @RequestMapping("/mq/fanout")
//    @ResponseBody
//    public Result<String> fanout() {
//        producer.sendFanout("hello,imooc");
//        return Result.success("Hello, world");
//    }
//
//    @RequestMapping("/mq/topic")
//    @ResponseBody
//    public Result<String> topic() {
//        producer.sendTopic("hello,imooc");
//        return Result.success("Hello, world");
//    }
//
//    @RequestMapping("/mq")
//    @ResponseBody
//    public Result<String> mq() {
//        producer.send("hello,imooc");
//        return Result.success("Hello, world");
//    }

    @RequestMapping("/hello")
    @ResponseBody
    public Result<String> home() {
        return Result.success("Hello，world");
    }

    @RequestMapping("/error")
    @ResponseBody
    public Result<String> error() {
        return Result.error(CodeMsg.SESSION_ERROR);
    }

    @RequestMapping("/hello/themaleaf")
    public String themaleaf(Model model) {
        model.addAttribute("name", "Joshua");
        return "hello";
    }

    @RequestMapping("/db/get")
    @ResponseBody
    public Result<User> dbGet() {
        User user = userService.getById(1);
        return Result.success(user);
    }

    @RequestMapping("/db/tx")
    @ResponseBody
    public Result<Boolean> dbTx() {
        userService.tx();
        return Result.success(true);
    }

    @RequestMapping("/redis/get")
    @ResponseBody
    public Result<User> redisGet() {
        User user = redisService.get(UserKey.getById,"" + 1, User.class);
        return Result.success(user);
    }

    @RequestMapping("/redis/set")
    @ResponseBody
    public Result<Boolean> redisSet() {
        User user = new User();
        user.setId(1);
        user.setName("August2");
        boolean ret = redisService.set(UserKey.getById, "" + 1, user);
        return Result.success(ret);
    }

}
