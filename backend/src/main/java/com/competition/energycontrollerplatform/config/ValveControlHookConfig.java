package com.competition.energycontrollerplatform.config;

import com.competition.energycontrollerplatform.integration.hook.ValveControlHook;
import com.competition.energycontrollerplatform.integration.hook.ValveControlHookComposite;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阀控 Hook 配置。
 * <p>
 * 提供 {@link ValveControlHook} 的真实实现（组合分发模式）：
 * 根据表计厂商类型自动分发到捷先 HTTP 路径或积成 TCP 路径。
 * <p>
 * 如需回退到 No-Op（例如在纯开发环境中不需要真实阀控），
 * 可在此处替换为 {@code new ValveControlHook() {}}。
 */
@Configuration
public class ValveControlHookConfig {

    /**
     * 返回组合分发的 ValveControlHook 实现。
     * <p>
     * Spring 会自动注入 {@link ValveControlHookComposite}（标注了 @Component），
     * 此处直接返回即可。保留此配置类以支持未来按 Profile 条件化选择实现。
     */
    @Bean
    public ValveControlHook valveControlHook(ValveControlHookComposite composite) {
        return composite;
    }
}
