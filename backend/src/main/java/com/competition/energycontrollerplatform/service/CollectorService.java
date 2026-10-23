package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.*;

import java.util.List;

/**
 * 基站/集中器管理服务接口。
 * <p>
 * <b>权限隔离策略：</b>
 * <ul>
 *   <li>ADMIN 可操作/查询所有集中器，可指定任意 customerId</li>
 *   <li>非 ADMIN 强制将 customerId 置为当前登录用户的 customerId，
 *       且查询时仅返回属于自己的集中器</li>
 * </ul>
 */
public interface CollectorService {

    /**
     * 分页查询集中器列表。
     *
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @param keyword    关键字（匹配 collCode/collName/location）
     * @param customerId 商户ID（NULL=全量，非ADMIN强制覆盖）
     * @param areaCode   区域代码筛选
     */
    IPage<CollectorVO> page(int pageNum, int pageSize, String keyword, Long customerId, String areaCode);

    /**
     * 按ID查询集中器详情。
     */
    CollectorVO getById(Long id);

    /**
     * 新增集中器。
     */
    CollectorVO create(CollectorRequest request);

    /**
     * 编辑集中器。
     */
    CollectorVO update(Long id, CollectorRequest request);

    /**
     * 删除集中器（逻辑删除）。
     */
    void delete(Long id);

    /**
     * 按商户查询集中器下拉列表（启用状态）。
     */
    List<CollectorVO> listByCustomer(Long customerId);

    // ──────────────────────── 统计 ────────────────────────

    /**
     * 集中器统计。
     * <p>返回总数及按区域分组统计。ADMIN 可查全量，非 ADMIN 仅统计本商户。</p>
     *
     * @param customerId 商户ID（NULL=全量，非ADMIN强制覆盖）
     * @param areaCode   区域代码筛选（可选）
     */
    CollectorStatisticsVO statistics(Long customerId, String areaCode);

    // ──────────────────────── 导出 ────────────────────────

    /**
     * 导出全量集中器列表（按筛选条件）。
     * <p>用于 Excel 导出，返回 CollectorExcelVO 列表。</p>
     */
    List<CollectorExcelVO> exportList(String keyword, Long customerId, String areaCode);

    /**
     * 批量导出指定ID的集中器。
     *
     * @param ids 集中器ID列表
     */
    List<CollectorExcelVO> exportBatch(List<Long> ids);

    // ──────────────────────── 电表/水表绑定 ────────────────────────

    /**
     * 查询基站下绑定的电表/水表列表。
     *
     * @param collectorId 集中器ID
     * @param pageNum     页码
     * @param pageSize    每页条数
     * @param keyword     关键字（模糊匹配 stealNo/dbNo/meteraddress/name）
     * @param meterType   表计类型（0=水表, 1=电表, null=全部）
     */
    IPage<CollectorMeterVO> listMeters(Long collectorId, int pageNum, int pageSize,
                                       String keyword, Integer meterType);

    /**
     * 绑定电表/水表到基站。
     * <p>将指定表计的 collectorCode 设为该集中器的 collCode。</p>
     *
     * @param collectorId 集中器ID
     * @param meterId     表计ID
     */
    void bindMeter(Long collectorId, Long meterId);

    /**
     * 解除单个电表/水表与基站的绑定。
     * <p>将该表计的 collectorCode 置为 null。</p>
     *
     * @param collectorId 集中器ID
     * @param meterId     表计ID
     */
    void unbindMeter(Long collectorId, Long meterId);

    /**
     * 解除基站下所有电表/水表的绑定。
     *
     * @param collectorId 集中器ID
     * @return 解除绑定的表计数量
     */
    int unbindAllMeters(Long collectorId);

    // ──────────────────────── 一键扫描绑定 ────────────────────────

    /**
     * 一键扫描并绑定所有在线且未绑定的电表/水表到该基站。
     * <p>
     * 自动发现同区域、同协议、在线且未绑定到任何集中器的表计，
     * 批量绑定到当前集中器。仅 ADMIN 可执行。
     * </p>
     *
     * @param collectorId 集中器ID
     * @return 成功绑定的表计数量
     */
    int bindAllOnlineMeters(Long collectorId);

    // ──────────────────────── 批量导入 ────────────────────────

    /**
     * 批量导入集中器。
     * <p>一次性导入多条集中器记录，单次最多500条。仅 ADMIN 可执行。</p>
     *
     * @param requests 集中器请求列表
     * @return 成功导入的集中器数量
     */
    int batchImport(List<CollectorRequest> requests);
}
