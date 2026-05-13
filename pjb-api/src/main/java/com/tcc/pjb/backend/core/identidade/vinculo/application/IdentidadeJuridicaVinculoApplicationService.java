package com.tcc.pjb.backend.core.identidade.vinculo.application;

import com.tcc.pjb.backend.core.identidade.resolucao.application.IdentidadeJuridicaResolucaoApplicationService;
import com.tcc.pjb.backend.core.identidade.resolucao.domain.IdentidadeJuridicaResolucaoAggregate;
import com.tcc.pjb.backend.core.identidade.resolucao.domain.IdentidadeJuridicaResolucaoEntrada;
import com.tcc.pjb.backend.core.identidade.resolucao.domain.IdentidadeJuridicaResolucaoStatus;
import com.tcc.pjb.backend.core.identidade.vinculo.domain.IdentidadeJuridicaPapelProcessual;
import com.tcc.pjb.backend.core.identidade.vinculo.domain.IdentidadeJuridicaVinculoAggregate;
import com.tcc.pjb.backend.core.identidade.vinculo.domain.IdentidadeJuridicaVinculoItem;
import com.tcc.pjb.backend.core.identidade.vinculo.domain.IdentidadeJuridicaVinculoParte;
import com.tcc.pjb.backend.core.identidade.vinculo.domain.IdentidadeJuridicaVinculoSolicitacao;
import com.tcc.pjb.backend.core.util.DeterministicUuid;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentidadeJuridicaVinculoApplicationService {

    private final ProcessoRepository processoRepository;
    private final IdentidadeJuridicaResolucaoApplicationService resolucaoApplicationService;

    public IdentidadeJuridicaVinculoApplicationService(ProcessoRepository processoRepository,
                                                       IdentidadeJuridicaResolucaoApplicationService resolucaoApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.resolucaoApplicationService = Objects.requireNonNull(resolucaoApplicationService);
    }

    @Transactional(readOnly = true)
    public IdentidadeJuridicaVinculoAggregate analisar(IdentidadeJuridicaVinculoSolicitacao solicitacao) {
        Processo processo = carregarProcesso(solicitacao);
        List<IdentidadeJuridicaVinculoParte> partes = extrairPartes(processo);
        List<IdentidadeJuridicaResolucaoEntrada> entradas = partes.stream()
                .map(this::toResolucaoEntrada)
                .toList();
        IdentidadeJuridicaResolucaoAggregate resolucao = resolucaoApplicationService.resolver(
                solicitacao.solicitante(),
                blankToNull(solicitacao.origemSolicitacao()) == null ? "IDENTIDADE_VINCULO" : solicitacao.origemSolicitacao(),
                entradas,
                List.of(processo.getNumero())
        );
        List<IdentidadeJuridicaVinculoItem> itens = montarItens(processo, partes, resolucao);
        List<String> alertas = montarAlertas(processo, resolucao, itens);
        return new IdentidadeJuridicaVinculoAggregate(
                processo.getId(),
                processo.getNumero(),
                partes,
                itens,
                alertas,
                resolucao,
                resolucao.grafo(),
                Instant.now()
        );
    }

    private Processo carregarProcesso(IdentidadeJuridicaVinculoSolicitacao solicitacao) {
        if (solicitacao.processoId() != null) {
            return processoRepository.findById(solicitacao.processoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", solicitacao.processoId()));
        }
        return processoRepository.findByNumero(solicitacao.numeroProcesso())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", solicitacao.numeroProcesso()));
    }

    private List<IdentidadeJuridicaVinculoParte> extrairPartes(Processo processo) {
        ArrayList<IdentidadeJuridicaVinculoParte> partes = new ArrayList<>();
        if (blankToNull(processo.getParteAutoraNome()) != null || blankToNull(processo.getParteAutoraCpf()) != null) {
            partes.add(new IdentidadeJuridicaVinculoParte(
                    IdentidadeJuridicaPapelProcessual.AUTOR,
                    processo.getParteAutoraNome(),
                    processo.getParteAutoraCpf(),
                    null,
                    null,
                    null,
                    "ATIVO",
                    Map.of("processo", processo.getNumero())
            ));
        }
        if (blankToNull(processo.getParteReuNome()) != null || blankToNull(processo.getParteReuCpf()) != null) {
            partes.add(new IdentidadeJuridicaVinculoParte(
                    IdentidadeJuridicaPapelProcessual.REU,
                    processo.getParteReuNome(),
                    processo.getParteReuCpf(),
                    null,
                    null,
                    null,
                    "PASSIVO",
                    Map.of("processo", processo.getNumero())
            ));
        }
        Usuario usuario = processo.getUsuario();
        if (usuario != null && (blankToNull(usuario.getNome()) != null || blankToNull(usuario.getCpf()) != null || blankToNull(usuario.getEmail()) != null)) {
            LinkedHashMap<String, String> atributos = new LinkedHashMap<>();
            atributos.put("processo", processo.getNumero());
            atributos.put("perfil", Objects.toString(usuario.getPerfil(), ""));
            atributos.put("tipoUsuario", usuario.getTipoUsuario() == null ? "" : usuario.getTipoUsuario().name());
            if (blankToNull(usuario.getOabUf()) != null) {
                atributos.put("oabUf", usuario.getOabUf());
            }
            partes.add(new IdentidadeJuridicaVinculoParte(
                    usuario.isAdvogado() ? IdentidadeJuridicaPapelProcessual.ADVOGADO : IdentidadeJuridicaPapelProcessual.REPRESENTANTE,
                    usuario.getNome(),
                    usuario.getCpf(),
                    usuario.getEmail(),
                    null,
                    firstNonBlank(usuario.getOabNormalizada(), usuario.getOab()),
                    null,
                    Map.copyOf(atributos)
            ));
        }
        return List.copyOf(partes);
    }

    private IdentidadeJuridicaResolucaoEntrada toResolucaoEntrada(IdentidadeJuridicaVinculoParte parte) {
        return new IdentidadeJuridicaResolucaoEntrada(
                parte.papel().name(),
                parte.nome(),
                parte.documento(),
                parte.email(),
                parte.telefone(),
                parte.numeroOab(),
                parte.polo(),
                parte.papel().name(),
                parte.atributos()
        );
    }

    private List<IdentidadeJuridicaVinculoItem> montarItens(Processo processo,
                                                            List<IdentidadeJuridicaVinculoParte> partes,
                                                            IdentidadeJuridicaResolucaoAggregate resolucao) {
        ArrayList<IdentidadeJuridicaVinculoItem> itens = new ArrayList<>();
        for (int index = 0; index < partes.size(); index++) {
            IdentidadeJuridicaVinculoParte parte = partes.get(index);
            if (index >= resolucao.itens().size()) {
                continue;
            }
            var itemResolvido = resolucao.itens().get(index);
            LinkedHashSet<String> processosCorrelatos = new LinkedHashSet<>();
            resolucao.grafo().vertices().stream()
                    .filter(vertice -> vertice.tipo() == com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVerticeTipo.PROCESSO)
                    .map(com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVertice::chaveCanonica)
                    .filter(numero -> !numero.equals(processo.getNumero()))
                    .forEach(processosCorrelatos::add);
            ArrayList<String> alertas = new ArrayList<>();
            if (itemResolvido.status() == IdentidadeJuridicaResolucaoStatus.AMBIGUA) {
                alertas.add("A identidade vinculada ficou ambígua e exige saneamento antes de consolidar prevenção ou redistribuição.");
            }
            if (itemResolvido.status() == IdentidadeJuridicaResolucaoStatus.FRACA) {
                alertas.add("A identidade vinculada ficou fraca e não deve sustentar decisão restritiva sem reforço documental.");
            }
            if (!processosCorrelatos.isEmpty()) {
                alertas.add("A parte já emerge no grafo com feitos correlatos além do processo raiz.");
            }
            ArrayList<String> fundamentos = new ArrayList<>();
            fundamentos.add("O vínculo processual reaproveita a resolução canônica e expõe o cluster de identidade na linha do processo.");
            fundamentos.addAll(itemResolvido.fundamentos());
            itens.add(new IdentidadeJuridicaVinculoItem(
                    DeterministicUuid.v5("pjb-identidade-vinculo-item", processo.getNumero() + "#" + parte.papel() + "#" + itemResolvido.verticeId()).toString(),
                    parte,
                    itemResolvido,
                    List.copyOf(processosCorrelatos),
                    List.copyOf(alertas),
                    List.copyOf(fundamentos)
            ));
        }
        return List.copyOf(itens);
    }

    private List<String> montarAlertas(Processo processo,
                                       IdentidadeJuridicaResolucaoAggregate resolucao,
                                       List<IdentidadeJuridicaVinculoItem> itens) {
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (!resolucao.conflitos().isEmpty()) {
            alertas.addAll(resolucao.conflitos());
        }
        long ambiguos = itens.stream().filter(item -> item.resolucao().status() == IdentidadeJuridicaResolucaoStatus.AMBIGUA).count();
        if (ambiguos > 0) {
            alertas.add("O processo contém identidades ambíguas que podem contaminar prevenção, conexão, sigilo e atuação representativa.");
        }
        long fracos = itens.stream().filter(item -> item.resolucao().status() == IdentidadeJuridicaResolucaoStatus.FRACA).count();
        if (fracos > 0) {
            alertas.add("O processo contém identidades fracas que devem ser fortalecidas por documento oficial, OAB ou outro identificador de alta confiança.");
        }
        Set<String> correlatos = new LinkedHashSet<>();
        itens.forEach(item -> correlatos.addAll(item.processosCorrelatos()));
        if (!correlatos.isEmpty()) {
            alertas.add("O grafo de identidade já projeta processos correlatos ao feito " + processo.getNumero() + ".");
        }
        return List.copyOf(alertas);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }
}
