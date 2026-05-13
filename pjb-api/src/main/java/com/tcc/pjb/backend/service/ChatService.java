package com.tcc.pjb.backend.service;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.orchestrator.IAOrchestrator;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.kernel.governance.NegotiationGovernedChatService;
import com.tcc.pjb.backend.core.kernel.governance.NegotiationMessageDecision;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationLanguageHeuristics;
import com.tcc.pjb.backend.mapper.ChatMensagemMapper;
import com.tcc.pjb.backend.model.dto.ChatMensagemRequest;
import com.tcc.pjb.backend.model.dto.ChatMensagemResponse;
import com.tcc.pjb.backend.model.dto.intelligence.AgreementChatAttachmentRequest;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ChatMensagemRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.PropostaAcordoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import com.tcc.pjb.backend.service.intelligence.AgreementChatGovernanceService;
import com.tcc.pjb.backend.service.intelligence.AgreementChatLedgerService;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMensagemRepository chatMensagemRepository;
    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PropostaAcordoRepository propostaAcordoRepository;
    private final ChatMensagemMapper chatMensagemMapper;
    private final IAOrchestrator iaOrchestrator;
    private final CurrentUserService currentUser;
    private final PjbAuthorizationService authz;
    private final AuditoriaInteligenteService auditoria;
    private final NegotiationGovernedChatService negotiationGovernedChatService;
    private final AgreementChatGovernanceService agreementChatGovernanceService;
    private final AgreementChatLedgerService agreementChatLedgerService;

    private static final Set<String> PALAVRAS_BLOQUEADAS = Set.of(
            "ofensa", "palavrão", "desrespeito", "xingar", "ameaçar", "idiota",
            "racismo", "preconceito", "matar", "violência", "estupro"
    );

    @Transactional(readOnly = true)
    public List<ChatMensagemResponse> buscarHistoricoDoProcesso(Long processoId) {
        if (processoId == null) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "processoId e obrigatorio");
        }
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado: " + processoId));
        enforceRead(processo);
        List<ChatMensagem> mensagens = chatMensagemRepository.findByProcesso_IdOrderByDataEnvioAsc(processoId);
        List<ChatMensagemResponse> responses = chatMensagemMapper.entidadeParaResponseLista(mensagens);
        PropostaAcordo proposta = propostaAcordoRepository.findTopByProcesso_IdOrderByDataAtualizacaoDesc(processoId).orElse(null);
        responses.forEach(response -> enrichSemanticMetadata(response, proposta));
        agreementChatLedgerService.enrichHistory(processo, mensagens, responses, proposta);
        return responses;
    }

    @Transactional
    public ChatMensagemResponse enviarMensagem(ChatMensagemRequest dto) {
        validarRequest(dto);

        Processo processo = processoRepository.findById(dto.getProcessoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado: " + dto.getProcessoId()));

        Usuario usuario = currentUser.getRequired();
        if (dto.getUsuarioId() != null && !dto.getUsuarioId().equals(usuario.getId())) {
            throw new AccessDeniedException("Acesso negado");
        }

        enforceRead(processo);
        validarProcessoParaChat(processo);

        String conteudo = dto.getConteudo().trim();
        validarConteudo(conteudo);
        PropostaAcordo propostaAtual = propostaAcordoRepository.findTopByProcesso_IdOrderByDataAtualizacaoDesc(processo.getId()).orElse(null);
        aplicarGovernancaNegocialSeNecessario(processo, propostaAtual, usuario, conteudo, dto);

        ChatMensagem mensagemUsuario = ChatMensagem.builder()
                .processo(processo)
                .usuario(usuario)
                .conteudo(conteudo)
                .dataEnvio(LocalDateTime.now())
                .build();

        ChatMensagem salvaUsuario = chatMensagemRepository.save(mensagemUsuario);

        try {
            String action = actionForChatSend(usuario);
            auditoria.registrarEventoImutavel(
                    action,
                    "PROCESSO_CHAT",
                    processo.getId(),
                    "processoId=" + processo.getId() + ";msgLen=" + conteudo.length()
            );
        } catch (Exception ignored) {
        }

        if (deveAcionarIA(conteudo)) {
            String requestId = UUID.randomUUID().toString();
            try {
                String actionIa = actionForChatIa(usuario);
                String iaAcao = selecionarAcaoIA(conteudo);
                auditoria.registrarEventoImutavel(
                        actionIa,
                        "IA_REQUEST",
                        requestId,
                        "processoId=" + processo.getId() + ";acao=" + iaAcao + ";msgLen=" + conteudo.length()
                );
            } catch (Exception ignored) {
            }

            try {
                gerarRespostaIA(processo, usuario, conteudo, requestId);
            } catch (Exception e) {
                log.error("Falha ao acionar IA no chat do processo {}: {}", processo.getId(), e.getMessage(), e);
            }
        }

        if (agreementChatLedgerService.isMajorNegotiationStep(conteudo) && isNegotiationCandidate(processo, conteudo, dto)) {
            AgreementChatLedgerService.RoundSnapshot snapshot = agreementChatLedgerService.nextRoundSnapshot(
                    chatMensagemRepository.findByProcesso_IdOrderByDataEnvioAsc(processo.getId()),
                    conteudo
            );
            postarMensagemSistema(processo, agreementChatLedgerService.renderRoundSystemMessage(snapshot));
        }

        PropostaAcordo propostaEnriquecida = propostaAcordoRepository.findTopByProcesso_IdOrderByDataAtualizacaoDesc(processo.getId()).orElse(null);
        ChatMensagemResponse response = chatMensagemMapper.entidadeParaResponse(salvaUsuario);
        enrichSemanticMetadata(response, propostaEnriquecida);
        agreementChatLedgerService.enrichHistory(processo, List.of(salvaUsuario), List.of(response), propostaEnriquecida);
        return response;
    }


    private void enrichSemanticMetadata(ChatMensagemResponse response, PropostaAcordo proposta) {
        if (response == null) {
            return;
        }
        String content = response.getConteudo() == null ? "" : response.getConteudo().trim();
        String lower = content.toLowerCase(Locale.ROOT);
        boolean systemMessage = content.startsWith("[SISTEMA]");
        boolean assistantMessage = content.startsWith("Laiane:");
        boolean agreementRelated = isAgreementRelated(lower);
        response.setAcordoRelacionado(agreementRelated);
        response.setStatusAcordo(proposta != null && proposta.getStatus() != null ? proposta.getStatus().name() : null);
        response.setAcordoPendenteHomologacao(proposta != null && proposta.getStatus() != null && "AGUARDANDO_HOMOLOGACAO_JUIZ".equals(proposta.getStatus().name()));
        response.setOrigemSistema(systemMessage ? "PJB_CORE" : assistantMessage ? "LAIANE" : "USUARIO_PROCESSUAL");
        response.setCanal(resolveChannel(response.getCanal(), agreementRelated, lower));
        response.setTipoMensagem(resolveMessageType(systemMessage, assistantMessage, agreementRelated, lower));
        response.setTagsIA(resolveTags(agreementRelated, systemMessage, assistantMessage, lower));
        response.setResumoIA(buildCompactSummary(systemMessage, assistantMessage, agreementRelated, lower, response));
        response.setPrioridadeAlta(response.isAcordoPendenteHomologacao() && isJudicialApprovalMarker(lower));
        response.setUrgente(response.isPrioridadeAlta() || lower.contains("urgência") || lower.contains("urgencia"));
        response.setPrecisaRevisaoHumana(response.isAcordoPendenteHomologacao() || lower.contains("revisão humana") || lower.contains("revisao humana"));
        response.setBloqueadaAlteracaoTermos(response.isAcordoPendenteHomologacao() && containsAny(lower, "nova proposta", "contraproposta", "alterar valor", "novos termos", "parcelamento"));
    }

    private String resolveChannel(String current, boolean agreementRelated, String lower) {
        if (current != null && !current.isBlank()) {
            return current;
        }
        if (agreementRelated) {
            return "ACORDO_PROCESSUAL";
        }
        if (lower.contains("audiência") || lower.contains("audiencia")) {
            return "COMUNICACAO_PROCESSUAL";
        }
        return "CHAT_PROCESSUAL";
    }

    private String resolveMessageType(boolean systemMessage, boolean assistantMessage, boolean agreementRelated, String lower) {
        if (assistantMessage) {
            return agreementRelated ? "ASSISTENTE_IA_ACORDO" : "ASSISTENTE_IA";
        }
        if (systemMessage && isJudicialApprovalMarker(lower)) {
            return "SISTEMA_HOMOLOGACAO_ACORDO";
        }
        if (systemMessage && agreementRelated) {
            return "SISTEMA_ACORDO";
        }
        if (agreementRelated) {
            return "NEGOCIACAO_PROCESSUAL";
        }
        return systemMessage ? "SISTEMA" : "MENSAGEM_PROCESSUAL";
    }

    private List<String> resolveTags(boolean agreementRelated, boolean systemMessage, boolean assistantMessage, String lower) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (agreementRelated) {
            tags.add("acordo");
        }
        if (containsAny(lower, "homologa", "gabinete", "apreciação judicial", "apreciacao judicial")) {
            tags.add("homologacao");
        }
        if (containsAny(lower, "parcel", "cronograma", "cláusula", "clausula", "multa")) {
            tags.add("clausulas");
        }
        if (containsAny(lower, "tema", "repetitivo", "repercussão", "repercussao")) {
            tags.add("tema_qualificado");
        }
        if (assistantMessage) {
            tags.add("laiane");
        }
        if (systemMessage) {
            tags.add("sistema");
        }
        return List.copyOf(tags);
    }

    private String buildCompactSummary(boolean systemMessage, boolean assistantMessage, boolean agreementRelated, String lower, ChatMensagemResponse response) {
        if (assistantMessage && agreementRelated) {
            return "Assistência negocial da IA disponível nesta mensagem.";
        }
        if (systemMessage && isJudicialApprovalMarker(lower)) {
            return "Mensagem sistêmica de apreciação judicial do acordo, com bloqueio de publicação automática até decisão do gabinete.";
        }
        if (systemMessage && agreementRelated) {
            return "Atualização sistêmica da trilha de negociação do acordo.";
        }
        if (agreementRelated) {
            return "Mensagem vinculada ao canal negocial do processo.";
        }
        return response.getResumoIA();
    }

    private boolean isAgreementRelated(String lower) {
        return containsAny(lower, "acordo", "negocia", "concilia", "media", "proposta", "parcel", "cláusula", "clausula", "homologa", "cronograma", "inadimplemento");
    }

    private boolean isJudicialApprovalMarker(String lower) {
        return containsAny(lower, "homologar", "devolver para revisão", "devolver para revisao", "rejeitar", "gabinete", "apreciação judicial", "apreciacao judicial");
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null || value.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && value.contains(token.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void aplicarGovernancaNegocialSeNecessario(Processo processo, PropostaAcordo proposta, Usuario usuario, String conteudo, ChatMensagemRequest dto) {
        if (proposta == null || !isNegotiationCandidate(processo, conteudo, dto)) {
            return;
        }
        AgreementChatGovernanceService.AgreementChannelPolicy channelPolicy = agreementChatGovernanceService.enforcePost(processo, proposta, usuario, dto != null ? dto.getCanal() : null, conteudo);
        List<ChatMensagem> recentChat = chatMensagemRepository.findTop80ByProcesso_IdOrderByDataEnvioDesc(processo.getId()).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ChatMensagem::getDataEnvio, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        NegotiationMessageDecision decision = negotiationGovernedChatService.evaluateOutboundMessage(
                processo,
                proposta,
                usuario,
                recentChat,
                conteudo,
                buildNegotiationSignals(processo, conteudo, dto, channelPolicy)
        );
        if (!decision.releaseAllowed()) {
            throw new RegraNegocioException(formatGovernanceBlock(decision));
        }
    }

    private boolean isNegotiationCandidate(Processo processo, String conteudo, ChatMensagemRequest dto) {
        if (processo == null || conteudo == null) {
            return false;
        }
        String lower = conteudo.toLowerCase(Locale.ROOT);
        if (dto != null && dto.getCanal() != null && dto.getCanal().toUpperCase(Locale.ROOT).contains("ACORDO")) {
            return true;
        }
        return containsNegotiationMarker(lower)
                || lower.contains("proposta")
                || lower.contains("parcel")
                || lower.contains("aceitamos")
                || lower.contains("aceite")
                || lower.contains("fechar")
                || lower.contains("cronograma")
                || lower.contains("multa");
    }

    private boolean containsNegotiationMarker(String lower) {
        if (lower == null || lower.isBlank()) {
            return false;
        }
        return NegotiationLanguageHeuristics.containsPositiveSettlementSignal(lower);
    }

    private List<String> buildNegotiationSignals(Processo processo, String conteudo, ChatMensagemRequest dto, AgreementChatGovernanceService.AgreementChannelPolicy channelPolicy) {
        List<String> signals = new ArrayList<>();
        if (processo != null) {
            if (processo.getFaseAtual() != null) {
                signals.add("Fase atual: " + processo.getFaseAtual().name());
            }
            if (processo.getStatusProcesso() != null) {
                signals.add("Status do processo: " + processo.getStatusProcesso().name());
            }
        }
        if (conteudo != null && !conteudo.isBlank()) {
            signals.add(conteudo.trim());
        }
        if (dto != null && dto.getCanal() != null && !dto.getCanal().isBlank()) {
            signals.add("Canal: " + dto.getCanal().trim());
        }
        if (channelPolicy != null) {
            signals.add("Canal negocial: " + channelPolicy.channel());
            signals.add("Estágio negocial: " + channelPolicy.stage());
            if (channelPolicy.freezeTermChanges()) {
                signals.add("Termos materiais congelados até decisão judicial");
            }
        }
        return List.copyOf(signals);
    }

    private String formatGovernanceBlock(NegotiationMessageDecision decision) {
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        lines.add("Envio bloqueado pela governança negocial: " + decision.decisionCode());
        if (decision.riskLevel() != null && !decision.riskLevel().isBlank()) {
            lines.add("Risco: " + decision.riskLevel());
        }
        if (decision.approvalBand() != null && !decision.approvalBand().isBlank()) {
            lines.add("Alçada: " + decision.approvalBand());
        }
        if (decision.releaseMode() != null && !decision.releaseMode().isBlank()) {
            lines.add("Modo de liberação: " + decision.releaseMode());
        }
        if (decision.policyTier() != null && !decision.policyTier().isBlank()) {
            lines.add("Camada de política: " + decision.policyTier());
        }
        lines.addAll(decision.reasons());
        lines.addAll(decision.mandatoryActions());
        return String.join(" | ", lines);
    }


    @Transactional
    public ChatMensagemResponse registrarAnexoNegocial(Long processoId, AgreementChatAttachmentRequest request) {
        if (processoId == null) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "processoId e obrigatorio");
        }
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado: " + processoId));
        Usuario usuario = currentUser.getRequired();
        enforceRead(processo);
        validarProcessoParaChat(processo);
        PropostaAcordo proposta = request != null && request.propostaId() != null
                ? propostaAcordoRepository.findById(request.propostaId()).orElse(null)
                : propostaAcordoRepository.findTopByProcesso_IdOrderByDataAtualizacaoDesc(processoId).orElse(null);
        agreementChatGovernanceService.enforcePost(processo, proposta, usuario, "ACORDO_PROCESSUAL", "anexo negocial " + (request == null ? "" : request.kind()));
        String content = agreementChatLedgerService.renderAttachmentMessage(
                request != null ? request.kind() : null,
                request != null ? request.label() : null,
                request != null ? request.url() : null,
                request != null ? request.mimeType() : null,
                request != null ? request.hash() : null,
                request != null ? request.bytes() : null
        );
        ChatMensagem entity = chatMensagemRepository.save(ChatMensagem.builder()
                .processo(processo)
                .usuario(usuario)
                .conteudo(content)
                .dataEnvio(LocalDateTime.now())
                .build());
        ChatMensagemResponse response = chatMensagemMapper.entidadeParaResponse(entity);
        enrichSemanticMetadata(response, proposta);
        agreementChatLedgerService.enrichHistory(processo, List.of(entity), List.of(response), proposta);
        return response;
    }

    private void enforceRead(Processo processo) {
        Usuario u = currentUser.getOrNull();
        if (u == null) {
            throw new AccessDeniedException("Acesso negado");
        }
        TipoUsuario t = u.getTipoUsuario();
        if (t == TipoUsuario.CIDADAO) {
            authz.requireReadProcessoAsCidadaoParte(processo);
            return;
        }
        authz.requireReadProcesso(processo);
    }

    @Transactional
    public void postarMensagemSistema(Processo processo, String mensagem) {
        if (processo == null || processo.getId() == null) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "processo invalido");
        }
        if (mensagem == null || mensagem.isBlank()) {
            return;
        }

        Usuario sistema = usuarioRepository.findByEmail("sistema@pjb.gov.br")
                .orElseGet(this::criarUsuarioSistemaFallback);

        ChatMensagem msg = ChatMensagem.builder()
                .processo(processo)
                .usuario(sistema)
                .conteudo("[SISTEMA] " + mensagem.trim())
                .dataEnvio(LocalDateTime.now())
                .build();

        chatMensagemRepository.save(msg);
    }

    private void gerarRespostaIA(Processo processo, Usuario usuario, String pergunta, String requestId) {
        IARequest request = IARequest.builder()
                .requestId(requestId)
                .correlationId("CHAT-" + processo.getId())
                .origem("CHAT_JURIDICO")
                .acao(selecionarAcaoIA(pergunta))
                .usuarioId(usuario.getId() != null ? String.valueOf(usuario.getId()) : null)
                .payload("pergunta_usuario", pergunta)
                .payload("processo_id", processo.getId())
                .payload("numero_unificado", processo.getNumeroUnificado())
                .payload("assunto", processo.getAssunto())
                .payload("resumo_processo", processo.getResumoIA())
                .payload("valor_causa", processo.getValorCausa())
                .build();

        IAResponse resposta = iaOrchestrator.processar(request);

        Usuario sistema = usuarioRepository.findByEmail("sistema@pjb.gov.br")
                .orElseGet(this::criarUsuarioSistemaFallback);

        String texto = resposta != null && resposta.getTexto() != null ? resposta.getTexto().trim() : "(sem resposta)";

        ChatMensagem respostaIA = ChatMensagem.builder()
                .processo(processo)
                .usuario(sistema)
                .conteudo("Laiane:\n" + texto)
                .dataEnvio(LocalDateTime.now())
                .build();

        chatMensagemRepository.save(respostaIA);
    }

    private String selecionarAcaoIA(String pergunta) {
        String p = pergunta.toLowerCase(Locale.ROOT);
        if (p.contains("cálculo") || p.contains("calculo") || p.contains("valor") || p.contains("juros")) {
            return "CALCULO_FINANCEIRO";
        }
        if (p.contains("risco") || p.contains("chance") || p.contains("probabilidade")) {
            return "ANALISE_RISCO";
        }
        if (p.contains("praz") || p.contains("prazo")) {
            return "GESTAO_PRAZOS";
        }
        return "CHAT_GERAL";
    }

    private boolean deveAcionarIA(String texto) {
        String t = texto.toLowerCase(Locale.ROOT);
        return t.startsWith("@ia")
                || t.startsWith("/pjb")
                || t.contains("analisar")
                || t.contains("calcular")
                || t.contains("previs")
                || t.contains("jurisprud")
                || t.contains("fundament");
    }

    private void validarRequest(ChatMensagemRequest dto) {
        if (dto == null) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "request nulo");
        }
        if (dto.getProcessoId() == null) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "processoId e obrigatorio");
        }
        if (dto.getUsuarioId() == null) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "usuarioId e obrigatorio");
        }
        if (dto.getConteudo() == null) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "conteudo e obrigatorio");
        }
    }

    private void validarProcessoParaChat(Processo processo) {
        StatusProcesso status = processo.getStatus();
        if (StatusProcesso.BAIXADO.equals(status) || StatusProcesso.ARQUIVADO.equals(status)) {
            throw new RegraNegocioException("Chat bloqueado. Processo encerrado ou arquivado.");
        }
    }

    private void validarConteudo(String conteudo) {
        if (conteudo.isBlank()) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "mensagem vazia");
        }
        if (conteudo.length() > 5000) {
            throw new ErroDeValidacaoException(TipoErroValidacao.TAMANHO_EXCEDIDO, "conteudo excede o limite permitido de 5000 caracteres");
        }

        String lower = conteudo.toLowerCase(Locale.ROOT);
        if (PALAVRAS_BLOQUEADAS.stream().anyMatch(lower::contains)) {
            throw new RegraNegocioException("Mensagem bloqueada por violar as normas de conduta.");
        }
    }

    private Usuario criarUsuarioSistemaFallback() {
        Usuario u = new Usuario();
        u.setNome("PJB IA System");
        u.setEmail("sistema@pjb.gov.br");
        u.setSenha("__SYSTEM__");
        u.setCpf("00000000000");
        u.setAtivo(true);
        return usuarioRepository.save(u);
    }

    public void postarMensagemSistema(Processo processo, Usuario usuarioSistema, String msg) {
        postarMensagemSistema(processo, msg);
    }

    private static String actionForChatSend(Usuario u) {
        if (u == null || u.getTipoUsuario() == null) return "CHAT_MENSAGEM_ENVIADA";
        TipoUsuario t = u.getTipoUsuario();
        if (t == TipoUsuario.ADVOGADO) return "ADV_CHAT_MENSAGEM_ENVIADA";
        if (t == TipoUsuario.CIDADAO) return "CID_CHAT_MENSAGEM_ENVIADA";
        if (t == TipoUsuario.MAGISTRADO) return "MAG_CHAT_MENSAGEM_ENVIADA";
        if (t == TipoUsuario.SERVIDOR) return "SRV_CHAT_MENSAGEM_ENVIADA";
        return "CHAT_MENSAGEM_ENVIADA";
    }

    private static String actionForChatIa(Usuario u) {
        if (u == null || u.getTipoUsuario() == null) return "CHAT_IA_ACIONADA";
        TipoUsuario t = u.getTipoUsuario();
        if (t == TipoUsuario.ADVOGADO) return "ADV_CHAT_IA_ACIONADA";
        if (t == TipoUsuario.CIDADAO) return "CID_CHAT_IA_ACIONADA";
        if (t == TipoUsuario.MAGISTRADO) return "MAG_CHAT_IA_ACIONADA";
        if (t == TipoUsuario.SERVIDOR) return "SRV_CHAT_IA_ACIONADA";
        return "CHAT_IA_ACIONADA";
    }
}
