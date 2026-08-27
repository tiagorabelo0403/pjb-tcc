package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.editor.EditorBootstrapResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.editor.IdentidadeVisualEfetivaDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.processual.peticionamento.identidade.PeticaoIdentidadeVisualService;
import com.tcc.pjb.backend.service.processual.peticionamento.rascunho.PeticaoDraftVersionamentoService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monta o contrato único de abertura do editor para o ator atual. Une, tipado, o que o frontend
 * precisa para renderizar o editor de peça: catálogo de formatação, identidade visual já resolvida
 * (institucional + individual), e os endpoints/limites de rascunho e mídia.
 */
@Service
public class PeticaoEditorBootstrapService {

    private static final long MAX_LOGO_BYTES = 2_000_000L;

    private final CurrentUserService currentUserService;
    private final RichTextFormatCatalog formatCatalog;
    private final PeticaoIdentidadeVisualService identidadeVisualService;

    public PeticaoEditorBootstrapService(CurrentUserService currentUserService,
                                         RichTextFormatCatalog formatCatalog,
                                         PeticaoIdentidadeVisualService identidadeVisualService) {
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.formatCatalog = Objects.requireNonNull(formatCatalog, "formatCatalog");
        this.identidadeVisualService = Objects.requireNonNull(identidadeVisualService, "identidadeVisualService");
    }

    @Transactional(readOnly = true)
    public EditorBootstrapResponse bootstrap() {
        Usuario usuario = currentUserService.getRequired();
        Map<String, Object> presetIdentidade = identidadeVisualService.resolvePresetParaAtor(usuario).orElse(Map.of());
        TipoUsuario tipo = usuario.getTipoUsuario();
        return new EditorBootstrapResponse(
                tipo == null ? null : tipo.papelArquitetural(),
                formatCatalog.toDto(),
                IdentidadeVisualEfetivaDto.fromPreset(presetIdentidade),
                new EditorBootstrapResponse.RascunhoCapabilitiesDto(
                        "/api/v1/peticionamento/inicial/rascunhos/{draftId}/autosave",
                        "/api/v1/peticionamento/inicial/rascunhos/{draftId}/versoes",
                        "/api/v1/peticionamento/inicial/rascunhos/{draftId}/versoes/{versaoSeq}",
                        "/api/v1/peticionamento/inicial/rascunhos/{draftId}/versoes/{versaoSeq}/restaurar",
                        PeticaoDraftVersionamentoService.MAX_VERSOES_RETIDAS,
                        true),
                new EditorBootstrapResponse.MidiaCapabilitiesDto(
                        MAX_LOGO_BYTES,
                        List.of("image/jpeg", "image/png"),
                        "/api/v1/peticionamento/identidade-visual/logo",
                        "/api/v1/peticionamento/identidade-visual/institucional/{escopoRef}/logo",
                        "/api/v1/peticionamento/editor/formato/validar",
                        "/api/v1/peticionamento/editor/formato/catalogo",
                        "/api/v1/peticionamento/editor/exportar/docx"));
    }
}
