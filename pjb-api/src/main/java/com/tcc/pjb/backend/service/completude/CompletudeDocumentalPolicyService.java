package com.tcc.pjb.backend.service.completude;

import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CompletudeDocumentalPolicyService {

    public record DiagnosticoCompletudeDocumental(
            boolean bloqueante,
            List<TipoDocumento> faltantes,
            RitoProcessual rito) {

        public DiagnosticoCompletudeDocumental {
            faltantes = faltantes == null ? List.of() : List.copyOf(faltantes);
        }
    }

    /**
     * Avalia se os anexos tipados cobrem todos os documentos obrigatórios do catálogo
     * para o rito canônico. Política de null: {@code Attachment.tipoDocumento == null}
     * não satisfaz nenhum requisito — ausência de tipo é rejeição explícita, nunca
     * passagem silenciosa.
     *
     * <p>Contrato de entrada: {@code rito == null} não é premissa válida na posição
     * canônica (pós-{@code applyRoutingSnapshot()}). Quando {@code rito == null},
     * o método recua defensivamente para {@code COMUM_ORDINARIO} e bloqueia o
     * ajuizamento — o recuo nunca substitui a precondição, apenas evita NPE em
     * chamadas fora de posição. Comportamento verificado em
     * {@code ritoNullDefaultsParaComumOrdinario}.
     *
     * <p>Posição em {@code AjuizarProcessoCommand.execute()}: após
     * {@code validateConnectorExecution()} porque {@code processo.getRito()} só é
     * canônico após {@code applyRoutingSnapshot()}. Invocar o catálogo com rito
     * não-resolvido produziria validação contra spec errada.
     */
    public DiagnosticoCompletudeDocumental diagnosticar(RitoProcessual rito, List<Attachment> anexos) {
        return diagnosticar(rito, anexos, null);
    }

    /**
     * Sobrecarga sensível ao instrumento de representação resolvido para o ator. Quando o
     * instrumento é regime de jus postulandi ({@code InstrumentoRepresentacaoProcessual#isJusPostulandi()}),
     * {@code PROCURACAO} deixa de compor os documentos obrigatórios: a parte que postula em nome
     * próprio não constitui advogado e não tem mandato a juntar. A dispensa é restrita a esse
     * documento — todos os demais requisitos do catálogo do rito continuam exigíveis.
     */
    public DiagnosticoCompletudeDocumental diagnosticar(RitoProcessual rito,
                                                        List<Attachment> anexos,
                                                        InstrumentoRepresentacaoProcessual instrumentoRepresentacao) {
        Set<TipoDocumento> presentes = (anexos == null ? List.<Attachment>of() : anexos)
                .stream()
                .map(Attachment::getTipoDocumento)
                .filter(t -> t != null)
                .collect(Collectors.toSet());
        return diagnosticar(rito, presentes, instrumentoRepresentacao);
    }

    public DiagnosticoCompletudeDocumental diagnosticar(RitoProcessual rito,
                                                        Set<TipoDocumento> tiposPresentes,
                                                        InstrumentoRepresentacaoProcessual instrumentoRepresentacao) {
        RitoProcessual ritoResolvido = rito == null ? RitoProcessual.COMUM_ORDINARIO : rito;
        boolean dispensaProcuracao = instrumentoRepresentacao != null && instrumentoRepresentacao.isJusPostulandi();

        List<TipoDocumento> obrigatorios = ProceduralCatalogSupport.snapshot(ritoResolvido)
                .documents()
                .stream()
                .filter(ProceduralCatalogSupport.DocumentSpec::required)
                .map(ProceduralCatalogSupport.DocumentSpec::code)
                .filter(code -> !(dispensaProcuracao && code == TipoDocumento.PROCURACAO))
                .toList();

        Set<TipoDocumento> presentes = tiposPresentes == null ? Set.of() : tiposPresentes;

        List<TipoDocumento> faltantes = obrigatorios.stream()
                .filter(req -> !presentes.contains(req))
                .toList();

        return new DiagnosticoCompletudeDocumental(!faltantes.isEmpty(), faltantes, ritoResolvido);
    }

    public ErroDeValidacaoException toException(DiagnosticoCompletudeDocumental diagnostico) {
        return new ErroDeValidacaoException(
                TipoErroValidacao.REGRA_NEGOCIO,
                "Ajuizamento bloqueado por incompletude documental: documentos obrigatórios ausentes para o rito "
                        + diagnostico.rito().name() + ".")
                .addMetadado("rito", diagnostico.rito().name())
                .addMetadado("faltantes",
                        diagnostico.faltantes().stream().map(TipoDocumento::name).toList());
    }
}
