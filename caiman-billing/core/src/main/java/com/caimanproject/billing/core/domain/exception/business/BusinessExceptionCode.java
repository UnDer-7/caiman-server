package com.caimanproject.billing.core.domain.exception.business;

import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessExceptionCode implements ExceptionCode {
    DEBTOR_NOT_FOUND("001", "One or more of the informed debtors were not found") {
        @Override
        public CaimanException createException(final String detail) {
            return new DebtorNotFoundException(this, detail);
        }

        @Override
        public CaimanException createException(final String detail, final Throwable originalCause) {
            return new DebtorNotFoundException(this, detail, originalCause);
        }

        @Override
        public CaimanException createException(final Throwable originalCause) {
            return new DebtorNotFoundException(this, originalCause);
        }

        @Override
        public CaimanException createException() {
            return new DebtorNotFoundException(this);
        }
    },
    DUPLICATE_CHARGE_PLAN_MEMBER_BY_DEBTOR_ID("002", "Duplicate debtor IDs found among the provided charge plan members") {
        @Override
        public CaimanException createException(final String detail) {
            return new DuplicateChargePlanMemberByDebtorIdBusinessException(this, detail);
        }

        @Override
        public CaimanException createException(final String detail, final Throwable originalCause) {
            return new DuplicateChargePlanMemberByDebtorIdBusinessException(this, detail, originalCause);
        }

        @Override
        public CaimanException createException(final Throwable originalCause) {
            return new DuplicateChargePlanMemberByDebtorIdBusinessException(this, originalCause);
        }

        @Override
        public CaimanException createException() {
            return new DuplicateChargePlanMemberByDebtorIdBusinessException(this);
        }
    },
    INVALID_ROTATION_ORDER("003", "Invalid rotation order found") {
        @Override
        public CaimanException createException(final String detail) {
            return new InvalidRotationOrderBusinessException(this, detail);
        }

        @Override
        public CaimanException createException(final String detail, final Throwable originalCause) {
            return new InvalidRotationOrderBusinessException(this, detail, originalCause);
        }

        @Override
        public CaimanException createException(final Throwable originalCause) {
            return new InvalidRotationOrderBusinessException(this, originalCause);
        }

        @Override
        public CaimanException createException() {
            return new InvalidRotationOrderBusinessException(this);
        }
    };

    private final String code;
    private final String message;

    @Override
    public ModulePrefix getModulePrefix() {
        return ModulePrefix.DEBTOR_BUSINESS;
    }
}
