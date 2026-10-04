package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.Processo;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class TransitoExpropriacaoNarrativeSupport {

    private final ExpropriationGovernanceResolver expropriationGovernanceResolver;
    private final ExpropriationAuctionCycleResolver expropriationAuctionCycleResolver;
    private final ExpropriationHomologationResolver expropriationHomologationResolver;
    private final ExpropriationSettlementResolver expropriationSettlementResolver;

    public TransitoExpropriacaoNarrativeSupport(ExpropriationGovernanceResolver expropriationGovernanceResolver,
                                                ExpropriationAuctionCycleResolver expropriationAuctionCycleResolver,
                                                ExpropriationHomologationResolver expropriationHomologationResolver,
                                                ExpropriationSettlementResolver expropriationSettlementResolver) {
        this.expropriationGovernanceResolver = Objects.requireNonNull(expropriationGovernanceResolver);
        this.expropriationAuctionCycleResolver = Objects.requireNonNull(expropriationAuctionCycleResolver);
        this.expropriationHomologationResolver = Objects.requireNonNull(expropriationHomologationResolver);
        this.expropriationSettlementResolver = Objects.requireNonNull(expropriationSettlementResolver);
    }

    public LinkedHashMap<String, Object> buildExpropriationMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("ADJUDICACAO_IMOVEL", expropriationGovernanceResolver.resolve(processo, "adjudicacao", "imovel", "direta", 150000D).toMap());
        matrix.put("ALIENACAO_VEICULO", expropriationGovernanceResolver.resolve(processo, "alienacao judicial", "veiculo", "privada", 80000D).toMap());
        matrix.put("HASTA_PUBLICA_QUOTAS", expropriationGovernanceResolver.resolve(processo, "hasta publica", "quotas societarias", "eletronica", 350000D).toMap());
        return matrix;
    }

    public LinkedHashMap<String, Object> buildAuctionCycleMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("PRIMEIRA_PRACA_IMOVEL", expropriationAuctionCycleResolver.resolve(processo, "hasta publica", "imovel", "eletronica", 1, 250000D).toMap());
        matrix.put("SEGUNDA_PRACA_VEICULO", expropriationAuctionCycleResolver.resolve(processo, "hasta publica", "veiculo", "eletronica", 2, 65000D).toMap());
        matrix.put("ADJUDICACAO_QUOTAS", expropriationAuctionCycleResolver.resolve(processo, "adjudicacao", "quotas societarias", "direta", 1, 410000D).toMap());
        return matrix;
    }

    public LinkedHashMap<String, Object> buildHomologationMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("ADJUDICACAO_IMOVEL", expropriationHomologationResolver.resolve(processo, "adjudicacao", "imovel", "direta", "Credor exequente", 180000D).toMap());
        matrix.put("ARREMATACAO_VEICULO", expropriationHomologationResolver.resolve(processo, "arrematacao", "veiculo", "eletronica", "Adquirente particular", 65000D).toMap());
        matrix.put("ARREMATACAO_QUOTAS", expropriationHomologationResolver.resolve(processo, "hasta publica", "quotas societarias", "eletronica", "Sociedade adquirente", 420000D).toMap());
        return matrix;
    }

    public LinkedHashMap<String, Object> buildSettlementMatrix(Processo processo) {
        LinkedHashMap<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("PRODUTO_INTEGRAL_IMOVEL", expropriationSettlementResolver.resolve(processo, "imovel", "deposito judicial", "hipoteca", "sim", 250000D, 0D, 220000D).toMap());
        matrix.put("PRODUTO_PARCIAL_VEICULO", expropriationSettlementResolver.resolve(processo, "veiculo", "deposito judicial", "trabalhista", "nao", 65000D, 35000D, 100000D).toMap());
        matrix.put("PRODUTO_COM_EXCEDENTE_DINHEIRO", expropriationSettlementResolver.resolve(processo, "dinheiro", "deposito judicial", "ordinaria", "nao", 120000D, 0D, 90000D).toMap());
        return matrix;
    }
}
