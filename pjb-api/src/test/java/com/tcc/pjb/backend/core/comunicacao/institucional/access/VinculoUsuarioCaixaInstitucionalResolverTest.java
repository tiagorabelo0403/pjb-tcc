package com.tcc.pjb.backend.core.comunicacao.institucional.access;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.AbrangenciaVinculoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class VinculoUsuarioCaixaInstitucionalResolverTest {

    @Test
    void membroDoMinisterioPublicoRecebeVinculosDaSuaUf() {
        CatalogoInstitucionalUnificadoService catalogo = new CatalogoInstitucionalUnificadoService(new StaticListableBeanFactory().getBeanProvider(com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService.class));
        EstruturaCaixaInstitucionalService estrutura = new EstruturaCaixaInstitucionalService();
        MatrizCapacidadeCaixaInstitucionalService matriz = new MatrizCapacidadeCaixaInstitucionalService();
        VinculoUsuarioCaixaInstitucionalResolver resolver = new VinculoUsuarioCaixaInstitucionalResolver(catalogo, estrutura, matriz);

        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        usuario.setUf("SP");
        usuario.setComarca("São Paulo");

        List<VinculoUsuarioCaixaInstitucional> vinculos = resolver.resolver(usuario, DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, "SP", null);

        assertFalse(vinculos.isEmpty());
        assertTrue(vinculos.stream().allMatch(v -> "SP".equals(v.unidade().uf())));
        assertTrue(vinculos.stream().anyMatch(v -> v.capacidades().contains(CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO)));
    }

    @Test
    void procuradoriaFederalRecebeAbrangenciaNacional() {
        CatalogoInstitucionalUnificadoService catalogo = new CatalogoInstitucionalUnificadoService(new StaticListableBeanFactory().getBeanProvider(com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService.class));
        EstruturaCaixaInstitucionalService estrutura = new EstruturaCaixaInstitucionalService();
        MatrizCapacidadeCaixaInstitucionalService matriz = new MatrizCapacidadeCaixaInstitucionalService();
        VinculoUsuarioCaixaInstitucionalResolver resolver = new VinculoUsuarioCaixaInstitucionalResolver(catalogo, estrutura, matriz);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setTipoUsuario(TipoUsuario.PROCURADORIA_FEDERAL);

        List<VinculoUsuarioCaixaInstitucional> vinculos = resolver.resolver(usuario, DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA, null, null);

        assertFalse(vinculos.isEmpty());
        assertTrue(vinculos.stream().map(v -> v.unidade().uf()).distinct().count() > 10);
        assertTrue(vinculos.stream().allMatch(v -> v.abrangencia() == AbrangenciaVinculoInstitucional.NACIONAL));
    }
}
