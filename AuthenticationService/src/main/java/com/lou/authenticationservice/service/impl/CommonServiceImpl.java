package com.lou.authenticationservice.service.impl;

import com.lou.authenticationservice.data.common.sms.SMSRequest;
import com.lou.authenticationservice.data.common.sms.SMSResponse;
import com.lou.authenticationservice.service.CommonService;
import com.lou.authenticationservice.utils.RandomNumUtil;
import com.lou.authenticationservice.utils.SendMailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.lou.authenticationservice.constants.user.registerConstant.REGISTER_CODE;

/**
 * @ClassName CommonServiceImpl
 * @Description TODO
 * @Author Lou
 * @Date 2025/5/30 17:37
 */

@Service
@Slf4j
public class CommonServiceImpl implements CommonService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public SMSResponse sendSms(SMSRequest request) {
        String phone = request.getPhone();
        String code = RandomNumUtil.getRandomNum();

        redisTemplate.opsForValue().set(REGISTER_CODE + phone, code, 5, TimeUnit.MINUTES);
        return new SMSResponse();
    }

//    // service
//    @Override
//    public void sendMail(String tagerMail) {
//        String code = new RandomCodeUtil().getRandomCode();
//        new SendMailUtil().sendEmailCode(tagerMail, code);
//        stringRedisTemplate.opsForValue().set(tagerMail, code, SMSConstant.SMS_EXPIRE_TIME, TimeUnit.MINUTES);
//    }
}
