package com.tcc.pjb.backend.core.comunicacao.institucional.persistence;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalCatalogUnitSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalCatalogUnitSnapshotRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;

@Component
public class InstitutionalCatalogPersistenceSyncService {

    private final CatalogoInstitucionalUnificadoService catalogo;
    private final InstitutionalCatalogUnitSnapshotRepository repository;
    private final InstitutionalSnapshotJsonCodec codec;
    private final Clock clock;
    private final ComarcaResolutionService comarcaResolutionService;

    public InstitutionalCatalogPersistenceSyncService(CatalogoInstitucionalUnificadoService catalogo,
                                                      InstitutionalCatalogUnitSnapshotRepository repository,
                                                      InstitutionalSnapshotJsonCodec codec,
                                                      Clock clock,
                                                      ComarcaResolutionService comarcaResolutionService) {
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.comarcaResolutionService = Objects.requireNonNull(comarcaResolutionService, "comarcaResolutionService");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronize() {
        Instant now = clock.instant();
        List<UnidadeInstitucional> units = catalogo.listarPorTipo(null);
        for (UnidadeInstitucional unit : units) {
            String snapshotJson = codec.write(unit);
            String snapshotHash = unit.codigo() + ":" + Integer.toHexString(snapshotJson.hashCode());
            Comarca comarcaEntidade = resolveComarca(unit.comarca(), unit.uf());
            repository.findTopByCodigoUnidadeOrderByVigenciaInicioDesc(unit.codigo())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(snapshotHash, snapshotJson, unit.ativa(), now);
                                existing.setComarcaEntidade(comarcaEntidade);
                                repository.save(existing);
                            },
                            () -> {
                                InstitutionalCatalogUnitSnapshot novo = new InstitutionalCatalogUnitSnapshot(
                                        unit.codigo(),
                                        unit.destinatarioKind().name(),
                                        unit.uf(),
                                        unit.comarca(),
                                        unit.foro(),
                                        unit.ramoDireito() == null ? null : unit.ramoDireito().name(),
                                        unit.grauJurisdicao() == null ? null : unit.grauJurisdicao().name(),
                                        unit.ativa(),
                                        now,
                                        null,
                                        snapshotHash,
                                        snapshotJson,
                                        now,
                                        now);
                                novo.setComarcaEntidade(comarcaEntidade);
                                repository.save(novo);
                            });
        }
    }

    private Comarca resolveComarca(String comarca, String uf) {
        if (comarca == null || comarca.isBlank()) {
            return null;
        }
        return comarcaResolutionService.resolver(comarca, uf).orElse(null);
    }
}
