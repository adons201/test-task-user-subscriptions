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
import ru.tms.dto.Subscription;
import ru.tms.services.SubscriptionService;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.flyway.enabled=true"})
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class SubscriptionControllerIntegrationTest {

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
    private SubscriptionService subscriptionService;

    @Autowired
    private Flyway flyway;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE user_subscriptions.subscriptions RESTART IDENTITY CASCADE;");
        jdbcTemplate.execute("TRUNCATE TABLE user_subscriptions.users RESTART IDENTITY CASCADE;");
    }

    @BeforeEach
    void setUp() {
        flyway.migrate();

        jdbcTemplate.execute("INSERT INTO user_subscriptions.users (username, version) VALUES ('testUser', 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.subscriptions (name, user_id, version) VALUES ('TestSubscription', 1, 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.subscriptions (name, user_id, version) VALUES ('Sub1', 1, 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.subscriptions (name, user_id, version) VALUES ('Sub2', 1, 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.subscriptions (name, user_id, version) VALUES ('Sub3', 1, 0)");
    }

    @Test
    @DisplayName("Должен корректно подписки по существующему пользователю")
    void getSubscriptionByUserId_ExistingId_ReturnsSubscriptions() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/user-subscriptions/v1/users/1/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(4))) // Исправлено количество возвращаемых элементов
                .andExpect(jsonPath("$[0].name").value("TestSubscription"));
    }

    @Test
    @DisplayName("Должен корректно вернуть топ 3 подписки")
    void findTopThreeSubscriptions_ReturnsTopSubscriptions() throws Exception {
        // Arrange
        List<String> expectedNames = Arrays.asList("PopularSub1", "PopularSub2", "PopularSub3");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.users (username, version) VALUES ('anotherUser2', 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.users (username, version) VALUES ('anotherUser3', 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.subscriptions (name, user_id, version) VALUES ('PopularSub1', 1, 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.subscriptions (name, user_id, version) VALUES ('PopularSub1', 2, 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.subscriptions (name, user_id, version) VALUES ('PopularSub1', 3, 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.subscriptions (name, user_id, version) VALUES ('PopularSub2', 1, 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.subscriptions (name, user_id, version) VALUES ('PopularSub2', 2, 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.subscriptions (name, user_id, version) VALUES ('PopularSub2', 3, 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.subscriptions (name, user_id, version) VALUES ('PopularSub3', 1, 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.subscriptions (name, user_id, version) VALUES ('PopularSub3', 2, 0)");
        jdbcTemplate.execute("INSERT INTO user_subscriptions.subscriptions (name, user_id, version) VALUES ('PopularSub3', 3, 0)");

        // Act & Assert
        mockMvc.perform(get("/user-subscriptions/v1/subscriptions/top"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0]", is(expectedNames.get(0))))
                .andExpect(jsonPath("$[1]", is(expectedNames.get(1))))
                .andExpect(jsonPath("$[2]", is(expectedNames.get(2))));
    }

    @Test
    @DisplayName("Должен корректно создать новую подписку")
    void createSubscription_ValidSubscription_ReturnsCreatedSubscription() throws Exception {
        // Arrange
        Subscription newSubscription = Subscription.builder().name("NewSubscription").user(1L).build();
        String subscriptionJson = objectMapper.writeValueAsString(newSubscription);

        // Act & Assert
        mockMvc.perform(post("/user-subscriptions/v1/users/1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("NewSubscription"));

        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) " +
                        "FROM user_subscriptions.subscriptions WHERE user_id = ? AND name = ?"
                , Integer.class, 1L, "NewSubscription");
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Должен корректно вернуть ошибку при отправке некорректной подписки")
    void createSubscription_InvalidSubscription_ReturnsBadRequest() throws Exception {
        // Arrange
        Subscription invalidSubscription = Subscription.builder().build();
        String invalidSubscriptionJson = objectMapper.writeValueAsString(invalidSubscription);

        // Act & Assert
        mockMvc.perform(post("/user-subscriptions/v1/users/1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidSubscriptionJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен корректно удалить существующую подписку")
    void deleteSubscription_ExistingSubscription_ReturnsNoContent() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/user-subscriptions/v1/users/1/subscriptions/1"))
                .andExpect(status().isNoContent());
        int count = jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM user_subscriptions.subscriptions WHERE id = ?", Integer.class, 1L);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Должен корректно вернуть пустую коллекцию для несуществующего пользователя")
    void getSubscriptionByUserId_NonExistingId_ReturnsEmptyList() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/user-subscriptions/v1/users/999/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }
}

