package com.tcc.pjb.backend.service.transito;

import com.tcc.pjb.backend.model.dto.transito.ArquivamentoCandidatoResponse;
import com.tcc.pjb.backend.model.dto.transito.ArquivamentoPainelResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArquivamentoPainelService {

    private static final int LIMITE_ITENS = 500;

    private final ProcessoRepository processoRepository;

    public ArquivamentoPainelService(ProcessoRepository processoRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    @Transactional(readOnly = true)
    public ArquivamentoPainelResponse candidatosPorVara(String vara) {
        Objects.requireNonNull(vara, "vara");
        Page<Processo> pagina = processoRepository.findByVaraAndStatusProcesso(
                vara, StatusProcesso.TRANSITO_EM_JULGADO, PageRequest.of(0, LIMITE_ITENS));
        List<ArquivamentoCandidatoResponse> candidatos = pagina.getContent().stream()
                .map(processo -> new ArquivamentoCandidatoResponse(
                        processo.getId(),
                        processo.getNumeroProcesso(),
                        processo.getClasseProcessual(),
                        processo.getDataUltimaMovimentacao()))
                .toList();
        return new ArquivamentoPainelResponse(vara, candidatos.size(), candidatos);
    }
}
