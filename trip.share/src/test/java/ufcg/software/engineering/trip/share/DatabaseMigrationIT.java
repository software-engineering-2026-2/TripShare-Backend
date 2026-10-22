package ufcg.software.engineering.trip.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.postgresql.PostgreSQLContainer;

class DatabaseMigrationIT {

    private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:16-alpine");
    private static final String USER = "10000000-0000-0000-0000-000000000001";
    private static final String OTHER_USER = "10000000-0000-0000-0000-000000000002";
    private static final String ADMIN = "10000000-0000-0000-0000-000000000003";
    private static final String DESTINATION = "20000000-0000-0000-0000-000000000001";
    private static final String TRIP = "40000000-0000-0000-0000-000000000001";
    private static final String ITINERARY = "60000000-0000-0000-0000-000000000001";
    private String schema;

    @BeforeAll
    static void startDatabase() {
        // Deliberately fails when Docker is unavailable; this profile must never silently skip.
        DATABASE.start();
    }

    @AfterAll
    static void stopDatabase() {
        DATABASE.stop();
    }

    @BeforeEach
    void createIsolatedSchema() throws SQLException {
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
    }

    private Flyway flyway(String... locations) {
        return Flyway.configure().dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
            .defaultSchema(schema).schemas(schema)
            .locations(locations.length == 0 ? new String[]{"classpath:db/migration"} : locations)
            .validateOnMigrate(true).validateMigrationNaming(true).cleanDisabled(true)
            .baselineOnMigrate(false).outOfOrder(false).ignoreMigrationPatterns(new String[0]).load();
    }

    private Connection connection() throws SQLException {
        var connection = DriverManager.getConnection(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
        try (var statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + schema);
        }
        return connection;
    }

