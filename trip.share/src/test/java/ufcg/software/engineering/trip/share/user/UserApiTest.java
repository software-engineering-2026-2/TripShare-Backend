package ufcg.software.engineering.trip.share.user;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private FirebaseTokenService tokenService;

    @BeforeEach
    void prepare() throws Exception {
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM auth");
        when(tokenService.validate("valid-token"))
                .thenReturn(new FirebaseIdentity("firebase-uid", "user@example.invalid"));
        register().andExpect(status().isCreated());
    }

    private ResultActions request(String endpoint, String body) throws Exception {
        return mvc.perform(post("/auth/" + endpoint).header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions patchMe(String body) throws Exception {
        return mvc.perform(patch("/users/me").header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions register() throws Exception {
        return request("register", """
                {"uuid":"firebase-uid","username":"traveler","name":"Test User",
                 "phone":"+5583999999999","bio":"Traveler","hometown":"Campina Grande"}
                """);
    }

    @Test
    void updateProfile() throws Exception {
        patchMe("{\"name\": \"Novo Nome\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novo Nome"))
                .andExpect(jsonPath("$.hometown").value("Campina Grande"));
    }




}
