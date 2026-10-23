package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.BillCategoryRequest;
import com.competition.energycontrollerplatform.entity.BillCategory;

import java.util.List;

/**
 * 账单分类服务接口。
 */
public interface BillCategoryService {

    /**
     * 分页查询账单分类列表。
     */
    IPage<BillCategory> listPage(int pageNum, int pageSize, String keyword);

    /**
     * 获取所有启用的账单分类（下拉列表）。
     */
    List<BillCategory> listEnabled();

    /**
     * 详情。
     */
    BillCategory getById(Long id);

    /**
     * 新增账单分类。
     */
    BillCategory create(BillCategoryRequest request);

    /**
     * 编辑账单分类。
     */
    BillCategory update(Long id, BillCategoryRequest request);

    /**
     * 删除账单分类（逻辑删除）。
     */
    void delete(Long id);
}
