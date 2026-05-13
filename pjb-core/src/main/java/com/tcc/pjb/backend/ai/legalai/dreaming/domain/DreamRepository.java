package com.tcc.pjb.backend.ai.legalai.dreaming.domain;

import java.util.List;
import java.util.Optional;

public interface DreamRepository {

    Dream salvar(Dream dream);

    Optional<Dream> buscarPorId(DreamId id);

    Optional<Dream> buscarPorAnthropicDreamId(String anthropicDreamId);

    List<Dream> listarPorStatus(DreamStatus status);
}
