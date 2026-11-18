package ufcg.software.engineering.trip.share.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ufcg.software.engineering.trip.share.service.AuthenticatedUser;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

@SpringBootTest(properties = {
        "app.auth.cookie.secure=true",
        "app.auth.cookie.same-site=None"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FirebaseSecureCookieTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean FirebaseTokenService tokenService;
    @MockitoBean FirebaseAuth firebaseAuth;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM auth");
        when(tokenService.validate("id-token"))
                .thenReturn(new AuthenticatedUser("firebase-uid", "traveler@example.invalid"));
        when(firebaseAuth.createSessionCookie(org.mockito.ArgumentMatchers.eq("id-token"), any()))
                .thenReturn("session-cookie");
    }

    @Test
    void registerWithCrossSiteConfiguration_setsSecureAndSameSiteNone() throws Exception {
        mvc.perform(post("/auth/register")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer id-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"uuid":"firebase-uid","username":"traveler","name":"Traveler"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("Secure"),
                                org.hamcrest.Matchers.containsString("SameSite=None"),
                                org.hamcrest.Matchers.containsString("HttpOnly"))));
    }
}
