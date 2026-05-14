package com.tcc.pjb.backend.service.perito;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PeritoSugestaoService {

    public record PeritoProfile(
            UUID peritoId,
            String nome,
            String cpf,
            List<String> especialidades,
            String comarcaId,
            List<String> comarcasAtendidas,
            int periciasPendentes,
            boolean ativo,
            boolean credenciadoCnj
    ) {}

    public record SugestaoPerito(
            UUID peritoId,
            String nome,
            String especialidade,
            int cargaAtual,
            String justificativa,
            boolean credenciadoCnj
    ) {}

    public List<SugestaoPerito> sugerirParaEspecialidade(
            String especialidade, String comarcaId,
            List<PeritoProfile> peritos) {
        return peritos.stream()
                .filter(PeritoProfile::ativo)
                .filter(PeritoProfile::credenciadoCnj)
                .filter(p -> p.especialidades().contains(especialidade))
                .filter(p -> p.comarcaId().equals(comarcaId)
                        || p.comarcasAtendidas().contains(comarcaId))
                .sorted((a, b) -> Integer.compare(a.periciasPendentes(), b.periciasPendentes()))
                .map(p -> new SugestaoPerito(
                        p.peritoId(), p.nome(), especialidade,
                        p.periciasPendentes(),
                        "Perito " + p.nome() + " com " + p.periciasPendentes()
                                + " perícia(s) pendente(s) — menor carga credenciada.",
                        p.credenciadoCnj()))
                .toList();
    }
}
