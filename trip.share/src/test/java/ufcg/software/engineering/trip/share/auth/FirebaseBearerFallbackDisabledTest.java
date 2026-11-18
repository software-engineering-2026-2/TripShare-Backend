package ufcg.software.engineering.trip.share.auth;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

@SpringBootTest(properties = "app.auth.bearer-fallback-enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FirebaseBearerFallbackDisabledTest {
    @Autowired MockMvc mvc;
    @MockitoBean FirebaseTokenService tokenService;

    @Test
    void bearerOnlyRequest_returns401WhenFallbackIsDisabled() throws Exception {
        mvc.perform(get("/users/00000000-0000-0000-0000-000000000001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer legacy-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(tokenService);
    }
}
