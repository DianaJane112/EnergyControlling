package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.*;

import java.util.List;

/**
 * 虚拟电表（总分表拓扑）服务接口。
 * <p>
 * 用于管理总表→分表的层级关系，并提供线损/损耗分析计算。
 * 线损 = 总表最新读数 - 所有分表最新读数之和。
 */
public interface ShadowMeterService {

    /**
     * 分页查询总分表拓扑关系（支持关键字搜索和多条件筛选）。
     */
    IPage<ShadowMeterVO> page(ShadowMeterPageRequest request);

    /**
     * 按ID查询拓扑关系详情。
     */
    ShadowMeterVO getById(Long id);

    /**
     * 创建总分表拓扑关系（同时可设置子表的商户名、分组、告警额等属性）。
     */
    ShadowMeterVO create(ShadowMeterRequest request);

    /**
     * 更新拓扑关系（同时可同步修改子表的商户名、分组、告警额等属性）。
     */
    ShadowMeterVO update(Long id, ShadowMeterRequest request);

    /**
     * 删除拓扑关系（逻辑删除）。
     */
    void delete(Long id);

    /**
     * 批量删除拓扑关系。
     *
     * @param ids 关系ID列表
     * @return 删除数量
     */
    int batchDelete(List<Long> ids);

    /**
     * 线损/损耗分析：根据总表ID查询其所有子表，计算绝对损耗量和损耗率。
     *
     * @param masterId 总表ID
     * @return 损耗分析结果
     */
    MasterLossVO lossAnalysis(Long masterId);

    /**
     * 虚拟电表拓扑关系统计汇总。
     *
     * @return 统计结果
     */
    ShadowMeterStatisticsVO getStatistics();

    // ──────────────── 关联真实电表 ────────────────

    /**
     * 查询指定虚拟电表下关联的真实电表列表。
     *
     * @param virtualMeterId 虚拟电表ID（即主列表每行的 child_meter_id）
     * @return 关联的真实电表列表
     */
    List<ShadowMeterRealMeterVO> listRealMeters(Long virtualMeterId);

    /**
     * 为虚拟电表关联多台真实电表。
     *
     * @param virtualMeterId 虚拟电表ID
     * @param meterIds       要关联的真实电表ID列表
     */
    void bindRealMeters(Long virtualMeterId, List<Long> meterIds);

    /**
     * 解除虚拟电表与真实电表的关联（逻辑删除关联关系）。
     *
     * @param relationId 关联关系ID（t_shadow_meter.id）
     */
    void unbindRealMeter(Long relationId);

    // ──────────────── 更多操作 ────────────────

    /**
     * 查询指定子表的充值记录。
     *
     * @param childMeterId 子表ID
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @return 充值记录分页
     */
    IPage<RechargeRecordVO> getRechargeRecords(Long childMeterId, int pageNum, int pageSize);

    /**
     * 查询指定子表的操作记录。
     *
     * @param childMeterId 子表ID
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @return 操作记录分页
     */
    IPage<OperationLogVO> getOperationLogs(Long childMeterId, int pageNum, int pageSize);

    /**
     * 生成指定子表的二维码信息（返回表计基本信息用于生成二维码）。
     *
     * @param childMeterId 子表ID
     * @return 二维码信息（含表号、商户名等可用于生成二维码的数据）
     */
    MeterResponse getQrCodeInfo(Long childMeterId);

    /**
     * 生成指定子表的二维码 PNG 图片字节数组。
     * <p>
     * 二维码内容格式：{@code METER:<meterType>:<dbNo>}，
     * 扫码后可通过小程序绑定接口自动绑定到商户。
     *
     * @param childMeterId 子表ID
     * @return PNG 格式二维码图片字节数组
     */
    byte[] generateQrCodeImage(Long childMeterId);

    /**
     * 查询指定子表的计费轨迹（资金流水明细）。
     *
     * @param childMeterId 子表ID
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @return 计费轨迹分页
     */
    IPage<BillingTrackVO> getBillingTrack(Long childMeterId, int pageNum, int pageSize);

    // ──────────────── 批量导出 ────────────────

    /**
     * 按筛选条件导出全量虚拟电表/水表拓扑关系为 Excel .xlsx 文件。
     *
     * @param response HTTP 响应
     * @param request  筛选条件
     */
    void exportToExcel(jakarta.servlet.http.HttpServletResponse response, ShadowMeterPageRequest request);

    /**
     * 按 ID 列表批量导出虚拟电表/水表拓扑关系为 Excel .xlsx 文件。
     *
     * @param response HTTP 响应
     * @param ids      关系ID列表
     */
    void exportByIdsToExcel(jakarta.servlet.http.HttpServletResponse response, java.util.List<Long> ids);
}
