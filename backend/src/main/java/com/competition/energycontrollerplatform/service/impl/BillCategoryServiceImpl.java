package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.BillCategoryRequest;
import com.competition.energycontrollerplatform.entity.BillCategory;
import com.competition.energycontrollerplatform.mapper.BillCategoryMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.BillCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账单分类服务实现。
 */
@Service
@RequiredArgsConstructor
public class BillCategoryServiceImpl implements BillCategoryService {

    private final BillCategoryMapper billCategoryMapper;

    @Override
    public IPage<BillCategory> listPage(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<BillCategory> wrapper = new LambdaQueryWrapper<>();
        // 使用 and() 包裹 OR 条件，确保与 @TableLogic 的 is_deleted=0 正确嵌套
        wrapper.and(StringUtils.hasText(keyword), w -> w
                .like(BillCategory::getCategoryName, keyword)
                .or()
                .like(BillCategory::getCategoryCode, keyword))
                .orderByAsc(BillCategory::getSortOrder)
                .orderByDesc(BillCategory::getCreateTime);

        Page<BillCategory> page = new Page<>(pageNum, pageSize);
        return billCategoryMapper.selectPage(page, wrapper);
    }

    @Override
    public List<BillCategory> listEnabled() {
        LambdaQueryWrapper<BillCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BillCategory::getStatus, 1)
                .orderByAsc(BillCategory::getSortOrder);
        return billCategoryMapper.selectList(wrapper);
    }

    @Override
    public BillCategory getById(Long id) {
        BillCategory category = billCategoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.of(400, "账单分类不存在");
        }
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillCategory create(BillCategoryRequest request) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可创建账单分类");
        }

        BillCategory category = new BillCategory();
        category.setCategoryName(request.getCategoryName());
        category.setCategoryCode(request.getCategoryCode());
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        billCategoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillCategory update(Long id, BillCategoryRequest request) {
        BillCategory category = billCategoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.of(400, "账单分类不存在");
        }

        category.setCategoryName(request.getCategoryName());
        category.setCategoryCode(request.getCategoryCode());
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : category.getSortOrder());
        category.setStatus(request.getStatus() != null ? request.getStatus() : category.getStatus());
        category.setUpdateTime(LocalDateTime.now());
        billCategoryMapper.updateById(category);
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可删除账单分类");
        }
        BillCategory category = billCategoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.of(400, "账单分类不存在");
        }
        billCategoryMapper.logicDeleteById(id); // 绕过 @TableLogic 的 update_time 空值问题
    }
}
