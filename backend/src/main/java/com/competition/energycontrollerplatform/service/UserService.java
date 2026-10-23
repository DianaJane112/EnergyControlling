package com.competition.energycontrollerplatform.service;

import com.competition.energycontrollerplatform.entity.User;

/**
 * 用户认证服务接口
 */
public interface UserService {

    /**
     * 校验用户名密码，认证成功返回用户实体。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 认证成功的用户
     * @throws com.competition.energycontrollerplatform.common.BusinessException 认证失败
     */
    User authenticate(String username, String password);

    /**
     * 更新用户最后登录时间。
     *
     * @param userId 用户 ID
     */
    void updateLastLoginTime(Long userId);

    /**
     * 根据用户 ID 查询用户信息。
     *
     * @param userId 用户 ID
     * @return 用户实体
     * @throws com.competition.energycontrollerplatform.common.BusinessException 用户不存在
     */
    User getById(Long userId);

    /**
     * 修改当前登录用户的密码。
     * <p>
     * 调用方需从 {@code SecurityContextHolder} 中提取当前用户 ID 传入。
     *
     * @param userId      当前登录用户 ID（由 Controller 从 SecurityContext 提取）
     * @param oldPassword 原密码明文
     * @param newPassword 新密码明文
     * @throws com.competition.energycontrollerplatform.common.BusinessException 原密码校验失败
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 递增用户的 tokenVersion，使所有已签发的 Token 即时报废。
     * 用于修改密码和登出场景。
     */
    void incrementTokenVersion(Long userId);
}
