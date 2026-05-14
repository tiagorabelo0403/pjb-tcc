package com.tcc.pjb.backend.service.secretariat.batch;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LoteInteligenteDiscoveryService {

    public record OportunidadeLote(
            RitoProcessual rito,
            String acaoTipo,
            String gargaloTipo,
            int quantidade,
            String mensagem,
            boolean aprovadoParaProcessamento
    ) {}

    private final LoteInteligenteSimilarityService similarityService;
    private final LoteInteligenteSafetyPolicy safetyPolicy;

    public LoteInteligenteDiscoveryService(
            LoteInteligenteSimilarityService similarityService,
            LoteInteligenteSafetyPolicy safetyPolicy) {
        this.similarityService = similarityService;
        this.safetyPolicy = safetyPolicy;
    }

    public List<OportunidadeLote> descobrir(List<LoteInteligenteSimilarityService.ItemSimilaridade> itens) {
        List<LoteInteligenteSimilarityService.GrupoSimilar> grupos = similarityService.agrupar(itens);
        List<OportunidadeLote> oportunidades = new ArrayList<>();

        for (LoteInteligenteSimilarityService.GrupoSimilar grupo : grupos) {
            LoteInteligenteSafetyPolicy.LoteSafetyInput safetyInput =
                    new LoteInteligenteSafetyPolicy.LoteSafetyInput(
                            grupo.acaoTipo(),
                            false,
                            true,
                            true,
                            grupo.tamanho(),
                            false);

            LoteInteligenteSafetyPolicy.LoteSafetyResultado safety = safetyPolicy.avaliar(safetyInput);

            oportunidades.add(new OportunidadeLote(
                    grupo.rito(),
                    grupo.acaoTipo(),
                    grupo.gargaloTipo(),
                    grupo.tamanho(),
                    grupo.descricao(),
                    safety.aprovado()));
        }

        return oportunidades;
    }

    public OportunidadeLote maiorOportunidade(List<LoteInteligenteSimilarityService.ItemSimilaridade> itens) {
        return descobrir(itens).stream()
                .filter(OportunidadeLote::aprovadoParaProcessamento)
                .findFirst()
                .orElse(null);
    }
}
