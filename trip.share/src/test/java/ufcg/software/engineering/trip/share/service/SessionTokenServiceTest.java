package ufcg.software.engineering.trip.share.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class SessionTokenServiceTest {
    private final SessionTokenService tokens = new SessionTokenService();

    @Test
    void generate_shouldContain32RandomBytesAndUseUrlSafeEncoding() {
        String first = tokens.generate();
        assertThat(first).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(tokens.generate());
        assertThat(Base64.getUrlDecoder().decode(first)).hasSize(32);
    }

    @Test
    void hash_shouldUseKnownSha256Vector() {
        assertThat(tokens.hash("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
