package com.newland.erp.enterprise.domain;

public enum LifecycleStatus {
    DRAFT,
    ACTIVE,
    INACTIVE;

    public LifecycleStatus activate() {
        return switch (this) {
            case DRAFT, INACTIVE -> ACTIVE;
            case ACTIVE -> throw new InvalidStateTransitionException("Record is already ACTIVE.");
        };
    }

    public LifecycleStatus deactivate() {
        return switch (this) {
            case ACTIVE -> INACTIVE;
            case DRAFT -> throw new InvalidStateTransitionException("DRAFT records cannot be deactivated.");
            case INACTIVE -> throw new InvalidStateTransitionException("Record is already INACTIVE.");
        };
    }
}