    private void execute(String sql) throws SQLException {
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(String sql) throws SQLException {
        try (var connection = connection(); var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private String value(String sql) throws SQLException {
        try (var connection = connection(); var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private void seed() throws SQLException {
        flyway().migrate();
        try (var connection = connection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("scripts/seeds/demo.sql"));
        }
    }

    private void rejected(String sql) {
        assertThatThrownBy(() -> execute(sql)).isInstanceOf(SQLException.class);
    }

    private Path copyMigrations(Path directory, int lastVersion) throws IOException {
        try (var files = Files.list(Path.of("src/main/resources/db/migration"))) {
            for (var file : files.toList()) {
                if (Integer.parseInt(file.getFileName().toString().substring(1, 2)) <= lastVersion) {
                    Files.copy(file, directory.resolve(file.getFileName()));
                }
            }
        }
        return directory;
    }

    @Test
    void migrate_emptyDatabase_shouldCreateSchemaAndOnlyReferenceData() throws SQLException {
        assertThat(flyway().migrate().migrationsExecuted).isEqualTo(7);
        assertThat(count("SELECT count(*) FROM categories")).isEqualTo(7);
        assertThat(count("SELECT count(*) FROM travel_paces")).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM transport_modes")).isEqualTo(4);
        assertThat(count("SELECT count(*) FROM users")).isZero();
        assertThat(count("SELECT count(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_type = 'BASE TABLE'"))
            .isEqualTo(28); // 27 domain tables plus Flyway history.
        assertThat(flyway().migrate().migrationsExecuted).isZero();
        assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE success")).isEqualTo(7);
    }

    @Test
    void migrate_previousVersion_shouldPreserveExistingData() throws SQLException {
        var earlier = Flyway.configure().configuration(flyway().getConfiguration()).target("2").load();
        earlier.migrate();
        execute("INSERT INTO users(username, name, email) VALUES ('existing', 'Existing User', 'existing@example.invalid')");
        assertThat(flyway().migrate().migrationsExecuted).isEqualTo(5);
        assertThat(value("SELECT name FROM users WHERE username = 'existing'")).isEqualTo("Existing User");
    }

    @Test
    void migrate_concurrentInstances_shouldApplyEachVersionOnce() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> tasks = List.of(() -> flyway().migrate().migrationsExecuted, () -> flyway().migrate().migrationsExecuted);
            var results = executor.invokeAll(tasks);
            assertThat(results.get(0).get() + results.get(1).get()).isEqualTo(7);
            assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE success")).isEqualTo(7);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void validate_modifiedMigration_shouldRejectChecksum(@TempDir Path directory) throws IOException {
        copyMigrations(directory, 6);
        var location = "filesystem:" + directory;
        flyway(location).migrate();
        Files.writeString(directory.resolve("V1__create_identity_and_catalog.sql"), "\nSELECT 1;\n", StandardOpenOption.APPEND);
        assertThatThrownBy(() -> flyway(location).migrate()).isInstanceOf(FlywayException.class).hasMessageContaining("checksum");
    }

    @Test
    void migrate_databaseNewerThanApplication_shouldRejectFutureVersion(@TempDir Path directory) throws IOException {
        flyway().migrate();
        copyMigrations(directory, 5);
        assertThatThrownBy(() -> flyway("filesystem:" + directory).migrate()).isInstanceOf(FlywayException.class);
    }

    @Test
    void springStartup_futureVersion_shouldRejectBeforeJpa(@TempDir Path directory) throws IOException {
        flyway().migrate();
        copyMigrations(directory, 5);
        assertThatThrownBy(() -> {
            try (var context = new SpringApplicationBuilder(Application.class).run(
                    "--spring.main.web-application-type=none", "--spring.docker.compose.enabled=false",
                    "--spring.datasource.url=" + DATABASE.getJdbcUrl(),
                    "--spring.datasource.username=" + DATABASE.getUsername(),
                    "--spring.datasource.password=" + DATABASE.getPassword(),
                    "--spring.flyway.default-schema=" + schema, "--spring.flyway.schemas=" + schema,
                    "--spring.flyway.locations=filesystem:" + directory)) {
                assertThat(context.isActive()).isFalse();
            }
        }).hasRootCauseInstanceOf(FlywayException.class);
    }

    @Test
    void migrate_unversionedNonEmptySchema_shouldRefuseBaseline() throws SQLException {
        execute("CREATE TABLE existing_data (id integer PRIMARY KEY)");
        assertThatThrownBy(() -> flyway().migrate()).isInstanceOf(FlywayException.class).hasMessageContaining("non-empty");
        assertThat(count("SELECT count(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'existing_data'"))
            .isEqualTo(1);
    }

    @Test
    void migrate_v7_shouldPreserveUsersAndRequireUniqueFirebaseUid() throws SQLException {
        Flyway.configure().configuration(flyway().getConfiguration()).target("6").load().migrate();
        execute("INSERT INTO users(username, name, email) VALUES ('existing', 'Existing User', 'existing@example.invalid')");
        assertThat(flyway().migrate().migrationsExecuted).isEqualTo(1);
        assertThat(value("SELECT name FROM users WHERE username = 'existing'")).isEqualTo("Existing User");
        assertThat(value("SELECT firebase_uid FROM users WHERE username = 'existing'")).isNull();
        execute("UPDATE users SET firebase_uid = 'firebase-uid' WHERE username = 'existing'");
        rejected("INSERT INTO users(username, name, email, firebase_uid) VALUES ('other', 'Other', 'other@example.invalid', 'firebase-uid')");
        rejected("UPDATE users SET firebase_uid = repeat('x',129) WHERE username = 'existing'");
        execute("INSERT INTO users(username, name, email) VALUES ('without_uid', 'Without UID', 'without@example.invalid')");
        assertThat(count("SELECT count(*) FROM users")).isEqualTo(2);
    }

    @Test
    void migrate_invalidSql_shouldRollBackVersion(@TempDir Path directory) throws Exception {
        copyMigrations(directory, 6);
        flyway("filesystem:" + directory).migrate();
        Files.writeString(directory.resolve("V7__broken.sql"), "CREATE TABLE rollback_probe(id integer); SELECT nonexistent_column FROM rollback_probe;");
        assertThatThrownBy(() -> flyway("filesystem:" + directory).migrate()).isInstanceOf(FlywayException.class);
        assertThat(count("SELECT count(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'rollback_probe'"))
            .isZero();
    }

    @Test
    void seed_repeatedExecution_shouldNotDuplicateOrOverwriteEdits() throws SQLException {
        seed();
        execute("UPDATE users SET name = 'Edited name' WHERE id = '" + USER + "'");
        seed();
        assertThat(count("SELECT count(*) FROM users")).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM notifications")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM itinerary_votes")).isEqualTo(1);
        assertThat(value("SELECT name FROM users WHERE id = '" + USER + "'")).isEqualTo("Edited name");
    }

    @Test
    void constraints_invalidIdentityAndIntervals_shouldRejectData() throws SQLException {
        seed();
        rejected("INSERT INTO users(username, name, email) VALUES ('DEMO_ALEX', 'Duplicate', 'other@example.invalid')");
        rejected("INSERT INTO users(username, name, email) VALUES ('another', 'Duplicate', 'ALEX@EXAMPLE.INVALID')");
        rejected("INSERT INTO user_sessions(user_id, token_hash, expires_at) VALUES ('" + USER + "', 'plain-token', now() + interval '1 hour')");
        rejected("INSERT INTO password_reset_tokens(user_id, token_hash, expires_at) VALUES ('" + USER + "', repeat('a',64), now() + interval '31 minutes')");
        rejected("INSERT INTO trip_days(trip_id, day_number, available_from, available_until) VALUES ('" + TRIP + "', 4, '12:00','10:00')");
        rejected("UPDATE trips SET ends_on = starts_on - 1 WHERE id = '" + TRIP + "'");
        rejected("UPDATE places SET latitude = 91, longitude = 0 WHERE id = '30000000-0000-0000-0000-000000000001'");
        rejected("INSERT INTO itinerary_likes(user_id, itinerary_id) VALUES (gen_random_uuid(), '" + ITINERARY + "')");
    }

    @Test
    void planning_activityDayAndPeriodChanges_shouldPreserveTripBoundaries() throws SQLException {
        seed();
        rejected("UPDATE itinerary_activities SET trip_day_id = '50000000-0000-0000-0000-000000000004' WHERE id = '70000000-0000-0000-0000-000000000001'");
        rejected("INSERT INTO trip_days(trip_id, day_number) VALUES ('" + TRIP + "',4)");
        rejected("UPDATE trips SET ends_on = starts_on + 1 WHERE id = '" + TRIP + "'");
        rejected("UPDATE trip_days SET trip_id = '40000000-0000-0000-0000-000000000002' WHERE id = '50000000-0000-0000-0000-000000000001'");
        // Warnings require application acknowledgement; overlapping times themselves are permitted.
        execute("UPDATE itinerary_activities SET starts_at = '10:30' WHERE id = '70000000-0000-0000-0000-000000000002'");
    }

    @Test
    void legs_changedActivityOrder_shouldDiscardStaleEstimates() throws SQLException {
        seed();
        execute("INSERT INTO itinerary_legs(from_activity_id,to_activity_id,transport_mode_code) VALUES ('70000000-0000-0000-0000-000000000001','70000000-0000-0000-0000-000000000002','WALKING')");
        assertThat(count("SELECT count(*) FROM itinerary_legs")).isEqualTo(1);
        execute("UPDATE itinerary_activities SET position = 3 WHERE id = '70000000-0000-0000-0000-000000000002'");
        assertThat(count("SELECT count(*) FROM itinerary_legs")).isZero();
        rejected("INSERT INTO itinerary_legs(from_activity_id,to_activity_id,transport_mode_code) VALUES ('70000000-0000-0000-0000-000000000002','70000000-0000-0000-0000-000000000001','WALKING')");
    }

    @Test
    void constraints_socialInteractions_shouldEnforceUniquenessAndLimits() throws SQLException {
        seed();
        for (var table : List.of("itinerary_likes", "itinerary_votes", "itinerary_favorites")) {
            rejected("INSERT INTO " + table + "(user_id,itinerary_id) VALUES ('" + OTHER_USER + "','" + ITINERARY + "')");
        }
        rejected("INSERT INTO itinerary_ratings(user_id,itinerary_id,rating) VALUES ('" + OTHER_USER + "','" + ITINERARY + "',5)");
        rejected("UPDATE itinerary_ratings SET rating = 6 WHERE itinerary_id = '" + ITINERARY + "'");
        rejected("INSERT INTO user_follows(follower_id,followed_id) VALUES ('" + USER + "','" + USER + "')");
        rejected("INSERT INTO itinerary_comments(user_id,itinerary_id,body) VALUES ('" + USER + "','" + ITINERARY + "','   ')");
        rejected("INSERT INTO itinerary_comments(user_id,itinerary_id,body) VALUES ('" + USER + "','" + ITINERARY + "',repeat('x',1001))");
        rejected("INSERT INTO itinerary_images(itinerary_id,object_key,content_type,size_bytes,alt_text,position) VALUES ('" + ITINERARY + "','bad','image/png',5000001,'test',1)");
        rejected("INSERT INTO itinerary_images(itinerary_id,object_key,content_type,size_bytes,alt_text,position) VALUES ('" + ITINERARY + "','bad','image/svg+xml',100,'test',1)");
        execute("INSERT INTO itinerary_images(itinerary_id,object_key,content_type,size_bytes,alt_text,position) VALUES ('" + ITINERARY + "','valid','image/jpeg',5000000,'test',1)");
        execute("UPDATE itinerary_comments SET body = 'Edited comment' WHERE id = '80000000-0000-0000-0000-000000000001'");
        assertThat(count("SELECT count(*) FROM itinerary_comments WHERE edited_at IS NOT NULL")).isEqualTo(1);
    }

    @Test
    void reportsAndNotifications_repeatedEvent_shouldNotDuplicate() throws SQLException {
        seed();
        execute("INSERT INTO content_reports(reporter_id,itinerary_id,reason) VALUES ('" + OTHER_USER + "','" + ITINERARY + "','SPAM')");
        rejected("INSERT INTO content_reports(reporter_id,itinerary_id,reason) VALUES ('" + OTHER_USER + "','" + ITINERARY + "','SPAM')");
        rejected("INSERT INTO content_reports(reporter_id,itinerary_id,reason) VALUES ('" + USER + "','" + ITINERARY + "','OTHER')");
        rejected("INSERT INTO notifications(recipient_id,actor_id,itinerary_id,type,event_key) VALUES ('" + USER + "','" + OTHER_USER + "','" + ITINERARY + "','COMMENT','demo:comment:1')");
        rejected("INSERT INTO notifications(recipient_id,actor_id,itinerary_id,type,event_key) VALUES ('" + USER + "','" + USER + "','" + ITINERARY + "','LIKE','self')");
    }

    @Test
    void publication_emptyItinerary_shouldRejectPublishing() throws SQLException {
        seed();
        rejected("DELETE FROM itinerary_activities WHERE itinerary_id = '" + ITINERARY + "'");
        assertThat(count("SELECT count(*) FROM itinerary_activities WHERE itinerary_id = '" + ITINERARY + "'")).isEqualTo(2);
    }

    @Test
    void unpublish_shouldPreserveInteractionsButHideAllPublicProjections() throws SQLException {
        seed();
        execute("UPDATE itineraries SET visibility = 'PRIVATE' WHERE id = '" + ITINERARY + "'");
        assertThat(count("SELECT count(*) FROM itinerary_likes")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM itinerary_favorites")).isEqualTo(1);
        for (var view : List.of("public_itineraries", "public_itinerary_activities", "public_itinerary_comments")) {
            assertThat(count("SELECT count(*) FROM " + view)).isZero();
        }
    }

    @Test
    void deleteTrip_shouldCascadeSocialLinksAndPreserveCopyAndAudit() throws SQLException {
        seed();
        execute("DELETE FROM trips WHERE id = '" + TRIP + "'");
        assertThat(count("SELECT count(*) FROM itineraries")).isEqualTo(1);
        assertThat(value("SELECT source_itinerary_id FROM itineraries")).isNull();
        assertThat(value("SELECT source_author_name FROM itineraries")).isEqualTo("Demo Alex");
        for (var table : List.of("itinerary_votes", "itinerary_likes", "itinerary_ratings", "itinerary_favorites", "itinerary_comments", "notifications", "content_reports")) {
            assertThat(count("SELECT count(*) FROM " + table)).isZero();
        }
        assertThat(count("SELECT count(*) FROM moderation_actions")).isEqualTo(1);
        assertThat(value("SELECT report_id FROM moderation_actions")).isNull();
    }

    private void publicRoute(String id, String title, String category, String publication) throws SQLException {
        execute("INSERT INTO itineraries(id,trip_id,title,description,category_code) VALUES ('" + id + "','" + TRIP + "','" + title + "','Test route','" + category + "')");
        execute("INSERT INTO itinerary_activities(itinerary_id,name,location) VALUES ('" + id + "','Activity','Known location')");
        execute("UPDATE itineraries SET visibility = 'PUBLIC', published_at = '" + publication + "' WHERE id = '" + id + "'");
    }

    @Test
    void ranking_shouldApplyAllTieBreakersAfterFilters() throws SQLException {
        seed();
        String mostVotes = "60000000-0000-0000-0000-000000000003";
        String oldest = "60000000-0000-0000-0000-000000000004";
        String lowerRating = "60000000-0000-0000-0000-000000000005";
        String unrated = "60000000-0000-0000-0000-000000000006";
        String largerId = "60000000-0000-0000-0000-000000000007";
        String otherCategory = "60000000-0000-0000-0000-000000000008";
        publicRoute(mostVotes, "Most votes", "MUSEUM", "2026-02-01T12:00:00Z");
        publicRoute(oldest, "Oldest", "MUSEUM", "2025-01-01T12:00:00Z");
        publicRoute(lowerRating, "Lower rating", "MUSEUM", "2025-01-01T12:00:00Z");
        publicRoute(unrated, "Unrated", "MUSEUM", "2025-01-01T12:00:00Z");
        publicRoute(largerId, "Larger id", "MUSEUM", "2026-01-01T12:00:00Z");
        publicRoute(otherCategory, "Other category", "LANDMARK", "2025-01-01T12:00:00Z");
        for (var id : List.of(mostVotes, oldest, lowerRating, largerId, otherCategory)) {
            execute("INSERT INTO itinerary_votes(user_id,itinerary_id) VALUES ('" + OTHER_USER + "','" + id + "')");
        }
        execute("INSERT INTO itinerary_votes(user_id,itinerary_id) VALUES ('" + ADMIN + "','" + mostVotes + "')");
        for (var id : List.of(oldest, largerId)) {
            execute("INSERT INTO itinerary_ratings(user_id,itinerary_id,rating) VALUES ('" + OTHER_USER + "','" + id + "',5)");
        }
        execute("INSERT INTO itinerary_ratings(user_id,itinerary_id,rating) VALUES ('" + OTHER_USER + "','" + lowerRating + "',4)");
        var ids = new ArrayList<String>();
        try (var connection = connection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT ranking_position,itinerary_id FROM itinerary_ranking('" + DESTINATION + "','MUSEUM') ORDER BY ranking_position")) {
            while (rows.next()) {
                assertThat(rows.getLong(1)).isEqualTo(ids.size() + 1);
                ids.add(rows.getString(2));
            }
        }
        assertThat(ids).containsExactly(mostVotes, oldest, ITINERARY, largerId, lowerRating, unrated);
        assertThat(value("SELECT average_rating FROM itinerary_ranking() WHERE itinerary_id = '" + unrated + "'")).isNull();
        assertThat(count("SELECT count(*) FROM itinerary_ranking(gen_random_uuid(),NULL)")).isZero();
    }

    @Test
    void publicViews_shouldHideModeratedAndSuspendedContentAndPrivateColumns() throws SQLException {
        seed();
        execute("UPDATE itineraries SET hidden_by_moderation = true WHERE id = '" + ITINERARY + "'");
        assertThat(count("SELECT count(*) FROM itinerary_ranking()")).isZero();
        execute("UPDATE itineraries SET hidden_by_moderation = false WHERE id = '" + ITINERARY + "'");
        execute("UPDATE users SET status = 'SUSPENDED' WHERE id = '" + USER + "'");
        assertThat(count("SELECT count(*) FROM public_itineraries")).isZero();
        assertThat(count("SELECT count(*) FROM public_profiles")).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM information_schema.columns WHERE table_schema = current_schema() AND table_name LIKE 'public_%' AND column_name IN ('email','phone','token_hash','starts_on','ends_on','trip_id')"))
            .isZero();
    }

    @Test
    void schema_foreignKeys_shouldHaveLeadingIndexes() throws SQLException {
        flyway().migrate();
        assertThat(count("""
            SELECT count(*) FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace
            WHERE n.nspname = current_schema() AND c.contype = 'f'
            AND NOT EXISTS (SELECT 1 FROM pg_index i WHERE i.indrelid = c.conrelid
                AND i.indisvalid AND i.indpred IS NULL AND i.indkey[0] = c.conkey[1])
            """)).isZero();
    }
}
