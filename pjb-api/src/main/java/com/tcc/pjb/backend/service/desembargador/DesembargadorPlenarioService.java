package com.tcc.pjb.backend.service.desembargador;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.configs.security.PasskeyRequiredException;
import com.tcc.pjb.backend.configs.security.PasskeyRequirementEnforcer;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.desembargador.DesembargadorPlenarioVotoRequest;
import com.tcc.pjb.backend.model.dto.desembargador.RelatorPlenarioDivergenciaDto;
import com.tcc.pjb.backend.model.dto.desembargador.RelatorPlenarioResponse;
import com.tcc.pjb.backend.model.dto.desembargador.RelatorPlenarioResultadoDto;
import com.tcc.pjb.backend.model.dto.desembargador.RelatorPlenarioVoteDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.VotoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.enums.TipoVotoColegiado;
import com.tcc.pjb.backend.modules.laiane.service.LaianeSentencaService;
import com.tcc.pjb.backend.service.julgamento.JulgamentoColegiadoService;

@Service
public class DesembargadorPlenarioService {

    private final JulgamentoColegiadoService julgamentoColegiadoService;
    private final LaianeSentencaService laianeSentencaService;
    private final CurrentUserService currentUserService;
    private final PasskeyRequirementEnforcer passkeyRequirementEnforcer;

