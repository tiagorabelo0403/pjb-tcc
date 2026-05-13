package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(
        name = "tb_expedicao_judicial",
        indexes = {
                @Index(name = "idx_expedicao_processo_id", columnList = "processo_id"),
                @Index(name = "idx_expedicao_destinatario_doc", columnList = "destinatario_documento"),
                @Index(name = "idx_expedicao_status", columnList = "status"),
                @Index(name = "idx_expedicao_modalidade", columnList = "modalidade"),
                @Index(name = "idx_expedicao_expedida_em", columnList = "expedida_em"),
                @Index(name = "idx_expedicao_hash_integridade", columnList = "hash_integridade", unique = true)
        }
)
public class ExpedicaoJudicial {

    public enum StatusExpedicao {
        EXPEDIDA,
        ENTREGUE_CONFIRMADA,
        LIDA_CONFIRMADA,
        PRESUMIDA_ENTREGUE,
        FRUSTRADA_DEFINITIVA,
        PENDENTE_OFICIAL,
        REMETIDA_CORREIO,
        PUBLICADA_EDITAL,
        DEVOLVIDA,
        CANCELADA
    }

    public enum TipoDestinatario {
        PESSOA_FISICA,
        PESSOA_JURIDICA,
        ADVOGADO_OAB,
        DEFENSOR_PUBLICO,
        MINISTERIO_PUBLICO,
        FAZENDA_PUBLICA,
        JUIZO_DEPRECADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expedicao_uuid", nullable = false, unique = true, length = 36)
    private String expedicaoUuid;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "numero_unificado", length = 25)
    private String numeroUnificado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comunicacao", nullable = false, length = 60)
    private TipoComunicacaoJudicial tipoComunicacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade", nullable = false, length = 60)
    private ModalidadeExpedicaoJudicial modalidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private StatusExpedicao status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_destinatario", nullable = false, length = 40)
    private TipoDestinatario tipoDestinatario;

    @Column(name = "destinatario_nome", nullable = false, length = 200)
    private String destinatarioNome;

    @Column(name = "destinatario_documento", nullable = false, length = 40)
    private String destinatarioDocumento;

    @Column(name = "destinatario_email", length = 200)
    private String destinatarioEmail;

    @Column(name = "destinatario_telefone", length = 20)
    private String destinatarioTelefone;

    @Column(name = "destinatario_endereco_entrega", length = 500)
    private String destinatarioEnderecoEntrega;

    @Column(name = "canal_digital_utilizado", length = 120)
    private String canalDigitalUtilizado;

    @Column(name = "expedida_em", nullable = false)
    private Instant expedidaEm;

    @Column(name = "presuncao_entrega_em")
    private Instant presuncaoEntregaEm;

    @Column(name = "confirmacao_entrega_em")
    private Instant confirmacaoEntregaEm;

    @Column(name = "leitura_confirmada_em")
    private Instant leituraConfirmadaEm;

    @Column(name = "prazo_resposta_inicio_em")
    private Instant prazoRespostaInicioEm;

    @Column(name = "tentativas_realizadas", nullable = false)
    private int tentativasRealizadas;

    @Column(name = "proxima_tentativa_em")
    private Instant proximaTentativaEm;

    @Column(name = "frustrada_em")
    private Instant frustradaEm;

    @Column(name = "motivo_frustracao", length = 1000)
    private String motivoFrustracao;

    @Column(name = "fallback_modalidade", length = 60)
    private String fallbackModalidade;

    @Column(name = "ramo_direito", length = 50)
    private String ramoDireito;

    @Column(name = "grau_jurisdicao", length = 30)
    private String grauJurisdicao;

    @Column(name = "instancia_expedidora", length = 120)
    private String instanciaExpedidora;

    @Column(name = "juiz_responsavel_id")
    private Long juizResponsavelId;

    @Column(name = "servidor_expedidor_id")
    private Long servidorExpedidorId;

    @Column(name = "mandado_id")
    private Long mandadoId;

    @Column(name = "oficial_id")
    private Long oficialId;

    @Column(name = "codigo_rastreio_correio", length = 20)
    private String codigoRastreioCorreio;

    @Column(name = "numero_edital", length = 80)
    private String numeroEdital;

    @Column(name = "ip_leitura", length = 45)
    private String ipLeitura;

    @Column(name = "device_fingerprint", length = 256)
    private String deviceFingerprint;

    @Column(name = "govbr_session_token", length = 256)
    private String govbrSessionToken;

