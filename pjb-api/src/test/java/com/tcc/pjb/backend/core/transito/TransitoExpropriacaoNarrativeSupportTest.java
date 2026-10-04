package com.tcc.pjb.backend.core.transito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TransitoExpropriacaoNarrativeSupportTest {

    private final ExpropriationGovernanceResolver governanceResolver = mock(ExpropriationGovernanceResolver.class);
    private final ExpropriationAuctionCycleResolver auctionCycleResolver = mock(ExpropriationAuctionCycleResolver.class);
    private final ExpropriationHomologationResolver homologationResolver = mock(ExpropriationHomologationResolver.class);
    private final ExpropriationSettlementResolver settlementResolver = mock(ExpropriationSettlementResolver.class);
    private final TransitoExpropriacaoNarrativeSupport support = new TransitoExpropriacaoNarrativeSupport(
            governanceResolver, auctionCycleResolver, homologationResolver, settlementResolver);

    @Test
    void buildExpropriationMatrixMaterializaOsTresCenarios() {
        ExpropriationGovernanceProfile profile = mock(ExpropriationGovernanceProfile.class);
        when(profile.toMap()).thenReturn(Map.of("modalidade", "adjudicacao"));
        when(governanceResolver.resolve(any(), any(), any(), any(), anyDouble())).thenReturn(profile);

        LinkedHashMap<String, Object> matrix = support.buildExpropriationMatrix(mock(Processo.class));

        assertThat(matrix).containsOnlyKeys("ADJUDICACAO_IMOVEL", "ALIENACAO_VEICULO", "HASTA_PUBLICA_QUOTAS");
    }
}
