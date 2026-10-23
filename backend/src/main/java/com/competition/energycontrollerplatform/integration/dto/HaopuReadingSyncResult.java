package com.competition.energycontrollerplatform.integration.dto;

/** Counts refer to readings, not distinct physical devices. */
public record HaopuReadingSyncResult(int inserted, int rejected, int failed) {}
