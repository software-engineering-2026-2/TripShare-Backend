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
import org.springframework.core.io.ResourceLoader;

@Configuration
@Profile("!test")
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {
    @Bean(destroyMethod = "delete")
    public FirebaseApp firebaseApp(FirebaseProperties properties, ResourceLoader loader) throws IOException {
        try (var input = loader.getResource(properties.keyPath()).getInputStream()) {
            var options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(input)).build();
            return FirebaseApp.initializeApp(options, "tripshare");
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }
}