    @Column(name = "acuse_hash", length = 64)
    private String acuseHash;

    @Column(name = "hash_integridade", length = 64, nullable = false, unique = true)
    private String hashIntegridade;

    @Column(name = "hash_anterior", length = 64)
    private String hashAnterior;

    @Column(name = "fundamentacao_legal", length = 1200)
    private String fundamentacaoLegal;

    @Column(name = "evasao_detectada", nullable = false)
    private boolean evasaoDetectada;

    @Column(name = "evasao_evidencias", length = 1000)
    private String evasaoEvidencias;

    @Column(name = "escalonado_para_juiz", nullable = false)
    private boolean escalonadoParaJuiz;

    @Column(name = "escalonamento_em")
    private Instant escalonamentoEm;

    @Column(name = "ciencia_judicial_em")
    private Instant cienciaJudicialEm;

    @Version
    @Column(name = "version")
    private Long version;

    protected ExpedicaoJudicial() {
    }

    public ExpedicaoJudicial(Long processoId,
                             String numeroUnificado,
                             TipoComunicacaoJudicial tipoComunicacao,
                             ModalidadeExpedicaoJudicial modalidade,
                             TipoDestinatario tipoDestinatario,
                             String destinatarioNome,
                             String destinatarioDocumento,
                             String ramoDireito,
                             String grauJurisdicao,
                             String hashAnterior,
                             String hashIntegridade,
                             String fundamentacaoLegal) {
        this.expedicaoUuid = UUID.randomUUID().toString();
        this.processoId = processoId;
        this.numeroUnificado = numeroUnificado;
        this.tipoComunicacao = tipoComunicacao;
        this.modalidade = modalidade;
        this.status = StatusExpedicao.EXPEDIDA;
        this.tipoDestinatario = tipoDestinatario;
        this.destinatarioNome = destinatarioNome;
        this.destinatarioDocumento = destinatarioDocumento;
        this.ramoDireito = ramoDireito;
        this.grauJurisdicao = grauJurisdicao;
        this.hashAnterior = hashAnterior;
        this.hashIntegridade = hashIntegridade;
        this.fundamentacaoLegal = fundamentacaoLegal;
        this.expedidaEm = Instant.now();
        this.tentativasRealizadas = 1;
        if (modalidade != null && modalidade.temPresuncaoAutomatica()) {
            this.presuncaoEntregaEm = expedidaEm.plusSeconds((long) modalidade.getHorasPresuncaoEntrega() * 3600L);
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (expedicaoUuid == null || expedicaoUuid.isBlank()) {
            expedicaoUuid = UUID.randomUUID().toString();
        }
        destinatarioNome = trimToNull(destinatarioNome);
        numeroUnificado = trimToNull(numeroUnificado);
        destinatarioEmail = lowerTrim(destinatarioEmail);
        destinatarioTelefone = digitsOrNull(destinatarioTelefone);
        destinatarioDocumento = normalizeDocumento(destinatarioDocumento);
        destinatarioEnderecoEntrega = trimToNull(destinatarioEnderecoEntrega);
        canalDigitalUtilizado = trimToNull(canalDigitalUtilizado);
        motivoFrustracao = trimToNull(motivoFrustracao);
        fallbackModalidade = trimToNull(fallbackModalidade);
        ramoDireito = upperTrim(ramoDireito);
        grauJurisdicao = upperTrim(grauJurisdicao);
        instanciaExpedidora = trimToNull(instanciaExpedidora);
        codigoRastreioCorreio = upperTrim(codigoRastreioCorreio);
        numeroEdital = upperTrim(numeroEdital);
        ipLeitura = trimToNull(ipLeitura);
        deviceFingerprint = trimToNull(deviceFingerprint);
        govbrSessionToken = trimToNull(govbrSessionToken);
        acuseHash = lowerTrim(acuseHash);
        hashIntegridade = lowerTrim(hashIntegridade);
        hashAnterior = lowerTrim(hashAnterior);
        fundamentacaoLegal = trimToNull(fundamentacaoLegal);
        evasaoEvidencias = trimToNull(evasaoEvidencias);
        if (tentativasRealizadas < 0) {
            tentativasRealizadas = 0;
        }
        if (status == null) {
            status = StatusExpedicao.EXPEDIDA;
        }
    }

    public void confirmarEntrega(String acuseHash, String ipLeitura, String deviceFingerprint, String govbrSessionToken) {
        this.status = StatusExpedicao.ENTREGUE_CONFIRMADA;
        this.confirmacaoEntregaEm = Instant.now();
        this.acuseHash = lowerTrim(acuseHash);
        this.ipLeitura = trimToNull(ipLeitura);
        this.deviceFingerprint = trimToNull(deviceFingerprint);
        this.govbrSessionToken = trimToNull(govbrSessionToken);
        this.prazoRespostaInicioEm = this.confirmacaoEntregaEm;
    }

    public void confirmarEntregaAutomatica(String acuseHash, String canalDigitalUtilizado) {
        this.status = StatusExpedicao.ENTREGUE_CONFIRMADA;
        this.confirmacaoEntregaEm = Instant.now();
        this.acuseHash = lowerTrim(acuseHash);
        this.canalDigitalUtilizado = trimToNull(canalDigitalUtilizado);
        this.prazoRespostaInicioEm = this.confirmacaoEntregaEm;
    }

    public void confirmarLeitura(Instant leituraEm, String acuseHash) {
        this.status = StatusExpedicao.LIDA_CONFIRMADA;
        this.leituraConfirmadaEm = leituraEm != null ? leituraEm : Instant.now();
        this.acuseHash = lowerTrim(acuseHash);
        this.prazoRespostaInicioEm = this.leituraConfirmadaEm;
    }

    public void marcarPresumidaEntregue() {
        if (status == StatusExpedicao.EXPEDIDA) {
            this.status = StatusExpedicao.PRESUMIDA_ENTREGUE;
            this.confirmacaoEntregaEm = Instant.now();
            this.prazoRespostaInicioEm = this.confirmacaoEntregaEm;
        }
    }

    public void registrarFrustracao(String motivo, ModalidadeExpedicaoJudicial fallback) {
        this.motivoFrustracao = trimToNull(motivo);
        this.frustradaEm = Instant.now();
        this.fallbackModalidade = fallback != null ? fallback.name() : null;
        if (fallback == null) {
            this.status = StatusExpedicao.FRUSTRADA_DEFINITIVA;
        }
    }

    public void incrementarTentativa(Instant proxima) {
        this.tentativasRealizadas++;
        this.proximaTentativaEm = proxima;
    }

    public void registrarEvasao(String evidencias) {
        this.evasaoDetectada = true;
        this.evasaoEvidencias = trimToNull(evidencias);
    }

    public void escalonarParaJuiz() {
        this.escalonadoParaJuiz = true;
        this.escalonamentoEm = Instant.now();
    }

    public void registrarCienciaJudicial() {
        this.cienciaJudicialEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getExpedicaoUuid() {
        return expedicaoUuid;
    }

    public Long getProcessoId() {
        return processoId;
    }

    public String getNumeroUnificado() {
        return numeroUnificado;
    }

    public TipoComunicacaoJudicial getTipoComunicacao() {
        return tipoComunicacao;
    }

    public ModalidadeExpedicaoJudicial getModalidade() {
        return modalidade;
    }

    public void setModalidade(ModalidadeExpedicaoJudicial modalidade) {
        this.modalidade = modalidade;
        if (modalidade != null && modalidade.temPresuncaoAutomatica()) {
            this.presuncaoEntregaEm = Instant.now().plusSeconds((long) modalidade.getHorasPresuncaoEntrega() * 3600L);
        } else {
            this.presuncaoEntregaEm = null;
        }
    }

    public StatusExpedicao getStatus() {
        return status;
    }

    public void setStatus(StatusExpedicao status) {
        this.status = status;
    }

    public TipoDestinatario getTipoDestinatario() {
        return tipoDestinatario;
    }

    public String getDestinatarioNome() {
        return destinatarioNome;
    }

    public String getDestinatarioDocumento() {
        return destinatarioDocumento;
    }

    public String getDestinatarioEmail() {
        return destinatarioEmail;
    }

    public void setDestinatarioEmail(String destinatarioEmail) {
        this.destinatarioEmail = destinatarioEmail;
    }

    public String getDestinatarioTelefone() {
        return destinatarioTelefone;
    }

    public void setDestinatarioTelefone(String destinatarioTelefone) {
        this.destinatarioTelefone = destinatarioTelefone;
    }

    public String getDestinatarioEnderecoEntrega() {
        return destinatarioEnderecoEntrega;
    }

    public void setDestinatarioEnderecoEntrega(String destinatarioEnderecoEntrega) {
        this.destinatarioEnderecoEntrega = destinatarioEnderecoEntrega;
    }

    public String getCanalDigitalUtilizado() {
        return canalDigitalUtilizado;
    }

    public void setCanalDigitalUtilizado(String canalDigitalUtilizado) {
        this.canalDigitalUtilizado = canalDigitalUtilizado;
    }

    public Instant getExpedidaEm() {
        return expedidaEm;
    }

    public Instant getPresuncaoEntregaEm() {
        return presuncaoEntregaEm;
    }

    public Instant getConfirmacaoEntregaEm() {
        return confirmacaoEntregaEm;
    }

    public Instant getLeituraConfirmadaEm() {
        return leituraConfirmadaEm;
    }

    public Instant getPrazoRespostaInicioEm() {
        return prazoRespostaInicioEm;
    }

    public int getTentativasRealizadas() {
        return tentativasRealizadas;
    }

    public Instant getProximaTentativaEm() {
        return proximaTentativaEm;
    }

    public Instant getFrustradaEm() {
        return frustradaEm;
    }

    public String getMotivoFrustracao() {
        return motivoFrustracao;
    }

    public String getFallbackModalidade() {
        return fallbackModalidade;
    }

    public String getRamoDireito() {
        return ramoDireito;
    }

    public String getGrauJurisdicao() {
        return grauJurisdicao;
    }

    public String getInstanciaExpedidora() {
        return instanciaExpedidora;
    }

    public void setInstanciaExpedidora(String instanciaExpedidora) {
        this.instanciaExpedidora = instanciaExpedidora;
    }

    public Long getJuizResponsavelId() {
        return juizResponsavelId;
    }

    public void setJuizResponsavelId(Long juizResponsavelId) {
        this.juizResponsavelId = juizResponsavelId;
    }

    public Long getServidorExpedidorId() {
        return servidorExpedidorId;
    }

    public void setServidorExpedidorId(Long servidorExpedidorId) {
        this.servidorExpedidorId = servidorExpedidorId;
    }

    public Long getMandadoId() {
        return mandadoId;
    }

    public void setMandadoId(Long mandadoId) {
        this.mandadoId = mandadoId;
    }

    public Long getOficialId() {
        return oficialId;
    }

    public void setOficialId(Long oficialId) {
        this.oficialId = oficialId;
    }

    public String getCodigoRastreioCorreio() {
        return codigoRastreioCorreio;
    }

    public void setCodigoRastreioCorreio(String codigoRastreioCorreio) {
        this.codigoRastreioCorreio = codigoRastreioCorreio;
    }

    public String getNumeroEdital() {
        return numeroEdital;
    }

    public void setNumeroEdital(String numeroEdital) {
        this.numeroEdital = numeroEdital;
    }

    public String getIpLeitura() {
        return ipLeitura;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public String getGovbrSessionToken() {
        return govbrSessionToken;
    }

    public String getAcuseHash() {
        return acuseHash;
    }

    public String getHashIntegridade() {
        return hashIntegridade;
    }

    public String getHashAnterior() {
        return hashAnterior;
    }

    public String getFundamentacaoLegal() {
        return fundamentacaoLegal;
    }

    public String getConteudoDoAto() {
        return fundamentacaoLegal;
    }

    public boolean isEvasaoDetectada() {
        return evasaoDetectada;
    }

    public String getEvasaoEvidencias() {
        return evasaoEvidencias;
    }

    public boolean isEscalonadoParaJuiz() {
        return escalonadoParaJuiz;
    }

    public Instant getEscalonamentoEm() {
        return escalonamentoEm;
    }

    public Instant getCienciaJudicialEm() {
        return cienciaJudicialEm;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEntregueOuLida() {
        return status == StatusExpedicao.ENTREGUE_CONFIRMADA
                || status == StatusExpedicao.LIDA_CONFIRMADA
                || status == StatusExpedicao.PRESUMIDA_ENTREGUE;
    }

    public boolean isPrazoRespostaIniciado() {
        return prazoRespostaInicioEm != null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String upperTrim(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String lowerTrim(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String digitsOrNull(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        String digits = normalized.replaceAll("\\D+", "");
        return digits.isBlank() ? null : digits;
    }

    private static String normalizeDocumento(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.chars().allMatch(Character::isDigit)) {
            return normalized;
        }
        String digits = normalized.replaceAll("\\D+", "");
        if (!digits.isBlank()) {
            return digits;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
}
