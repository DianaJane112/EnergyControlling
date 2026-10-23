package com.competition.energycontrollerplatform.service;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文件存储后端工厂。
 * <p>
 * 利用 Spring 的 {@code Map<String, FileStorageService>} 自动注入机制，
 * 将所有实现了 {@link FileStorageService} 的 Bean 按名称收集到 Map 中，
 * 运行时根据 {@code storage.active} 配置动态选择激活的后端。
 * <p>
 * <b>扩展新存储后端只需两步：</b>
 * <ol>
 *   <li>创建实现类，标注 {@code @Service("xxxFileStorageService")}</li>
 *   <li>在 {@code application.yml} 中添加对应的配置段</li>
 * </ol>
 * 无需修改本类。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageFactory {

    private final StorageProperties storageProperties;
    private final Map<String, FileStorageService> storageServiceMap;

    private FileStorageService activeStorage;

    @PostConstruct
    public void init() {
        String active = storageProperties.getActive();
        String beanName = active.toLowerCase() + "FileStorageService";
        this.activeStorage = storageServiceMap.get(beanName);
        if (this.activeStorage == null) {
            throw new IllegalStateException(
                    "未找到存储后端 Bean: " + beanName
                    + "（storage.active=" + active + "）"
                    + "，当前可用后端: " + storageServiceMap.keySet()
                    + "。请检查 application.yml 中的 storage.active 配置");
        }
        log.info("[存储工厂] 当前激活后端: {} → bean: {}", active, beanName);
    }

    /**
     * 获取当前激活的存储后端。
     */
    public FileStorageService getStorage() {
        return activeStorage;
    }

    /**
     * 按存储类型编码获取指定后端（用于跨后端迁移/备份场景）。
     *
     * @param storageType 存储类型编码（如 "local", "oss", "minio"）
     * @return 对应的存储后端
     * @throws BusinessException 如果指定的类型未注册
     */
    public FileStorageService getStorage(String storageType) {
        if (storageType == null || storageType.isBlank()) {
            return activeStorage;
        }
        String beanName = storageType.toLowerCase() + "FileStorageService";
        FileStorageService service = storageServiceMap.get(beanName);
        if (service == null) {
            throw BusinessException.of(400, "不支持的存储类型: " + storageType
                    + "，可用类型: " + storageServiceMap.keySet());
        }
        return service;
    }
}
