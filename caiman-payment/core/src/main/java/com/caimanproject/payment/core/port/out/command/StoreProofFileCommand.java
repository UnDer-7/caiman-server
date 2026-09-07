package com.caimanproject.payment.core.port.out.command;

import lombok.Builder;

@Builder
public record StoreProofFileCommand(
        byte[] fileContent, String fileContentType, String debtorName, String chargePlanName, int cycleIndex) {}
