package ufcg.software.engineering.trip.share.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ufcg.software.engineering.trip.share.dto.RegisterRequest;
import ufcg.software.engineering.trip.share.service.AuthService;
import ufcg.software.engineering.trip.share.service.FirebaseIdentity;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthPostgresIT {
    private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        DATABASE.start();
        properties.add("spring.datasource.url", DATABASE::getJdbcUrl);
        properties.add("spring.datasource.username", DATABASE::getUsername);
        properties.add("spring.datasource.password", DATABASE::getPassword);
        properties.add("spring.flyway.enabled", () -> true);
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @AfterAll
    static void stop() {
        DATABASE.stop();
    }

    @Autowired private MockMvc mvc;
    @Autowired private AuthService service;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private FirebaseTokenService tokenService;

    @BeforeEach
    void clearUsers() {
        jdbc.execute("TRUNCATE TABLE users, auth CASCADE");
    }

    private int register(String token, String uid, String username) throws Exception {
        return mvc.perform(post("/auth/register").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"uuid\":\"" + uid + "\",\"username\":\"" + username + "\",\"name\":\"Test\"}"))
                .andReturn().getResponse().getStatus();
    }

    @ParameterizedTest
    @ValueSource(strings = {"uid", "email", "username"})
    void register_concurrentDuplicates_shouldCreateOneProfileWithoutOrphanAuth(String duplicate) throws Exception {
        String secondUid = duplicate.equals("uid") ? "uid-one" : "uid-two";
        String secondEmail = duplicate.equals("email") ? "ONE@EXAMPLE.INVALID" : "two@example.invalid";
        String secondUsername = duplicate.equals("username") ? "TRAVELER" : "second";
        var barrier = new CyclicBarrier(2);
        when(tokenService.validate("one")).thenAnswer(invocation -> {
            barrier.await(15, TimeUnit.SECONDS);
            return new FirebaseIdentity("uid-one", "one@example.invalid");
        });
        when(tokenService.validate("two")).thenAnswer(invocation -> {
            barrier.await(15, TimeUnit.SECONDS);
            return new FirebaseIdentity(secondUid, secondEmail);
        });
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> tasks = List.of(
                    () -> register("one", "uid-one", "traveler"),
                    () -> register("two", secondUid, secondUsername));
            var results = executor.invokeAll(tasks, 30, TimeUnit.SECONDS);
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(201, 409);
        } finally {
            executor.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users u JOIN auth a ON a.id = u.auth_id", Integer.class)).isEqualTo(1);
    }

    @Test
    void register_userInsertFailure_shouldRollBackAuthInsert() {
        // Bypass HTTP validation to exercise transaction rollback after auth has been persisted.
        var request = new RegisterRequest("uid-one", "x".repeat(51), "Test", null, null, null);
        assertThatThrownBy(() -> service.register(request, new FirebaseIdentity("uid-one", "one@example.invalid")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isZero();
    }

    @Test
    void registerAndLogin_postgresMappings_shouldReturnSameUser() throws Exception {
        when(tokenService.validate("one")).thenReturn(new FirebaseIdentity("firebase-uid", "one@example.invalid"));
        assertThat(register("one", "firebase-uid", "traveler")).isEqualTo(201);
        var response = mvc.perform(post("/auth/login").header("Authorization", "Bearer one")
                .contentType(MediaType.APPLICATION_JSON).content("{\"uuid\":\"firebase-uid\"}"))
                .andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("\"uuid\":\"firebase-uid\"", "\"username\":\"traveler\"");
    }
}
