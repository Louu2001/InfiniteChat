package com.lou.offlinedatastoreservice.controller;

import com.lou.offlinedatastoreservice.common.Result;
import com.lou.offlinedatastoreservice.data.offlineMessage.OfflineMsgRequest;
import com.lou.offlinedatastoreservice.data.offlineMessage.OfflineMsgResponse;
import com.lou.offlinedatastoreservice.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/offline")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/message")
    public Result<OfflineMsgResponse> getOfflineMessage(@Valid OfflineMsgRequest request) {
        OfflineMsgResponse response = messageService.getOfflineMessage(request);

        return Result.ok(response);
    }
}
