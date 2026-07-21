package com.newland.erp.masterdata.domain;

import java.util.Locale;

public enum MasterDataType {
    ORGANIZATION,
    COMPANY,
    BUSINESS_UNIT,
    BRANCH,
    WAREHOUSE,
    WAREHOUSE_ZONE,
    WAREHOUSE_BIN,
    CURRENCY,
    EXCHANGE_RATE,
    COUNTRY,
    PROVINCE,
    CITY,
    ADDRESS,
    UNIT_OF_MEASURE,
    UNIT_CONVERSION,
    TAX_CATEGORY,
    TAX_RATE,
    PAYMENT_TERMS,
    PAYMENT_METHOD,
    SHIPPING_METHOD,
    INCOTERMS,
    LANGUAGE,
    TIME_ZONE,
    FISCAL_CALENDAR,
    NUMBER_SERIES,
    DOCUMENT_TYPE,
    ATTACHMENT_CATEGORY,
    PRODUCT_CATEGORY,
    PRODUCT_BRAND,
    PRODUCT_FAMILY,
    PRODUCT_ATTRIBUTE,
    ATTRIBUTE_VALUE,
    BARCODE_TYPE;

    public static MasterDataType fromSlug(final String value) {
        return MasterDataType.valueOf(value.replace('-', '_').toUpperCase(Locale.ROOT));
    }

    public String slug() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
