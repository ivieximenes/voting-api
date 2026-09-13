package com.sicredi.voting.service;

import com.sicredi.voting.domain.Topic;
import com.sicredi.voting.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @InjectMocks
    private TopicService topicService;

    @Test
    void shouldCreateTopic() {
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> inv.getArgument(0));

        Topic topic = topicService.create("Pauta X", "Descrição da pauta");

        assertThat(topic.getTitle()).isEqualTo("Pauta X");
        assertThat(topic.getDescription()).isEqualTo("Descrição da pauta");
        assertThat(topic.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldThrowNotFoundWhenTopicDoesNotExist() {
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.findById(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void shouldListAllTopics() {
        when(topicRepository.findAll()).thenReturn(List.of(
                new Topic("Pauta 1", "d1"),
                new Topic("Pauta 2", "d2")
        ));

        var result = topicService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting("title").containsExactly("Pauta 1", "Pauta 2");
    }
}