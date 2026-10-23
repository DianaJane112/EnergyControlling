package com.competition.energycontrollerplatform.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.miniapp.dto.InvoiceVO;
import com.competition.energycontrollerplatform.miniapp.entity.Invoice;
import com.competition.energycontrollerplatform.miniapp.mapper.InvoiceMapper;
import com.competition.energycontrollerplatform.miniapp.service.MiniAppInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 小程序票据服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiniAppInvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice>
        implements MiniAppInvoiceService {

    @Override
    public IPage<InvoiceVO> getInvoiceList(Long customerId, Integer pageNum, Integer pageSize, String invoiceStatus) {
        Page<Invoice> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<Invoice>()
                .eq(Invoice::getCustomerId, customerId)
                .orderByDesc(Invoice::getCreateTime);

        if (invoiceStatus != null && !invoiceStatus.isBlank()) {
            wrapper.eq(Invoice::getInvoiceStatus, invoiceStatus.toUpperCase());
        }

        IPage<Invoice> resultPage = page(page, wrapper);

        List<InvoiceVO> records = resultPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        IPage<InvoiceVO> voPage = new Page<>(pageNum, pageSize, resultPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public InvoiceVO getInvoiceDetail(Long invoiceId, Long customerId) {
        Invoice invoice = getById(invoiceId);
        if (invoice == null) {
            throw new BusinessException(404, "票据不存在");
        }
        if (!customerId.equals(invoice.getCustomerId())) {
            throw new BusinessException(403, "无权查看该票据");
        }
        return toVO(invoice);
    }

    // ────────────────── 私有方法 ──────────────────

    private InvoiceVO toVO(Invoice invoice) {
        return InvoiceVO.builder()
                .id(invoice.getId())
                .invoiceNo(invoice.getInvoiceNo())
                .invoiceType(invoice.getInvoiceType())
                .amount(invoice.getAmount())
                .invoiceStatus(invoice.getInvoiceStatus())
                .issueTime(invoice.getIssueTime())
                .createTime(invoice.getCreateTime())
                .remark(invoice.getRemark())
                .build();
    }
}
