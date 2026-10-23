package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.config.StorageProperties;
import com.competition.energycontrollerplatform.dto.FileUploadResult;
import com.competition.energycontrollerplatform.entity.FileRecord;
import com.competition.energycontrollerplatform.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件管理 Controller。
 * <p>
 * 提供文件上传、下载、预览、信息查询、分页列表和删除的 REST API。
 * 所有操作均经过魔数检测（上传时）和存储后端抽象层。
 */
@Slf4j
@Tag(name = "文件管理", description = "文件上传、下载、预览、查询、删除接口")
@RestController
@RequestMapping("/api/v1/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final StorageProperties storageProperties;

    // ──────────────────────── 上传 ────────────────────────

    @Operation(
            summary = "文件上传",
            description = "上传任意文件（图片/文档/Excel等）。服务端通过魔数检测真实MIME类型，"
                    + "不支持的文件类型会被拦截。支持关联业务模块和业务ID。"
    )
    @Log(title = "文件管理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public Result<FileUploadResult> upload(
            @Parameter(description = "文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务模块（report / QR / bill / contract / meter-photo）")
            @RequestParam(required = false) String module,
            @Parameter(description = "关联业务ID")
            @RequestParam(required = false) Long refId) {
        FileUploadResult result = fileService.upload(file, module, refId);
        // 本地存储：动态拼接完整访问 URL
        if (result.getAccessUrl() == null) {
            String localUrl = buildLocalUrl(result.getStoredName());
            result.setAccessUrl(localUrl);
        }
        return Result.success(result);
    }

    // ──────────────────────── 下载 ────────────────────────

    @Operation(summary = "文件下载", description = "根据文件ID下载文件（Content-Disposition: attachment）")
    @Log(title = "文件管理", businessType = BusinessType.QUERY)
    @GetMapping("/download/{id}")
    public void download(
            @Parameter(description = "文件ID", required = true) @PathVariable Long id,
            HttpServletResponse response) {
        FileRecord record = fileService.getById(id);
        setResponseHeaders(response, record, false);
        streamToResponse(id, response);
    }

    // ──────────────────────── 预览 ────────────────────────

    @Operation(summary = "文件预览/在线查看", description = "根据文件ID在线预览（Content-Disposition: inline），"
            + "浏览器可直接渲染图片/PDF等类型")
    @GetMapping("/preview/{id}")
    public void preview(
            @Parameter(description = "文件ID", required = true) @PathVariable Long id,
            HttpServletResponse response) {
        FileRecord record = fileService.getById(id);
        setResponseHeaders(response, record, true);
        streamToResponse(id, response);
    }

    // ──────────────────────── 信息 ────────────────────────

    @Operation(summary = "获取文件信息", description = "获取文件元数据（不含文件内容）")
    @GetMapping("/info/{id}")
    public Result<FileUploadResult> info(
            @Parameter(description = "文件ID", required = true) @PathVariable Long id) {
        FileRecord record = fileService.getById(id);
        FileUploadResult result = FileUploadResult.from(record);
        // 本地存储：动态拼接完整 URL
        if (result.getAccessUrl() == null) {
            result.setAccessUrl(buildLocalUrl(result.getStoredName()));
        }
        return Result.success(result);
    }

    // ──────────────────────── 分页列表 ────────────────────────

    @Operation(summary = "文件列表分页查询", description = "按模块/关联业务ID分页查询文件记录")
    @GetMapping("/page")
    public Result<IPage<FileRecord>> page(
            @Parameter(description = "业务模块") @RequestParam(required = false) String module,
            @Parameter(description = "关联业务ID") @RequestParam(required = false) Long refId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
        IPage<FileRecord> page = fileService.listPage(module, refId, pageNum, pageSize);
        return Result.success(page);
    }

    // ──────────────────────── 删除 ────────────────────────

    @Operation(summary = "删除文件", description = "删除文件（物理删除存储对象 + 逻辑删除数据库记录）")
    @Log(title = "文件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "文件ID", required = true) @PathVariable Long id) {
        fileService.delete(id);
        return Result.success("文件已删除");
    }

    // ──────────────────────── 私有辅助 ────────────────────────

    /**
     * 设置 HTTP 响应头（Content-Type、Content-Disposition、文件名编码）。
     */
    private void setResponseHeaders(HttpServletResponse response, FileRecord record, boolean inline) {
        String contentType = record.getMimeType() != null
                ? record.getMimeType()
                : "application/octet-stream";
        response.setContentType(contentType);
        String disposition = inline ? "inline" : "attachment";
        String encodedName = URLEncoder.encode(record.getOriginalName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                disposition + "; filename*=UTF-8''" + encodedName);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }

    /**
     * 将文件内容流式写入 HTTP 响应。
     */
    private void streamToResponse(Long id, HttpServletResponse response) {
        try (InputStream inputStream = fileService.download(id);
             OutputStream outputStream = response.getOutputStream()) {
            StreamUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            log.error("[文件下载] 失败: id={}", id, e);
            // 此时响应头可能已发送，无法返回 JSON；仅记录日志
        }
    }

    /**
     * 为本地存储文件构建完整的 HTTP 访问 URL。
     */
    private String buildLocalUrl(String storedName) {
        String urlPrefix = storageProperties.getLocal().getUrlPrefix();
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(urlPrefix)
                .path("/")
                .path(storedName)
                .toUriString();
    }
}
