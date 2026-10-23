package com.competition.energycontrollerplatform.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.SensitiveDataMasker;
import com.competition.energycontrollerplatform.miniapp.dto.BindCustomerRequest;
import com.competition.energycontrollerplatform.miniapp.dto.MiniAppLoginVO;
import com.competition.energycontrollerplatform.miniapp.dto.PhoneLoginRequest;
import com.competition.energycontrollerplatform.miniapp.dto.WechatLoginRequest;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.miniapp.entity.MiniAppUser;
import com.competition.energycontrollerplatform.integration.wechat.WechatApiClient;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.VirtualMeterViewerMapper;
import com.competition.energycontrollerplatform.miniapp.mapper.MiniAppUserMapper;
import com.competition.energycontrollerplatform.miniapp.security.MiniappJwtUtils;
import com.competition.energycontrollerplatform.miniapp.service.MiniAppUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小程序用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiniAppUserServiceImpl extends ServiceImpl<MiniAppUserMapper, MiniAppUser> implements MiniAppUserService {

    private final MiniappJwtUtils miniappJwtUtils;
    private final CustomerMapper customerMapper;
    private final MeterMapper meterMapper;
    private final WechatApiClient wechatApiClient;
    private final VirtualMeterViewerMapper viewerMapper;

    @Value("${miniapp.sms.dev-mode:true}")
    private boolean smsDevMode;

    @Value("${miniapp.sms.dev-code:000000}")
    private String devCode;

    @Override
    public MiniAppLoginVO wechatLogin(WechatLoginRequest request) {
        // 1. 调用微信 API 换取 openid（开发阶段自动使用 Mock，生产环境真实调用）
        WechatApiClient.WechatSession session = wechatApiClient.code2Session(request.getCode());
        String openid = session.getOpenid();

        // 2. 根据 openid 查找用户
        MiniAppUser user = lambdaQuery()
                .eq(MiniAppUser::getOpenid, openid)
                .one();

        if (user == null) {
            user = new MiniAppUser();
            user.setOpenid(openid);
            user.setUnionid(session.getUnionid());
            user.setLoginType(0);
            user.setStatus(1);
            user.setTokenVersion(1);
            user.setLastLoginTime(LocalDateTime.now());
            save(user);
            log.info("小程序微信用户首次登录已登记: userId={}, openid={}",
                    user.getId(), SensitiveDataMasker.maskOpenId(openid));
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(403, "该账号已被禁用，请联系管理员");
        }

        // 3. 更新 unionid（生产环境 code2Session 可能返回，用于公众号-小程序用户打通）及最后登录时间
        if (session.getUnionid() != null && !session.getUnionid().isBlank()) {
            user.setUnionid(session.getUnionid());
        }
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);

        // 4. 生成 Token 并返回
        String token = miniappJwtUtils.generateToken(user.getId(), user.getCustomerId(), "WECHAT", user.getTokenVersion());
        return buildLoginVO(user, token, !hasAnyAccess(user));
    }

    @Override
    @Transactional
    public MiniAppLoginVO phoneLogin(PhoneLoginRequest request) {
        // 1. 验证短信验证码
        if (smsDevMode) {
            if (!devCode.equals(request.getCode())) {
                throw new BusinessException("验证码错误，开发模式固定为 " + devCode);
            }
        } else {
            // TODO: 生产环境对接云通讯 SDK 验证短信验证码
            throw new BusinessException("手机号登录暂未开放，请使用微信登录");
        }

        // 2. 根据手机号查找用户
        MiniAppUser user = lambdaQuery()
                .eq(MiniAppUser::getPhone, request.getPhone())
                .one();

        if (user == null) {
            // 3. 新用户自动注册：通过手机号匹配商户
            Customer customer = customerMapper.selectOne(
                    new LambdaQueryWrapper<Customer>()
                            .eq(Customer::getPhone, request.getPhone())
                            .eq(Customer::getIsDeleted, 0)
            );

            if (customer == null) {
                throw new BusinessException(404, "未找到与该手机号关联的商户账户，请联系管理员绑定");
            }

            user = new MiniAppUser();
            user.setPhone(request.getPhone());
            user.setCustomerId(customer.getId());
            user.setLoginType(1); // 手机号登录
            user.setStatus(1);
            user.setLastLoginTime(LocalDateTime.now());
            save(user);

            log.info("小程序用户自动注册: phone={}, customerId={}", SensitiveDataMasker.maskPhone(request.getPhone()), customer.getId());
        } else {
            if (user.getStatus() == 0) {
                throw new BusinessException(403, "该账号已被禁用，请联系管理员");
            }
            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            updateById(user);
        }

        // 4. 生成 Token 并返回
        String token = miniappJwtUtils.generateToken(user.getId(), user.getCustomerId(), "PHONE", user.getTokenVersion());
        return buildLoginVO(user, token, !hasAnyAccess(user));
    }

    @Override
    @Transactional
    public MiniAppLoginVO bindCustomer(BindCustomerRequest request) {
        // 1. 验证短信验证码
        if (smsDevMode) {
            if (!devCode.equals(request.getCode())) {
                throw new BusinessException("验证码错误，开发模式固定为 " + devCode);
            }
        } else {
            throw new BusinessException("手机号登录暂未开放，请使用微信登录");
        }

        // 2. 根据手机号匹配商户
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getPhone, request.getPhone())
                        .eq(Customer::getIsDeleted, 0)
        );

        if (customer == null) {
            throw new BusinessException(404,
                    "未找到与该手机号关联的商户账户，请联系管理员在商户档案中登记该手机号");
        }

        // 3. 查找或创建 MiniAppUser（按 openid 优先，其次按 phone 匹配以免 uk_phone 冲突）
        MiniAppUser user = lambdaQuery()
                .eq(MiniAppUser::getOpenid, request.getOpenid())
                .one();

        if (user == null) {
            // 按 phone 查找已有用户（测试数据/历史数据已占用该手机号的情况）
            MiniAppUser phoneUser = lambdaQuery()
                    .eq(MiniAppUser::getPhone, request.getPhone())
                    .one();

            if (phoneUser != null) {
                // 已有用户占用该手机号 → 更新 openid 完成绑定（避免 uk_phone 冲突）
                phoneUser.setOpenid(request.getOpenid());
                if (request.getUnionid() != null && !request.getUnionid().isBlank()) {
                    phoneUser.setUnionid(request.getUnionid());
                }
                phoneUser.setCustomerId(customer.getId());
                phoneUser.setLastLoginTime(LocalDateTime.now());
                phoneUser.setUpdateTime(LocalDateTime.now());
                updateById(phoneUser);
                user = phoneUser;
                log.info("微信用户绑定商户（复用已有手机号）: openid={}, phone={}, customerId={}",
                        SensitiveDataMasker.maskOpenId(request.getOpenid()), SensitiveDataMasker.maskPhone(request.getPhone()), customer.getId());
            } else {
                // 全新用户：创建记录并绑定商户
                user = new MiniAppUser();
                user.setOpenid(request.getOpenid());
                if (request.getUnionid() != null && !request.getUnionid().isBlank()) {
                    user.setUnionid(request.getUnionid());
                }
                user.setPhone(request.getPhone());
                user.setCustomerId(customer.getId());
                user.setLoginType(0); // 微信登录
                user.setStatus(1);
                user.setLastLoginTime(LocalDateTime.now());
                save(user);
                log.info("微信用户绑定商户成功: openid={}, phone={}, customerId={}",
                        SensitiveDataMasker.maskOpenId(request.getOpenid()), SensitiveDataMasker.maskPhone(request.getPhone()), customer.getId());
            }
        } else {
            // 已有 openid 记录 → 补充绑定
            if (user.getStatus() == 0) {
                throw new BusinessException(403, "该账号已被禁用，请联系管理员");
            }
            user.setPhone(request.getPhone());
            if (request.getUnionid() != null && !request.getUnionid().isBlank()) {
                user.setUnionid(request.getUnionid());
            }
            user.setCustomerId(customer.getId());
            user.setLastLoginTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            updateById(user);
            log.info("微信用户更新绑定: openid={}, phone={}, customerId={}",
                    SensitiveDataMasker.maskOpenId(request.getOpenid()), SensitiveDataMasker.maskPhone(request.getPhone()), customer.getId());
        }

        // 4. 生成 Token 并返回
        String token = miniappJwtUtils.generateToken(user.getId(), user.getCustomerId(), "WECHAT", user.getTokenVersion());
        return buildLoginVO(user, token, !hasAnyAccess(user));
    }

    @Override
    public MiniAppLoginVO getCurrentUserInfo(Long userId) {
        MiniAppUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        String token = miniappJwtUtils.generateToken(user.getId(), user.getCustomerId(),
                user.getLoginType() == 0 ? "WECHAT" : "PHONE", user.getTokenVersion());
        return buildLoginVO(user, token, !hasAnyAccess(user));
    }

    @Override
    @Transactional
    public void incrementTokenVersion(Long userId) {
        MiniAppUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        MiniAppUser update = new MiniAppUser();
        update.setId(userId);
        update.setTokenVersion((user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1);
        update.setUpdateTime(LocalDateTime.now());
        updateById(update);
    }

    // ────────────────── 私有方法 ──────────────────

    /**
     * 构建登录响应 VO。
     */
    private MiniAppLoginVO buildLoginVO(MiniAppUser user, String token, boolean needBindCustomer) {
        // 查询商户信息
        Customer customer = user.getCustomerId() != null ? customerMapper.selectById(user.getCustomerId()) : null;
        MiniAppLoginVO.CustomerInfo customerInfo = null;
        if (customer != null) {
            customerInfo = MiniAppLoginVO.CustomerInfo.builder()
                    .customerId(customer.getId())
                    .customerName(customer.getCustomerName())
                    .customerCode(customer.getCustomerCode())
                    .balance(customer.getBalance())
                    .build();
        }

        // 查询商户下的表计列表
        MiniAppLoginVO.UserInfo userInfo = MiniAppLoginVO.UserInfo.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .loginType(user.getLoginType() == 0 ? "WECHAT" : "PHONE")
                .build();

        return MiniAppLoginVO.builder()
                .token(token)
                .openid(user.getOpenid())
                .unionid(user.getUnionid())
                .needBindCustomer(needBindCustomer)
                .userInfo(userInfo)
                .customerInfo(customerInfo)
                .build();
    }

    private boolean hasAnyAccess(MiniAppUser user) {
        return user.getCustomerId() != null || !viewerMapper.selectActiveMeterIds(user.getId()).isEmpty();
    }
}
