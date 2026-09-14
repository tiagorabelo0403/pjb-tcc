package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import static com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoPilarSupport.available;
import static com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoPilarSupport.capacidade;
import static com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoPilarSupport.pilar;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyService;
import com.tcc.pjb.backend.core.idempotency.ActionIdempotencyService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoCapacidade;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoFactoryApplicationService;
import com.tcc.pjb.backend.core.processo.transicao.application.ProcessoConvivenciaTransicaoApplicationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Avalia o pilar "interoperabilidade e migração" (Fatia F6 -- extraído de
 * PjbArquiteturaSubstituicaoNacionalApplicationService).
 */
@Component
public class PjbArquiteturaInteroperabilidadePilarEvaluator {

    private final ObjectProvider<PjbSubstituicaoLegadosApplicationService> substituicaoLegadosProvider;
    private final ObjectProvider<ProcessoMigracaoFactoryApplicationService> migracaoFactoryProvider;
    private final ObjectProvider<ProcessoConvivenciaTransicaoApplicationService> transicaoProvider;
    private final ObjectProvider<ProcessoMigracaoApplicationService> migracaoProvider;
    private final ObjectProvider<AuditLedgerService> auditLedgerServiceProvider;
    private final ObjectProvider<ActionIdempotencyService> actionIdempotencyServiceProvider;
    private final ObjectProvider<RequestIdempotencyService> requestIdempotencyServiceProvider;

    public PjbArquiteturaInteroperabilidadePilarEvaluator(
            ObjectProvider<PjbSubstituicaoLegadosApplicationService> substituicaoLegadosProvider,
            ObjectProvider<ProcessoMigracaoFactoryApplicationService> migracaoFactoryProvider,
            ObjectProvider<ProcessoConvivenciaTransicaoApplicationService> transicaoProvider,
            ObjectProvider<ProcessoMigracaoApplicationService> migracaoProvider,
            ObjectProvider<AuditLedgerService> auditLedgerServiceProvider,
            ObjectProvider<ActionIdempotencyService> actionIdempotencyServiceProvider,
            ObjectProvider<RequestIdempotencyService> requestIdempotencyServiceProvider) {
        this.substituicaoLegadosProvider = Objects.requireNonNull(substituicaoLegadosProvider);
        this.migracaoFactoryProvider = Objects.requireNonNull(migracaoFactoryProvider);
        this.transicaoProvider = Objects.requireNonNull(transicaoProvider);
        this.migracaoProvider = Objects.requireNonNull(migracaoProvider);
        this.auditLedgerServiceProvider = Objects.requireNonNull(auditLedgerServiceProvider);
        this.actionIdempotencyServiceProvider = Objects.requireNonNull(actionIdempotencyServiceProvider);
        this.requestIdempotencyServiceProvider = Objects.requireNonNull(requestIdempotencyServiceProvider);
    }

    public PjbArquiteturaSubstituicaoPilar avaliar() {
        ArrayList<PjbArquiteturaSubstituicaoCapacidade> capacidades = new ArrayList<>();
        capacidades.add(capacidade(
                "interop.integracao",
                "Conectores nacionais, submissão e sincronização",
                available(substituicaoLegadosProvider),
                91,
                List.of("PjbSubstituicaoLegadosApplicationService", "AdministradorNacionalGovernanceService"),
                List.of("Ampliar matriz de conector por tribunal e trilha de corte gradual")
        ));
        capacidades.add(capacidade(
                "interop.importacao-legado",
                "Importação e fábrica de migração com mapeamento canônico",
                available(migracaoFactoryProvider),
                93,
                List.of("ProcessoMigracaoFactoryApplicationService", "Acervo, movimentos, documentos, filas e sigilos modelados"),
                List.of("Concluir lacres finais de assinatura e revalidação em lote nacional")
        ));
        capacidades.add(capacidade(
                "interop.shadow-e-convivencia",
                "Shadow mode, convivência com legado e reversibilidade",
                available(transicaoProvider) && available(migracaoProvider),
                90,
                List.of("ProcessoConvivenciaTransicaoApplicationService", "ProcessoMigracaoApplicationService"),
                List.of("Endurecer dual-write governado, rollback e checkpoints por onda de tribunal")
        ));
        capacidades.add(capacidade(
                "interop.reconciliacao-metadata",
                "Reconciliação de metadados e trilha comparativa",
                available(migracaoProvider),
                88,
                List.of("ProcessoMigracaoApplicationService", "Comparação de sombra, readiness e divergência controlada"),
                List.of("Amarrar reconciliação intertribunal e saneamento automático de divergências canônicas")
        ));
        capacidades.add(capacidade(
                "interop.auditoria-rollback",
                "Auditoria, rollback seguro e replay controlado",
                available(auditLedgerServiceProvider) && available(actionIdempotencyServiceProvider) && available(requestIdempotencyServiceProvider),
                87,
                List.of("AuditLedgerService", "ActionIdempotencyService", "RequestIdempotencyService"),
                List.of("Fechar rollback transacional por corte de tribunal e rito de alta criticidade")
        ));
        return pilar(
                "interoperabilidade-migracao",
                "Camada pesada de interoperabilidade e migração",
                capacidades,
                List.of(
                        "Fechar runbook de corte, reversão e replay por tribunal, ramo e rito sensível.",
                        "Endurecer reconciliação de assinatura, documento e metadado em migração massiva."
                )
        );
    }
}
