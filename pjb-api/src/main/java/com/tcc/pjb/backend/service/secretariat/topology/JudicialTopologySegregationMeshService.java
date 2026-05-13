package com.tcc.pjb.backend.service.secretariat.topology;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingProfile;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingResolver;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudicialTopologySegregationMeshService {

    private final ProcessoRepository processoRepository;
    private final SecretariatOperationalRoutingResolver secretariatRoutingResolver;
    private final JuizGabineteRoutingResolver juizGabineteRoutingResolver;

    public JudicialTopologySegregationMeshService(ProcessoRepository processoRepository,
                                                  SecretariatOperationalRoutingResolver secretariatRoutingResolver,
                                                  JuizGabineteRoutingResolver juizGabineteRoutingResolver) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.secretariatRoutingResolver = Objects.requireNonNull(secretariatRoutingResolver);
        this.juizGabineteRoutingResolver = Objects.requireNonNull(juizGabineteRoutingResolver);
    }

    @Transactional(readOnly = true)
    public JudicialTopologySegregationMeshSnapshot snapshot(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        SecretariatOperationalRoutingProfile routing = secretariatRoutingResolver.resolve(processo);
        JuizGabineteRoutingProfile gabinete = juizGabineteRoutingResolver.resolve(processo);
        NationalJudicialTopologyService.NationalJudicialTopologyProfile topology = gabinete.topology();

        LinkedHashMap<String, Object> tribunal = new LinkedHashMap<>();
        tribunal.put("codigo", topology.judicialOrganCode());
        tribunal.put("nome", topology.judicialOrganName());
        tribunal.put("tipo", topology.judicialOrganKind());
        tribunal.put("instanciaAxis", topology.instanceAxis());
        tribunal.put("forumAxis", topology.forumAxis());

        LinkedHashMap<String, Object> forum = new LinkedHashMap<>();
        forum.put("coverageMode", topology.coverage() == null ? null : topology.coverage().coverageMode());
        forum.put("sourceMunicipality", topology.coverage() == null ? null : topology.coverage().sourceMunicipality());
        forum.put("seatMunicipality", topology.coverage() == null ? null : topology.coverage().seatMunicipality());
        forum.put("sourceUf", topology.coverage() == null ? null : topology.coverage().sourceUf());
        forum.put("territorialScope", topology.coverage() == null ? null : topology.coverage().territorialScope());
        forum.put("unitDescriptor", topology.unitDescriptor());

        LinkedHashMap<String, Object> secretaria = new LinkedHashMap<>();
        secretaria.put("secretariatCode", routing.secretariatCode());
        secretaria.put("receiptQueueCode", routing.receiptQueueCode());
        secretaria.put("saneamentoQueueCode", routing.saneamentoQueueCode());
        secretaria.put("audienceQueueCode", routing.audienceQueueCode());
        secretaria.put("executionQueueCode", routing.executionQueueCode());
        secretaria.put("receiptInboxKey", routing.receiptInboxKey());
        secretaria.put("audienceInboxKey", routing.audienceInboxKey());
        secretaria.put("executionInboxKey", routing.executionInboxKey());
        secretaria.put("specialization", routing.specialization() == null ? null : routing.specialization().toMap());
        secretaria.put("namespacePjb", routing.specialization() == null ? null : routing.specialization().namespacePjb());
        secretaria.put("painelPjb", routing.specialization() == null ? null : routing.specialization().painelPjb());
        secretaria.put("secretariatClass", routing.specialization() == null ? null : routing.specialization().secretariatClass());
        secretaria.put("secretariatInstanceClass", routing.specialization() == null ? null : routing.specialization().secretariatInstanceClass());
        secretaria.put("secretariatBranchClass", routing.specialization() == null ? null : routing.specialization().secretariatBranchClass());
        secretaria.put("specializedSecretariatName", routing.specialization() == null ? null : routing.specialization().specializedSecretariatName());
        secretaria.put("connectedCapabilities", routing.specialization() == null ? null : routing.specialization().connectedCapabilities());

        LinkedHashMap<String, Object> gabineteMap = new LinkedHashMap<>();
        gabineteMap.put("gabineteDesk", gabinete.gabineteDesk());
        gabineteMap.put("gabineteInboxKey", gabinete.gabineteInboxKey());
        gabineteMap.put("advisoryDesk", gabinete.advisoryDesk());
        gabineteMap.put("hearingDesk", gabinete.hearingDesk());
        gabineteMap.put("coordinationDesk", gabinete.coordinationDesk());
        gabineteMap.put("redistributionDesk", gabinete.redistributionDesk());
        gabineteMap.put("sessionChannel", gabinete.sessionChannel());

        tribunal.entrySet().removeIf(entry -> entry.getValue() == null);
        forum.entrySet().removeIf(entry -> entry.getValue() == null);
        secretaria.entrySet().removeIf(entry -> entry.getValue() == null);
        gabineteMap.entrySet().removeIf(entry -> entry.getValue() == null);

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("topologyKey", topology.topologyKey());
        metadata.put("routeKey", routing.routeKey());
        metadata.put("gabineteRouteKey", gabinete.routeKey());
        metadata.put("organizationalPath", topology.organizationalPath());
        metadata.put("tipoJustica", routing.tipoJustica());
        metadata.put("regimeAxis", routing.regimeAxis());
        metadata.put("ramoAxis", routing.ramoAxis());
        metadata.put("deskAxis", routing.deskAxis());
        metadata.put("laneAxis", topology.laneAxis());
        metadata.put("barriers", topology.isolationBarriers());
        metadata.put("topology", topology.toMap());
        metadata.put("secretariatRouting", routing.toMap());
        metadata.put("gabineteRouting", gabinete.toMap());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);

        return new JudicialTopologySegregationMeshSnapshot(
                processo.getId(),
                processo.getNumeroProcesso(),
                topology.topologyKey(),
                routing.tipoJustica(),
                topology.instanceAxis(),
                routing.regimeAxis(),
                topology.laneAxis(),
                topology.organizationalPath(),
                List.copyOf(topology.isolationBarriers()),
                Map.copyOf(tribunal),
                Map.copyOf(forum),
                Map.copyOf(secretaria),
                Map.copyOf(gabineteMap),
                Collections.unmodifiableMap(metadata)
        );
    }

    public record JudicialTopologySegregationMeshSnapshot(
            Long processoId,
            String numeroProcesso,
            String topologyKey,
            String tipoJustica,
            String instanciaAxis,
            String regimeAxis,
            String laneAxis,
            String organizationalPath,
            List<String> barriers,
            Map<String, Object> tribunal,
            Map<String, Object> forum,
            Map<String, Object> secretaria,
            Map<String, Object> gabinete,
            Map<String, Object> metadata
    ) {}
}
