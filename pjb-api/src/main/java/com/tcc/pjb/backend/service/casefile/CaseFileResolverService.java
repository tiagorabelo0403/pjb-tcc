package com.tcc.pjb.backend.service.casefile;

import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.kernel.casefile.CaseFileEventStore;
import com.tcc.pjb.backend.core.kernel.casefile.CaseFileEventType;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.core.kernel.recursal.ProceedingKeyFactory;
import com.tcc.pjb.backend.core.kernel.recursal.context.ProceduralContext;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.casefile.CaseFile;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceeding;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingRole;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.CaseFileRepository;
import com.tcc.pjb.backend.model.repository.CaseProceedingRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;

@Service
public class CaseFileResolverService {

    private final ProcessoRepository processoRepository;
    private final CaseFileRepository caseFileRepository;
    private final CaseProceedingRepository proceedingRepository;
    private final CaseFileEventStore eventStore;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final ProcessoRitoSnapshotService processoRitoSnapshotService;

    public CaseFileResolverService(ProcessoRepository processoRepository,
                                  CaseFileRepository caseFileRepository,
                                  CaseProceedingRepository proceedingRepository,
                                  CaseFileEventStore eventStore,
                                  ProceduralCanonicalResolver proceduralCanonicalResolver,
                                  ProcessoRitoSnapshotService processoRitoSnapshotService) {
        this.processoRepository = processoRepository;
        this.caseFileRepository = caseFileRepository;
        this.proceedingRepository = proceedingRepository;
        this.eventStore = eventStore;
        this.proceduralCanonicalResolver = proceduralCanonicalResolver;
        this.processoRitoSnapshotService = processoRitoSnapshotService;
    }

