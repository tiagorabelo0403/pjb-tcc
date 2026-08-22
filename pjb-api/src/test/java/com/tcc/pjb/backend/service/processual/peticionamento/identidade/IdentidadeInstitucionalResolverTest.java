package com.tcc.pjb.backend.service.processual.peticionamento.identidade;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.processual.peticionamento.identidade.IdentidadeInstitucionalResolver.ClasseIdentidade;
import com.tcc.pjb.backend.service.processual.peticionamento.identidade.IdentidadeInstitucionalResolver.IdentidadeInstitucionalDescriptor;
import org.junit.jupiter.api.Test;

class IdentidadeInstitucionalResolverTest {

    private final IdentidadeInstitucionalResolver resolver = new IdentidadeInstitucionalResolver();

    @Test
    void magistraturaEstadualResolveTribunalDeJusticaPorUf() {
        IdentidadeInstitucionalDescriptor d = resolver.resolver(TipoUsuario.JUIZ_ESTADUAL, "CE");
        assertThat(d.classe()).isEqualTo(ClasseIdentidade.INSTITUCIONAL);
        assertThat(d.poderRamo()).isEqualTo("PODER_JUDICIARIO");
        assertThat(d.escopoRef()).isEqualTo("PJ-EST-CE");
        assertThat(d.cabecalhoSugerido().get(0)).isEqualTo("PODER JUDICIÁRIO");
        assertThat(d.paletaDefault()).containsEntry("origem", IdentidadeInstitucionalResolver.ORIGEM_DEFAULT);
    }

    @Test
    void ministroResolveTribunalSuperiorSemChutarQual() {
        IdentidadeInstitucionalDescriptor d = resolver.resolver(TipoUsuario.MINISTRO, null);
        assertThat(d.escopoRef()).isEqualTo("PJ-SUPERIOR");
        assertThat(d.cabecalhoSugerido()).contains("Tribunal Superior");
    }

    @Test
    void juizFederalETrabalhistaSeDiferenciam() {
        assertThat(resolver.resolver(TipoUsuario.JUIZ_FEDERAL, "SP").escopoRef()).isEqualTo("PJ-FED-SP");
        assertThat(resolver.resolver(TipoUsuario.JUIZ_TRABALHISTA, "SP").escopoRef()).isEqualTo("PJ-TRAB-SP");
    }

    @Test
    void ministerioPublicoEstadualVsFederal() {
        assertThat(resolver.resolver(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO, "BA").escopoRef()).isEqualTo("MP-EST-BA");
        assertThat(resolver.resolver(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO, "BA").nomeOrgao()).contains("MINISTÉRIO PÚBLICO");
        assertThat(resolver.resolver(TipoUsuario.PROCURADOR_GERAL_REPUBLICA, null).escopoRef()).isEqualTo("MP-FED");
    }

    @Test
    void defensoriaEstadualVsUniao() {
        assertThat(resolver.resolver(TipoUsuario.DEFENSOR_PUBLICO, "RN").escopoRef()).isEqualTo("DP-EST-RN");
        assertThat(resolver.resolver(TipoUsuario.DEFENSOR_PUBLICO_FEDERAL, "RN").escopoRef()).isEqualTo("DP-FED");
        assertThat(resolver.resolver(TipoUsuario.DEFENSOR_PUBLICO_FEDERAL, "RN").nomeOrgao()).isEqualTo("DEFENSORIA PÚBLICA DA UNIÃO");
    }

    @Test
    void procuradoriasMunicipalEstadualFederal() {
        assertThat(resolver.resolver(TipoUsuario.PROCURADORIA_MUNICIPAL, "MG").escopoRef()).isEqualTo("PROC-MUN-MG");
        assertThat(resolver.resolver(TipoUsuario.PROCURADORIA_ESTADUAL, "MG").escopoRef()).isEqualTo("PROC-EST-MG");
        assertThat(resolver.resolver(TipoUsuario.PROCURADORIA_FEDERAL, "MG").nomeOrgao()).isEqualTo("ADVOCACIA-GERAL DA UNIÃO");
    }

    @Test
    void procuradoriaMunicipalDesceAoMunicipioRealQuandoConhecido() {
        IdentidadeInstitucionalDescriptor d = resolver.resolver(TipoUsuario.PROCURADORIA_MUNICIPAL, "CE", "Juazeiro do Norte");
        assertThat(d.escopoRef()).isEqualTo("PROC-MUN-CE-JUAZEIRO-DO-NORTE");
        assertThat(d.nomeOrgao()).isEqualTo("PROCURADORIA-GERAL DO MUNICÍPIO DE JUAZEIRO DO NORTE");
        // sem município conhecido, cai para a UF sem inventar cidade
        assertThat(resolver.resolver(TipoUsuario.PROCURADORIA_MUNICIPAL, "CE", null).escopoRef()).isEqualTo("PROC-MUN-CE");
    }

    @Test
    void advogadoEhProfissionalIndividualComOab() {
        IdentidadeInstitucionalDescriptor d = resolver.resolver(TipoUsuario.ADVOGADO, "CE");
        assertThat(d.classe()).isEqualTo(ClasseIdentidade.PROFISSIONAL_INDIVIDUAL);
        assertThat(d.escopoRef()).isNull();
        assertThat(d.registroLabel()).isEqualTo("OAB");
    }

    @Test
    void peritoNaoTemBrasaoInstitucionalEUsaRegistroDoConselhoCerto() {
        // o cuidado central: perito e' profissional-individual, com o conselho certo por especialidade
        assertThat(resolver.resolver(TipoUsuario.PERITO_MEDICO, "CE").classe()).isEqualTo(ClasseIdentidade.PROFISSIONAL_INDIVIDUAL);
        assertThat(resolver.resolver(TipoUsuario.PERITO_MEDICO, "CE").registroLabel()).isEqualTo("CRM");
        assertThat(resolver.resolver(TipoUsuario.PERITO_INSS, "CE").registroLabel()).isEqualTo("CRM");
        assertThat(resolver.resolver(TipoUsuario.PERITO_ENGENHARIA, "CE").registroLabel()).isEqualTo("CREA");
        assertThat(resolver.resolver(TipoUsuario.PERITO_CONTABIL, "CE").registroLabel()).isEqualTo("CRC");
        assertThat(resolver.resolver(TipoUsuario.PERITO_CRIMINAL, "CE").registroLabel()).isEqualTo("Registro profissional");
        assertThat(resolver.resolver(TipoUsuario.PERITO_MEDICO, "CE").escopoRef()).isNull();
    }

    @Test
    void ufAusenteNaoQuebraResolveComMarcadorBr() {
        IdentidadeInstitucionalDescriptor d = resolver.resolver(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO, null);
        assertThat(d.escopoRef()).isEqualTo("MP-EST-BR");
    }
}
