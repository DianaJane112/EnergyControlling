package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.BatchOperationResponse;
import com.competition.energycontrollerplatform.dto.BillCreateRequest;
import com.competition.energycontrollerplatform.dto.BillPageRequest;
import com.competition.energycontrollerplatform.entity.Bill;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 账单记录服务接口。
 */
public interface BillService {

    /** 分页查询账单列表 */
    IPage<Bill> listPage(BillPageRequest request);

    /** 查询账单详情 */
    Bill getById(Long id);

    /** 新增账单 */
    Bill create(BillCreateRequest request);

    /** 批量新增账单 */
    BatchOperationResponse batchCreate(List<BillCreateRequest> requests);

    /** 编辑账单（含支付状态变更钩子：待审核→已支付自动执行充值到账） */
    Bill update(Long id, BillCreateRequest request);

    /** 单条逻辑删除 */
    void delete(Long id);

    /** 批量逻辑删除（兼容 {"ids":[...]} 和 [...] 两种 JSON 格式） */
    BatchOperationResponse batchDelete(String rawBody);

    /** 按筛选条件导出账单列表 Excel */
    void exportList(HttpServletResponse response, BillPageRequest request) throws IOException;

    /** 下载批量导入模板 Excel */
    void downloadTemplate(HttpServletResponse response) throws IOException;
}
