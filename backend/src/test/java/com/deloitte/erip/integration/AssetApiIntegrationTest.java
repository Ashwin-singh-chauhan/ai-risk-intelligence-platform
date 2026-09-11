package com.deloitte.erip.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test against a real Postgres (via Testcontainers) exercising the asset
 * creation and search flow through Spring Security's RBAC layer.
 *
 * Requires a working Docker daemon - run with `mvn -Dtest=AssetApiIntegrationTest test`.
 * Kafka autoconfiguration is left in place; SecurityEventConsumer simply never receives
 * messages in this test since nothing is published to the topic.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssetApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("erip_test")
            .withUsername("erip_app")
            .withPassword("erip_test_password");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "analyst@erip.com", roles = "SECURITY_ANALYST")
    void createAndRetrieveAsset_roundTripsSuccessfully() throws Exception {
        Map<String, Object> request = Map.of(
                "assetTag", "AST-IT-0001",
                "name", "integration-test-server",
                "assetType", "SERVER",
                "businessUnit", "Corporate IT",
                "criticality", "HIGH",
                "exposure", "DMZ",
                "environment", "PRODUCTION");

        mockMvc.perform(post("/api/v1/assets")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetTag").value("AST-IT-0001"))
                .andExpect(jsonPath("$.criticality").value("HIGH"));

        mockMvc.perform(get("/api/v1/assets").param("query", "integration-test-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].assetTag").value("AST-IT-0001"));
    }

    @Test
    void listAssets_withoutAuthentication_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/assets")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "viewer@erip.com", roles = "VIEWER")
    void createAsset_asViewer_isForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/assets")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
