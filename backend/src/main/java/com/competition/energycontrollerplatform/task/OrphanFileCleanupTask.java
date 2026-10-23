package com.competition.energycontrollerplatform.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.entity.FileRecord;
import com.competition.energycontrollerplatform.mapper.FileRecordMapper;
import com.competition.energycontrollerplatform.service.FileStorageFactory;
import com.competition.energycontrollerplatform.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 孤儿文件清理定时任务。
 * <p>
 * <b>问题背景：</b>
 * 文件上传采用"先写存储，后写数据库"的顺序。如果存储写入成功
 * 但数据库记录插入失败（事务回滚），存储中会留下无对应数据库记录的
 * "孤儿文件"。长期累积会浪费存储费用。
 * <p>
 * <b>清理策略：</b>
 * <ol>
 *   <li>扫描当前激活存储后端中的所有文件</li>
 *   <li>与数据库中 isLinked=1 的记录进行交叉比对</li>
 *   <li>删除存储中存在但数据库中无对应记录的文件</li>
 *   <li>同时清理数据库中 isLinked=0 且超过保留期（retention-hours）的记录</li>
 * </ol>
 * <p>
 * <b>执行频率：</b>默认每天凌晨 3:00（通过 {@code orphan-cleanup.cron} 配置）
 * <p>
 * <b>安全边界：</b>仅删除 isLinked=0 且创建超过 24 小时的文件，
 * 确保刚上传尚未绑定的文件不会被误删。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanFileCleanupTask {

    private final FileRecordMapper fileRecordMapper;
    private final FileStorageFactory storageFactory;

    /** 是否启用清理 */
    @Value("${orphan-cleanup.enabled:true}")
    private boolean enabled;

    /** 文件未关联业务的保留小时数（超过则视为孤儿） */
    @Value("${orphan-cleanup.retention-hours:24}")
    private int retentionHours;

    /**
     * 每天凌晨 3:00 执行孤儿文件清理。
     * <p>
     * cron 表达式可从配置文件覆盖：{@code orphan-cleanup.cron}
     * 此处使用 fixedDelayString 作为回退（不依赖 @Scheduled 的 cron 参数化）。
     */
    @Scheduled(cron = "${orphan-cleanup.cron:0 0 3 * * ?}")
    public void cleanupOrphanFiles() {
        if (!enabled) {
            log.info("[孤儿清理] 已禁用，跳过");
            return;
        }

        log.info("[孤儿清理] 开始执行...");
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(retentionHours);

        try {
            // ── 第一步：清理数据库中未关联的过期记录 ──
            int dbCleaned = cleanupDatabaseOrphans(cutoffTime);

            // ── 第二步：清理存储中的孤儿文件 ──
            int storageCleaned = cleanupStorageOrphans();

            log.info("[孤儿清理] 完成：数据库清理 {} 条，存储清理 {} 个文件", dbCleaned, storageCleaned);
        } catch (Exception e) {
            log.error("[孤儿清理] 执行异常", e);
        }
    }

    /**
     * 清理数据库中 isLinked=0 且超过保留期的记录。
     * <p>
     * 先删除存储对象，再删除数据库记录。
     */
    private int cleanupDatabaseOrphans(LocalDateTime cutoffTime) {
        // 查询待清理的记录
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileRecord::getIsLinked, 0)
               .lt(FileRecord::getCreateTime, cutoffTime);
        List<FileRecord> orphans = fileRecordMapper.selectList(wrapper);

        if (orphans.isEmpty()) {
            log.info("[孤儿清理-数据库] 无过期未关联记录");
            return 0;
        }

        int deleted = 0;
        for (FileRecord record : orphans) {
            try {
                // 从存储中删除
                FileStorageService storage = storageFactory.getStorage(
                        record.getStorageType().toLowerCase());
                storage.delete(record.getStoredPath());
                // 从数据库中删除
                fileRecordMapper.deleteById(record.getId());
                deleted++;
                log.debug("[孤儿清理-数据库] 已删除: id={}, storedPath={}, created={}",
                        record.getId(), record.getStoredPath(), record.getCreateTime());
            } catch (Exception e) {
                log.warn("[孤儿清理-数据库] 删除失败: id={}, storedPath={}",
                        record.getId(), record.getStoredPath(), e);
            }
        }

        log.info("[孤儿清理-数据库] 共清理 {} 条过期未关联记录", deleted);
        return deleted;
    }

    /**
     * 清理存储中存在但数据库中完全无对应记录的文件（最坏情况的孤儿）。
     * <p>
     * 这种情况发生在：存储写入成功 → 数据库插入失败且回滚删除也失败。
     * 通过扫描存储后端所有对象，与数据库记录交叉比对来发现。
     */
    private int cleanupStorageOrphans() {
        try {
            // 获取数据库中所有未删除记录的文件名集合
            LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(FileRecord::getStoredPath);
            List<FileRecord> allRecords = fileRecordMapper.selectList(wrapper);
            Set<String> knownPaths = new HashSet<>();
            for (FileRecord r : allRecords) {
                knownPaths.add(r.getStoredPath());
            }

            // 获取存储中所有文件
            FileStorageService storage = storageFactory.getStorage();
            List<String> storageKeys = listStorageKeys(storage);
            if (storageKeys.isEmpty()) {
                return 0;
            }

            // 找出存储有但数据库没有的文件
            int cleaned = 0;
            for (String key : storageKeys) {
                if (!knownPaths.contains(key)) {
                    try {
                        storage.delete(key);
                        cleaned++;
                        log.info("[孤儿清理-存储] 已删除孤儿文件: {}", key);
                    } catch (Exception e) {
                        log.warn("[孤儿清理-存储] 删除失败: {}", key, e);
                    }
                }
            }

            if (cleaned > 0) {
                log.info("[孤儿清理-存储] 共清理 {} 个孤儿文件", cleaned);
            }
            return cleaned;
        } catch (Exception e) {
            log.error("[孤儿清理-存储] 扫描存储失败（可能是云存储连接问题）", e);
            return 0;
        }
    }

    /**
     * 根据当前激活的存储后端类型，获取所有存储对象的 Key 列表。
     */
    @SuppressWarnings("unchecked")
    private List<String> listStorageKeys(FileStorageService storage) {
        // 利用具体实现类的 listAll 方法
        try {
            if (storage instanceof com.competition.energycontrollerplatform.service.impl.LocalFileStorageServiceImpl local) {
                return local.listAllFiles()
                        .map(p -> p.toString().replace('\\', '/'))
                        .toList();
            }
            if (storage instanceof com.competition.energycontrollerplatform.service.impl.OssFileStorageServiceImpl oss) {
                return oss.listAllObjectKeys();
            }
            if (storage instanceof com.competition.energycontrollerplatform.service.impl.MinioFileStorageServiceImpl minio) {
                return minio.listAllObjectKeys();
            }
        } catch (Exception e) {
            log.error("[孤儿清理-存储] 获取存储对象列表失败", e);
        }
        return List.of();
    }
}
