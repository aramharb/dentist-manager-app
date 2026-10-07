package com.example.demo.tenant;

/**
 * Holds the cabinet of the authenticated user for the current thread. It is set by the
 * authentication filter before any service runs and always cleared afterwards. Hibernate
 * reads it (see {@link CabinetTenantResolver}) to restrict every query on tenant data to
 * that cabinet.
 */
public final class CabinetContext {
    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private CabinetContext() {
    }

    public static Long current() {
        return CURRENT.get();
    }

    /** The cabinet of the caller, or an error when the caller belongs to none (platform admin). */
    public static Long require() {
        Long cabinetId = CURRENT.get();
        if (cabinetId == null) {
            throw new IllegalStateException("This operation requires a user that belongs to a cabinet.");
        }
        return cabinetId;
    }

    /** True when {@code cabinetId} is the cabinet of the caller. */
    public static boolean isCurrent(Long cabinetId) {
        return java.util.Objects.equals(cabinetId, CURRENT.get());
    }

    public static void set(Long cabinetId) {
        if (cabinetId == null) CURRENT.remove();
        else CURRENT.set(cabinetId);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
