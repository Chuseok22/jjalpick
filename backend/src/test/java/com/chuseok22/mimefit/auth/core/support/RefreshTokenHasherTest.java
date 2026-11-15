package com.chuseok22.mimefit.auth.core.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenHasherTest {

    @Test
    void hash_sameInput_sameOutput() {
        assertThat(RefreshTokenHasher.hash("token")).isEqualTo(RefreshTokenHasher.hash("token"));
    }

    @Test
    void hash_knownVector() {
        assertThat(RefreshTokenHasher.hash("abc"))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void hash_length64() {
        assertThat(RefreshTokenHasher.hash("any-refresh-token")).hasSize(64);
    }

}
