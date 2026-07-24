package com.caimanproject.billing.core.domain.types;

import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessExceptionCode implements ExceptionCode {
    DEBTOR_NOT_FOUND("001", "The informed debtors were not found"),
    DUPLICATE_CHARGE_PLAN_MEMBER_BY_DEBTOR_ID(
            "002", "Duplicate debtor IDs found among the provided charge plan members"),
    INVALID_ROTATION_ORDER("003", "Invalid rotation order found"),
    INVALID_ENDS_AT("005", "endsAt must be after startsAt"),
    ROTATION_ORDER_NOT_ALLOWED("006", "rotationOrder is not allowed for SPLIT charge plans"),
    ROTATION_ORDER_GAP("007", "rotationOrder values must be sequential starting at 1, with no gaps");

    private final String code;
    private final String message;

    @Override
    public ModulePrefix getModulePrefix() {
        return ModulePrefix.DEBTOR_BUSINESS;
    }
}
