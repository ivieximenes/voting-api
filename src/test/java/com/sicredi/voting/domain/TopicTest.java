package com.sicredi.voting.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class TopicTest {

    @Test
    void shouldCreateTopicWithTitleAndDescription() {
        var title = "Aprovação de novo estatuto";
        var description = "Votação sobre as mudanças propostas no estatuto da associação";

        var topic = new Topic(title, description);

        assertThat(topic.getId()).isNull();
        assertThat(topic.getTitle()).isEqualTo(title);
        assertThat(topic.getDescription()).isEqualTo(description);
        assertThat(topic.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void shouldCreateTopicWithoutDescription() {
        var title = "Eleição de diretores";

        var topic = new Topic(title, null);

        assertThat(topic.getTitle()).isEqualTo(title);
        assertThat(topic.getDescription()).isNull();
        assertThat(topic.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldHaveTitleWithMaxLength120() {
        var longTitle = "a".repeat(120);
        var topic = new Topic(longTitle, null);

        assertThat(topic.getTitle()).hasSize(120);
    }

    @Test
    void shouldHaveDescriptionWithMaxLength500() {
        var longDescription = "b".repeat(500);
        var topic = new Topic("Título", longDescription);

        assertThat(topic.getDescription()).hasSize(500);
    }

}
