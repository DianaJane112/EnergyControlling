package com.competition.energycontrollerplatform.service.impl;
import com.competition.energycontrollerplatform.entity.MeterOperationTrace;
import com.competition.energycontrollerplatform.mapper.MeterOperationTraceMapper;
import com.competition.energycontrollerplatform.service.MeterOperationTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
@Service @RequiredArgsConstructor
public class MeterOperationTraceServiceImpl implements MeterOperationTraceService {
  private final MeterOperationTraceMapper mapper;
  public void record(MeterOperationTrace trace) {
    if (trace.getOperationTime()==null) trace.setOperationTime(LocalDateTime.now());
    if (trace.getCreateTime()==null) trace.setCreateTime(LocalDateTime.now());
    if (trace.getResultStatus()==null) trace.setResultStatus("SUCCESS");
    if (trace.getOperationTerminal()==null) trace.setOperationTerminal("API");
    mapper.insert(trace);
  }
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFailure(MeterOperationTrace trace) {
    trace.setResultStatus("FAILED"); if (trace.getOperationTerminal()==null) trace.setOperationTerminal("SYSTEM");
    if (trace.getOperationTime()==null) trace.setOperationTime(LocalDateTime.now()); if (trace.getCreateTime()==null) trace.setCreateTime(LocalDateTime.now()); mapper.insert(trace);
  }
}
