package com.nbbang.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class AccountCipherTests {
    private final AccountCipher cipher = new AccountCipher("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
    @Test void encryptsWithRandomIvAndDecrypts() {
        String first = cipher.encrypt("123456789012");
        String second = cipher.encrypt("123456789012");
        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("123456789012");
        assertThat(cipher.decrypt(second)).isEqualTo("123456789012");
    }
}
