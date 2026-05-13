package com.tcc.pjb.backend.service.processual.recursal.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.ai.juridica.v3.core.LegalDraftingService;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.processual.recursal.workspace.PerfilRecursalDescriptor;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalDraftPreviewAssembler;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.tcc.pjb.backend.core.processo.recursal.domain.PreclusaoTipo;

@ExtendWith(MockitoExtension.class)
class RecursalDraftPreviewAssemblerTest {

    @Mock
    private LegalDraftingService legalDraftingService;

    private RecursalDraftPreviewAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new RecursalDraftPreviewAssembler(legalDraftingService);
    }

    @Test
    void deveRetornarRazoesQuandoMotorDeMinutaNaoResponderConteudoUtil() {
        when(legalDraftingService.draftRecurso(anyMap())).thenReturn("   ");

        String minuta = assembler.buildDraftPreview(
                processo(),
                descriptor(),
                "APELACAO",
                "Razões centrais do recurso.",
                "Fundamentação jurídica base.",
                admissibility(),
                new Object(),
                "Observação operacional"
        );

        assertThat(minuta).isEqualTo("Razões centrais do recurso.");
    }

    @Test
    void deveSubstituirPlaceholderDasRazoesQuandoMinutaForGerada() {
        when(legalDraftingService.draftRecurso(anyMap())).thenReturn("Minuta padronizada: [ERRO DE FATO/DIREITO, PRECEDENTES, TESE]");

        String minuta = assembler.buildDraftPreview(
                processo(),
                descriptor(),
                "AGRAVO DE INSTRUMENTO",
                "Erro de fato demonstrado com precedente vinculante.",
                "Fundamentação jurídica base.",
                admissibility(),
                new Object(),
                "Observação operacional"
        );

        assertThat(minuta)
                .contains("Erro de fato demonstrado com precedente vinculante.")
                .doesNotContain("[ERRO DE FATO/DIREITO, PRECEDENTES, TESE]");
    }

    private Processo processo() {
        Processo processo = Processo.builder().id(77L).numeroProcesso("0001234-56.2026.8.06.0001").build();
        processo.setNumeroUnificado("0001234-56.2026.8.06.0001");
        processo.setParteAutoraNome("Maria da Silva");
        processo.setParteReuNome("João Souza");
        processo.setResumoIA("Resumo IA da decisão");
        processo.setObjetoProcessual("Objeto processual relevante");
        return processo;
    }

    private PerfilRecursalDescriptor descriptor() {
        return new PerfilRecursalDescriptor(
                "ADVOCACIA",
                "ADVOGADO",
                new String[]{"ROLE_ADVOGADO"},
                "ADVOCACIA_PETICAO_RECURSAL",
                "ADVOCACIA_RECURSO",
                1,
                false,
                false,
                false
        );
    }

    private RecursalAdmissibilityResponse admissibility() {
        try {
            RecordComponent[] components = RecursalAdmissibilityResponse.class.getRecordComponents();
            Object[] arguments = new Object[components.length];
            for (int index = 0; index < components.length; index++) {
                RecordComponent component = components[index];
                arguments[index] = valueFor(component.getName(), component.getType());
            }
            Constructor<RecursalAdmissibilityResponse> constructor = RecursalAdmissibilityResponse.class.getDeclaredConstructor(
                    java.util.Arrays.stream(components)
                            .map(RecordComponent::getType)
                            .toArray(Class[]::new)
            );
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Object valueFor(String componentName, Class<?> componentType) {
        if (componentType == boolean.class) {
            return switch (componentName) {
                case "admissivelEmTese", "juizoAdmissibilidadeOrigem", "tempestivo", "preparoExigido", "preparoSatisfeito" -> true;
                default -> false;
            };
        }
        if (componentType == LocalDate.class) {
            return switch (componentName) {
                case "dataProtocolo" -> LocalDate.of(2026, 4, 15);
                case "dataLimite" -> LocalDate.of(2026, 4, 30);
                default -> null;
            };
        }
        if (componentType == PreclusaoTipo.class) {
            return PreclusaoTipo.NENHUMA;
        }
        if (List.class.isAssignableFrom(componentType)) {
            return switch (componentName) {
                case "connectorWarnings" -> List.of("conector-ok");
                case "alertas" -> List.of("alerta-1", "alerta-2");
                case "fundamentos" -> List.of("fundamento-1");
                case "labels" -> List.of("label-1");
                default -> List.of();
            };
        }
        if (Map.class.isAssignableFrom(componentType)) {
            return new HashMap<>(Map.of("fingerprint", "abc"));
        }
        if (componentType == String.class) {
            return switch (componentName) {
                case "perfilRecursal" -> "ADVOCACIA";
                case "tribunalDestino" -> "TJCE";
                case "instanciaDestino" -> "SECOND_INSTANCE";
                case "autoridadeJulgamento" -> "CAMARA_CIVEL";
                case "autoridadeOrigem" -> "JUÍZO A QUO";
                case "autoridadeDestino" -> "TURMA_RECURSAL";
                case "tipoPrazo" -> "DIAS_UTEIS";
                case "counterReasonsMode" -> "ABRIR_CONTRARRAZOES";
                case "effectMode" -> "DEVOLUTIVO";
                case "reviewDesk" -> "REVIEW_01";
                case "connectorBaseUrl" -> "https://tribunal.exemplo/api";
                default -> componentName.toUpperCase();
            };
        }
        if (componentType == int.class) {
            return 0;
        }
        if (componentType == long.class) {
            return 0L;
        }
        if (componentType == double.class) {
            return 0D;
        }
        if (componentType == float.class) {
            return 0F;
        }
        if (componentType == short.class) {
            return (short) 0;
        }
        if (componentType == byte.class) {
            return (byte) 0;
        }
        if (componentType == char.class) {
            return '\0';
        }
        if (componentType.isEnum()) {
            return componentType.getEnumConstants()[0];
        }
        return null;
    }
}
