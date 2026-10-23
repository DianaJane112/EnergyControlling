package com.competition.energycontrollerplatform.controller;

import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.miniapp.entity.MiniAppUser;
import com.competition.energycontrollerplatform.service.VirtualMeterViewerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/virtual-meters")
@RequiredArgsConstructor
public class VirtualMeterViewerController {
    private final VirtualMeterViewerService service;

    @GetMapping("/{id}/account-detail")
    public Result<VirtualMeterAccountVO> accountDetail(@PathVariable Long id) {
        return Result.success(service.accountDetail(id));
    }

    @GetMapping("/{id}/viewers")
    public Result<List<VirtualMeterViewerVO>> viewers(@PathVariable Long id) {
        return Result.success(service.list(id));
    }

    @PostMapping("/{id}/viewers")
    public Result<VirtualMeterViewerVO> bind(@PathVariable Long id, @Valid @RequestBody VirtualMeterViewerRequest request) {
        return Result.success(service.bind(id, request));
    }

    @DeleteMapping("/{id}/viewers/{viewerId}")
    public Result<Void> delete(@PathVariable Long id, @PathVariable Long viewerId) {
        service.delete(id, viewerId);
        return Result.success();
    }

    @PutMapping("/{id}/viewers/{viewerId}/status")
    public Result<Void> status(@PathVariable Long id, @PathVariable Long viewerId,
                               @Valid @RequestBody VirtualMeterViewerStatusRequest request) {
        service.updateStatus(id, viewerId, request.getStatus());
        return Result.success();
    }

    @GetMapping("/viewer-candidates")
    public Result<List<MiniAppUser>> candidates(@RequestParam(required = false) String keyword) {
        return Result.success(service.candidates(keyword));
    }
}
