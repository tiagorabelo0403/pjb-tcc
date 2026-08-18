package com.tcc.pjb.backend.service.processual.recursal;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.model.dto.processual.representacao.RepresentacaoProcessualPolicyResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.prazo.CalendarioUteisService;
import com.tcc.pjb.backend.service.processual.recursal.workspace.PerfilRecursalDescriptor;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import com.tcc.pjb.backend.service.recurso.RecursoAdmissibilidadeService;
import com.tcc.pjb.backend.service.recurso.RecursoProcessualTipo;
import com.tcc.pjb.backend.service.recurso.RecursoTempestividadeGuardService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RecursalValidacaoMinimaService {

    private static final Set<LegalAppealType> JUIZADO_JUS_POSTULANDI_APPEAL_TYPES = Set.of(
            LegalAppealType.EMBARGOS_DECLARACAO
    );

    private static final Set<LegalAppealType> TRABALHISTA_JUS_POSTULANDI_APPEAL_TYPES = Set.of(
            LegalAppealType.RECURSO_ORDINARIO_TRABALHISTA,
            LegalAppealType.EMBARGOS_DECLARACAO
    );

    private static final Set<LegalAppealType> JEF_JUS_POSTULANDI_APPEAL_TYPES = Set.of(
            LegalAppealType.EMBARGOS_DECLARACAO
    );

    private final RecursoAdmissibilidadeService admissibilidadeService;
    private final RecursoTempestividadeGuardService tempestividadeService;
    private final WorkItemRepository workItemRepository;
    private final RepresentacaoProcessualPolicyService representacaoProcessualPolicyService;

    public RecursalValidacaoMinimaService(RecursoAdmissibilidadeService admissibilidadeService,
                                          RecursoTempestividadeGuardService tempestividadeService,
                                          WorkItemRepository workItemRepository,
                                          RepresentacaoProcessualPolicyService representacaoProcessualPolicyService) {
        this.admissibilidadeService = admissibilidadeService;
        this.tempestividadeService = tempestividadeService;
        this.workItemRepository = workItemRepository;
        this.representacaoProcessualPolicyService = representacaoProcessualPolicyService;
    }

    public RecursalValidacaoMinimaResult validar(Processo processo,
                                                 Usuario usuario,
                                                 LegalAppealType appealType,
                                                 String recursoTemplateCode,
                                                 boolean preparoDispensado,
                                                 PerfilRecursalDescriptor descriptor) {
        if (processo == null || processo.getId() == null) {
            throw new IllegalArgumentException("Processo valido e persistido e obrigatorio para interposicao recursal.");
        }
        if (appealType == null || appealType == LegalAppealType.OUTRO) {
            throw new IllegalArgumentException("Tipo recursal nao reconhecido para interposicao.");
        }
        if (appealType.isOutsideCurrentMeshCatalog()) {
            throw new IllegalArgumentException("Tipo recursal ainda nao possui fluxo operacional ativo.");
        }
        RecursoProcessualTipo tipoProcessual = toRecursoProcessualTipo(appealType);
        if (tipoProcessual == null) {
            throw new IllegalArgumentException("Tipo recursal sem correspondencia processual minima.");
        }
        boolean singularidade = recursoTemplateCode == null || recursoTemplateCode.isBlank()
                || workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processo.getId(), recursoTemplateCode, WorkItemStatus.CANCELADO).isEmpty();
        boolean decisaoRecorrivel = decisaoRecorrivel(processo, appealType);
        boolean cabimentoLegal = cabimentoLegal(processo, appealType, decisaoRecorrivel);
        boolean elegivelPorJusPostulandi = elegivelPorJusPostulandi(usuario, processo, appealType);
        boolean legitimidade = legitimidade(usuario, elegivelPorJusPostulandi);
        boolean representacaoRegular = representacaoRegular(usuario, elegivelPorJusPostulandi);
        boolean mandatoJuntado = mandatoJuntado(usuario);
        RecursoAdmissibilidadeService.AdmissibilidadeResult admissibilidade = admissibilidadeService.avaliar(
                new RecursoAdmissibilidadeService.AdmissibilidadeInput(
                        uuidFrom(processo.getId()),
                        tipoProcessual,
                        legitimidade,
                        decisaoRecorrivel,
                        cabimentoLegal,
                        singularidade,
                        representacaoRegular,
                        mandatoJuntado,
                        decisaoRecorrivel
                )
        );
        LocalDate dataReferencia = dataReferencia(processo);
        TipoUsuario tipoUsuario = usuario == null ? null : usuario.getTipoUsuario();
        RecursoTempestividadeGuardService.TempestividadeResult tempestividade = tempestividadeService.avaliar(
                new RecursoTempestividadeGuardService.TempestividadeInput(
                        uuidFrom(processo.getId()),
                        tipoProcessual,
                        dataReferencia,
                        descriptor != null && descriptor.isInstitucional() && tipoUsuario != null && tipoUsuario.isProcuradoria(),
                        descriptor != null && descriptor.isMinisterioPublico(),
                        tipoUsuario != null && tipoUsuario.isDefensoriaPublica()
                ),
                new CalendarioUteisService.CalendarioInput(
                        processo.getComarca(),
                        processo.getUf(),
                        List.of(),
                        List.of(),
                        false,
                        null,
                        null
                )
        );
        if (!admissibilidade.admissivel()) {
            throw new IllegalArgumentException("Recurso inadmissivel: " + String.join("; ", admissibilidade.obstaculos()));
        }
        if (!tempestividade.tempestivo()) {
            throw new IllegalArgumentException("Recurso intempestivo: prazo final " + tempestividade.prazoFinal());
        }
        return new RecursalValidacaoMinimaResult(tipoProcessual, dataReferencia, admissibilidade, tempestividade);
    }

    private RecursoProcessualTipo toRecursoProcessualTipo(LegalAppealType appealType) {
        return switch (appealType) {
            case APELACAO, APELACAO_PENAL -> RecursoProcessualTipo.APELACAO;
            case AGRAVO_INSTRUMENTO, AGRAVO_RESP_RE, AGRAVO_RECURSO_REVISTA -> RecursoProcessualTipo.AGRAVO_DE_INSTRUMENTO;
            case AGRAVO_INTERNO -> RecursoProcessualTipo.AGRAVO_INTERNO;
            case AGRAVO_REGIMENTAL -> RecursoProcessualTipo.AGRAVO_REGIMENTAL;
            case EMBARGOS_DECLARACAO -> RecursoProcessualTipo.EMBARGOS_DECLARACAO;
            case EMBARGOS_INFRINGENTES -> RecursoProcessualTipo.EMBARGOS_INFRINGENTES;
            case RESP -> RecursoProcessualTipo.RECURSO_ESPECIAL;
            case RE -> RecursoProcessualTipo.RECURSO_EXTRAORDINARIO;
            case RECURSO_ORDINARIO_CONSTITUCIONAL, RECURSO_ORDINARIO_TRABALHISTA -> RecursoProcessualTipo.RECURSO_ORDINARIO;
            case RECURSO_INOMINADO -> RecursoProcessualTipo.RECURSO_INOMINADO_JEC;
            case RESE -> RecursoProcessualTipo.RECURSO_EM_SENTIDO_ESTRITO;
            default -> null;
        };
    }

    private boolean decisaoRecorrivel(Processo processo, LegalAppealType appealType) {
        if (processo.getStatusProcesso() != null && processo.getStatusProcesso().isPosDecisao()) {
            return true;
        }
        if (processo.getFaseAtual() == FaseProcessual.JULGAMENTO || processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            return true;
        }
        if (processo.getResultadoFinal() != null && !processo.getResultadoFinal().isBlank()) {
            return true;
        }
        return appealType == LegalAppealType.AGRAVO_INSTRUMENTO && processo.getDataUltimaMovimentacao() != null;
    }

    private boolean cabimentoLegal(Processo processo, LegalAppealType appealType, boolean decisaoRecorrivel) {
        if (!decisaoRecorrivel) {
            return false;
        }
        StatusProcesso status = processo.getStatusProcesso();
        if (status != null && (status.isTransitado() || status.isArquivadoOuBaixado())) {
            return false;
        }
        RitoProcessual rito = processo.getRito();
        return switch (appealType) {
            case APELACAO, APELACAO_PENAL -> processo.getFaseAtual() != FaseProcessual.RECURSAL;
            case RECURSO_INOMINADO -> rito != null && rito.isJuizado();
            case RESP, RE, AGRAVO_RESP_RE, AGRAVO_INTERNO, AGRAVO_REGIMENTAL -> processo.getFaseAtual() == FaseProcessual.RECURSAL || tribunalDeSegundoGrau(processo);
            default -> true;
        };
    }

    private boolean tribunalDeSegundoGrau(Processo processo) {
        String tribunal = firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal());
        if (tribunal == null) {
            return false;
        }
        String normalized = tribunal.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.startsWith("TJ")
                || normalized.startsWith("TRF")
                || normalized.startsWith("TRT")
                || normalized.startsWith("TRE")
                || normalized.equals("STJ")
                || normalized.equals("STF")
                || normalized.equals("TST")
                || normalized.equals("TSE");
    }

    private boolean legitimidade(Usuario usuario, boolean elegivelPorJusPostulandi) {
        TipoUsuario tipo = usuario == null ? null : usuario.getTipoUsuario();
        return (tipo != null && (tipo.isAdvocacia() || tipo.isDefensoriaPublica() || tipo.isMinisterioPublico() || tipo.isProcuradoria()))
                || elegivelPorJusPostulandi;
    }

    private boolean representacaoRegular(Usuario usuario, boolean elegivelPorJusPostulandi) {
        TipoUsuario tipo = usuario == null ? null : usuario.getTipoUsuario();
        if (tipo == null) {
            return false;
        }
        if (!tipo.isAdvocacia()) {
            return tipo.isDefensoriaPublica() || tipo.isMinisterioPublico() || tipo.isProcuradoria() || elegivelPorJusPostulandi;
        }
        return usuario.getOabNumero() != null && !usuario.getOabNumero().isBlank();
    }

    private boolean elegivelPorJusPostulandi(Usuario usuario, Processo processo, LegalAppealType appealType) {
        TipoUsuario tipo = usuario == null ? null : usuario.getTipoUsuario();
        if (tipo == null || tipo.isAdvocacia() || tipo.isDefensoriaPublica() || tipo.isMinisterioPublico() || tipo.isProcuradoria()) {
            return false;
        }
        RepresentacaoProcessualPolicyResponse policy = representacaoProcessualPolicyService.resolve(
                processo, usuario, null, null, null, false, false, null, null);
        if (!policy.regularidadeSuficiente()) {
            return false;
        }
        InstrumentoRepresentacaoProcessual instrumento = InstrumentoRepresentacaoProcessual.fromString(policy.resolvedInstrument());
        if (instrumento == InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JUIZADO) {
            return JUIZADO_JUS_POSTULANDI_APPEAL_TYPES.contains(appealType);
        }
        if (instrumento == InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA) {
            return TRABALHISTA_JUS_POSTULANDI_APPEAL_TYPES.contains(appealType);
        }
        if (instrumento == InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JEF) {
            return JEF_JUS_POSTULANDI_APPEAL_TYPES.contains(appealType);
        }
        return false;
    }

    private boolean mandatoJuntado(Usuario usuario) {
        TipoUsuario tipo = usuario == null ? null : usuario.getTipoUsuario();
        if (tipo == null) {
            return false;
        }
        return !tipo.isAdvocacia() || usuario.getOabNumero() != null && !usuario.getOabNumero().isBlank();
    }

    private LocalDate dataReferencia(Processo processo) {
        if (processo.getDataUltimaMovimentacao() != null) {
            return processo.getDataUltimaMovimentacao().toLocalDate();
        }
        if (processo.getDataAtualizacao() != null) {
            return processo.getDataAtualizacao().toLocalDate();
        }
        if (processo.getDataCriacao() != null) {
            return processo.getDataCriacao().toLocalDate();
        }
        return LocalDate.now(ZoneOffset.UTC);
    }

    private UUID uuidFrom(Long id) {
        return new UUID(0L, id == null ? 0L : id);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
