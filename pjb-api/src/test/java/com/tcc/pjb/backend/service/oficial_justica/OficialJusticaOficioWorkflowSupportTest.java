package com.tcc.pjb.backend.service.oficial_justica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain.DestinatarioProcessual;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain.ResolucaoDestinatarioProcessualResult;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioProcessualKind;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TrilhoComunicacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OficialJusticaOficioWorkflowSupportTest {

    @Test
    void criarJuntadaDiretaNoProcessoMaterializaCanalDiretoComFundamentoPadrao() {
        WorkItemRepository repository = mock(WorkItemRepository.class);
        when(repository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Processo processo = Processo.builder()
                .id(44L)
                .faseAtual(FaseProcessual.CONHECIMENTO)
                .uf("CE")
                .comarca("Morada Nova")
                .build();
        Usuario usuario = new Usuario();
        usuario.setUf("CE");
        usuario.setComarca("Morada Nova");
        WorkItem principal = WorkItem.builder()
                .id(99L)
                .type(WorkItemType.EXPEDICAO)
                .status(WorkItemStatus.CONCLUIDO)
                .build();
        OficialJusticaOficioRequest request = new OficialJusticaOficioRequest(
                "Ofício de constatação",
                "Ministério Público",
                "Conteúdo governado",
                null,
                null,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE
        );

        WorkItem juntada = OficialJusticaOficioWorkflowSupport.criarJuntadaDiretaNoProcesso(
                repository,
                processo,
                usuario,
                principal,
                request,
                Map.of("templateCode", "OFICIO_PADRAO", "contentHash", "abc123", "renderedBody", "texto final"),
                false
        );

        assertThat(juntada.getType()).isEqualTo(WorkItemType.JUNTADA);
        assertThat(juntada.getQueueCode()).isEqualTo("PROCESSO_DIRETO_OFICIAL");
        assertThat(juntada.getInboxKey()).isEqualTo("PROCESSO_DIRETO:44");
        assertThat(juntada.getBaseLegal()).isEqualTo("Fundamento institucional do oficial de justiça");
        assertThat(juntada.getDescricao()).contains("OFICIO_ORIGINAL_GOVERNADO");
        assertThat(juntada.getDescricao()).contains("abc123");
        assertThat(juntada.getDueAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void buildDestinatarioMapPropagaResolucaoInstitucionalSemRuido() {
        DestinatarioProcessual destinatario = new DestinatarioProcessual(
                DestinatarioProcessualKind.UNIDADE_INSTITUCIONAL,
                TrilhoComunicacaoProcessual.INSTITUCIONAL_CAIXA,
                null,
                "12345678000199",
                "Promotoria de Justiça",
                "mp@example.test",
                "85999990000",
                null,
                null,
                "CE",
                "Morada Nova",
                null,
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                "MP-CE-001",
                true,
                true,
                false,
                true,
                List.of("caixa institucional ativa"),
                "hash-destinatario"
        );
        ResolucaoDestinatarioProcessualResult resolucao = new ResolucaoDestinatarioProcessualResult(
                destinatario,
                TrilhoComunicacaoProcessual.INSTITUCIONAL_CAIXA,
                false,
                true,
                false,
                true,
                List.of("trilho institucional"),
                "hash-resolucao"
        );
        OficialJusticaOficioRequest request = new OficialJusticaOficioRequest(
                "Ofício para ciência",
                "Ministério Público",
                "Conteúdo institucional",
                "Fundamento expresso",
                null,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE
        );

        Map<String, Object> out = OficialJusticaOficioWorkflowSupport.buildDestinatarioMap(resolucao, request);

        assertThat(out)
                .containsEntry("nomeExibicao", "Promotoria de Justiça")
                .containsEntry("documentoPrincipal", "12345678000199")
                .containsEntry("hashResolucao", "hash-resolucao")
                .containsEntry("trilho", "INSTITUCIONAL_CAIXA")
                .containsEntry("destinatarioInstitucionalKind", "MINISTERIO_PUBLICO")
                .containsEntry("papelProcessualInstitucional", "FISCAL_ORDEM_JURIDICA")
                .containsEntry("unidadeInstitucionalCodigo", "MP-CE-001")
                .containsEntry("destinatarioLivre", "Ministério Público");
        assertThat(out).doesNotContainKey("oabNumero");
    }
}
