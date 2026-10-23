package com.competition.energycontrollerplatform.miniapp.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.miniapp.dto.InvoiceVO;
import com.competition.energycontrollerplatform.miniapp.security.MiniappSecurityContext;
import com.competition.energycontrollerplatform.miniapp.service.MiniAppInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序 - 票据控制器
 */
@RestController
@RequestMapping("/api/v1/miniapp/invoice")
@RequiredArgsConstructor
@Tag(name = "小程序-票据", description = "票据/收据查询")
public class MiniAppInvoiceController {

    private final MiniAppInvoiceService miniAppInvoiceService;

    @GetMapping("/list")
    @Operation(summary = "票据列表", description = "分页查询票据列表，支持按开票状态筛选")
    public Result<IPage<InvoiceVO>> invoiceList(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "开票状态: NOT_ISSUED/ISSUED，不传=全部") @RequestParam(required = false) String invoiceStatus) {

        Long customerId = MiniappSecurityContext.getCurrentCustomerId();
        IPage<InvoiceVO> page = miniAppInvoiceService.getInvoiceList(customerId, pageNum, pageSize, invoiceStatus);
        return Result.success(page);
    }

    @GetMapping("/{invoiceId}")
    @Operation(summary = "票据详情", description = "查看单条票据的详细信息")
    public Result<InvoiceVO> invoiceDetail(@PathVariable Long invoiceId) {
        Long customerId = MiniappSecurityContext.getCurrentCustomerId();
        InvoiceVO vo = miniAppInvoiceService.getInvoiceDetail(invoiceId, customerId);
        return Result.success(vo);
    }
}
