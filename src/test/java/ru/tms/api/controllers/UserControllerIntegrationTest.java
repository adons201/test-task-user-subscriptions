package ru.tms.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.tms.dto.User;
import ru.tms.services.UserService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class UserControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2)
                    .withStartupTimeout(Duration.ofSeconds(60)));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "?currentSchema=user_subscriptions");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    void setUp() {
        flyway.migrate();

        jdbcTemplate.execute("INSERT INTO user_subscriptions.users (username, version) VALUES ('testUser', 0)");
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE user_subscriptions.users RESTART IDENTITY CASCADE;");
    }

    @Test
    @DisplayName("Должен корректно вернуть пользователя")
    void getUserById_ExistingId_ReturnsUser() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/user-subscriptions/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testUser"));
    }

    @Test
    @DisplayName("Должен корректно не найти пользователя")
    void getUserById_NonExistingId_ReturnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/user-subscriptions/v1/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Должен корректно создать пользователя")
    void createUser_ValidUser_ReturnsCreatedUser() throws Exception {
        // Arrange
        User newUser = User.builder().username("newUser").build();
        String userJson = objectMapper.writeValueAsString(newUser);

        // Act & Assert
        mockMvc.perform(post("/user-subscriptions/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("newUser"));
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_subscriptions.users WHERE username = 'newUser'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Должен корректно упасть в ошибку при создании пользователя")
    void createUser_InvalidUser_ReturnsBadRequest() throws Exception {
        // Arrange
        User invalidUser = User.builder().build();
        String userJson = objectMapper.writeValueAsString(invalidUser);

        // Act & Assert
        mockMvc.perform(post("/user-subscriptions/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен корректно обновить пользователя")
    void updateUser_ExistingUser_ReturnsUpdatedUser() throws Exception {
        // Arrange
        User updatedUser = User.builder().username("updatedUser").build();
        String userJson = objectMapper.writeValueAsString(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/user-subscriptions/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("updatedUser"));
        String username = jdbcTemplate.queryForObject("SELECT username FROM user_subscriptions.users WHERE id = 1", String.class);
        assertThat(username).isEqualTo("updatedUser");
    }

    @Test
    @DisplayName("Должен корректно упасть в ошибку при обновлении пользователя")
    void updateUser_NonExistingUser_ReturnsNotFound() throws Exception {
        // Arrange
        User updatedUser = User.builder().username("updatedUser").build();
        String userJson = objectMapper.writeValueAsString(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/user-subscriptions/v1/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_ExistingUser_ReturnsOk() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/user-subscriptions/v1/users/1"))
                .andExpect(status().isOk());
        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_subscriptions.users WHERE id = 1", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Должен корректно упасть в ошибку при удалении пользователя")
    void deleteUser_NonExistingUser_ReturnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/user-subscriptions/v1/users/999"))
                .andExpect(status().isNotFound());
    }
}
