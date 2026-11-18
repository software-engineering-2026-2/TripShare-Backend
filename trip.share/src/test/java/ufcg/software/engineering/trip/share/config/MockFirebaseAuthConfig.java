package ufcg.software.engineering.trip.share.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("test")
class MockFirebaseAuthConfig {
    @Bean
    FirebaseAuth firebaseAuthForTests() throws Exception {
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        when(firebaseAuth.createSessionCookie(anyString(), any())).thenReturn("mock-session-cookie");
        return firebaseAuth;
    }
}
