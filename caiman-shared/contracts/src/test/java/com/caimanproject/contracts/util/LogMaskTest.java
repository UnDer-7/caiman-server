package com.caimanproject.contracts.util;

import com.caimanproject.test.annotation.UnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
class LogMaskTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("emailCases")
    void email(String input, String expected) {
        assertThat(LogMask.email(input)).isEqualTo(expected);
    }

    static Stream<Arguments> emailCases() {
        return Stream.of(
            Arguments.of(null,                  null),
            Arguments.of("",                    ""),
            Arguments.of("notanemail",           "notanemail"),
            Arguments.of("@domain.com",          "@domain.com"),
            Arguments.of("a@b.com",              "*@b.com"),
            Arguments.of("ab@b.com",             "**@b.com"),
            Arguments.of("abc@b.com",            "ab***@b.com"),
            Arguments.of("mateus@gmail.com",     "ma***@gmail.com"),
            Arguments.of("user@sub.domain.org",  "us***@sub.domain.org")
        );
    }
}
