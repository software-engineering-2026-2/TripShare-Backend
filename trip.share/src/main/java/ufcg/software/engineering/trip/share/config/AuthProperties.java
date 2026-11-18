package ufcg.software.engineering.trip.share.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(boolean bearerFallbackEnabled, @Valid Cookie cookie) {
    public AuthProperties {
        if (cookie == null) {
            throw new IllegalArgumentException("app.auth.cookie configuration is required");
        }
    }

    public record Cookie(
            @NotBlank String name,
            @NotNull Duration maxAge,
            boolean secure,
            @NotBlank String sameSite,
            String domain,
            boolean checkRevoked) {
        public Cookie {
            if (maxAge == null || maxAge.compareTo(Duration.ofMinutes(5)) < 0
                    || maxAge.compareTo(Duration.ofDays(14)) > 0) {
                throw new IllegalArgumentException("Session cookie max-age must be between 5 minutes and 14 days");
            }
            String normalizedSameSite = sameSite == null ? "" : sameSite.toLowerCase(Locale.ROOT);
            if (!normalizedSameSite.equals("lax") && !normalizedSameSite.equals("strict")
                    && !normalizedSameSite.equals("none")) {
                throw new IllegalArgumentException("SameSite must be Lax, Strict or None");
            }
            if (normalizedSameSite.equals("none") && !secure) {
                throw new IllegalArgumentException("SameSite=None requires Secure=true");
            }
        }
    }
}
