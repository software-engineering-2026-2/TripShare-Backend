package ufcg.software.engineering.trip.share.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileApiTest {
    private static final String ALICE_TOKEN = "alice-token";
    private static final String BOB_TOKEN = "bob-token";
    private static final String ALICE_UID = "uid-alice";
    private static final String BOB_UID = "uid-bob";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private FirebaseTokenService tokenService;

    @BeforeEach
    void prepare() throws Exception {
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM auth");
        when(tokenService.validate(ALICE_TOKEN))
                .thenReturn(new FirebaseIdentity(ALICE_UID, "alice@example.invalid"));
        when(tokenService.validate(BOB_TOKEN))
                .thenReturn(new FirebaseIdentity(BOB_UID, "bob@example.invalid"));
        register(ALICE_TOKEN, ALICE_UID, "alice", "Alice", "Campina Grande");
        register(BOB_TOKEN, BOB_UID, "bob", "Bob", "Recife");
    }

    private void register(String token, String uid, String username, String name, String hometown)
            throws Exception {
        mvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"uuid":"%s","username":"%s","name":"%s","phone":"+5583999999999",
                                 "bio":"Bio inicial","hometown":"%s"}
                                """.formatted(uid, username, name, hometown)))
                .andExpect(status().isCreated());
    }

    private ResultActions patchMe(String token, String body) throws Exception {
        return mvc.perform(patch("/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions getProfile(String uid, String token) throws Exception {
        var request = get("/users/" + uid);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request);
    }

    private ProfileSnapshot snapshot(String uid) {
        return jdbc.queryForObject("""
                SELECT CAST(u.id AS varchar), CAST(u.auth_id AS varchar), a.uuid, u.username,
                       u.name, u.email, u.phone, u.bio, u.hometown, u.role, u.status,
                       CAST(u.created_at AS varchar)
                FROM users u JOIN auth a ON a.id = u.auth_id
                WHERE a.uuid = ?
                """, (rs, rowNum) -> new ProfileSnapshot(
                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12)), uid);
    }

    private record ProfileSnapshot(String id, String authId, String uuid, String username,
            String name, String email, String phone, String bio, String hometown,
            String role, String status, String createdAt) {
    }

    @Test
    void updateProfile_allEditableFields_shouldPersistAndKeepProtectedFields() throws Exception {
        var aliceBefore = snapshot(ALICE_UID);
        var bobBefore = snapshot(BOB_UID);

        patchMe(ALICE_TOKEN, """
                {"name":"Alice Souza","phone":"+5583988887777","bio":"Mochileira",
                 "hometown":"João Pessoa"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceBefore.id()))
                .andExpect(jsonPath("$.authId").value(aliceBefore.authId()))
                .andExpect(jsonPath("$.uuid").value(ALICE_UID))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.name").value("Alice Souza"))
                .andExpect(jsonPath("$.email").value("alice@example.invalid"))
                .andExpect(jsonPath("$.phone").value("+5583988887777"))
                .andExpect(jsonPath("$.bio").value("Mochileira"))
                .andExpect(jsonPath("$.hometown").value("João Pessoa"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        var aliceAfter = snapshot(ALICE_UID);
        assertThat(aliceAfter.name()).isEqualTo("Alice Souza");
        assertThat(aliceAfter.phone()).isEqualTo("+5583988887777");
        assertThat(aliceAfter.bio()).isEqualTo("Mochileira");
        assertThat(aliceAfter.hometown()).isEqualTo("João Pessoa");
        assertThat(aliceAfter.id()).isEqualTo(aliceBefore.id());
        assertThat(aliceAfter.authId()).isEqualTo(aliceBefore.authId());
        assertThat(aliceAfter.uuid()).isEqualTo(aliceBefore.uuid());
        assertThat(aliceAfter.username()).isEqualTo(aliceBefore.username());
        assertThat(aliceAfter.email()).isEqualTo(aliceBefore.email());
        assertThat(aliceAfter.role()).isEqualTo(aliceBefore.role());
        assertThat(aliceAfter.status()).isEqualTo(aliceBefore.status());
        assertThat(aliceAfter.createdAt()).isEqualTo(aliceBefore.createdAt());
        assertThat(snapshot(BOB_UID)).isEqualTo(bobBefore);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void updateProfile_blankName_shouldReturn400AndKeepData(String name) throws Exception {
        assertRejected("{\"name\":\"" + name + "\"}");
    }

    @ParameterizedTest
    @CsvSource({"name,121", "bio,501", "hometown,121"})
    void updateProfile_exceedLimit_shouldReturn400AndKeepData(String field, int size) throws Exception {
        assertRejected("{\"" + field + "\":\"" + "a".repeat(size) + "\"}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "++5511", "+0123456789", "+55 83 99999-9999",
            "+1234567890123456"})
    void updateProfile_invalidPhone_shouldReturn400AndKeepData(String phone) throws Exception {
        assertRejected("{\"phone\":\"" + phone + "\"}");
    }

    private void assertRejected(String body) throws Exception {
        var aliceBefore = snapshot(ALICE_UID);
        var bobBefore = snapshot(BOB_UID);

        patchMe(ALICE_TOKEN, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request body"));

        assertThat(snapshot(ALICE_UID)).isEqualTo(aliceBefore);
        assertThat(snapshot(BOB_UID)).isEqualTo(bobBefore);
    }

    @Test
    void updateProfile_anotherUserIdentifiers_shouldNotEditAnotherProfile() throws Exception {
        var aliceBefore = snapshot(ALICE_UID);
        var bobBefore = snapshot(BOB_UID);

        patchMe(ALICE_TOKEN, """
                {"name":"Alice Alterada","id":"%s","uuid":"uid-bob","username":"bob",
                 "email":"bob@example.invalid","role":"ADMIN","status":"SUSPENDED"}
                """.formatted(bobBefore.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceBefore.id()))
                .andExpect(jsonPath("$.uuid").value(ALICE_UID))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.invalid"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        var aliceAfter = snapshot(ALICE_UID);
        assertThat(aliceAfter.name()).isEqualTo("Alice Alterada");
        assertThat(aliceAfter.id()).isEqualTo(aliceBefore.id());
        assertThat(aliceAfter.authId()).isEqualTo(aliceBefore.authId());
        assertThat(aliceAfter.uuid()).isEqualTo(ALICE_UID);
        assertThat(aliceAfter.username()).isEqualTo("alice");
        assertThat(aliceAfter.email()).isEqualTo("alice@example.invalid");
        assertThat(aliceAfter.role()).isEqualTo("USER");
        assertThat(aliceAfter.status()).isEqualTo("ACTIVE");
        assertThat(snapshot(BOB_UID)).isEqualTo(bobBefore);

        mvc.perform(patch("/users/" + bobBefore.id())
                        .header("Authorization", "Bearer " + ALICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\"}"))
                .andExpect(status().isMethodNotAllowed());

        assertThat(snapshot(BOB_UID)).isEqualTo(bobBefore);
        assertThat(snapshot(ALICE_UID)).isEqualTo(aliceAfter);
    }

    @Test
    void getPublicProfile_registeredUser_shouldReturnOnlyPublicFields() throws Exception {
        var bob = snapshot(BOB_UID);

        // bob.id() to get internal UUID instead of FireBase BOB_UID
        var body = getProfile(bob.id(), ALICE_TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.name").value("Bob"))
                .andExpect(jsonPath("$.bio").value("Bio inicial"))
                .andExpect(jsonPath("$.hometown").value("Recife"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.authId").doesNotExist())
                .andExpect(jsonPath("$.uuid").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("bob@example.invalid", "+5583999999999");
    }

    @Test
    void getPublicProfile_afterUpdate_shouldReturnUpdatedPublicFields() throws Exception {
        patchMe(ALICE_TOKEN, """
                {"name":"Alice Souza","bio":"Mochileira","hometown":"João Pessoa"}
                """).andExpect(status().isOk());

        var alice = snapshot(ALICE_UID);
        var body = getProfile(alice.id(), BOB_TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.name").value("Alice Souza"))
                .andExpect(jsonPath("$.bio").value("Mochileira"))
                .andExpect(jsonPath("$.hometown").value("João Pessoa"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("alice@example.invalid");
    }

    @Test
    void getPublicProfile_withoutToken_shouldReturn401() throws Exception {
        var bob = snapshot(BOB_UID);
        getProfile(bob.id(), null)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Firebase Bearer token is required"));
    }

    @Test
    void getPublicProfile_unknownId_shouldReturn404() throws Exception {
        getProfile("00000000-0000-0000-0000-000000000000", ALICE_TOKEN)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User profile not found"));
    }

    @Test
    void getPublicProfile_suspendedUser_shouldReturn403WithoutPublicData() throws Exception {
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE username = 'bob'");

        var bob = snapshot(BOB_UID);
        getProfile(bob.id(), ALICE_TOKEN)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("User is suspended"))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }
}
