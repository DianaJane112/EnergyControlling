package com.competition.energycontrollerplatform.miniapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.miniapp.entity.Invoice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 票据 Mapper
 */
@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {
}
