package com.tcc.pjb.backend.core.governance.fonte.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.governance.fonte.domain.FonteSoberanaConfiabilidade;
import com.tcc.pjb.backend.core.governance.fonte.domain.FonteSoberanaStatus;
import com.tcc.pjb.backend.core.governance.fonte.domain.ProcessoFonteSoberanaAggregate;
import com.tcc.pjb.backend.core.governance.fonte.domain.ProcessoFonteSoberanaRegistro;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaSupportBridge;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.governance.FonteSoberanaSnapshotEntity;
import com.tcc.pjb.backend.model.repository.governance.FonteSoberanaSnapshotRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoFonteSoberanaApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoMalhaSupportBridge processoMalhaSupportBridge;
    private final FonteSoberanaSnapshotRepository fonteSoberanaSnapshotRepository;

    public ProcessoFonteSoberanaApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                   ProcessoMalhaSupportBridge processoMalhaSupportBridge,
                                                   FonteSoberanaSnapshotRepository fonteSoberanaSnapshotRepository) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoMalhaSupportBridge = Objects.requireNonNull(processoMalhaSupportBridge);
        this.fonteSoberanaSnapshotRepository = Objects.requireNonNull(fonteSoberanaSnapshotRepository);
    }

    @Transactional
    public ProcessoFonteSoberanaAggregate consolidar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        return consolidar(contexto);
    }

    @Transactional
    public ProcessoFonteSoberanaAggregate consolidar(ProcessoRuntimeContext contexto) {
        Processo processo = contexto.processo();
        Usuario usuario = processo.getUsuario();
        Instant agora = Instant.now();
        List<ProcessoFonteSoberanaRegistro> registros = new ArrayList<>();
        registros.add(registro("processo.numero", "processo", primeiraNaoVazia(processo.getNumero(), processo.getNumeroUnificado()), "tb_processo.numero", "connectorProtocolReference", FonteSoberanaConfiabilidade.OFICIAL, 30, "numero-legacy"));
        registros.add(registro("processo.ramo", "classificacao", processo.getRamoDireito() == null ? "" : processo.getRamoDireito().name(), "tb_processo.ramo_direito", "painel-contextual", FonteSoberanaConfiabilidade.OFICIAL, 30, "ramo-heuristico"));
        registros.add(registro("processo.sigilo", "seguranca", processo.getNivelSigilo() == null ? "PUBLICO" : processo.getNivelSigilo().name(), "tb_processo.nivel_sigilo", "malha-sigilo", FonteSoberanaConfiabilidade.OFICIAL, 10, "sigilo-publico"));
        registros.add(registro("parte.autora.cpf", "parte", processo.getParteAutoraCpf(), "tb_processo.parte_autora_cpf", "grafo-identidade", confiabilidadeDocumento(processo.getParteAutoraCpf()), 10, "parte-autora-sem-cpf"));
        registros.add(registro("parte.reu.cpf", "parte", processo.getParteReuCpf(), "tb_processo.parte_reu_cpf", "grafo-identidade", confiabilidadeDocumento(processo.getParteReuCpf()), 10, "parte-reu-sem-cpf"));
        registros.add(registro("ator.cpf", "ator", usuario == null ? "" : usuario.getCpf(), "tb_usuario.cpf", "govbr-assurance", confiabilidadeDocumento(usuario == null ? null : usuario.getCpf()), 15, "usuario-sem-cpf"));
        registros.add(registro("ator.oab", "ator", usuario == null ? "" : primeiraNaoVazia(usuario.getOabNormalizada(), usuario.getOab()), "tb_usuario.oab", "oab-federada", usuario != null && usuario.isAdvogado() ? FonteSoberanaConfiabilidade.DERIVADA_VERIFICADA : FonteSoberanaConfiabilidade.DERIVADA, 15, "usuario-sem-oab"));
        Map<String, ProcessoFonteSoberanaRegistro> consolidados = new LinkedHashMap<>();
        for (ProcessoFonteSoberanaRegistro registro : registros) {
            consolidados.put(registro.chave(), ajustarStatus(registro, agora));
        }
        List<ProcessoFonteSoberanaRegistro> saida = List.copyOf(consolidados.values());
        int media = saida.isEmpty() ? 0 : (int) Math.round(saida.stream().mapToInt(item -> item.confiabilidade().score()).average().orElse(0));
        boolean conflito = saida.stream().anyMatch(item -> item.status() == FonteSoberanaStatus.CONFLITANTE);
        boolean exigeRefresh = saida.stream().anyMatch(item -> item.status() == FonteSoberanaStatus.EXPIRADA || item.status() == FonteSoberanaStatus.REVALIDAR);
        String digest = Hashes.sha256Hex(saida.toString());
        FonteSoberanaSnapshotEntity snapshot = fonteSoberanaSnapshotRepository.findByProcessoId(processo.getId())
                .orElseGet(FonteSoberanaSnapshotEntity::new);
        snapshot.setProcessoId(processo.getId());
        snapshot.setNumeroProcesso(contexto.numeroReferencia());
        snapshot.setConfiabilidadeMedia(media);
        snapshot.setPossuiConflito(conflito);
        snapshot.setExigeRefresh(exigeRefresh);
        snapshot.setDigest(digest);
        snapshot.setPayloadJson(saida.toString());
        snapshot.setAtualizadoEm(agora);
        fonteSoberanaSnapshotRepository.save(snapshot);
        DecisionTraceService trace = processoMalhaSupportBridge.decisionTraceService();
        if (trace != null) {
            trace.record("governance.fonte.soberana", "Processo", String.valueOf(processo.getId()), BigDecimal.valueOf(media), saida.toString(), saida.toString(), digest, digest, "PJB-FONTE", "refresh=" + exigeRefresh);
        }
        AuditLedgerService audit = processoMalhaSupportBridge.auditLedgerService();
        if (audit != null) {
            audit.appendSafely("FONTE_SOBERANA_ATUALIZADA", "Processo", String.valueOf(processo.getId()), digest, "media=" + media + ";conflito=" + conflito + ";refresh=" + exigeRefresh);
        }
        return new ProcessoFonteSoberanaAggregate(processo.getId(), contexto.numeroReferencia(), saida, media, conflito, exigeRefresh, agora);
    }

    private ProcessoFonteSoberanaRegistro registro(String chave,
                                                   String dominio,
                                                   String valor,
                                                   String fonteOficial,
                                                   String fonteDerivada,
                                                   FonteSoberanaConfiabilidade confiabilidade,
                                                   long validadeDias,
                                                   String fallback) {
        String normalizado = Objects.toString(valor, "").trim();
        FonteSoberanaStatus status = normalizado.isBlank() ? FonteSoberanaStatus.REVALIDAR : FonteSoberanaStatus.VALIDA;
        String fallbackAplicado = normalizado.isBlank() ? fallback : "";
        String digest = Hashes.sha256Hex(String.join("#", chave, dominio, normalizado, fonteOficial, fonteDerivada, confiabilidade.name(), status.name(), fallbackAplicado));
        return new ProcessoFonteSoberanaRegistro(chave, dominio, normalizado, fonteOficial, fonteDerivada, confiabilidade, status, Instant.now().plus(validadeDias, ChronoUnit.DAYS), fallbackAplicado, digest);
    }

    private ProcessoFonteSoberanaRegistro ajustarStatus(ProcessoFonteSoberanaRegistro registro, Instant referencia) {
        if (registro.expirada(referencia)) {
            return new ProcessoFonteSoberanaRegistro(registro.chave(), registro.dominio(), registro.valor(), registro.fonteOficial(), registro.fonteDerivada(), registro.confiabilidade(), FonteSoberanaStatus.EXPIRADA, registro.validoAte(), registro.fallbackAplicado(), registro.digest());
        }
        if (registro.valor().isBlank()) {
            return registro;
        }
        if (registro.fonteOficial().equalsIgnoreCase("tb_processo.nivel_sigilo") && !registro.valor().startsWith("SIGILO") && !registro.valor().startsWith("SEGREDO") && !registro.valor().equals("PUBLICO")) {
            return new ProcessoFonteSoberanaRegistro(registro.chave(), registro.dominio(), registro.valor(), registro.fonteOficial(), registro.fonteDerivada(), registro.confiabilidade(), FonteSoberanaStatus.CONFLITANTE, registro.validoAte(), registro.fallbackAplicado(), registro.digest());
        }
        return registro;
    }

    private FonteSoberanaConfiabilidade confiabilidadeDocumento(String valor) {
        String normalizado = Objects.toString(valor, "").replaceAll("\\D", "");
        if (normalizado.length() == 11 || normalizado.length() == 14) {
            return FonteSoberanaConfiabilidade.DERIVADA_VERIFICADA;
        }
        return FonteSoberanaConfiabilidade.DERIVADA;
    }

    private String primeiraNaoVazia(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor.trim();
            }
        }
        return "";
    }
}
