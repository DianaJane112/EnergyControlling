package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.dto.MeterGroupRequest;
import com.competition.energycontrollerplatform.dto.MeterGroupVO;
import com.competition.energycontrollerplatform.service.MeterGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分组管理 Controller。
 * <p>
 * 提供分组的完整 CRUD 接口，挂载商户级数据权限隔离。
 * 非 ADMIN 角色仅可操作和查询属于自己 merchant_id 的分组。
 */
@Tag(name = "电能/水能管理 - 项目信息 - 分组管理", description = "电表分组增删改查，支撑按分组统计用电量")
@RestController
@RequestMapping("/api/v1/meter-group")
@RequiredArgsConstructor
@Validated
public class MeterGroupController {

    private final MeterGroupService meterGroupService;

    @Operation(summary = "分页查询分组列表", description = "支持按关键字（分组名称）模糊搜索，按商户维度过滤。"
            + "非 ADMIN 角色强制仅返回所属商户的分组。")
    @GetMapping
    public Result<IPage<MeterGroupVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "关键字（分组名称模糊匹配）") @RequestParam(required = false) String keyword,
            @Parameter(description = "商户ID（ADMIN可选，非ADMIN强制覆盖）") @RequestParam(required = false) Long merchantId) {
        IPage<MeterGroupVO> page = meterGroupService.page(pageNum, pageSize, keyword, merchantId);
        return Result.success(page);
    }

    @Operation(summary = "分页查询分组列表（带/page路径）", description = "与 GET / 功能相同，兼容前端旧路径。")
    @GetMapping("/page")
    public Result<IPage<MeterGroupVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "关键字（分组名称模糊匹配）") @RequestParam(required = false) String keyword,
            @Parameter(description = "商户ID（ADMIN可选，非ADMIN强制覆盖）") @RequestParam(required = false) Long merchantId) {
        IPage<MeterGroupVO> page = meterGroupService.page(pageNum, pageSize, keyword, merchantId);
        return Result.success(page);
    }

    @Operation(summary = "查询分组详情")
    @GetMapping("/{id}")
    public Result<MeterGroupVO> getById(@Parameter(description = "分组ID") @PathVariable Long id) {
        MeterGroupVO vo = meterGroupService.getById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建分组", description = "非 ADMIN 角色强制将 merchantId 设为当前登录用户关联的商户。")
    @PostMapping
    public Result<MeterGroupVO> create(@Valid @RequestBody MeterGroupRequest request) {
        MeterGroupVO vo = meterGroupService.create(request);
        return Result.success(vo);
    }

    @Operation(summary = "更新分组", description = "仅分组所属商户或 ADMIN 可修改。")
    @PutMapping("/{id}")
    public Result<MeterGroupVO> update(
            @Parameter(description = "分组ID") @PathVariable Long id,
            @Valid @RequestBody MeterGroupRequest request) {
        MeterGroupVO vo = meterGroupService.update(id, request);
        return Result.success(vo);
    }

    @Operation(summary = "删除分组（逻辑删除）", description = "仅分组所属商户或 ADMIN 可删除。")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "分组ID") @PathVariable Long id) {
        meterGroupService.delete(id);
        return Result.success();
    }

    @Operation(summary = "获取商户分组下拉列表", description = "返回指定商户所有启用状态的分组，用于电表绑定分组时的下拉选择。")
    @GetMapping("/list-by-merchant")
    public Result<List<MeterGroupVO>> listByMerchant(
            @Parameter(description = "商户ID（ADMIN可选，非ADMIN强制覆盖）") @RequestParam(required = false) Long merchantId) {
        List<MeterGroupVO> list = meterGroupService.listByMerchant(merchantId);
        return Result.success(list);
    }

    @Operation(summary = "获取分组树形结构", description = "返回指定商户的分组树（含父子层级），用于前端树形组件渲染。"
            + "每个节点包含 children 子分组列表，顶级分组 parentId=null。")
    @GetMapping("/tree")
    public Result<List<MeterGroupVO>> tree(
            @Parameter(description = "商户ID（ADMIN可选，非ADMIN强制覆盖）") @RequestParam(required = false) Long merchantId) {
        List<MeterGroupVO> tree = meterGroupService.getTree(merchantId);
        return Result.success(tree);
    }
}
