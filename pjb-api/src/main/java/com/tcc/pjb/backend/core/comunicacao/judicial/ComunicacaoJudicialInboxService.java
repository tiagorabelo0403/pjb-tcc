package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.cidadao.AreaLinks;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoComunicacaoJudicialDto;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoComunicacoesJudiciaisResponse;
import com.tcc.pjb.backend.model.dto.cidadao.Links;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.advocacia.entity.util.CriptografiaPJB;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import com.tcc.pjb.backend.service.cidadao.CidadaoProcessoCardMapper;

@Service
public class ComunicacaoJudicialInboxService {

    private final CurrentUserService currentUserService;
    private final ExpedicaoJudicialRepository expedicaoJudicialRepository;
    private final LaianeProcuracaoRepository procuracaoRepository;
    private final ClienteRepository clienteRepository;

    public ComunicacaoJudicialInboxService(CurrentUserService currentUserService,
                                           ExpedicaoJudicialRepository expedicaoJudicialRepository,
                                           LaianeProcuracaoRepository procuracaoRepository,
                                           ClienteRepository clienteRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.expedicaoJudicialRepository = Objects.requireNonNull(expedicaoJudicialRepository, "expedicaoJudicialRepository");
        this.procuracaoRepository = Objects.requireNonNull(procuracaoRepository, "procuracaoRepository");
        this.clienteRepository = Objects.requireNonNull(clienteRepository, "clienteRepository");
    }

    public CidadaoComunicacoesJudiciaisResponse minhasComunicacoes() {
        Usuario usuario = currentUserService.getRequired();
        List<CidadaoComunicacaoJudicialDto> itens = usuario.getTipoUsuario() == TipoUsuario.ADVOGADO
                ? minhasComunicacoesComoAdvogado(usuario)
                : minhasComunicacoesComoDestinatario(usuario);
        int pendentesCiencia = (int) itens.stream().filter(CidadaoComunicacaoJudicialDto::requerCiencia).count();
        return new CidadaoComunicacoesJudiciaisResponse(
                LocalDateTime.now(),
                itens.size(),
                pendentesCiencia,
                itens,
                "/api/v1/ui/legend",
                defaultLinks()
        );
    }

    private List<CidadaoComunicacaoJudicialDto> minhasComunicacoesComoDestinatario(Usuario usuario) {
        String documento = normalizarDocumento(usuario.getCpf());
        if (documento == null || documento.isBlank()) {
            return List.of();
        }
        return expedicaoJudicialRepository
                .findTop100ByDestinatarioDocumentoOrderByExpedidaEmDesc(documento)
                .stream()
                .filter(this::visivelNoPortal)
                .map(expedicao -> toDto(expedicao, false))
                .limit(50)
                .toList();
    }

