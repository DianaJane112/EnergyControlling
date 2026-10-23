package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.CurvePointVO;
import com.competition.energycontrollerplatform.dto.MeterReadingPageRequest;
import com.competition.energycontrollerplatform.dto.MeterReadingVO;

import java.util.List;

/**
 * 表计实时读数服务接口
 */
public interface MeterReadingService {

    /**
     * 分页查询历史读数
     */
    IPage<MeterReadingVO> listPage(MeterReadingPageRequest request);

    /**
     * 获取最近24小时曲线数据（电压/电流/功率/电量）
     */
    List<CurvePointVO> getCurve(Long meterId);
}
