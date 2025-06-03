package com.lou.authenticationservice.controller;

import com.lou.authenticationservice.common.Result;
import com.lou.authenticationservice.data.common.sms.SMSRequest;
import com.lou.authenticationservice.data.common.sms.SMSResponse;
import com.lou.authenticationservice.data.common.uploadUrl.UploadUrlResponse;
import com.lou.authenticationservice.data.common.uploadUrl.UploadUrlRequest;

import com.lou.authenticationservice.service.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @ClassName CommonController
 * @Description TODO
 * @Author Lou
 * @Date 2025/5/30 17:32
 */


@Slf4j
@RestController
@RequestMapping("/api/v1/user/common")
public class CommonController {

    @Autowired
    private CommonService commonService;

    @PostMapping("/sms")
    public Result<SMSResponse> sendSms(@RequestBody @Valid SMSRequest request) throws Exception{
        SMSResponse response = commonService.sendSms(request);

        return Result.ok(response);
    }

    @PostMapping("/uploadUrl")
    public Result<UploadUrlResponse> getUploadUrl(@Valid UploadUrlRequest request) throws Exception{
        UploadUrlResponse response = commonService.uploadUrl(request);

        return Result.ok(response);
    }


//    @GetMapping("/getCode")
//    public BaseResponse<String> mail(@RequestParam("targetEmail") String targetEmail){
//        userService.sendMail(targetEmail);
//        return ResultUtils.success(TMSMSConstant.SMS_SEND_SUCCESS_MSG);
//    }

}