    private List<CidadaoComunicacaoJudicialDto> minhasComunicacoesComoAdvogado(Usuario usuario) {
        Map<String, CidadaoComunicacaoJudicialDto> merged = new LinkedHashMap<>();
        String documentoAdvogado = normalizarDocumento(usuario.getCpf());
        if (documentoAdvogado != null) {
            expedicaoJudicialRepository.findTop100ByDestinatarioDocumentoOrderByExpedidaEmDesc(documentoAdvogado)
                    .stream()
                    .filter(this::visivelNoPortal)
                    .map(expedicao -> toDto(expedicao, false))
                    .forEach(dto -> merged.put(dto.expedicaoUuid(), dto));
        }
        Set<Long> processoIds = new java.util.LinkedHashSet<>(procuracaoRepository.findProcessoIdsByAdvogadoAndStatus(usuario.getId(), LaianeProcuracaoStatus.ATIVA));
        if (!processoIds.isEmpty()) {
            expedicaoJudicialRepository.findTop200ByProcessoIdInOrderByExpedidaEmDesc(processoIds)
                    .stream()
                    .filter(this::visivelNoPortal)
                    .filter(expedicao -> representaDestinatario(usuario, expedicao))
                    .map(expedicao -> toDto(expedicao, true))
                    .forEach(dto -> merged.putIfAbsent(dto.expedicaoUuid(), dto));
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(CidadaoComunicacaoJudicialDto::expedidaEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(50)
                .toList();
    }

    private boolean representaDestinatario(Usuario advogado, ExpedicaoJudicial expedicao) {
        if (advogado == null || advogado.getId() == null || expedicao == null) {
            return false;
        }
        String documento = normalizarDocumento(expedicao.getDestinatarioDocumento());
        if (documento == null || documento.isBlank()) {
            return false;
        }
        if (Objects.equals(documento, normalizarDocumento(advogado.getCpf()))) {
            return false;
        }
        String hash = CriptografiaPJB.hashCpfCnpj(documento);
        return hash != null && clienteRepository.existsByCpfHashAndAdvogado_Id(hash, advogado.getId());
    }

    private boolean visivelNoPortal(ExpedicaoJudicial expedicao) {
        if (expedicao == null || expedicao.getStatus() == null) {
            return false;
        }
        return switch (expedicao.getStatus()) {
            case EXPEDIDA,
                 ENTREGUE_CONFIRMADA,
                 LIDA_CONFIRMADA,
                 PRESUMIDA_ENTREGUE,
                 PENDENTE_OFICIAL,
                 REMETIDA_CORREIO,
                 PUBLICADA_EDITAL -> true;
            case FRUSTRADA_DEFINITIVA,
                 DEVOLVIDA,
                 CANCELADA -> false;
        };
    }

    private CidadaoComunicacaoJudicialDto toDto(ExpedicaoJudicial expedicao, boolean representado) {
        String titulo = titulo(expedicao, representado);
        String resumo = resumo(expedicao, representado);
        boolean requerCiencia = expedicao.getStatus() == ExpedicaoJudicial.StatusExpedicao.EXPEDIDA
                || expedicao.getStatus() == ExpedicaoJudicial.StatusExpedicao.ENTREGUE_CONFIRMADA
                || expedicao.getStatus() == ExpedicaoJudicial.StatusExpedicao.PRESUMIDA_ENTREGUE
                || expedicao.getStatus() == ExpedicaoJudicial.StatusExpedicao.PUBLICADA_EDITAL;
        return new CidadaoComunicacaoJudicialDto(
                expedicao.getExpedicaoUuid(),
                expedicao.getProcessoId(),
                expedicao.getNumeroUnificado(),
                titulo,
                resumo,
                expedicao.getTipoComunicacao() != null ? expedicao.getTipoComunicacao().name() : null,
                expedicao.getStatus().name(),
                expedicao.getModalidade() != null ? expedicao.getModalidade().name() : null,
                requerCiencia,
                expedicao.getPrazoRespostaInicioEm() != null,
                toLocalDateTime(expedicao.getExpedidaEm()),
                toLocalDateTime(expedicao.getPrazoRespostaInicioEm()),
                toLocalDateTime(expedicao.getPresuncaoEntregaEm()),
                linksFor(expedicao.getProcessoId())
        );
    }

    private String titulo(ExpedicaoJudicial expedicao, boolean representado) {
        String numero = expedicao.getNumeroUnificado() != null ? expedicao.getNumeroUnificado() : String.valueOf(expedicao.getProcessoId());
        if (expedicao.getTipoComunicacao() == null) {
            return representado ? "Comunicação judicial de cliente representado no processo " + numero : "Comunicação judicial no processo " + numero;
        }
        if (expedicao.getTipoComunicacao().isCitacao()) {
            return representado ? "Citação de cliente representado no processo " + numero : "Citação no processo " + numero;
        }
        if (expedicao.getTipoComunicacao().isIntimacao()) {
            return representado ? "Intimação de cliente representado no processo " + numero : "Intimação no processo " + numero;
        }
        if (expedicao.getTipoComunicacao().isMandado()) {
            return representado ? "Mandado de cliente representado no processo " + numero : "Mandado no processo " + numero;
        }
        return representado ? "Comunicação de cliente representado no processo " + numero : "Comunicação judicial no processo " + numero;
    }

    private String resumo(ExpedicaoJudicial expedicao, boolean representado) {
        String status = switch (expedicao.getStatus()) {
            case EXPEDIDA -> representado ? "Ato expedido para parte representada e aguardando ciência do destinatário." : "Ato expedido e aguardando ciência do destinatário.";
            case ENTREGUE_CONFIRMADA -> representado ? "Entrega confirmada para parte representada. Reavalie conteúdo e prazos do processo." : "Entrega confirmada. Verifique o conteúdo e os prazos do processo.";
            case LIDA_CONFIRMADA -> representado ? "Ciência confirmada em nome de parte representada. O fluxo segue auditado no PJB." : "Ciência confirmada no portal. O fluxo da comunicação segue auditado.";
            case PRESUMIDA_ENTREGUE -> representado ? "Entrega presumida aplicada para parte representada conforme a modalidade adotada." : "Entrega presumida aplicada conforme a modalidade adotada.";
            case PENDENTE_OFICIAL -> representado ? "Mandado encaminhado para diligência de Oficial de Justiça em favor de parte representada." : "Mandado encaminhado para diligência de Oficial de Justiça.";
            case REMETIDA_CORREIO -> representado ? "Ato remetido por correio com rastreio operacional ativo para parte representada." : "Ato remetido por correio com rastreio operacional ativo.";
            case PUBLICADA_EDITAL -> representado ? "Ato convertido em edital para parte representada. Confira a publicação oficial e os efeitos processuais." : "Ato convertido em edital. Confira a publicação oficial e os efeitos processuais.";
            case FRUSTRADA_DEFINITIVA -> representado ? "A comunicação da parte representada restou frustrada e exige reavaliação judicial." : "A comunicação restou frustrada e exige reavaliação judicial.";
            case DEVOLVIDA -> representado ? "A comunicação da parte representada retornou sem cumprimento útil." : "A comunicação retornou sem cumprimento útil.";
            case CANCELADA -> representado ? "A comunicação da parte representada foi cancelada." : "A comunicação foi cancelada.";
        };
        String modalidade = expedicao.getModalidade() != null ? expedicao.getModalidade().name().toLowerCase(Locale.ROOT).replace('_', ' ') : "modalidade não identificada";
        return status + " Modalidade: " + modalidade + ".";
    }

    private static Links linksFor(Long processoId) {
        return processoId == null ? null : CidadaoProcessoCardMapper.linksFor(processoId);
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static AreaLinks defaultLinks() {
        return new AreaLinks(
                "/api/v1/ui/legend",
                "/api/v1/ui/accessibility/preference",
                "/api/v1/ui/presentation/reading-preference",
                "/api/v1/ui/presentation/bundle",
                "/api/v1/chat",
                "/api/v1/chat/processo/{processoId}"
        );
    }

    private static String normalizarDocumento(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }
}
