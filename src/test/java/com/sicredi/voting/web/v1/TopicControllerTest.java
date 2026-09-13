package com.sicredi.voting.web.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicredi.voting.domain.Topic;
import com.sicredi.voting.service.TopicService;
import com.sicredi.voting.web.dto.CreateTopicRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(TopicController.class)
class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TopicService topicService;

    @Test
    void shouldCreateTopic() throws Exception {
        var request = new CreateTopicRequest("Pauta X", "Descrição");
        var saved = new Topic("Pauta X", "Descrição");
        when(topicService.create(anyString(), any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Pauta X"))
                .andExpect(jsonPath("$.description").value("Descrição"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturn400WhenTitleIsBlank() throws Exception {
        var request = new CreateTopicRequest("", "Descrição");

        mockMvc.perform(post("/api/v1/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenTitleExceedsMaxLength() throws Exception {
        var request = new CreateTopicRequest("A".repeat(121), "Descrição");

        mockMvc.perform(post("/api/v1/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindTopicById() throws Exception {
        var topic = new Topic("Pauta", "Desc");
        when(topicService.findById(1L)).thenReturn(topic);

        mockMvc.perform(get("/api/v1/topics/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pauta"))
                .andExpect(jsonPath("$.description").value("Desc"));
    }

    @Test
    void shouldReturn404WhenTopicDoesNotExist() throws Exception {
        when(topicService.findById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta não encontrada"));

        mockMvc.perform(get("/api/v1/topics/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListTopics() throws Exception {
        when(topicService.findAll()).thenReturn(List.of(
                new Topic("Pauta 1", "d1"),
                new Topic("Pauta 2", "d2")
        ));

        mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Pauta 1"))
                .andExpect(jsonPath("$[1].title").value("Pauta 2"));
    }
}