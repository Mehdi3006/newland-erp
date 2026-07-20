package com.newland.erp.enterprise.domain;

public record BranchCode(String value) {
    public BranchCode {
        value = TextValue.businessCode(value, "branchCode");
    }
}
