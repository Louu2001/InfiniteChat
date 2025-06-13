package com.lou.contactservice.controller;

import com.lou.contactservice.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {

    @GetMapping("/user")
    public Result<UserResponse> getUser() {
        UserResponse response = new UserResponse();
        response.setAvatar("www.baidu.com");

        return Result.ok(response);
    }
}
