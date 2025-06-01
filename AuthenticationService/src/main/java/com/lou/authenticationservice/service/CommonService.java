package com.lou.authenticationservice.service;

import com.lou.authenticationservice.data.common.sms.SMSRequest;
import com.lou.authenticationservice.data.common.sms.SMSResponse;

public interface CommonService {
    SMSResponse sendSms(SMSRequest request);

//    void sendMail(String targetMail);
}
