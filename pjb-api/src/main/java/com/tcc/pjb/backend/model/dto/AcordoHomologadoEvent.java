package com.tcc.pjb.backend.model.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AcordoHomologadoEvent {

    
    private Long processoId;
    private UUID propostaUuid;
    private Long juizId;

    
    private String juizNome;
    private String tribunal;
    private String comarca;
    private String tipoAcordo;
    private Double valorAcordo;
    private String moeda;
    private boolean sigiloso;
    private int nivelSigilo;
    private String status;
    private LocalDateTime dataHomologacao;
    private LocalDateTime ultimaAtualizacao;
    private boolean ativo;
    private boolean arquivado;
    private LocalDateTime dataArquivamento;
    private boolean recorrivel;
    private String categoriaRisco;
    private double confiancaIA;
    private String resumoIA;
    private List<String> tagsIA;
    private String hashIntegridade;
    private String origemSistema;
    private String referenciaExterna;
    private List<String> partesEnvolvidas;
    private String advogadoResponsavel;
    private String advogadoOab;
    private boolean urgente;
    private boolean prioridadeAlta;
    private boolean suspenderConta;
    private int tentativasViolacao;
    private boolean notificarUsuario;
    private boolean notificarModerador;
    private String urlConsulta;
    private String urlDownload;
    private String urlAjuda;
    private String urlPolitica;
    private String idioma;
    private String canal;
    private String ipOrigem;
    private String deviceInfo;
    private boolean criptografado;
    private boolean validaAssinatura;
    private String assinaturaDigital;
    private boolean detectouFraude;
    private boolean detectouSpam;
    private boolean detectouFakeNews;
    private boolean detectouPlagio;
    private boolean detectouViolencia;
    private boolean detectouDiscursoOdio;
    private boolean detectouAssedio;
    private boolean detectouRacismo;
    private boolean detectouSexismo;
    private boolean detectouPhishing;
    private boolean detectouMalware;
    private boolean detectouConteudoAdulto;
    private boolean detectouDadosPessoais;
    private boolean detectouSegredoJustica;
    private String justificativa;
    private String decisor;
    private int severidade;
    private boolean recorrivelIA;

    
    public AcordoHomologadoEvent(Long pId, UUID propId, Long jId) {
        this.processoId = pId;
        this.propostaUuid = propId;
        this.juizId = jId;
    }

    
    public AcordoHomologadoEvent(Long processoId, UUID propostaUuid, Long juizId,
                                 String juizNome, String tribunal, String comarca,
                                 String tipoAcordo, Double valorAcordo, String moeda,
                                 boolean sigiloso, int nivelSigilo, String status,
                                 LocalDateTime dataHomologacao, LocalDateTime ultimaAtualizacao,
                                 boolean ativo, boolean arquivado, LocalDateTime dataArquivamento,
                                 boolean recorrivel, String categoriaRisco, double confiancaIA,
                                 String resumoIA, List<String> tagsIA, String hashIntegridade,
                                 String origemSistema, String referenciaExterna, List<String> partesEnvolvidas,
                                 String advogadoResponsavel, String advogadoOab, boolean urgente,
                                 boolean prioridadeAlta, boolean suspenderConta, int tentativasViolacao,
                                 boolean notificarUsuario, boolean notificarModerador, String urlConsulta,
                                 String urlDownload, String urlAjuda, String urlPolitica, String idioma,
                                 String canal, String ipOrigem, String deviceInfo, boolean criptografado,
                                 boolean validaAssinatura, String assinaturaDigital, boolean detectouFraude,
                                 boolean detectouSpam, boolean detectouFakeNews, boolean detectouPlagio,
                                 boolean detectouViolencia, boolean detectouDiscursoOdio, boolean detectouAssedio,
                                 boolean detectouRacismo, boolean detectouSexismo, boolean detectouPhishing,
                                 boolean detectouMalware, boolean detectouConteudoAdulto, boolean detectouDadosPessoais,
                                 boolean detectouSegredoJustica, String justificativa, String decisor,
                                 int severidade, boolean recorrivelIA) {
        this.processoId = processoId;
        this.propostaUuid = propostaUuid;
        this.juizId = juizId;
        this.juizNome = juizNome;
        this.tribunal = tribunal;
        this.comarca = comarca;
        this.tipoAcordo = tipoAcordo;
        this.valorAcordo = valorAcordo;
        this.moeda = moeda;
        this.sigiloso = sigiloso;
        this.nivelSigilo = nivelSigilo;
        this.status = status;
        this.dataHomologacao = dataHomologacao;
        this.ultimaAtualizacao = ultimaAtualizacao;
        this.ativo = ativo;
        this.arquivado = arquivado;
        this.dataArquivamento = dataArquivamento;
        this.recorrivel = recorrivel;
        this.categoriaRisco = categoriaRisco;
        this.confiancaIA = confiancaIA;
        this.resumoIA = resumoIA;
        this.tagsIA = tagsIA;
        this.hashIntegridade = hashIntegridade;
        this.origemSistema = origemSistema;
        this.referenciaExterna = referenciaExterna;
        this.partesEnvolvidas = partesEnvolvidas;
        this.advogadoResponsavel = advogadoResponsavel;
        this.advogadoOab = advogadoOab;
        this.urgente = urgente;
        this.prioridadeAlta = prioridadeAlta;
        this.suspenderConta = suspenderConta;
        this.tentativasViolacao = tentativasViolacao;
        this.notificarUsuario = notificarUsuario;
        this.notificarModerador = notificarModerador;
        this.urlConsulta = urlConsulta;
        this.urlDownload = urlDownload;
        this.urlAjuda = urlAjuda;
        this.urlPolitica = urlPolitica;
        this.idioma = idioma;
        this.canal = canal;
        this.ipOrigem = ipOrigem;
        this.deviceInfo = deviceInfo;
        this.criptografado = criptografado;
        this.validaAssinatura = validaAssinatura;
        this.assinaturaDigital = assinaturaDigital;
        this.detectouFraude = detectouFraude;
        this.detectouSpam = detectouSpam;
        this.detectouFakeNews = detectouFakeNews;
        this.detectouPlagio = detectouPlagio;
        this.detectouViolencia = detectouViolencia;
        this.detectouDiscursoOdio = detectouDiscursoOdio;
        this.detectouAssedio = detectouAssedio;
        this.detectouRacismo = detectouRacismo;
        this.detectouSexismo = detectouSexismo;
        this.detectouPhishing = detectouPhishing;
        this.detectouMalware = detectouMalware;
        this.detectouConteudoAdulto = detectouConteudoAdulto;
        this.detectouDadosPessoais = detectouDadosPessoais;
        this.detectouSegredoJustica = detectouSegredoJustica;
        this.justificativa = justificativa;
        this.decisor = decisor;
        this.severidade = severidade;
        this.recorrivelIA = recorrivelIA;
    }
}