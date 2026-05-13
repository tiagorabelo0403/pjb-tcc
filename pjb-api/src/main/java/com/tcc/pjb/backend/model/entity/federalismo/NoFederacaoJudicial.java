package com.tcc.pjb.backend.model.entity.federalismo;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import com.tcc.pjb.backend.domain.enums.TipoJustica;

@PjbDataOwnership(module = PjbModuleId.INTEGRACOES_EXTERNAS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(
        name = "tb_no_federacao_judicial",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_no_federacao_judicial_codigo", columnNames = "codigo_tribunal")
        },
        indexes = {
                @Index(name = "idx_no_federacao_status", columnList = "status_atual, ultima_heartbeat_em"),
                @Index(name = "idx_no_federacao_tipo", columnList = "tipo_justica, uf"),
                @Index(name = "idx_no_federacao_regiao", columnList = "regiao, zona"),
                @Index(name = "idx_no_federacao_schema", columnList = "versao_schema_atual")
        }
)
public class NoFederacaoJudicial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_tribunal", nullable = false, length = 20)
    private String codigoTribunal;

    @Column(name = "nome", nullable = false, length = 180)
    private String nome;

    @Column(name = "uf", length = 2)
    private String uf;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_justica", nullable = false, length = 30)
    private TipoJustica tipoJustica;

    @Column(name = "endpoint_principal", nullable = false, length = 240)
    private String endpointPrincipal;

    @Column(name = "endpoint_backup", length = 240)
    private String endpointBackup;

    @Column(name = "kafka_brokers", length = 500)
    private String kafkaBrokers;

    @Column(name = "chave_publica_base64", columnDefinition = "TEXT")
    private String chavePublicaBase64;

    @Column(name = "chave_publica_fingerprint", length = 64)
    private String chavePublicaFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_atual", nullable = false, length = 30)
    private StatusNoFederacao statusAtual;

    @Column(name = "ultima_heartbeat_em")
    private Instant ultimaHeartbeatEm;

    @Column(name = "ultima_sincronizacao_em")
    private Instant ultimaSincronizacaoEm;

    @Column(name = "ultima_falha_em")
    private Instant ultimaFalhaEm;

    @Column(name = "backlog_pendente", nullable = false)
    private long backlogPendente;

    @Column(name = "capacidade_backlog", nullable = false)
    private long capacidadeBacklog;

    @Column(name = "operacao_autonoma_ativa", nullable = false)
    private boolean operacaoAutonomaAtiva;

    @Column(name = "aceita_recepcao_eventos", nullable = false)
    private boolean aceitaRecepcaoEventos;

    @Column(name = "versao_schema_atual", nullable = false)
    private long versaoSchemaAtual;

    @Column(name = "regiao", length = 60)
    private String regiao;

    @Column(name = "zona", length = 60)
    private String zona;

    @Column(name = "prioridade_failover", nullable = false)
    private int prioridadeFailover;

    @Column(name = "clock_logico", nullable = false)
    private long clockLogico;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_no_federacao_topico", joinColumns = @JoinColumn(name = "no_id"))
    @Column(name = "topico", nullable = false, length = 180)
    private Set<String> topicosPermitidos = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_no_federacao_capacidade", joinColumns = @JoinColumn(name = "no_id"))
    @Column(name = "capacidade", nullable = false, length = 120)
    private Set<String> capacidades = new LinkedHashSet<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    @Column(name = "versao")
    private Long versao;

    protected NoFederacaoJudicial() {
    }

    public NoFederacaoJudicial(String codigoTribunal,
                               String nome,
                               String uf,
                               TipoJustica tipoJustica,
                               String endpointPrincipal) {
        this.codigoTribunal = Objects.requireNonNull(codigoTribunal);
        this.nome = Objects.requireNonNull(nome);
        this.uf = uf;
        this.tipoJustica = Objects.requireNonNull(tipoJustica);
        this.endpointPrincipal = Objects.requireNonNull(endpointPrincipal);
        this.statusAtual = StatusNoFederacao.OFFLINE_ISOLADO;
        this.capacidadeBacklog = 10_000L;
        this.aceitaRecepcaoEventos = true;
        this.operacaoAutonomaAtiva = true;
        this.versaoSchemaAtual = 1L;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        this.codigoTribunal = normalizeUpper(this.codigoTribunal);
        this.nome = normalizeText(this.nome);
        this.uf = normalizeUpper(this.uf);
        this.endpointPrincipal = normalizeText(this.endpointPrincipal);
        this.endpointBackup = normalizeText(this.endpointBackup);
        this.kafkaBrokers = normalizeText(this.kafkaBrokers);
        this.regiao = normalizeText(this.regiao);
        this.zona = normalizeText(this.zona);
        this.topicosPermitidos = sanitizeSet(this.topicosPermitidos);
        this.capacidades = sanitizeSet(this.capacidades);
        if (this.statusAtual == null) {
            this.statusAtual = StatusNoFederacao.OFFLINE_ISOLADO;
        }
        if (this.capacidadeBacklog < 1) {
            this.capacidadeBacklog = 1L;
        }
        if (this.backlogPendente < 0) {
            this.backlogPendente = 0L;
        }
        if (this.prioridadeFailover < 0) {
            this.prioridadeFailover = 0;
        }
        if (this.prioridadeFailover > 100) {
            this.prioridadeFailover = 100;
        }
        if (this.versaoSchemaAtual < 1) {
            this.versaoSchemaAtual = 1L;
        }
        Instant agora = Instant.now();
        if (this.criadoEm == null) {
            this.criadoEm = agora;
        }
        this.atualizadoEm = agora;
    }

    public boolean estaOnline() {
        if (!(statusAtual == StatusNoFederacao.ONLINE || statusAtual == StatusNoFederacao.SINCRONIZANDO || statusAtual == StatusNoFederacao.DEGRADADO)) {
            return false;
        }
        return ultimaHeartbeatEm != null && ultimaHeartbeatEm.isAfter(Instant.now().minusSeconds(120));
    }

    public boolean precisaSincronizar() {
        return statusAtual == StatusNoFederacao.OFFLINE_RECONECTANDO || statusAtual == StatusNoFederacao.SINCRONIZANDO || backlogPendente > 0;
    }

    public double disponibilidadeFederativa() {
        double backlogFactor = 1.0d - Math.min(1.0d, (double) backlogPendente / (double) Math.max(1L, capacidadeBacklog));
        double statusFactor = switch (statusAtual) {
            case ONLINE -> 1.0d;
            case SINCRONIZANDO -> 0.85d;
            case DEGRADADO -> 0.65d;
            case OFFLINE_RECONECTANDO -> 0.35d;
            case MANUTENCAO -> 0.10d;
            case OFFLINE_ISOLADO -> 0.25d;
        };
        return Math.max(0.0d, Math.min(1.0d, backlogFactor * statusFactor));
    }

    public boolean suportaTopico(String topico) {
        if (topico == null || topico.isBlank()) {
            return false;
        }
        if (topicosPermitidos.isEmpty()) {
            return true;
        }
        String normalized = normalizeUpper(topico);
        return topicosPermitidos.stream().anyMatch(item -> normalizeUpper(item).equals(normalized));
    }

    public boolean suportaCapacidade(String capacidade) {
        if (capacidade == null || capacidade.isBlank()) {
            return false;
        }
        String normalized = normalizeUpper(capacidade);
        return capacidades.stream().anyMatch(item -> normalizeUpper(item).equals(normalized));
    }

    public void registrarHeartbeat(StatusNoFederacao status, long versaoSchemaAtual) {
        this.statusAtual = Objects.requireNonNull(status);
        this.versaoSchemaAtual = Math.max(1L, versaoSchemaAtual);
        this.ultimaHeartbeatEm = Instant.now();
        this.clockLogico = this.clockLogico + 1L;
    }

    public void registrarSincronizacaoConcluida(long backlogRestante) {
        this.backlogPendente = Math.max(0L, backlogRestante);
        this.ultimaSincronizacaoEm = Instant.now();
        this.statusAtual = this.backlogPendente == 0 ? StatusNoFederacao.ONLINE : StatusNoFederacao.SINCRONIZANDO;
        this.clockLogico = this.clockLogico + 1L;
    }

    public void registrarFalha(String motivo) {
        this.ultimaFalhaEm = Instant.now();
        if (this.statusAtual != StatusNoFederacao.MANUTENCAO) {
            this.statusAtual = StatusNoFederacao.DEGRADADO;
        }
        this.operacaoAutonomaAtiva = true;
        this.clockLogico = this.clockLogico + 1L;
    }

    public void incrementarBacklog() {
        this.backlogPendente = this.backlogPendente + 1L;
        this.clockLogico = this.clockLogico + 1L;
    }

    public void decrementarBacklog() {
        this.backlogPendente = Math.max(0L, this.backlogPendente - 1L);
        this.clockLogico = this.clockLogico + 1L;
    }

    private static Set<String> sanitizeSet(Set<String> source) {
        Set<String> result = new LinkedHashSet<>();
        if (source == null) {
            return result;
        }
        for (String item : source) {
            String normalized = normalizeUpper(item);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    private static String normalizeUpper(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public Long getId() {
        return id;
    }

    public String getCodigoTribunal() {
        return codigoTribunal;
    }

    public String getNome() {
        return nome;
    }

    public String getUf() {
        return uf;
    }

    public TipoJustica getTipoJustica() {
        return tipoJustica;
    }

    public String getEndpointPrincipal() {
        return endpointPrincipal;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public void setTipoJustica(TipoJustica tipoJustica) {
        this.tipoJustica = tipoJustica;
    }

    public void setEndpointPrincipal(String endpointPrincipal) {
        this.endpointPrincipal = endpointPrincipal;
    }

    public String getEndpointBackup() {
        return endpointBackup;
    }

    public void setEndpointBackup(String endpointBackup) {
        this.endpointBackup = endpointBackup;
    }

    public String getKafkaBrokers() {
        return kafkaBrokers;
    }

    public void setKafkaBrokers(String kafkaBrokers) {
        this.kafkaBrokers = kafkaBrokers;
    }

    public String getChavePublicaBase64() {
        return chavePublicaBase64;
    }

    public void setChavePublicaBase64(String chavePublicaBase64) {
        this.chavePublicaBase64 = chavePublicaBase64;
    }

    public String getChavePublicaFingerprint() {
        return chavePublicaFingerprint;
    }

    public void setChavePublicaFingerprint(String chavePublicaFingerprint) {
        this.chavePublicaFingerprint = chavePublicaFingerprint;
    }

    public StatusNoFederacao getStatusAtual() {
        return statusAtual;
    }

    public void setStatusAtual(StatusNoFederacao statusAtual) {
        this.statusAtual = statusAtual;
    }

    public Instant getUltimaHeartbeatEm() {
        return ultimaHeartbeatEm;
    }

    public Instant getUltimaSincronizacaoEm() {
        return ultimaSincronizacaoEm;
    }

    public Instant getUltimaFalhaEm() {
        return ultimaFalhaEm;
    }

    public long getBacklogPendente() {
        return backlogPendente;
    }

    public void setBacklogPendente(long backlogPendente) {
        this.backlogPendente = backlogPendente;
    }

    public long getCapacidadeBacklog() {
        return capacidadeBacklog;
    }

    public void setCapacidadeBacklog(long capacidadeBacklog) {
        this.capacidadeBacklog = capacidadeBacklog;
    }

    public boolean isOperacaoAutonomaAtiva() {
        return operacaoAutonomaAtiva;
    }

    public void setOperacaoAutonomaAtiva(boolean operacaoAutonomaAtiva) {
        this.operacaoAutonomaAtiva = operacaoAutonomaAtiva;
    }

    public boolean isAceitaRecepcaoEventos() {
        return aceitaRecepcaoEventos;
    }

    public void setAceitaRecepcaoEventos(boolean aceitaRecepcaoEventos) {
        this.aceitaRecepcaoEventos = aceitaRecepcaoEventos;
    }

    public long getVersaoSchemaAtual() {
        return versaoSchemaAtual;
    }

    public void setVersaoSchemaAtual(long versaoSchemaAtual) {
        this.versaoSchemaAtual = versaoSchemaAtual;
    }

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) {
        this.regiao = regiao;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public int getPrioridadeFailover() {
        return prioridadeFailover;
    }

    public void setPrioridadeFailover(int prioridadeFailover) {
        this.prioridadeFailover = prioridadeFailover;
    }

    public long getClockLogico() {
        return clockLogico;
    }

    public Set<String> getTopicosPermitidos() {
        return topicosPermitidos;
    }

    public void setTopicosPermitidos(Set<String> topicosPermitidos) {
        this.topicosPermitidos = topicosPermitidos;
    }

    public Set<String> getCapacidades() {
        return capacidades;
    }

    public void setCapacidades(Set<String> capacidades) {
        this.capacidades = capacidades;
    }
}
