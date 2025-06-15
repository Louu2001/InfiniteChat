package com.lou.messagingservice.controller;

import com.lou.messagingservice.common.Result;
import com.lou.messagingservice.data.sendMsg.SendMsgRequest;
import com.lou.messagingservice.data.sendMsg.SendMsgResponse;
import com.lou.messagingservice.feign.ContactServiceFeign;
import com.lou.messagingservice.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/test")
public class SendMsgController {

    @Autowired
    private ContactServiceFeign contactServiceFeign;

    @Autowired
    private MessageService messageService;

    @GetMapping("/feign")
    public Result<?> getUser(){
        Result<?> user = contactServiceFeign.getUser();

        return Result.ok(user);
    }

    @PostMapping("/v1/chat/session")
    public Result<SendMsgResponse> sendMsg(@RequestBody SendMsgRequest request) throws Exception{
        SendMsgResponse response = messageService.sendMessage(request);

        return Result.ok(response);
    }

    @GetMapping("/hello")
    public Result<?> get(){
//        Result<?> user = contactServiceFeign.getUser();

        return Result.ok("");
    }
}
