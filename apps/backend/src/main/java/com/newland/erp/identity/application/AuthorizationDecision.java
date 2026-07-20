package com.newland.erp.identity.application;

import com.newland.erp.identity.domain.OrganizationScope;

public record AuthorizationDecision(boolean granted, String capability, OrganizationScope scope, String reason) {
}
