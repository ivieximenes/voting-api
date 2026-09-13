package com.sicredi.voting.repository;

import com.sicredi.voting.domain.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers 
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TopicRepositoryTest {

    @Container 
    @ServiceConnection 
        static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TopicRepository repository;

    @Test
    void shouldSaveTopic() {
        var topic = new Topic("Pauta de Teste", "Descrição da pauta");

        var saved = repository.save(topic);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Pauta de Teste");
        assertThat(saved.getDescription()).isEqualTo("Descrição da pauta");
    }

    @Test
    void shouldFindTopicById() {
        var topic = new Topic("Eleição de Diretores", "Votação para eleição");
        var saved = repository.save(topic);

        var found = repository.findById(saved.getId());

        assertThat(found)
                .isPresent()
                .contains(saved);
    }

    @Test
    void shouldReturnEmptyWhenTopicNotFound() {
        var found = repository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void shouldUpdateTopic() {
        var topic = new Topic("Pauta Original", "Descrição original");
        var saved = repository.save(topic);

        var foundTopic = repository.findById(saved.getId()).orElseThrow();
        assertThat(foundTopic.getTitle()).isEqualTo("Pauta Original");
    }

    @Test
    void shouldDeleteTopic() {
        var topic = new Topic("Pauta para Deletar", null);
        var saved = repository.save(topic);

        repository.deleteById(saved.getId());

        var found = repository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCountTopics() {
        repository.deleteAll(); 

        repository.save(new Topic("Pauta 1", "Desc 1"));
        repository.save(new Topic("Pauta 2", "Desc 2"));
        repository.save(new Topic("Pauta 3", null));

        var count = repository.count();

        assertThat(count).isEqualTo(3);
    }

    @Test
    void shouldSaveTopicWithNullDescription() {
        var topic = new Topic("Pauta sem Descrição", null);

        var saved = repository.save(topic);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDescription()).isNull();
    }

}
