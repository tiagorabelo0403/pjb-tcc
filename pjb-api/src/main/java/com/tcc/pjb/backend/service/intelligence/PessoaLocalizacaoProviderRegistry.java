package com.tcc.pjb.backend.service.intelligence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService.EnderecoInfo;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService.EnderecoPessoaRegistryClient;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService.MandadoInfo;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService.MandadoPessoaRegistryClient;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService.RestricaoInfo;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService.RestricaoPessoaRegistryClient;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService.SnapshotEndereco;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService.SnapshotMandado;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService.SnapshotRestricao;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoRequest;

@Service
public class PessoaLocalizacaoProviderRegistry {

    private final List<EnderecoPessoaRegistryClient> enderecoClients;
    private final List<RestricaoPessoaRegistryClient> restricaoClients;
    private final List<MandadoPessoaRegistryClient> mandadoClients;

    public PessoaLocalizacaoProviderRegistry(List<EnderecoPessoaRegistryClient> enderecoClients,
                                             List<RestricaoPessoaRegistryClient> restricaoClients,
                                             List<MandadoPessoaRegistryClient> mandadoClients) {
        this.enderecoClients = enderecoClients == null ? List.of() : List.copyOf(enderecoClients);
        this.restricaoClients = restricaoClients == null ? List.of() : List.copyOf(restricaoClients);
        this.mandadoClients = mandadoClients == null ? List.of() : List.copyOf(mandadoClients);
    }

    public SnapshotEndereco consultarEnderecos(String cpf, Usuario executor, PessoaLocalizacaoRequest request, Processo processoContexto) {
        if (enderecoClients.isEmpty()) {
            return new SnapshotEndereco(false, false, List.of(), List.of("Provedor externo de localização residencial não configurado."));
        }
        LinkedHashMap<String, EnderecoInfo> enderecos = new LinkedHashMap<>();
        LinkedHashSet<String> highlights = new LinkedHashSet<>();
        boolean enabled = false;
        boolean realtime = false;
        for (EnderecoPessoaRegistryClient client : enderecoClients) {
            SnapshotEndereco snapshot = safeEndereco(client, cpf, executor, request, processoContexto);
            enabled = enabled || snapshot.enabled();
            realtime = realtime || snapshot.realtime();
            snapshot.highlights().stream().filter(Objects::nonNull).forEach(highlights::add);
            for (EnderecoInfo endereco : snapshot.enderecos()) {
                if (endereco == null) {
                    continue;
                }
                String key = dedupeEnderecoKey(endereco);
                EnderecoInfo atual = enderecos.get(key);
                if (atual == null || score(atual) < score(endereco)) {
                    enderecos.put(key, normalizarEndereco(endereco));
                }
            }
        }
        if (!enabled && highlights.isEmpty()) {
            highlights.add("Nenhum provedor de endereço retornou disponibilidade operacional.");
        }
        return new SnapshotEndereco(enabled, realtime, List.copyOf(enderecos.values()), List.copyOf(highlights));
    }

    public SnapshotRestricao consultarRestricoes(String cpf, Usuario executor, PessoaLocalizacaoRequest request, Processo processoContexto) {
        if (restricaoClients.isEmpty()) {
            return new SnapshotRestricao(false, false, List.of(), List.of("Motor de restrições patrimoniais/pessoais não configurado."));
        }
        LinkedHashMap<String, RestricaoInfo> restricoes = new LinkedHashMap<>();
        LinkedHashSet<String> highlights = new LinkedHashSet<>();
        boolean enabled = false;
        boolean realtime = false;
        for (RestricaoPessoaRegistryClient client : restricaoClients) {
            SnapshotRestricao snapshot = safeRestricao(client, cpf, executor, request, processoContexto);
            enabled = enabled || snapshot.enabled();
            realtime = realtime || snapshot.realtime();
            snapshot.highlights().stream().filter(Objects::nonNull).forEach(highlights::add);
            for (RestricaoInfo restricao : snapshot.restricoes()) {
                if (restricao == null) {
                    continue;
                }
                restricoes.putIfAbsent(dedupeRestricaoKey(restricao), normalizarRestricao(restricao));
            }
        }
        if (!enabled && highlights.isEmpty()) {
            highlights.add("Nenhum provedor de restrição retornou disponibilidade operacional.");
        }
        return new SnapshotRestricao(enabled, realtime, List.copyOf(restricoes.values()), List.copyOf(highlights));
    }

