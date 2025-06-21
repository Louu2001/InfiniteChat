package com.lou.momentservice.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.alibaba.nacos.shaded.com.google.gson.Gson;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lou.momentservice.Exception.DatabaseException;
import com.lou.momentservice.constants.ConfigEnum;
import com.lou.momentservice.constants.ErrorEnum;
import com.lou.momentservice.constants.MomentConstants;
import com.lou.momentservice.data.createMoment.CreateMomentRequest;
import com.lou.momentservice.data.createMoment.CreateMomentResponse;
import com.lou.momentservice.model.vo.MomentVO;
import com.lou.momentservice.model.Moment;
import com.lou.momentservice.model.User;
import com.lou.momentservice.service.FriendService;
import com.lou.momentservice.service.MomentNotificationService;
import com.lou.momentservice.service.MomentService;
import com.lou.momentservice.mapper.MomentMapper;
import com.lou.momentservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Lou
 * @description 针对表【moment(朋友圈)】的数据库操作Service实现
 * @createDate 2025-06-21 20:06:02
 */
@Service
@Slf4j
public class MomentServiceImpl extends ServiceImpl<MomentMapper, Moment> implements MomentService {

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserService userService;

    @Autowired
    private MomentNotificationService momentNotificationService;

    private final Gson gson = new Gson();

    @Override
    public CreateMomentResponse createMoment(CreateMomentRequest request) throws Exception {

        Long userId = Long.valueOf(request.getUserId());

        MomentVO momentVO = createMomentWithNotification(userId, request.getText(), request.getMediaUrls());

        CreateMomentResponse createMomentResponse = new CreateMomentResponse();
        createMomentResponse.setUserId(momentVO.getUserId())
                .setText(momentVO.getText())
                .setMediaUrls(momentVO.getMediaUrls())
                .setMomentId(createMomentResponse.getMomentId());
        return createMomentResponse;


    }

    @Override
    public Long getMomentOwnerId(Long momentId) {
        Moment moment = this.getById(momentId);

        return moment != null ? moment.getUserId() : null;
    }


    private MomentVO createMomentWithNotification(Long userId, String text, List<String> mediaUrls) throws Exception {
        // 保存朋友圈
        MomentVO momentVO = saveMoment(userId, text, mediaUrls);

        // 获取用户头像
        User user = userService.getById(userId);
        String avatar = user != null ? user.getAvatar() : null;


        // 发通知给朋友
        List<Long> friendIds = friendService.getFriendIds(userId);

        // 发送朋友圈创建通知
        momentNotificationService.sendMomentCreationNotification(userId,avatar,momentVO.getMomentId(),friendIds);

        return momentVO;
    }

    @Transactional
    public MomentVO saveMoment(Long userId, String text, List<String> urls) {
        // 将URL列表转换为JSON字符串
        String mediaUrls = gson.toJson(urls);

        // 创建朋友圈实体
        Moment moment = createMomentEntity(userId, text, mediaUrls);

        // 保存到数据库
        if (!this.save(moment)) {
            throw new DatabaseException(ErrorEnum.DATABASE_ERROR.getCode(), MomentConstants.ERROR_SAVE_FAILED);
        }

        log.info(MomentConstants.LOG_SAVE_SUCCESS, moment);

        return convertToMomentVO(moment, urls);
    }

    private Moment createMomentEntity(Long userId, String text, String mediaUrls) {
        Snowflake snowflake = createSnowflake();
        Moment moment = new Moment();
        moment.setUserId(userId)
                .setText(text)
                .setMediaUrl(mediaUrls)
                .setMomentId(snowflake.nextId());

        return moment;
    }

    private Snowflake createSnowflake() {
        return IdUtil.getSnowflake(
                Integer.parseInt(ConfigEnum.WORKED_ID.getValue()),
                Integer.parseInt(ConfigEnum.DATACENTER_ID.getValue())
        );
    }

    private MomentVO convertToMomentVO(Moment moment, List<String> urls) {
        MomentVO momentVO = new MomentVO();
        BeanUtil.copyProperties(moment, momentVO);
        momentVO.setMediaUrls(urls);

        return momentVO;
    }
}




