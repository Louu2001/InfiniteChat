package com.lou.messagingservice.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lou.messagingservice.common.ServiceException;
import com.lou.messagingservice.constants.BalanceLogType;
import com.lou.messagingservice.constants.RedPacketConstants;
import com.lou.messagingservice.constants.RedPacketStatus;
import com.lou.messagingservice.data.receviceRedPacket.ReceiveRedPacketResponse;
import com.lou.messagingservice.mapper.BalanceLogMapper;
import com.lou.messagingservice.mapper.RedPacketMapper;
import com.lou.messagingservice.mapper.UserBalanceMapper;
import com.lou.messagingservice.model.BalanceLog;
import com.lou.messagingservice.model.RedPacket;
import com.lou.messagingservice.model.RedPacketReceive;
import com.lou.messagingservice.model.UserBalance;
import com.lou.messagingservice.service.GetRedPacketService;
import com.lou.messagingservice.service.RedPacketReceiveService;
import com.lou.messagingservice.mapper.RedPacketReceiveMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;

/**
 * @author loujun
 * @description 针对表【red_packet_receive(红包领取记录表)】的数据库操作Service实现
 * @createDate 2025-06-15 16:48:46
 */
@Service
public class RedPacketReceiveServiceImpl extends ServiceImpl<RedPacketMapper, RedPacket>
        implements RedPacketReceiveService {

    private final UserBalanceMapper userBalanceMapper;

    private final BalanceLogMapper balanceLogMapper;

    private final RedPacketReceiveMapper redPacketReceiveMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final GetRedPacketService getRedPacketService;

    private final Snowflake snowflake;

    //Redis Lua脚本用于原子性地检查并递减红包数量
    private static final String RED_PACKET_LUA_SCRIPT = RedPacketConstants.RED_PACKET_LUA_SCRIPT.getValue();

    private static final String RED_PACKET_KEY_PREFIX = RedPacketConstants.RED_PACKET_KEY_PREFIX.getValue();

    private static final Integer CLAIMED = RedPacketStatus.CLAIMED.getStatus();

    public RedPacketReceiveServiceImpl(UserBalanceMapper userBalanceMapper, BalanceLogMapper balanceLogMapper, RedPacketReceiveMapper redPacketReceiveMapper, RedisTemplate<String, Object> redisTemplate,
                                       GetRedPacketService getRedPacketService) {
        this.userBalanceMapper = userBalanceMapper;
        this.balanceLogMapper = balanceLogMapper;
        this.redPacketReceiveMapper = redPacketReceiveMapper;
        this.redisTemplate = redisTemplate;
        this.getRedPacketService = getRedPacketService;
        this.snowflake = IdUtil.getSnowflake(
                Integer.parseInt(RedPacketConstants.WORKED_ID.getValue()),
                Integer.parseInt(RedPacketConstants.DATACENTER_ID.getValue())
        );
    }

    /**
     * 领取红包
     *
     * @param userId      用户ID
     * @param redPacketId 红包ID
     * @return ReceiveRedPacketResponse h红包领取响应
     * @throws ServiceException 业务异常
     */
    @Transactional
    public ReceiveRedPacketResponse receiveRedPacket(Long userId, Long redPacketId) throws ServiceException {

        //检查用户是否已经领取过红包，如果已经领取到则返回红包详情页
        BigDecimal amount = verifyUserHasNotReceived(redPacketId, userId);
        if (amount != null) {
            return new ReceiveRedPacketResponse(amount, 0);
        }

        //尝试抢红包
        Integer result = grabRedPacket(redPacketId);
        if (result.equals(CLAIMED)) {
            return new ReceiveRedPacketResponse(null, CLAIMED);
        }

        // 获取红包信息
        RedPacket redPacket = getRedPacketById(redPacketId);

        //检查红包状态
        Integer status = validateRedPacketStatus(redPacket);
        if (status != 0) {
            return new ReceiveRedPacketResponse(null, status);
        }

        // 计算领取金额
        BigDecimal receivedAmount = computeReceivedAmount(redPacket);

        // 更新红包信息
        updateRedPacketInfo(redPacket, receivedAmount);

        // 插入领取记录
        LocalDateTime receivedTime = logRedPacketReceive(redPacketId, userId, receivedAmount);

        // 更新用户余额
        adjustUserBalance(userId, receivedAmount);

        // 记录余额变动日志
        logBalanceChange(userId, receivedAmount, redPacketId);

        // 构建响应对象
        return new ReceiveRedPacketResponse(receivedAmount, status);
    }

    private Integer grabRedPacket(Long redPacketId) {
        String redPacketCountKey = RED_PACKET_KEY_PREFIX + redPacketId;
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(RED_PACKET_LUA_SCRIPT);
        redisScript.setResultType(Long.class);
        try {
            Long result = redisTemplate.execute(redisScript, Collections.singletonList(redPacketCountKey));
            if (result == null) {
                throw new IllegalStateException("Redis 脚本执行返回 null");
            }
            return result.intValue();
        } catch (Exception e) {
            throw new RuntimeException("执行 Redis Lua 脚本时出错", e);
        }
    }

    /**
     * 获取红包信息，通过ID查询红包。
     *
     * @param redPacketId 红包ID
     * @return RedPacket 红包对象
     * @throws ServiceException 如果红包不存在
     */
    private RedPacket getRedPacketById(Long redPacketId) throws ServiceException {
        RedPacket redPacket = this.getById(redPacketId);
        if (redPacket == null) {
            throw new ServiceException("红包不存在");
        }
        return redPacket;
    }

    private Integer validateRedPacketStatus(RedPacket redPacket) throws ServiceException {
        if (Objects.equals(redPacket.getStatus(), RedPacketStatus.EXPIRED.getStatus())) {
            return RedPacketStatus.EXPIRED.getStatus();
        }
        if (redPacket.getRemainingCount() <= 0) {
            return RedPacketStatus.CLAIMED.getStatus();
        }
        return 0;
    }

    /**
     * 验证用户是否领取过该红包
     *
     * @param redPacketId 红包ID
     * @param userId      用户ID
     * @return 用户已领取金额
     */
    private BigDecimal verifyUserHasNotReceived(Long redPacketId, Long userId) {
        QueryWrapper<RedPacketReceive> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("red_packet_id", redPacketId).eq("receiver_id", userId);
        RedPacketReceive redPacketReceive = redPacketReceiveMapper.selectOne(queryWrapper);
        if (redPacketReceive == null) {
            return null;
        }
        return redPacketReceive.getAmount();
    }

    /**
     * 计算用户领取的红包金额，基于红包类型和剩余金额
     *
     * @param redPacket 红包对象
     * @return BigDecimal 领取金额
     * @throws ServiceException 如果红包类型未知
     */
    private BigDecimal computeReceivedAmount(RedPacket redPacket) throws ServiceException {
        if (Objects.equals(redPacket.getRedPacketType(), RedPacketConstants.RED_PACKET_TYPE_NORMAL.getIntValue())) {
            return calculateNormalRedPacket(redPacket);
        } else if (Objects.equals(redPacket.getRedPacketType(), RedPacketConstants.RED_PACKET_TYPE_RANDOM.getIntValue())) {
            return calculateRandomRedPacket(redPacket);
        } else {
            throw new ServiceException("未知的红包类型");
        }
    }

    private void updateRedPacketInfo(RedPacket redPacket, BigDecimal receivedAmount) throws ServiceException {
        redPacket.setRemainingAmount(redPacket.getRemainingAmount().subtract(receivedAmount));
        redPacket.setRemainingCount(redPacket.getRemainingCount() - 1);

        if (redPacket.getRemainingCount() == 0) {
            redPacket.setStatus(RedPacketStatus.CLAIMED.getStatus());
            redisTemplate.delete("red_packet:count" + redPacket.getRedPacketId());
        }

        boolean updateSuccess = this.updateById(redPacket);
        if (!updateSuccess) {
            throw new ServiceException("更新红包信息失败");
        }
    }

    private LocalDateTime logRedPacketReceive(Long redPacketId, Long userId, BigDecimal receivedAmount) throws ServiceException {
        RedPacketReceive receive = new RedPacketReceive();
        receive.setRedPacketReceiveId(generateId())
                .setRedPacketId(redPacketId)
                .setRedPacketReceiveId(userId)
                .setAmount(receivedAmount)
                .setReceivedAt(LocalDateTime.now());

        int insertResult = redPacketReceiveMapper.insert(receive);
        if (insertResult != 1) {
            throw new ServiceException("红包领取记录插入失败");
        }
        return receive.getReceivedAt();
    }


    /**
     * 更新用户余额
     *
     * @param userId        用户ID
     * @param receiveAmount 领取金额
     * @throws ServiceException 如果更新用户余额失败
     */
    private void adjustUserBalance(Long userId, BigDecimal receiveAmount) throws ServiceException {
        UserBalance userBalance = userBalanceMapper.selectById(userId);
        if (userBalance == null) {
            throw new ServiceException("用户余额信息不存在");
        }

        userBalance.setBalance(userBalance.getBalance().add(receiveAmount))
                .setUpdatedAt(LocalDateTime.now());

        int updateResult = userBalanceMapper.updateById(userBalance);
        if (updateResult != 1) {
            throw new ServiceException("更新用户余额失败");
        }
    }

    /**
     * 记录用户余额变动日志
     *
     * @param userId         用户ID
     * @param receivedAmount 变动余额
     * @param redPacketId    关联红包ID
     * @throws ServiceException 如果插入余额变动日志失败
     */
    private void logBalanceChange(Long userId, BigDecimal receivedAmount, Long redPacketId) throws ServiceException {
        BalanceLog balanceLog = new BalanceLog();
        balanceLog.setBalanceLogId(generateId())
                .setUserId(userId)
                .setAmount(receivedAmount)
                .setType(BalanceLogType.RECEIVE_RED_PACKET.getType())
                .setRelatedId(redPacketId)
                .setCreatedAt(LocalDateTime.now());

        int insertResult = balanceLogMapper.insert(balanceLog);
        if (insertResult != 1) {
            throw new ServiceException("记录余额变动日志失败");
        }
    }


    private BigDecimal calculateNormalRedPacket(RedPacket redPacket) {
        return redPacket.getTotalAmount()
                .divide(new BigDecimal(redPacket.getTotalCount()), RedPacketConstants.DIVIDE_SCALE.getDivideScale(), RoundingMode.DOWN);
    }

    /**
     * 计算拼手气红包的领取金额，随机分配
     *
     * @param redPacket 红包对象
     * @return bigDecimal 领取金额
     */
    private BigDecimal calculateRandomRedPacket(RedPacket redPacket) {
        if (redPacket.getRemainingCount() == 1) {
            //最后一个红包，领取剩余所有金额
            return redPacket.getRemainingAmount();
        } else {
            // 计算最大可领取金额
            BigDecimal maxAmount = redPacket.getRemainingAmount()
                    .divide(new BigDecimal(redPacket.getRemainingCount()), RedPacketConstants.DIVIDE_SCALE.getDivideScale(), RoundingMode.DOWN)
                    .multiply(RedPacketConstants.RANDOM_MULTIPLIER.getBigDecimalValue());
            return generateRandomAmount(RedPacketConstants.MIN_AMOUNT.getBigDecimalValue(), maxAmount);
        }
    }

    /**
     * 生成指定范围内的随机金额
     *
     * @param min 最小金额
     * @param max 最大金额
     * @return BigDecimal 随机金额
     */
    private BigDecimal generateRandomAmount(BigDecimal min, BigDecimal max) {
        BigDecimal range = max.subtract(min);
        BigDecimal randomInRange = range.multiply(BigDecimal.valueOf(Math.random()));
        BigDecimal randomAmount = min.add(randomInRange).setScale(RedPacketConstants.AMOUNT_SCALE.getDivideScale(), RoundingMode.DOWN);
        return randomAmount.compareTo(min) < 0 ? min : randomAmount;
    }

    /**
     * 生成唯一ID,使用雪花算法
     *
     * @return Long 唯一ID
     */
    private Long generateId() {
        return snowflake.nextId();
    }

}




