package com.tcc.pjb.backend.service.processual.peticionamento.workspace;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.model.repository.PrecedenteRepository;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoJurisprudenciaWorkspaceService {

    private final PrecedenteRepository precedenteRepository;
    private final ProceduralCatalogService proceduralCatalogService;

    public PeticionamentoJurisprudenciaWorkspaceService(PrecedenteRepository precedenteRepository,
                                                        ProceduralCatalogService proceduralCatalogService) {
        this.precedenteRepository = Objects.requireNonNull(precedenteRepository);
        this.proceduralCatalogService = Objects.requireNonNull(proceduralCatalogService);
    }

    public WorkspaceProjection resolve(PeticionamentoSessaoRequest request,
                                       PeticionamentoInitialIntakeWorkspaceService.IntakeResult intake) {
        String query = buildQuery(request, intake);
        if (query == null) {
            return WorkspaceProjection.empty();
        }
        String ramoRaw = firstNonBlank(
                request == null ? null : request.getRamoDireito(),
                intake == null ? null : intake.resolvedDraftRequest().ramoDireito()
        );
        String ritoRaw = firstNonBlank(
                request == null ? null : request.getRitoProcessual(),
                intake == null ? null : intake.resolvedDraftRequest().ritoProcessual()
        );
        String classeRaw = firstNonBlank(
                request == null ? null : request.getClasseProcessual(),
                intake == null ? null : intake.resolvedDraftRequest().classeProcessual()
        );
        RamoDireito ramo = RamoDireito.fromString(ramoRaw);
        RitoProcessual rito = proceduralCatalogService.resolveRito(ritoRaw, ramoRaw, classeRaw);
        List<Precedente> precedentes = precedenteRepository.search(null, null, ramo, rito, query, PageRequest.of(0, 5)).getContent();
        ArrayList<Map<String, Object>> items = new ArrayList<>();
        for (Precedente precedente : precedentes) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("id", precedente.getId());
            row.put("fonte", precedente.getFonte() == null ? null : precedente.getFonte().name());
            row.put("tipo", precedente.getTipo() == null ? null : precedente.getTipo().name());
            row.put("identificador", precedente.getIdentificador());
            row.put("titulo", precedente.getTitulo());
            row.put("tese", precedente.getTese());
            row.put("ementaResumo", precedente.getEmentaResumo());
            row.put("dataPublicacao", precedente.getDataPublicacao() == null ? null : precedente.getDataPublicacao().toString());
            row.put("urlReferencia", precedente.getUrlReferencia());
            row.put("sinalRecencia", resolveRecencySignal(precedente.getDataPublicacao()));
            row.put("aderenciaProcedure", resolveProcedureSignal(rito, precedente));
            items.add(Map.copyOf(row));
        }
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("A janela jurisprudencial foi alinhada ao ramo e ao rito resolvidos para a sessão de peticionamento.");
        if (classeRaw != null && !classeRaw.isBlank()) {
            fundamentos.add("A consulta preserva proximidade material com a classe processual informada no intake.");
        }
        if (!items.isEmpty()) {
            fundamentos.add("A amostra prioriza precedentes mais recentes dentro da mesma moldura procedimental, sem abrir trilha paralela ao editor nativo.");
        }
        String profile = items.isEmpty() ? "SEM_PRECEDENTE_MATERIALIZADO" : "JANELA_PRECEDENTE_ATIVA";
        return new WorkspaceProjection(
                profile,
                query,
                ramo == null ? null : ramo.name(),
                rito == null ? null : rito.name(),
                List.copyOf(items),
                List.copyOf(fundamentos),
                !items.isEmpty()
        );
    }

    private String buildQuery(PeticionamentoSessaoRequest request,
                              PeticionamentoInitialIntakeWorkspaceService.IntakeResult intake) {
        if (request == null && intake == null) {
            return null;
        }
        String joined = Stream.of(
                        request == null ? null : request.getTituloCaso(),
                        request == null ? null : request.getClasseProcessual(),
                        request == null ? null : request.getAssuntoTpu(),
                        request == null ? null : request.getMateriaPrincipal(),
                        request == null ? null : request.getNaturezaJuridica(),
                        request == null ? null : request.getTextoFatosResumido(),
                        request == null ? null : firstListToken(request.getFundamentosJuridicos()),
                        request == null ? null : firstListToken(request.getPedidos()),
                        intake == null ? null : intake.resolvedDraftRequest().tituloCaso(),
                        intake == null ? null : intake.resolvedDraftRequest().classeProcessual(),
                        intake == null ? null : intake.resolvedDraftRequest().naturezaJuridica(),
                        intake == null ? null : firstListToken(intake.resolvedDraftRequest().fatos()),
                        intake == null ? null : firstListToken(intake.resolvedDraftRequest().pedidos())
                )
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(6)
                .reduce((left, right) -> left + ' ' + right)
                .orElse(null);
        if (joined == null) {
            return null;
        }
        String normalized = joined.replaceAll("\\s+", " ").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String resolveRecencySignal(LocalDate dataPublicacao) {
        if (dataPublicacao == null) {
            return "SEM_DATA";
        }
        LocalDate thresholdRecent = LocalDate.now().minusYears(2);
        LocalDate thresholdStable = LocalDate.now().minusYears(6);
        if (!dataPublicacao.isBefore(thresholdRecent)) {
            return "RECENTE";
        }
        if (!dataPublicacao.isBefore(thresholdStable)) {
            return "ESTAVEL";
        }
        return "HISTORICO";
    }

    private String resolveProcedureSignal(RitoProcessual rito, Precedente precedente) {
        if (rito == null || precedente == null || precedente.getRitoSugerido() == null) {
            return "ADERENCIA_MATERIAL";
        }
        return rito == precedente.getRitoSugerido() ? "ADERENCIA_DIRETA" : "ADERENCIA_MATERIAL";
    }

    private String firstListToken(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
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

    public record WorkspaceProjection(String profile,
                                      String query,
                                      String ramo,
                                      String rito,
                                      List<Map<String, Object>> items,
                                      List<String> fundamentos,
                                      boolean ativo) {
        public static WorkspaceProjection empty() {
            return new WorkspaceProjection(
                    "SEM_JANELA_DE_CONSULTA",
                    null,
                    null,
                    null,
                    List.of(),
                    List.of("A sessão ainda não reuniu massa semântica suficiente para abrir a janela jurisprudencial com segurança."),
                    false
            );
        }

        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("profile", profile);
            map.put("query", query);
            map.put("ramo", ramo);
            map.put("rito", rito);
            map.put("items", items);
            map.put("fundamentos", fundamentos);
            map.put("ativo", ativo);
            map.put("usageMode", ativo ? "EDITOR_NATIVO_ASSISTIDO" : "AGUARDANDO_MASSA_SEMANTICA");
            return Map.copyOf(map);
        }
    }
}
