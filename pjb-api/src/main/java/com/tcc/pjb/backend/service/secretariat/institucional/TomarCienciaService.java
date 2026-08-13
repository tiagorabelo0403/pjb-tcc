package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TomarCienciaService {

    private final SecretariaInstitucionalItemRepository repository;
    private final AuditLedgerService auditService;
    private final ProcessoRepository processoRepository;
    private final PrazoFatalCalculator prazoFatalCalculator;
    private final UnidadeInstituicaoRepository unidadeRepository;
    private final UnidadeInstitucionalVisibilityPolicy visibilityPolicy;

    public TomarCienciaService(SecretariaInstitucionalItemRepository repository,
                               AuditLedgerService auditService,
                               ProcessoRepository processoRepository,
                               PrazoFatalCalculator prazoFatalCalculator,
                               UnidadeInstituicaoRepository unidadeRepository,
                               UnidadeInstitucionalVisibilityPolicy visibilityPolicy) {
        this.repository = Objects.requireNonNull(repository);
        this.auditService = Objects.requireNonNull(auditService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.prazoFatalCalculator = Objects.requireNonNull(prazoFatalCalculator);
        this.unidadeRepository = Objects.requireNonNull(unidadeRepository);
        this.visibilityPolicy = Objects.requireNonNull(visibilityPolicy);
    }

    @Transactional
    public void tomarCiencia(Usuario usuario, Long itemId) {
        SecretariaInstitucionalItem item = repository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado: " + itemId));
        verificarPosse(usuario, item);
        if (item.getIntimadoEm() == null) {
            Instant marco = Instant.now();
            item.setIntimadoEm(marco);
            item.setStatus(StatusSecretariaInstitucionalItem.EM_ANALISE);
            calcularPrazoFatal(item, marco);
            repository.save(item);
            auditService.appendSafely("SECRETARIA_INSTITUCIONAL_CIENCIA", "SECRETARIA_INSTITUCIONAL_ITEM " + itemId);
        }
    }

    @Transactional
    public void concluir(Usuario usuario, Long itemId) {
        SecretariaInstitucionalItem item = repository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado: " + itemId));
        verificarPosse(usuario, item);
        if (item.getStatus() != StatusSecretariaInstitucionalItem.CONCLUIDO) {
            item.setStatus(StatusSecretariaInstitucionalItem.CONCLUIDO);
            repository.save(item);
            auditService.appendSafely("SECRETARIA_INSTITUCIONAL_CONCLUSAO", "SECRETARIA_INSTITUCIONAL_ITEM " + itemId);
        }
    }

    private void calcularPrazoFatal(SecretariaInstitucionalItem item, Instant marco) {
        processoRepository.findById(item.getProcessoId())
                .ifPresent(processo -> item.setPrazoFatal(prazoFatalCalculator.calcular(item, processo, marco)));
    }

    private void verificarPosse(Usuario usuario, SecretariaInstitucionalItem item) {
        Long unidadeId = item.getUnidadeInstitucionalId();
        if (unidadeId == null) {
            if (usuario.getTipoUsuario() != TipoUsuario.ADMINISTRADOR) {
                throw new SecurityException("Item sem unidade institucional resolvida — acesso restrito a administrador.");
            }
            return;
        }
        UnidadeInstituicao unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada: " + unidadeId));
        if (!visibilityPolicy.podeVer(usuario, unidade)) {
            throw new SecurityException("Usuário não tem visibilidade sobre esta unidade institucional");
        }
    }
}
