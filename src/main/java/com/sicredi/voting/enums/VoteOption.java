package com.sicredi.voting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Opção de voto do associado em uma pauta.
 *
 * <p>O nome do enum e o valor gravado no banco sao iguais ("YES"/"NO"),
 * gracas a {@code @Enumerated(EnumType.STRING)}. O campo {@code label}
 * existe apenas para exibicao em respostas que precisem do rotulo
 * em portugues.</p>
 */
@Getter
@RequiredArgsConstructor
public enum VoteOption {
    YES("Sim"),
    NO("Não");

    private final String label;
}