    @Transactional
    public CaseFileResolution resolveForProcesso(Long processoId, LegalIntegrationSystem originSystem) {
        Objects.requireNonNull(processoId, "processoId é obrigatório");

        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));

        CaseFile caseFile = caseFileRepository.findByRootProcessoId(processoId).orElse(null);

        if (caseFile == null) {
            CaseProceeding linked = proceedingRepository.findFirstByLinkedProcessoId(processoId).orElse(null);
            if (linked != null) {
                caseFile = caseFileRepository.findById(linked.getCaseFileId()).orElse(null);
            }
        }

        if (caseFile == null) {
            caseFile = createCaseFile(processoId);
        }

        ProceduralContext ctx = buildContext(processo, originSystem);
        CaseProceeding anchor = assureRootProceeding(caseFile, processo, ctx);

        return new CaseFileResolution(caseFile, anchor, ctx);
    }

    private CaseFile createCaseFile(Long processoId) {
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                CaseFile cf = CaseFile.builder()
                        .rootProcessoId(processoId)
                        .build();
                CaseFile saved = caseFileRepository.save(cf);
                eventStore.append(saved.getId(), CaseFileEventType.CASEFILE_CREATED, new CaseFileCreatedPayload(processoId));
                return saved;
            } catch (DataIntegrityViolationException race) {
                
                CaseFile existing = caseFileRepository.findByRootProcessoId(processoId).orElse(null);
                if (existing != null) return existing;
                if (attempts >= 3) throw race;
            }
        }
    }

    private CaseProceeding assureRootProceeding(CaseFile caseFile, Processo processo, ProceduralContext ctx) {
        CaseProceeding existing = proceedingRepository.findFirstByCaseFileIdAndLinkedProcessoIdAndParentProceedingKeyIsNull(caseFile.getId(), processo.getId()).orElse(null);
        if (existing != null) return existing;

        String tribunal = ctx.tribunal();
        String numero = ctx.numeroUnificado();
        String rootKey = ProceedingKeyFactory.rootKey(caseFile.getId(), ctx.currentInstance(), tribunal, numero);

        CaseProceeding proceeding = CaseProceeding.builder()
                .caseFileId(caseFile.getId())
                .proceedingKey(rootKey)
                .shadow(false)
                .status(CaseProceedingStatus.ACTIVE)
                .instanceLevel(ctx.currentInstance())
                .court(tribunal)
                .numeroUnificado(numero)
                .linkedProcessoId(processo.getId())
                .continuityTrack(resolveTrack(processo))
                .proceedingRole(CaseProceedingRole.ROOT)
                .sourceFaseProcessual(processo.getFaseAtual())
                .sourceStatusProcesso(processo.getStatusProcesso())
                .secrecy(ctx.currentSecrecy())
                .sourceSystem(ctx.originSystem())
                .build();

        try {
            CaseProceeding saved = proceedingRepository.save(proceeding);
            eventStore.append(caseFile.getId(), CaseFileEventType.ROOT_PROCEEDING_ASSURED,
                    new RootProceedingAssuredPayload(rootKey, processo.getId(), numero, tribunal));
            return saved;
        } catch (DataIntegrityViolationException race) {
            return proceedingRepository.findFirstByCaseFileIdAndLinkedProcessoId(caseFile.getId(), processo.getId())
                    .orElseThrow(() -> race);
        }
    }

    private ProceduralContext buildContext(Processo p, LegalIntegrationSystem originSystem) {
        Jurisdicao j = p.getJurisdicao();
        CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(java.util.Map.ofEntries(
                java.util.Map.entry("rito", p.getRito() != null ? p.getRito().name() : ""),
                java.util.Map.entry("ramoDireito", p.getRamoDireito() != null ? p.getRamoDireito().name() : ""),
                java.util.Map.entry("classeTpu", java.util.Objects.toString(p.getClasseProcessual(), "")),
                java.util.Map.entry("classeProcessual", java.util.Objects.toString(p.getClasseProcessual(), "")),
                java.util.Map.entry("assunto", java.util.Objects.toString(p.getAssunto(), "")),
                java.util.Map.entry("competencia", p.getTipoJustica() != null ? p.getTipoJustica().name() : ""),
                java.util.Map.entry("uf", j != null ? java.util.Objects.toString(j.getUf(), "") : ""),
                java.util.Map.entry("comarca", j != null ? java.util.Objects.toString(j.getCidade(), "") : ""),
                java.util.Map.entry("tribunalCodigo", j != null ? java.util.Objects.toString(j.getSigla(), "") : "")
        ));
        TipoJustica tj = resolveTipoJustica(p.getTipoJustica(), canonicalContext);
        String uf = canonicalContext.metadata().get("uf") instanceof String ufValue && !ufValue.isBlank()
                ? ufValue
                : (j != null ? java.util.Objects.toString(j.getUf(), "") : "");
        String tribunal = canonicalContext.tribunalCodigo() != null && !canonicalContext.tribunalCodigo().isBlank()
                ? canonicalContext.tribunalCodigo()
                : (j != null ? (j.getSigla() != null ? j.getSigla() : j.getNome()) : "");
        InstanceLevel instance = mapInstance(j != null ? j.getGrau() : null);
        NivelSigilo sigilo = p.getNivelSigilo() != null ? p.getNivelSigilo() : NivelSigilo.PUBLICO;
        com.tcc.pjb.backend.model.entity.enums.RamoDireito ramoDireito = p.getRamoDireito() != null
                ? p.getRamoDireito()
                : com.tcc.pjb.backend.model.entity.enums.RamoDireito.fromString(canonicalContext.ramoDireito());
        if (ramoDireito == null) {
            ramoDireito = com.tcc.pjb.backend.model.entity.enums.RamoDireito.CIVIL;
        }
        var ritoSnapshot = processoRitoSnapshotService.resolve(p, null);
        com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual rito = ritoSnapshot.rito() != null
                ? ritoSnapshot.rito()
                : (canonicalContext.rito() != null ? canonicalContext.rito() : com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.COMUM_ORDINARIO);

        return new ProceduralContext(
                p.getId(),
                p.getNumeroUnificado() != null ? p.getNumeroUnificado() : p.getNumeroProcesso(),
                tj,
                ramoDireito,
                p.getMateria() != null ? p.getMateria() : (j != null ? j.getMateria() : com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao.CIVIL),
                rito,
                uf,
                tribunal,
                instance,
                sigilo,
                originSystem != null ? originSystem : LegalIntegrationSystem.OTHER
        );
    }


    private static TipoJustica resolveTipoJustica(TipoJustica atual, CanonicalContext canonicalContext) {
        if (atual != null) {
            return atual;
        }
        String ramo = canonicalContext == null ? null : canonicalContext.ramoJusticaNacional();
        if (ramo == null || ramo.isBlank()) {
            return TipoJustica.ESTADUAL;
        }
        return switch (ramo.toUpperCase(java.util.Locale.ROOT)) {
            case "ELEITORAL", "ELEITORAL_SUPERIOR" -> TipoJustica.ELEITORAL;
            case "TRABALHO", "TRABALHO_SUPERIOR" -> TipoJustica.TRABALHO;
            case "FEDERAL" -> TipoJustica.FEDERAL;
            case "MILITAR_ESTADUAL" -> TipoJustica.MILITAR_ESTADUAL;
            case "MILITAR_FEDERAL", "MILITAR_SUPERIOR" -> TipoJustica.MILITAR_FEDERAL;
            case "SUPERIOR", "SUPERIOR_STF" -> TipoJustica.SUPERIOR;
            default -> TipoJustica.ESTADUAL;
        };
    }


    private static CaseContinuityTrack resolveTrack(Processo processo) {
        if (processo == null) return CaseContinuityTrack.CONHECIMENTO;
        if (processo.getStatusProcesso() == com.tcc.pjb.backend.model.entity.enums.StatusProcesso.ARQUIVADO) return CaseContinuityTrack.ARQUIVADO;
        if (processo.getStatusProcesso() == com.tcc.pjb.backend.model.entity.enums.StatusProcesso.TRANSITO_EM_JULGADO) return CaseContinuityTrack.TRANSITO;
        if (processo.getStatusProcesso() == com.tcc.pjb.backend.model.entity.enums.StatusProcesso.CUMPRIMENTO_SENTENCA) return CaseContinuityTrack.CUMPRIMENTO;
        if (processo.getFaseAtual() == com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual.RECURSAL) return CaseContinuityTrack.RECURSAL;
        if (processo.getFaseAtual() == com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual.CUMPRIMENTO_SENTENCA) return CaseContinuityTrack.CUMPRIMENTO;
        if (processo.getFaseAtual() == com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual.EXECUCAO) return CaseContinuityTrack.EXECUCAO;
        return CaseContinuityTrack.CONHECIMENTO;
    }

    private static InstanceLevel mapInstance(GrauJurisdicao grau) {
        if (grau == null) return InstanceLevel.FIRST_INSTANCE;
        return switch (grau) {
            case PRIMEIRO_GRAU -> InstanceLevel.FIRST_INSTANCE;
            case SEGUNDO_GRAU -> InstanceLevel.SECOND_INSTANCE;
            case SUPERIOR -> InstanceLevel.SUPERIOR;
            case CONSTITUCIONAL -> InstanceLevel.EXTRAORDINARY;
        };
    }

    
    private record CaseFileCreatedPayload(Long rootProcessoId) {}

    private record RootProceedingAssuredPayload(String proceedingKey, Long processoId, String numero, String tribunal) {}
}
