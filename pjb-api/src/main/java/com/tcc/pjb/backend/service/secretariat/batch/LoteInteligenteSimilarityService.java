package com.tcc.pjb.backend.service.secretariat.batch;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class LoteInteligenteSimilarityService {

    public record ItemSimilaridade(
            UUID processoId,
            String numeroProcesso,
            RitoProcessual rito,
            String acaoTipo,
            String gargaloTipo
    ) {}

    public record GrupoSimilar(
            RitoProcessual rito,
            String acaoTipo,
            String gargaloTipo,
            List<ItemSimilaridade> itens
    ) {
        public int tamanho() { return itens.size(); }

        public String descricao() {
            return tamanho() + " processo(s) — rito " + rito.name()
                    + " — ação: " + acaoTipo + " — gargalo: " + gargaloTipo;
        }
    }

    public List<GrupoSimilar> agrupar(List<ItemSimilaridade> itens) {
        Map<String, List<ItemSimilaridade>> agrupados = itens.stream()
                .collect(Collectors.groupingBy(i -> chaveGrupo(i)));

        return agrupados.values().stream()
                .filter(grupo -> grupo.size() >= 2)
                .map(grupo -> {
                    ItemSimilaridade ref = grupo.get(0);
                    return new GrupoSimilar(ref.rito(), ref.acaoTipo(), ref.gargaloTipo(), grupo);
                })
                .sorted((a, b) -> Integer.compare(b.tamanho(), a.tamanho()))
                .toList();
    }

    private String chaveGrupo(ItemSimilaridade item) {
        return item.rito().name() + "|" + item.acaoTipo() + "|" + item.gargaloTipo();
    }
}
