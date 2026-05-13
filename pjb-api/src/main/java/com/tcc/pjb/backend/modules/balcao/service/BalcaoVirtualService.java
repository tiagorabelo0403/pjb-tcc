package com.tcc.pjb.backend.modules.balcao.service;

import com.tcc.pjb.backend.modules.balcao.dto.AbrirAtendimentoRequest;
import com.tcc.pjb.backend.modules.balcao.dto.BalcaoAtendimentoResponse;
import com.tcc.pjb.backend.modules.balcao.entity.BalcaoAtendimentoStatus;
import com.tcc.pjb.backend.modules.balcao.entity.BalcaoVirtualAtendimento;
import com.tcc.pjb.backend.modules.balcao.repository.BalcaoVirtualAtendimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class BalcaoVirtualService {

    private static final List<BalcaoAtendimentoStatus> STATUS_ATIVOS =
            List.of(BalcaoAtendimentoStatus.AGUARDANDO, BalcaoAtendimentoStatus.CHAMADO, BalcaoAtendimentoStatus.EM_ATENDIMENTO);

    private final BalcaoVirtualAtendimentoRepository repository;

    public BalcaoVirtualService(BalcaoVirtualAtendimentoRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Transactional
    public BalcaoAtendimentoResponse abrir(AbrirAtendimentoRequest request) {
        Instant agora = Instant.now();
        BalcaoVirtualAtendimento atendimento = BalcaoVirtualAtendimento.builder()
                .tipo(request.tipo())
                .status(BalcaoAtendimentoStatus.AGUARDANDO)
                .solicitanteNome(request.solicitanteNome())
                .solicitanteOab(request.solicitanteOab())
                .solicitanteOabUf(request.solicitanteOabUf())
                .solicitanteCpf(request.solicitanteCpf())
                .numeroProcesso(request.numeroProcesso())
                .vara(request.vara())
                .descricao(request.descricao())
                .criadoEm(agora)
                .build();
        BalcaoVirtualAtendimento salvo = repository.save(atendimento);
        salvo.setProtocolo("BV" + String.format("%08d", salvo.getId()));
        salvo = repository.save(salvo);
        return BalcaoAtendimentoResponse.de(salvo, posicaoNaFila(salvo));
    }

    @Transactional(readOnly = true)
    public Optional<BalcaoAtendimentoResponse> buscarPorProtocolo(String protocolo) {
        return repository.findByProtocolo(protocolo)
                .map(e -> BalcaoAtendimentoResponse.de(e, posicaoNaFila(e)));
    }

    @Transactional(readOnly = true)
    public Optional<BalcaoAtendimentoResponse> buscarPorId(Long id) {
        return repository.findById(id)
                .map(e -> BalcaoAtendimentoResponse.de(e, posicaoNaFila(e)));
    }

    @Transactional(readOnly = true)
    public List<BalcaoAtendimentoResponse> listarFila(String vara) {
        List<BalcaoVirtualAtendimento> fila = vara != null
                ? repository.findFilaPorVara(vara, STATUS_ATIVOS)
                : repository.findFila(STATUS_ATIVOS);
        return fila.stream()
                .map(e -> BalcaoAtendimentoResponse.de(e, fila.indexOf(e) + 1))
                .toList();
    }

    @Transactional
    public BalcaoAtendimentoResponse chamar(Long id, String atendidoPor) {
        BalcaoVirtualAtendimento e = buscarAtivo(id);
        if (e.getStatus() != BalcaoAtendimentoStatus.AGUARDANDO) {
            throw new IllegalStateException("Atendimento não está aguardando");
        }
        e.setStatus(BalcaoAtendimentoStatus.CHAMADO);
        e.setAtendidoPor(atendidoPor);
        e.setChamadoEm(Instant.now());
        return BalcaoAtendimentoResponse.de(repository.save(e), 0);
    }

    @Transactional
    public BalcaoAtendimentoResponse iniciar(Long id) {
        BalcaoVirtualAtendimento e = buscarAtivo(id);
        if (e.getStatus() != BalcaoAtendimentoStatus.CHAMADO) {
            throw new IllegalStateException("Atendimento não foi chamado");
        }
        e.setStatus(BalcaoAtendimentoStatus.EM_ATENDIMENTO);
        e.setIniciadoEm(Instant.now());
        return BalcaoAtendimentoResponse.de(repository.save(e), 0);
    }

    @Transactional
    public BalcaoAtendimentoResponse concluir(Long id, String observacoes) {
        BalcaoVirtualAtendimento e = buscarAtivo(id);
        if (e.getStatus() != BalcaoAtendimentoStatus.EM_ATENDIMENTO) {
            throw new IllegalStateException("Atendimento não está em andamento");
        }
        e.setStatus(BalcaoAtendimentoStatus.CONCLUIDO);
        e.setObservacoes(observacoes);
        e.setEncerradoEm(Instant.now());
        return BalcaoAtendimentoResponse.de(repository.save(e), 0);
    }

    @Transactional
    public BalcaoAtendimentoResponse naoCompareceu(Long id) {
        BalcaoVirtualAtendimento e = buscarAtivo(id);
        if (e.getStatus() != BalcaoAtendimentoStatus.CHAMADO) {
            throw new IllegalStateException("Atendimento não está no status CHAMADO");
        }
        e.setStatus(BalcaoAtendimentoStatus.NAO_COMPARECEU);
        e.setEncerradoEm(Instant.now());
        return BalcaoAtendimentoResponse.de(repository.save(e), 0);
    }

    @Transactional
    public BalcaoAtendimentoResponse cancelar(Long id) {
        BalcaoVirtualAtendimento e = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Atendimento não encontrado: " + id));
        if (e.getStatus().isTerminal()) {
            throw new IllegalStateException("Atendimento já encerrado");
        }
        e.setStatus(BalcaoAtendimentoStatus.CANCELADO);
        e.setEncerradoEm(Instant.now());
        return BalcaoAtendimentoResponse.de(repository.save(e), 0);
    }

    private BalcaoVirtualAtendimento buscarAtivo(Long id) {
        BalcaoVirtualAtendimento e = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Atendimento não encontrado: " + id));
        if (e.getStatus().isTerminal()) {
            throw new IllegalStateException("Atendimento já encerrado");
        }
        return e;
    }

    private int posicaoNaFila(BalcaoVirtualAtendimento e) {
        if (e.getStatus() != BalcaoAtendimentoStatus.AGUARDANDO) {
            return 0;
        }
        List<BalcaoVirtualAtendimento> fila = e.getVara() != null
                ? repository.findFilaPorVara(e.getVara(), List.of(BalcaoAtendimentoStatus.AGUARDANDO))
                : repository.findFila(List.of(BalcaoAtendimentoStatus.AGUARDANDO));
        int idx = fila.stream().map(BalcaoVirtualAtendimento::getId).toList().indexOf(e.getId());
        return idx >= 0 ? idx + 1 : 0;
    }
}
