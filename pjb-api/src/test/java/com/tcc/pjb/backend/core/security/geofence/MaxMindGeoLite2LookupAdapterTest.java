package com.tcc.pjb.backend.core.security.geofence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MaxMindGeoLite2LookupAdapterTest {

    @Test
    void semBaseConfiguradaLookupRetornaIndisponivelSemLancarExcecao() {
        GeofenceProperties props = new GeofenceProperties(null, List.of(), true);
        MaxMindGeoLite2LookupAdapter adapter = new MaxMindGeoLite2LookupAdapter(props);
        adapter.carregar();

        GeoLookupResult resultado = adapter.lookup("8.8.8.8");

        assertThat(resultado.disponivel()).isFalse();
    }

    @Test
    void caminhoDeArquivoInexistenteNaoLancaExcecaoNaInicializacao() {
        GeofenceProperties props = new GeofenceProperties("/caminho/que/nao/existe.mmdb", List.of(), true);
        MaxMindGeoLite2LookupAdapter adapter = new MaxMindGeoLite2LookupAdapter(props);

        adapter.carregar();

        assertThat(adapter.lookup("8.8.8.8").disponivel()).isFalse();
    }
}
