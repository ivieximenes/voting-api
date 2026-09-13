package com.sicredi.voting.domain;

import org.junit.jupiter.api.Test;
import com.sicredi.voting.enums.VoteOption;
import static org.assertj.core.api.Assertions.assertThat;

class VoteTest {

    @Test
    void shouldCreateVoteWithTopicMemberAndOption() {
        var topicId = 1L;
        var memberId = "12345678900";
        var option = VoteOption.NAO;

        var vote = new Vote(topicId, memberId, option);

        assertThat(vote.getId()).isNull();
        assertThat(vote.getTopicId()).isEqualTo(topicId);
        assertThat(vote.getMemberId()).isEqualTo(memberId);
        assertThat(vote.getOption()).isEqualTo(option);
        assertThat(vote.getVotedAt()).isNotNull();
    }

    @Test
    void shouldSupportVoteWithOptionNO() {
        var vote = new Vote(1L, "12345678900", VoteOption.NAO);

        assertThat(vote.getOption()).isEqualTo(VoteOption.NAO);
    }

    @Test
    void shouldAcceptCpfWithMaxLength11() {
        var cpf = "12345678901";
        var vote = new Vote(1L, cpf, VoteOption.SIM);

        assertThat(vote.getMemberId()).hasSize(11).isEqualTo(cpf);
    }

}
