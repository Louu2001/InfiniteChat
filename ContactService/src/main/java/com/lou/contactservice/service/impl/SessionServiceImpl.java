package com.lou.contactservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lou.contactservice.model.Session;
import com.lou.contactservice.service.SessionService;
import com.lou.contactservice.mapper.SessionMapper;
import org.springframework.stereotype.Service;

/**
* @author Lou
* @description 针对表【session(会话表)】的数据库操作Service实现
* @createDate 2025-06-28 14:30:15
*/
@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session>
    implements SessionService{

}




