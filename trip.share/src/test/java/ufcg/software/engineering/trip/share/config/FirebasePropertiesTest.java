package ufcg.software.engineering.trip.share.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class FirebasePropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class)
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withInitializer(context -> context.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME));

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FirebaseProperties.class)
    static class PropertiesConfig {
    }

    @Test
    void bind_withoutEnvironmentVariable_shouldUseLocalCredentialPath() {
        runner.run(context -> assertThat(context.getBean(FirebaseProperties.class).keyPath())
                .isEqualTo("file:../.secrets/firebase.json"));
    }

    @Test
    void bind_environmentVariable_shouldResolveExternalCredentialPath() {
        runner.withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                new SystemEnvironmentPropertySource("test-env", Map.of("FIREBASE_KEY_PATH", "file:/run/secrets/firebase.json"))))
                .run(context -> assertThat(context.getBean(FirebaseProperties.class).keyPath())
                        .isEqualTo("file:/run/secrets/firebase.json"));
    }

    @Test
    void bind_blankCredentialPath_shouldFailStartup() {
        runner.withPropertyValues("app.firebase.key-path=")
                .run(context -> assertThat(context).hasFailed());
    }
}
