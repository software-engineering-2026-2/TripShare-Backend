package ufcg.software.engineering.trip.share.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class FirebasePropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class)
            .withPropertyValues("app.firebase.key-path=${FIREBASE_KEY_PATH:}");

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FirebaseProperties.class)
    static class PropertiesConfig {
    }

    @Test
    void bind_environmentVariable_shouldResolveExternalCredentialPath() {
        runner.withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                new SystemEnvironmentPropertySource("test-env", Map.of("FIREBASE_KEY_PATH", "file:/external/firebase.json"))))
                .run(context -> assertThat(context.getBean(FirebaseProperties.class).keyPath())
                        .isEqualTo("file:/external/firebase.json"));
    }

    @Test
    void bind_blankCredentialPath_shouldFailStartup() {
        runner.withPropertyValues("app.firebase.key-path=")
                .run(context -> assertThat(context).hasFailed());
    }
}
