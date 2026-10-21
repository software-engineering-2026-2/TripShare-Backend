package ufcg.software.engineering.trip.share;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void contextLoads_withPersistence_shouldConnectToDatabase() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.isValid(5)).isTrue();
		}
		assertThat(entityManagerFactory.isOpen()).isTrue();
	}

}
