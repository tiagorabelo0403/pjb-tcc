package com.tcc.pjb.backend.core.processo.ciencia.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.ciencia.domain.CienciaDiesAQuoCalculator;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.comunicacao.CienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.CanalCiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusCiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoCienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicaoCiencia;
import com.tcc.pjb.backend.model.repository.CienciaProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.notification.IntimacaoMulticanalService;
import com.tcc.pjb.backend.service.prazo.CalendarioUteisService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CienciaProcessualApplicationService {

    private final CienciaProcessualRepository cienciaRepository;
    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CienciaDiesAQuoCalculator diesAQuoCalculator;
    private final IntimacaoMulticanalService intimacaoService;
    private final AuditLedgerService auditLedgerService;

    public CienciaProcessualApplicationService(
            CienciaProcessualRepository cienciaRepository,
            ProcessoRepository processoRepository,
            UsuarioRepository usuarioRepository,
            CalendarioUteisService calendarioUteisService,
            IntimacaoMulticanalService intimacaoService,
            AuditLedgerService auditLedgerService) {
        this.cienciaRepository = cienciaRepository;
        this.processoRepository = processoRepository;
        this.usuarioRepository = usuarioRepository;
        this.diesAQuoCalculator = new CienciaDiesAQuoCalculator(calendarioUteisService);
        this.intimacaoService = intimacaoService;
        this.auditLedgerService = auditLedgerService;
    }

    @Transactional
    public CienciaProcessual registrar(
            Long processoId,
            Long usuarioId,
            TipoCienciaProcessual tipo,
            CanalCiencia canal,
            GrauJurisdicaoCiencia grau,
            String instanciaTribunal,
            String ritoProcessual,
            String poloProcessual,
            Long recursoOrigemId,
            Long atoProcessualId,
            String hashConteudo,
            Instant dataDisponibilizacao,
            Instant dataPublicacaoDje,
            String numeroEdicaoDje) {

        if (atoProcessualId != null) {
            Optional<CienciaProcessual> existente =
                    cienciaRepository.findByAtoProcessualIdAndUsuarioId(atoProcessualId, usuarioId);
            if (existente.isPresent()) return existente.get();
        }

        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", usuarioId));

        Instant diesAQuo = diesAQuoCalculator.calcularDiesAQuo(
                canal, dataDisponibilizacao, dataPublicacaoDje);

        CienciaProcessual ciencia = new CienciaProcessual(
                processo, usuario, tipo, canal, grau,
                ritoProcessual, instanciaTribunal, poloProcessual,
                recursoOrigemId, atoProcessualId,
                processo.getNumeroProcesso(), numeroEdicaoDje,
                hashConteudo, dataDisponibilizacao, dataPublicacaoDje, diesAQuo);

        CienciaProcessual salva = cienciaRepository.save(ciencia);

        if (canal.exigeConfirmacaoAtiva()) {
            intimacaoService.dispatch(
                    processoId, usuarioId,
                    "Ciência processual — " + tipo.label(),
                    "Você possui uma ciência processual pendente: " + tipo.label(),
                    null, false);
        }

        auditLedgerService.appendSafely(
                "CIENCIA_REGISTRADA",
                "CienciaProcessual",
                String.valueOf(salva.getId()),
                hashConteudo);

        return salva;
    }

    @Transactional
    public CienciaProcessual confirmar(Long cienciaId, Long usuarioId,
                                       String ip, String userAgentHash,
                                       String sessionTokenHash) {
        CienciaProcessual ciencia = cienciaRepository.findById(cienciaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("CienciaProcessual", cienciaId));

        if (!ciencia.getUsuario().getId().equals(usuarioId)) {
            throw new SecurityException(
                    "Usuário " + usuarioId + " não é destinatário da ciência " + cienciaId);
        }

        ciencia.confirmar(ip, userAgentHash, sessionTokenHash, Instant.now());
        CienciaProcessual salva = cienciaRepository.save(ciencia);

        auditLedgerService.appendSafely(
                "CIENCIA_CONFIRMADA",
                "CienciaProcessual",
                String.valueOf(cienciaId));

        return salva;
    }

    @Transactional
    public int processarExpirados() {
        int total = 0;
        while (true) {
            List<CienciaProcessual> lote = cienciaRepository
                    .findPendentesExpirados(Instant.now(), PageRequest.of(0, 50));
            if (lote.isEmpty()) break;
            for (CienciaProcessual c : lote) {
                c.expirar(Instant.now());
            }
            cienciaRepository.saveAll(lote);
            total += lote.size();
            if (lote.size() < 50) break;
        }
        return total;
    }

    @Transactional(readOnly = true)
    public List<CienciaProcessual> buscarPendentes(Long processoId) {
        return cienciaRepository.findByProcessoIdAndStatus(processoId, StatusCiencia.PENDENTE);
    }

    @Transactional(readOnly = true)
    public List<CienciaProcessual> buscarPorUsuario(Long usuarioId) {
        return cienciaRepository.findByUsuarioIdAndStatus(usuarioId, StatusCiencia.PENDENTE);
    }

    @Transactional(readOnly = true)
    public List<CienciaProcessual> buscarPorGrau(Long processoId, GrauJurisdicaoCiencia grau) {
        return cienciaRepository.findByProcessoIdAndGrau(processoId, grau);
    }

    @Transactional(readOnly = true)
    public List<CienciaProcessual> buscarPorPolo(Long processoId, String polo) {
        return cienciaRepository.findPendentesByProcessoAndPolo(processoId, polo);
    }
}
