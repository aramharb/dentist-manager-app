package com.example.demo.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Tells Hibernate which cabinet the current session works for. Without a cabinet (login,
 * platform admin, background threads) the resolver answers {@link #NO_CABINET}, an id no
 * row can have: tenant data is then invisible and cannot be written (fail closed).
 */
public class CabinetTenantResolver implements CurrentTenantIdentifierResolver<Long> {
    public static final Long NO_CABINET = 0L;

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long cabinetId = CabinetContext.current();
        return cabinetId == null ? NO_CABINET : cabinetId;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
