package com.imstargg.core.enums;

import java.util.Arrays;
import java.util.Optional;

public interface EnumCodeType {

    String getCode();

    String getTitle();

    static <T extends Enum<T> & EnumCodeType> T fromCode(Class<T> enumClass, String code) {
        return findByCode(enumClass, code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No [" + enumClass.getName() + "] with code: " + code));
    }

    static <T extends Enum<T> & EnumCodeType> Optional<T> findByCode(Class<T> enumClass, String code) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.getCode().equals(code))
                .findAny();
    }
}
