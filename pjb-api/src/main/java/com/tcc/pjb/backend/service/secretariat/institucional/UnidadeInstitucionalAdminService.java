package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstitucionalAbrangencia;
import com.tcc.pjb.backend.model.entity.enums.StatusUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.InstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstitucionalAbrangenciaRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnidadeInstitucionalAdminService {

    private final InstituicaoRepository instituicaoRepository;
    private final UnidadeInstituicaoRepository unidadeRepository;
    private final UnidadeInstitucionalAbrangenciaRepository abrangenciaRepository;
    private final SecretariaInstitucionalEnfileiramentoService enfileiramentoService;
    private final AuditLedgerService auditService;

    public UnidadeInstitucionalAdminService(InstituicaoRepository instituicaoRepository,
                                            UnidadeInstituicaoRepository unidadeRepository,
                                            UnidadeInstitucionalAbrangenciaRepository abrangenciaRepository,
                                            SecretariaInstitucionalEnfileiramentoService enfileiramentoService,
                                            AuditLedgerService auditService) {
        this.instituicaoRepository = Objects.requireNonNull(instituicaoRepository);
        this.unidadeRepository = Objects.requireNonNull(unidadeRepository);
        this.abrangenciaRepository = Objects.requireNonNull(abrangenciaRepository);
        this.enfileiramentoService = Objects.requireNonNull(enfileiramentoService);
        this.auditService = Objects.requireNonNull(auditService);
    }

    @Transactional
    public Instituicao criarInstituicao(TipoInstituicao tipo, String nome, String sigla) {
        Instituicao instituicao = new Instituicao();
        instituicao.setTipo(tipo);
        instituicao.setNome(nome);
        instituicao.setSigla(sigla);
        Instituicao salva = instituicaoRepository.save(instituicao);
        auditService.appendSafely("INSTITUICAO_CRIADA", "INSTITUICAO " + salva.getId() + " tipo=" + tipo);
        return salva;
    }

    @Transactional
    public UnidadeInstituicao criarUnidade(Long instituicaoId, String nome, TipoUnidadeInstitucional tipo, String comarca, String uf) {
        Instituicao instituicao = instituicaoRepository.findById(instituicaoId)
                .orElseThrow(() -> new IllegalArgumentException("Instituição não encontrada: " + instituicaoId));
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        unidade.setInstituicao(instituicao);
        unidade.setNome(nome);
        unidade.setTipo(tipo);
        unidade.setComarca(comarca);
        unidade.setUf(uf);
        UnidadeInstituicao salva = unidadeRepository.save(unidade);
        auditService.appendSafely("UNIDADE_INSTITUICAO_CRIADA", "UNIDADE_INSTITUICAO " + salva.getId() + " tipo=" + tipo);
        return salva;
    }

    // Transacao propria e separada de criarUnidade de proposito: SecretariaInstitucionalItemGravador.gravar
    // roda em REQUIRES_NEW (conexao fisica separada, so enxerga dados ja commitados de outras
    // transacoes). Se este reprocessamento rodasse na MESMA transacao que acabou de inserir a
    // unidade, a linha ainda nao commitada seria invisivel pro REQUIRES_NEW e o UPDATE do item
    // falharia por violacao de FK — precisa que criarUnidade ja tenha retornado (e commitado)
    // antes desta chamada comecar. Por isso o controller invoca os dois metodos em sequencia,
    // nunca um metodo orquestrando os dois via self-invocation (o proxy @Transactional do Spring
    // nao intercepta chamadas internas this.metodo(), mesma armadilha do EquipeSwitchInterceptor).
    @Transactional
    public void reprocessarBacklogAposCriacaoDeUnidade(UnidadeInstituicao unidade) {
        int reprocessados = enfileiramentoService.reprocessarSemUnidade(unidade.getTipo());
        if (reprocessados > 0) {
            auditService.appendSafely("SECRETARIA_INSTITUCIONAL_REPROCESSAMENTO_EM_LOTE",
                    "tipo=" + unidade.getTipo() + " itensResolvidos=" + reprocessados + " apos criacao de unidade " + unidade.getId());
        }
    }

    @Transactional
    public UnidadeInstitucionalAbrangencia adicionarAbrangencia(Long unidadeId, String comarcaAtendida) {
        UnidadeInstituicao unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada: " + unidadeId));
        UnidadeInstitucionalAbrangencia abrangencia = new UnidadeInstitucionalAbrangencia();
        abrangencia.setUnidadeInstitucionalId(unidadeId);
        abrangencia.setComarcaAtendida(comarcaAtendida);
        UnidadeInstitucionalAbrangencia salva = abrangenciaRepository.save(abrangencia);
        auditService.appendSafely("UNIDADE_INSTITUICAO_ABRANGENCIA_ADICIONADA",
                "UNIDADE_INSTITUICAO " + unidadeId + " comarca=" + comarcaAtendida);
        int reprocessados = enfileiramentoService.reprocessarSemUnidade(unidade.getTipo());
        if (reprocessados > 0) {
            auditService.appendSafely("SECRETARIA_INSTITUCIONAL_REPROCESSAMENTO_EM_LOTE",
                    "tipo=" + unidade.getTipo() + " itensResolvidos=" + reprocessados + " apos abrangencia nova em unidade " + unidadeId);
        }
        return salva;
    }

    @Transactional
    public void desativarUnidade(Long unidadeId) {
        UnidadeInstituicao unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada: " + unidadeId));
        unidade.setStatusUnidade(StatusUnidadeInstitucional.INATIVA);
        unidadeRepository.save(unidade);
        auditService.appendSafely("UNIDADE_INSTITUICAO_DESATIVADA", "UNIDADE_INSTITUICAO " + unidadeId);
    }
}
