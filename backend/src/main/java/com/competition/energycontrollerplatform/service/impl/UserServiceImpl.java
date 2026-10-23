package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.CacheNullValueHelper;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.entity.User;
import com.competition.energycontrollerplatform.mapper.UserMapper;
import com.competition.energycontrollerplatform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户认证服务实现。
 * <p>
 * 使用 BCrypt 进行密码匹配校验。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CacheNullValueHelper cacheNullValueHelper;

    @Override
    public User authenticate(String username, String password) {
        // 1. 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username)
               .eq(User::getStatus, 1);  // 仅启用状态的用户可登录
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw BusinessException.of(Result.CODE_UNAUTHORIZED, "用户名或密码错误");
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw BusinessException.of(Result.CODE_UNAUTHORIZED, "用户名或密码错误");
        }

        return user;
    }

    @Override
    @Cacheable(value = "user", key = "#userId")
    public User getById(Long userId) {
        if (cacheNullValueHelper.isNullCached("user", userId)) {
            throw BusinessException.of("用户不存在");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            cacheNullValueHelper.cacheNull("user", userId);
            throw BusinessException.of("用户不存在");
        }
        return user;
    }

    @Override
    @Transactional
    public void updateLastLoginTime(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "user", key = "#userId")
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        // 1. 根据当前登录用户 ID 查询完整用户实体
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }

        // 2. 比对原密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw BusinessException.of(400, "原密码输入错误");
        }

        // 3. BCrypt 加密新密码并通过 MyBatis-Plus 更新
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);
    }

    @Override
    @Transactional
    @CacheEvict(value = "user", key = "#userId")
    public void incrementTokenVersion(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        User update = new User();
        update.setId(userId);
        update.setTokenVersion((user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1);
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);
    }
}
