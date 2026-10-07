package com.example.demo.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CabinetTenantResolverTests {
    private final CabinetTenantResolver resolver = new CabinetTenantResolver();

    @AfterEach
    void clear() {
        CabinetContext.clear();
    }

    @Test
    void sessionWorksForTheCabinetOfTheCaller() {
        CabinetContext.set(7L);

        assertEquals(7L, resolver.resolveCurrentTenantIdentifier());
        assertTrue(CabinetContext.isCurrent(7L));
        assertFalse(CabinetContext.isCurrent(8L));
    }

    @Test
    void withoutCabinetTheResolverFailsClosedOnAnIdNoRowHas() {
        assertEquals(CabinetTenantResolver.NO_CABINET, resolver.resolveCurrentTenantIdentifier());
        assertThrows(IllegalStateException.class, CabinetContext::require);
    }
}
