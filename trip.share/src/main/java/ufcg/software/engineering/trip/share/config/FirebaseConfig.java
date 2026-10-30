package ufcg.software.engineering.trip.share.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.IOException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@Configuration
@Profile("!test")
@EnableConfigurationProperties(FirebaseProperties.class)

public class FirebaseConfig {
    @Bean(destroyMethod = "delete")
    public FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        String keyPath = properties.keyPath();

        Resource resource = keyPath.startsWith("classpath:")
                ? new ClassPathResource(keyPath.substring("classpath:".length()))
                : new FileSystemResource(keyPath);

        try (var input = resource.getInputStream()) {
            var options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(input))
                    .build();

            return FirebaseApp.initializeApp(options, "tripshare");
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }
}
