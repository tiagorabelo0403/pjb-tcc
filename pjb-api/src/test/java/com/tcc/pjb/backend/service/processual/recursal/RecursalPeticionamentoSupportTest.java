package com.tcc.pjb.backend.service.processual.recursal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.EmbargosGroundCode;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesType;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.LocalDateTime;
import com.tcc.pjb.backend.service.processual.recursal.workspace.PerfilRecursalDescriptor;
import org.junit.jupiter.api.Test;

class RecursalPeticionamentoSupportTest {

    private final RecursalPeticionamentoSupport support = new RecursalPeticionamentoSupport();

    @Test
    void deveResolverPerfilDaDefensoriaComIsencaoEFilasProprias() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.DEFENSOR_PUBLICO);

        PerfilRecursalDescriptor descriptor = support.descriptorOf(usuario);

        assertThat(descriptor.profileCode()).isEqualTo("DEFENSORIA");
        assertThat(descriptor.peticaoQueueCode()).isEqualTo("DEFENSORIA_PETICAO_RECURSAL");
        assertThat(descriptor.recursoQueueCode()).isEqualTo("DEFENSORIA_RECURSO");
        assertThat(descriptor.autoIsencaoBase()).isTrue();
    }

    @Test
    void deveClassificarEmbargosDeDeclaracaoPelosFundamentosDetectados() {
        assertThat(support.resolveEmbargosGrounds("Há omissão, contradição e erro material", LegalAppealType.EMBARGOS_DECLARACAO))
                .containsExactlyInAnyOrder(
                        EmbargosGroundCode.OMISSAO,
                        EmbargosGroundCode.CONTRADICAO,
                        EmbargosGroundCode.ERRO_MATERIAL
                );
    }

    @Test
    void deveInferirEspecieRecursalTrabalhistaParaAgravoDeInstrumento() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.TRABALHISTA_ORDINARIO);

        assertThat(support.resolveSpeciesType("agravo de instrumento", null, LegalAppealType.AGRAVO_INSTRUMENTO, processo))
                .isEqualTo(RecursalMeshSpeciesType.AGITRAB);
    }

    @Test
    void deveInferirTribunalInstanciaOrgaoEDataComFallbacksDoProcesso() {
        Processo processo = new Processo();
        processo.setUf("ce");
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setDataAtualizacao(LocalDateTime.of(2026, 4, 15, 10, 30));

        assertThat(support.inferTribunalDetalhado(processo, RecursalTribunal.TJ)).isNotNull();
        assertThat(support.inferInstanceLevel(processo, RecursalTribunal.TJ)).isEqualTo(InstanceLevel.SECOND_INSTANCE);
        assertThat(support.inferOrgaoProlator(processo, LegalAppealType.APELACAO, InstanceLevel.SECOND_INSTANCE))
                .isEqualTo(OrgaoJulgadorTipo.COLEGIADO);
        assertThat(support.inferDataIntimacao(processo)).hasToString("2026-04-15");
    }

    @Test
    void deveMontarNumeroSeguroComFallbackParaIdDoProcesso() {
        Processo processo = new Processo();
        processo.setId(77L);

        assertThat(support.safeNumeroProcesso(processo)).isEqualTo("PROCESSO-77");
    }
}
