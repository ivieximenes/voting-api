package com.sicredi.voting.web.v1;

import com.sicredi.voting.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TopicFlowIT extends AbstractIntegrationTest {

    @Test
    void shouldCreateAndFindTopic() throws Exception {
        Long topicId = createTopic(
                "Balance sheet approval",
                "Vote on the fiscal year balance sheet");

        mockMvc.perform(get("/api/v1/topics/{id}", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(topicId))
                .andExpect(jsonPath("$.title").value("Balance sheet approval"))
                .andExpect(jsonPath("$.description").value("Vote on the fiscal year balance sheet"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldCreateTopicWithoutDescription() throws Exception {
        Long topicId = createTopic("Topic without description", null);

        mockMvc.perform(get("/api/v1/topics/{id}", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Topic without description"))
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void shouldReturn404WhenTopicDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/topics/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void shouldReturn400WhenTitleIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/topics")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn400WhenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/topics")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"","description":"desc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn400WhenTitleExceedsMaxLength() throws Exception {
        String longTitle = "A".repeat(121);

        mockMvc.perform(post("/api/v1/topics")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"desc"}
                                """.formatted(longTitle)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}