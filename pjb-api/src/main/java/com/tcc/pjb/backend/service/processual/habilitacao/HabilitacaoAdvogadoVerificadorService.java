package com.tcc.pjb.backend.service.processual.habilitacao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class HabilitacaoAdvogadoVerificadorService {

    public record HabilitacaoInput(
            UUID processoId,
            String oab,
            String ufOab,
            LocalDate dataVencimentoOab,
            boolean procuracaoJuntada,
            boolean substabelecimentoNecessario,
            boolean substabelecidoProcurador,
            boolean acessoSistema
    ) {}

    public record HabilitacaoResult(
            UUID processoId,
            String oab,
            boolean habilitado,
            List<String> pendencias,
            List<String> alertas
    ) {}

    public HabilitacaoResult verificar(HabilitacaoInput input) {
        List<String> pendencias = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        if (input.oab() == null || input.oab().isBlank()) {
            pendencias.add("OAB não informado — advogado sem habilitação nos autos.");
        }
        if (!input.procuracaoJuntada()) {
            pendencias.add("Procuração não juntada ou não localizada nos autos.");
        }
        if (input.substabelecimentoNecessario() && !input.substabelecidoProcurador()) {
            pendencias.add("Substabelecimento necessário não localizado nos autos.");
        }
        if (input.dataVencimentoOab() != null
                && input.dataVencimentoOab().isBefore(LocalDate.now())) {
            alertas.add("OAB com possível validade vencida — verificar situação no sítio da OAB.");
        }
        if (!input.acessoSistema()) {
            alertas.add("Advogado sem acesso configurado ao sistema — verificar cadastro.");
        }
        return new HabilitacaoResult(input.processoId(), input.oab(),
                pendencias.isEmpty(), List.copyOf(pendencias), List.copyOf(alertas));
    }
}
