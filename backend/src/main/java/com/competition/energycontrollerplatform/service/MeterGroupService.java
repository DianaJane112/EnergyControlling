package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.MeterGroupRequest;
import com.competition.energycontrollerplatform.dto.MeterGroupVO;

import java.util.List;

/**
 * 分组管理服务接口。
 * <p>
 * 商户数据权限隔离：非 ADMIN 角色只能操作/查询属于自己 merchant_id 的分组。
 */
public interface MeterGroupService {

    /**
     * 分页查询分组列表。
     *
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @param keyword    关键字（模糊匹配分组名称）
     * @param merchantId 商户ID（ADMIN可选，非ADMIN强制覆盖）
     */
    IPage<MeterGroupVO> page(int pageNum, int pageSize, String keyword, Long merchantId);

    /**
     * 按ID查询分组详情。
     */
    MeterGroupVO getById(Long id);

    /**
     * 创建分组。
     */
    MeterGroupVO create(MeterGroupRequest request);

    /**
     * 更新分组。
     */
    MeterGroupVO update(Long id, MeterGroupRequest request);

    /**
     * 删除分组（逻辑删除）。
     */
    void delete(Long id);

    /**
     * 获取指定商户的所有启用分组（下拉列表用）。
     */
    List<MeterGroupVO> listByMerchant(Long merchantId);

    /**
     * 获取指定商户的分组树形结构（含父子层级）。
     *
     * @param merchantId 商户ID（ADMIN可选，非ADMIN强制覆盖）
     * @return 顶级分组列表，每个分组下挂载 children 子分组
     */
    List<MeterGroupVO> getTree(Long merchantId);
}
