package ufcg.software.engineering.trip.share.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(Duration sessionTtl) {
    public AuthProperties {
        if (sessionTtl == null || sessionTtl.isNegative() || sessionTtl.isZero()) {
            throw new IllegalArgumentException("app.auth.session-ttl deve ser uma duração positiva.");
        }
    }
}
