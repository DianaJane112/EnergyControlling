package com.competition.energycontrollerplatform.service;
import com.competition.energycontrollerplatform.entity.MeterOperationTrace;
public interface MeterOperationTraceService { void record(MeterOperationTrace trace); void recordFailure(MeterOperationTrace trace); }
