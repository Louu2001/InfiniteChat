package com.lou.realtimecommunicationservice.controller;

import com.lou.realtimecommunicationservice.common.Result;
import com.lou.realtimecommunicationservice.data.ReceiveMessage.PushMoment.PushMomentRequest;
import com.lou.realtimecommunicationservice.service.impl.NettyMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName PushController
 * @Description TODO
 * @Author Lou
 * @Date 2025/6/21 21:41
 */

@RestController
@RequestMapping("/api/v1/message/push")
@Slf4j
public class PushController {

    @Autowired
    private NettyMessageService nettyMessageService;

    @PostMapping
    public Result<?> receiveNoticeMoment(@RequestBody PushMomentRequest request){
        nettyMessageService.sendNoticeMoment(request);
        return Result.ok(null);
    }
}