    public DesembargadorPlenarioService(JulgamentoColegiadoService julgamentoColegiadoService,
                                        LaianeSentencaService laianeSentencaService,
                                        CurrentUserService currentUserService,
                                        PasskeyRequirementEnforcer passkeyRequirementEnforcer) {
        this.julgamentoColegiadoService = Objects.requireNonNull(julgamentoColegiadoService);
        this.laianeSentencaService = Objects.requireNonNull(laianeSentencaService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.passkeyRequirementEnforcer = Objects.requireNonNull(passkeyRequirementEnforcer);
    }

    @Transactional(readOnly = true)
    public RelatorPlenarioResponse painelRelator(Long sessaoId) {
        requireDesembargador();
        JulgamentoColegiado sessao = julgamentoColegiadoService.getRequired(sessaoId);
        List<VotoColegiado> votos = julgamentoColegiadoService.listVotos(sessaoId);
        List<RelatorPlenarioDivergenciaDto> divergencias = detectarDivergencias(votos);
        boolean possuiDivergencia = !divergencias.isEmpty();
        List<RelatorPlenarioVoteDto> votosDto = votos.stream()
                .map(voto -> toVoteDto(voto, possuiDivergencia))
                .sorted(Comparator.comparing(RelatorPlenarioVoteDto::ordem, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        RelatorPlenarioResultadoDto resultado = computarResultado(sessao, votos);
        String ementaSugerida = laianeSentencaService.sugerirEmentaColegiada(sessao, votos);
        String minutaAcordaoParcial = construirMinutaParcial(sessao, votos, resultado, ementaSugerida, divergencias);
        return new RelatorPlenarioResponse(
                sessao.getId(),
                sessao.getProcesso() != null ? sessao.getProcesso().getId() : null,
                sessao.getProcesso() != null ? firstNonBlank(sessao.getProcesso().getNumeroUnificado(), sessao.getProcesso().getNumeroProcesso()) : null,
                sessao.getTribunalSigla(),
                sessao.getOrgaoJulgador(),
                sessao.getRelatorNome(),
                sessao.getStatus() != null ? sessao.getStatus().name() : null,
                votosDto,
                divergencias,
                ementaSugerida,
                resultado,
                minutaAcordaoParcial,
                LocalDateTime.now()
        );
    }

    @Transactional
    public RelatorPlenarioVoteDto registrarVoto(Long sessaoId, DesembargadorPlenarioVotoRequest request) {
        Usuario usuario = requireDesembargador();
        validarStepUp(usuario);
        VotoColegiado voto = julgamentoColegiadoService.registrarVoto(
                sessaoId,
                request.ordem(),
                usuario.getNome(),
                usuario.getTipoUsuario().name(),
                request.papel(),
                request.votoTipo(),
                request.votoResumo(),
                request.documentoRef()
        );
        return toVoteDto(voto, voto.getVotoTipo() == TipoVotoColegiado.DIVERGIR);
    }

    private List<RelatorPlenarioDivergenciaDto> detectarDivergencias(List<VotoColegiado> votos) {
        if (votos == null || votos.isEmpty()) {
            return List.of();
        }
        List<RelatorPlenarioDivergenciaDto> divergencias = new ArrayList<>();
        Map<String, List<String>> porTipo = new LinkedHashMap<>();
        for (VotoColegiado voto : votos) {
            String chave = voto.getVotoTipo() == null ? "OUTRO" : voto.getVotoTipo().name();
            porTipo.computeIfAbsent(chave, ignored -> new ArrayList<>()).add(firstNonBlank(voto.getMagistradoNome(), "MAGISTRADO"));
        }
        if (porTipo.size() > 1) {
            porTipo.forEach((tipo, magistrados) -> divergencias.add(new RelatorPlenarioDivergenciaDto(
                    "VOTO_TIPO",
                    tipo,
                    List.copyOf(magistrados)
            )));
        }

        Map<String, List<String>> fundamentos = new LinkedHashMap<>();
        for (VotoColegiado voto : votos) {
            String resumo = normalizeResumo(voto.getVotoResumo());
            if (resumo.isBlank()) {
                continue;
            }
            fundamentos.computeIfAbsent(resumo, ignored -> new ArrayList<>()).add(firstNonBlank(voto.getMagistradoNome(), "MAGISTRADO"));
        }
        if (fundamentos.size() > 1) {
            fundamentos.entrySet().stream()
                    .sorted(Map.Entry.<String, List<String>>comparingByValue(Comparator.comparingInt(List::size)).reversed())
                    .limit(3)
                    .forEach(entry -> divergencias.add(new RelatorPlenarioDivergenciaDto(
                            "FUNDAMENTO",
                            entry.getKey(),
                            List.copyOf(entry.getValue())
                    )));
        }
        return List.copyOf(divergencias);
    }

    private RelatorPlenarioResultadoDto computarResultado(JulgamentoColegiado sessao, List<VotoColegiado> votos) {
        int favor = sessao.getPlacarFavor() == null ? 0 : sessao.getPlacarFavor();
        int contra = sessao.getPlacarContra() == null ? 0 : sessao.getPlacarContra();
        int parcial = sessao.getPlacarParcial() == null ? 0 : sessao.getPlacarParcial();
        int outros = sessao.getPlacarOutros() == null ? 0 : sessao.getPlacarOutros();
        if (favor + contra + parcial + outros == 0 && votos != null) {
            for (VotoColegiado voto : votos) {
                TipoVotoColegiado tipo = voto.getVotoTipo();
                if (tipo == null) {
                    outros++;
                    continue;
                }
                switch (tipo) {
                    case DAR_PROVIMENTO, ACOMPANHAR_RELATOR -> favor++;
                    case NEGAR_PROVIMENTO -> contra++;
                    case PARCIAL_PROVIMENTO, DAR_PROVIMENTO_EM_PARTE -> parcial++;
                    default -> outros++;
                }
            }
        }
        String tendencia = favor > contra && favor >= parcial ? "FAVORAVEL"
                : contra > favor && contra >= parcial ? "DESFAVORAVEL"
                : parcial > 0 ? "PARCIAL"
                : "EM_DELIBERACAO";
        int gruposAtivos = 0;
        if (favor > 0) gruposAtivos++;
        if (contra > 0) gruposAtivos++;
        if (parcial > 0) gruposAtivos++;
        if (outros > 0) gruposAtivos++;
        return new RelatorPlenarioResultadoDto(favor, contra, parcial, outros, tendencia, gruposAtivos <= 1);
    }

    private String construirMinutaParcial(JulgamentoColegiado sessao,
                                          List<VotoColegiado> votos,
                                          RelatorPlenarioResultadoDto resultado,
                                          String ementaSugerida,
                                          List<RelatorPlenarioDivergenciaDto> divergencias) {
        String numeroProcesso = sessao.getProcesso() != null
                ? firstNonBlank(sessao.getProcesso().getNumeroUnificado(), sessao.getProcesso().getNumeroProcesso(), "sem numero")
                : "sem numero";
        StringBuilder sb = new StringBuilder(512);
        sb.append("RELATORIO PARCIAL — processo ").append(numeroProcesso).append(". ");
        sb.append("Orgao julgador: ").append(firstNonBlank(sessao.getOrgaoJulgador(), "colegiado")).append(". ");
        sb.append("Relatoria: ").append(firstNonBlank(sessao.getRelatorNome(), "nao identificada")).append(". ");
        sb.append("Resultado parcial: ").append(resultado.tendencia())
                .append(" (favor=").append(resultado.favor())
                .append(", contra=").append(resultado.contra())
                .append(", parcial=").append(resultado.parcial())
                .append(", outros=").append(resultado.outros()).append("). ");
        if (!divergencias.isEmpty()) {
            sb.append("Ha divergencias relevantes em ").append(divergencias.size()).append(" eixo(s). ");
        }
        if (votos != null && !votos.isEmpty()) {
            sb.append("Ultimos fundamentos computados: ");
            votos.stream()
                    .map(VotoColegiado::getVotoResumo)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(texto -> !texto.isBlank())
                    .limit(2)
                    .forEach(texto -> sb.append(texto.length() <= 140 ? texto : texto.substring(0, 140)).append("; "));
        }
        sb.append("Ementa sugerida: ").append(ementaSugerida);
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private RelatorPlenarioVoteDto toVoteDto(VotoColegiado voto, boolean possuiDivergencia) {
        return new RelatorPlenarioVoteDto(
                voto.getId(),
                voto.getOrdem(),
                voto.getMagistradoNome(),
                voto.getMagistradoCargo(),
                voto.getPapel() != null ? voto.getPapel().name() : null,
                voto.getVotoTipo() != null ? voto.getVotoTipo().name() : null,
                voto.getVotoResumo(),
                voto.getProferidoEm(),
                possuiDivergencia || voto.getVotoTipo() == TipoVotoColegiado.DIVERGIR
        );
    }

    private void validarStepUp(Usuario usuario) {
        try {
            passkeyRequirementEnforcer.exigirParaMagistratura(usuario.getId(), usuario.getTipoUsuario());
        } catch (PasskeyRequiredException e) {
            throw new IllegalArgumentException("Step-up de passkey obrigatorio para votar em plenario.", e);
        }
    }

    private Usuario requireDesembargador() {
        Usuario usuario = currentUserService.getRequired();
        if (usuario.getTipoUsuario() != TipoUsuario.DESEMBARGADOR
                && usuario.getTipoUsuario() != TipoUsuario.DESEMBARGADOR_FEDERAL) {
            throw new IllegalStateException("Operacao exclusiva de desembargador.");
        }
        return usuario;
    }

    private String normalizeResumo(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 160) {
            normalized = normalized.substring(0, 160);
        }
        return normalized.toUpperCase(Locale.ROOT);
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
}
