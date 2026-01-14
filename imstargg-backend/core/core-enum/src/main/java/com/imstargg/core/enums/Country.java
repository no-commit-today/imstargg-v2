package com.imstargg.core.enums;

public enum Country {

    GLOBAL("global"),
    KOREA("kr"),
    ;

    private final String code;

    Country(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
