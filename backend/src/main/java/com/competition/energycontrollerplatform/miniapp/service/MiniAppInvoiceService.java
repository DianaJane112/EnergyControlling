package com.competition.energycontrollerplatform.miniapp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.competition.energycontrollerplatform.miniapp.dto.InvoiceVO;
import com.competition.energycontrollerplatform.miniapp.entity.Invoice;

/**
 * 小程序票据服务接口
 */
public interface MiniAppInvoiceService extends IService<Invoice> {

    /**
     * 分页查询票据列表。
     *
     * @param customerId    商户ID
     * @param pageNum       页码
     * @param pageSize      每页条数
     * @param invoiceStatus 开票状态筛选（NOT_ISSUED/ISSUED，null=全部）
     * @return 分页结果
     */
    IPage<InvoiceVO> getInvoiceList(Long customerId, Integer pageNum, Integer pageSize, String invoiceStatus);

    /**
     * 查询票据详情。
     *
     * @param invoiceId  票据ID
     * @param customerId 商户ID（用于权限校验）
     * @return 票据VO
     */
    InvoiceVO getInvoiceDetail(Long invoiceId, Long customerId);
}