    public SnapshotMandado consultarMandados(String cpf, Usuario executor, PessoaLocalizacaoRequest request, Processo processoContexto) {
        if (mandadoClients.isEmpty()) {
            return new SnapshotMandado(false, false, List.of(), List.of("Consulta de mandados por pessoa não configurada."));
        }
        LinkedHashMap<String, MandadoInfo> mandados = new LinkedHashMap<>();
        LinkedHashSet<String> highlights = new LinkedHashSet<>();
        boolean enabled = false;
        boolean realtime = false;
        for (MandadoPessoaRegistryClient client : mandadoClients) {
            SnapshotMandado snapshot = safeMandado(client, cpf, executor, request, processoContexto);
            enabled = enabled || snapshot.enabled();
            realtime = realtime || snapshot.realtime();
            snapshot.highlights().stream().filter(Objects::nonNull).forEach(highlights::add);
            for (MandadoInfo mandado : snapshot.mandados()) {
                if (mandado == null) {
                    continue;
                }
                mandados.putIfAbsent(dedupeMandadoKey(mandado), normalizarMandado(mandado));
            }
        }
        if (!enabled && highlights.isEmpty()) {
            highlights.add("Nenhum provedor de mandados retornou disponibilidade operacional.");
        }
        return new SnapshotMandado(enabled, realtime, List.copyOf(mandados.values()), List.copyOf(highlights));
    }

    private static SnapshotEndereco safeEndereco(EnderecoPessoaRegistryClient client,
                                                 String cpf,
                                                 Usuario executor,
                                                 PessoaLocalizacaoRequest request,
                                                 Processo processoContexto) {
        try {
            SnapshotEndereco snapshot = client.consultarPorCpf(cpf, executor, request, processoContexto);
            return snapshot == null ? new SnapshotEndereco(false, false, List.of(), List.of()) : snapshot;
        } catch (Exception ex) {
            return new SnapshotEndereco(false, false, List.of(), List.of(ex.getClass().getSimpleName()));
        }
    }

    private static SnapshotRestricao safeRestricao(RestricaoPessoaRegistryClient client,
                                                   String cpf,
                                                   Usuario executor,
                                                   PessoaLocalizacaoRequest request,
                                                   Processo processoContexto) {
        try {
            SnapshotRestricao snapshot = client.consultarPorCpf(cpf, executor, request, processoContexto);
            return snapshot == null ? new SnapshotRestricao(false, false, List.of(), List.of()) : snapshot;
        } catch (Exception ex) {
            return new SnapshotRestricao(false, false, List.of(), List.of(ex.getClass().getSimpleName()));
        }
    }

    private static SnapshotMandado safeMandado(MandadoPessoaRegistryClient client,
                                               String cpf,
                                               Usuario executor,
                                               PessoaLocalizacaoRequest request,
                                               Processo processoContexto) {
        try {
            SnapshotMandado snapshot = client.consultarPorCpf(cpf, executor, request, processoContexto);
            return snapshot == null ? new SnapshotMandado(false, false, List.of(), List.of()) : snapshot;
        } catch (Exception ex) {
            return new SnapshotMandado(false, false, List.of(), List.of(ex.getClass().getSimpleName()));
        }
    }

    private static String dedupeEnderecoKey(EnderecoInfo endereco) {
        return key(endereco.fonte(), endereco.tipo(), endereco.descricao(), endereco.bairro(), endereco.cidade(), endereco.uf(), endereco.cep());
    }

    private static String dedupeRestricaoKey(RestricaoInfo restricao) {
        return key(restricao.sistema(), restricao.tipo(), restricao.situacao(), restricao.referencia(), restricao.descricao());
    }

    private static String dedupeMandadoKey(MandadoInfo mandado) {
        return key(mandado.sistema(), mandado.tipo(), mandado.situacao(), mandado.referencia(), mandado.descricao());
    }

    private static String key(String... parts) {
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                values.add(part.trim().toUpperCase());
            }
        }
        return String.join("|", values);
    }

    private static double score(EnderecoInfo endereco) {
        double base = endereco.confianca();
        if (endereco.principal()) {
            base += 1.0d;
        }
        if (!endereco.parcial()) {
            base += 0.5d;
        }
        return base;
    }

    private static EnderecoInfo normalizarEndereco(EnderecoInfo endereco) {
        return new EnderecoInfo(
                endereco.fonte(),
                endereco.tipo(),
                endereco.descricao(),
                endereco.bairro(),
                endereco.cidade(),
                endereco.uf(),
                endereco.cep(),
                endereco.principal(),
                endereco.parcial(),
                endereco.confianca(),
                endereco.atualizadoEm() == null ? Instant.now() : endereco.atualizadoEm()
        );
    }

    private static RestricaoInfo normalizarRestricao(RestricaoInfo restricao) {
        return new RestricaoInfo(
                restricao.sistema(),
                restricao.tipo(),
                restricao.situacao(),
                restricao.referencia(),
                restricao.descricao(),
                restricao.atualizadoEm() == null ? Instant.now() : restricao.atualizadoEm()
        );
    }

    private static MandadoInfo normalizarMandado(MandadoInfo mandado) {
        return new MandadoInfo(
                mandado.sistema(),
                mandado.tipo(),
                mandado.situacao(),
                mandado.referencia(),
                mandado.descricao(),
                mandado.atualizadoEm() == null ? Instant.now() : mandado.atualizadoEm()
        );
    }
}
