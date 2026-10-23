package com.competition.energycontrollerplatform.mapper;

import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.testbase.TestMockBeans;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

/**
 * MeterMapper 集成测试 —— 验证批量 SQL 和逻辑删除/恢复。
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestMockBeans.class)
@Transactional
@DisplayName("MeterMapper - 批量操作 & 逻辑删除")
class MeterMapperTest {

    @Autowired private MeterMapper meterMapper;

    private Long insertMeter(String dbNo, Integer meterType, Integer status) {
        Meter m = new Meter();
        m.setDbNo(dbNo);
        m.setMeterType(meterType);
        m.setStatus(status);
        m.setOnline(1);
        m.setVender("chenman");
        m.setName("测试表计");
        meterMapper.insert(m);
        return m.getId();
    }

    private Long insertMeterExactly(String dbNo, Integer meterType, String name, Long customerId) {
        Meter m = new Meter();
        m.setDbNo(dbNo);
        m.setMeterType(meterType);
        m.setName(name);
        m.setCustomerId(customerId);
        m.setStatus(1);
        m.setOnline(1);
        m.setVender("chenman");
        meterMapper.insert(m);
        return m.getId();
    }

    // ──────────────────────── updateStatus ────────────────────────

    @Test @DisplayName("updateStatus — 更改运行状态")
    void updateStatus() {
        Long id = insertMeter("UT001", 1, 1);
        assertThat(meterMapper.updateStatus(id, 0)).isEqualTo(1);
        Meter m = meterMapper.selectById(id);
        assertThat(m.getStatus()).isEqualTo(0);
    }

    // ──────────────────────── logicDeleteById / restoreById ────────────────────────

    @Test @DisplayName("logicDeleteById → 标记删除")
    void logicDeleteById() {
        Long id = insertMeter("UT002", 1, 1);
        assertThat(meterMapper.logicDeleteById(id)).isEqualTo(1);
        assertThat(meterMapper.selectById(id)).isNull();
    }

    @Test @DisplayName("restoreById → 恢复")
    void restoreById() {
        Long id = insertMeter("UT003", 1, 1);
        meterMapper.logicDeleteById(id);
        assertThat(meterMapper.restoreById(id)).isEqualTo(1);
        Meter m = meterMapper.selectById(id);
        assertThat(m).isNotNull();
        assertThat(m.getIsDeleted()).isEqualTo(0);
    }

    @Test @DisplayName("batchRestoreByIds → 批量恢复")
    void batchRestoreByIds() {
        Long id1 = insertMeter("UT004", 1, 1);
        Long id2 = insertMeter("UT005", 1, 1);
        meterMapper.logicDeleteById(id1);
        meterMapper.logicDeleteById(id2);
        assertThat(meterMapper.batchRestoreByIds(Arrays.asList(id1, id2))).isEqualTo(2);
    }

    // ──────────────────────── batchUpdatePrice ────────────────────────

    @Test @DisplayName("batchUpdatePrice — 按ID列表更新")
    void batchUpdatePrice_byIds() {
        Long id1 = insertMeter("UT006", 1, 1);
        Long id2 = insertMeter("UT007", 1, 1);
        BigDecimal newPrice = BigDecimal.valueOf(1.50);
        int rows = meterMapper.batchUpdatePrice(Arrays.asList(id1, id2), 1, "1.50", newPrice, null, null);
        assertThat(rows).isEqualTo(2);
    }

    @Test @DisplayName("batchUpdatePriceGlobal — 全部电表")
    void batchUpdatePrice_global() {
        insertMeter("UT008", 1, 1);
        insertMeter("UT009", 1, 1);
        assertThat(meterMapper.batchUpdatePriceGlobal(1, "1.20", BigDecimal.valueOf(1.20), null, 1)).isEqualTo(2);
    }

    @Test @DisplayName("batchUpdatePriceByCustomer — 按商户")
    void batchUpdatePrice_byCustomer() {
        Long cid = 100L;
        insertMeterExactly("UT010", 1, "A", cid);
        insertMeterExactly("UT011", 1, "B", cid);
        insertMeterExactly("UT012", 1, "C", 200L);  // 另一个商户
        int rows = meterMapper.batchUpdatePriceByCustomer(cid, 1, "2.00", BigDecimal.valueOf(2.00), null, null);
        assertThat(rows).isEqualTo(2);
    }

    // ──────────────────────── updateValveState ────────────────────────

    @Test @DisplayName("updateValveState — 更新阀门状态")
    void updateValveState() {
        Long id = insertMeter("UT013", 1, 1);
        assertThat(meterMapper.updateValveState(id, 0, 2)).isEqualTo(1);
        Meter m = meterMapper.selectById(id);
        assertThat(m.getStatus()).isEqualTo(0);
        assertThat(m.getStateValve()).isEqualTo(2);
    }
}
