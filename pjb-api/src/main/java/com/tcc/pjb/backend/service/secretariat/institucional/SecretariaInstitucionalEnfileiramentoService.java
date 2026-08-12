package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstitucionalAbrangencia;
import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstitucionalAbrangenciaRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecretariaInstitucionalEnfileiramentoService {

    private final UnidadeInstituicaoRepository unidadeRepository;
    private final UnidadeInstitucionalAbrangenciaRepository abrangenciaRepository;
    private final SecretariaInstitucionalItemRepository itemRepository;
    private final SecretariaInstitucionalItemGravador gravador;
    private final AuditLedgerService auditService;
    private final ProcessoRepository processoRepository;

    public SecretariaInstitucionalEnfileiramentoService(UnidadeInstituicaoRepository unidadeRepository,
                                                         UnidadeInstitucionalAbrangenciaRepository abrangenciaRepository,
                                                         SecretariaInstitucionalItemRepository itemRepository,
                                                         SecretariaInstitucionalItemGravador gravador,
                                                         AuditLedgerService auditService,
                                                         ProcessoRepository processoRepository) {
        this.unidadeRepository = Objects.requireNonNull(unidadeRepository);
        this.abrangenciaRepository = Objects.requireNonNull(abrangenciaRepository);
        this.itemRepository = Objects.requireNonNull(itemRepository);
        this.gravador = Objects.requireNonNull(gravador);
        this.auditService = Objects.requireNonNull(auditService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    @Transactional
    public SecretariaInstitucionalItem enfileirar(Long processoId, String comarca, TipoUnidadeInstitucional tipo,
                                                   MotivoEnfileiramentoInstitucional motivo, int prazoBaseDias) {
        if (itemRepository.existePendenteOuEmAnalise(processoId, tipo)) {
            return null;
        }

        Long unidadeResolvidaId = resolverUnidade(tipo, comarca);
        boolean prazoEmDobro = tipo == TipoUnidadeInstitucional.PROMOTORIA || tipo == TipoUnidadeInstitucional.NUCLEO_DEFENSORIA;

        SecretariaInstitucionalItem item = new SecretariaInstitucionalItem();
        item.setProcessoId(processoId);
        item.setTipoInstituicaoAlvo(tipo);
        item.setMotivo(motivo);
        item.setPrazoBaseDias(prazoBaseDias);
        item.setPrazoEmDobro(prazoEmDobro);
        item.setCriadoEm(Instant.now());
        if (unidadeResolvidaId != null) {
            item.setUnidadeInstitucionalId(unidadeResolvidaId);
            item.setStatus(StatusSecretariaInstitucionalItem.PENDENTE);
        } else {
            item.setUnidadeInstitucionalId(null);
            item.setStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA);
        }

        SecretariaInstitucionalItem salvo;
        try {
            salvo = gravador.gravar(item);
        } catch (DataIntegrityViolationException concorrencia) {
            return null;
        }
        auditService.appendSafely("SECRETARIA_INSTITUCIONAL_ENFILEIRAMENTO",
                "SECRETARIA_INSTITUCIONAL_ITEM " + salvo.getId() + " processo=" + processoId
                        + " tipo=" + tipo + " status=" + salvo.getStatus());
        return salvo;
    }

    @Transactional
    public int reprocessarSemUnidade(TipoUnidadeInstitucional tipo) {
        List<SecretariaInstitucionalItem> presos = itemRepository.findByStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA)
                .stream()
                .filter(i -> i.getTipoInstituicaoAlvo() == tipo)
                .toList();
        int resolvidos = 0;
        for (SecretariaInstitucionalItem item : presos) {
            Processo processo = processoRepository.findById(item.getProcessoId()).orElse(null);
            if (processo == null || processo.getComarca() == null) {
                continue;
            }
            Long unidadeResolvidaId = resolverUnidade(tipo, processo.getComarca());
            if (unidadeResolvidaId == null) {
                continue;
            }
            item.setUnidadeInstitucionalId(unidadeResolvidaId);
            item.setStatus(StatusSecretariaInstitucionalItem.PENDENTE);
            SecretariaInstitucionalItem salvo;
            try {
                salvo = gravador.gravar(item);
            } catch (DataIntegrityViolationException concorrencia) {
                continue;
            }
            auditService.appendSafely("SECRETARIA_INSTITUCIONAL_REPROCESSAMENTO",
                    "SECRETARIA_INSTITUCIONAL_ITEM " + salvo.getId() + " resolvido para unidade=" + unidadeResolvidaId);
            resolvidos++;
        }
        return resolvidos;
    }

    private Long resolverUnidade(TipoUnidadeInstitucional tipo, String comarca) {
        List<UnidadeInstituicao> sediadas = unidadeRepository.findByTipoAndComarca(tipo, comarca);
        if (sediadas.size() == 1) {
            return sediadas.get(0).getId();
        }
        if (sediadas.size() > 1) {
            return null;
        }
        List<UnidadeInstitucionalAbrangencia> cobertura = abrangenciaRepository.findByComarcaAtendida(comarca);
        List<Long> candidatas = cobertura.stream()
                .map(UnidadeInstitucionalAbrangencia::getUnidadeInstitucionalId)
                .map(unidadeRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(u -> u.getTipo() == tipo)
                .map(UnidadeInstituicao::getId)
                .toList();
        return candidatas.size() == 1 ? candidatas.get(0) : null;
    }
}
