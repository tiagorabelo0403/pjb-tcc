package com.tcc.pjb.backend.service.ui.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiCssTokenDto;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiPresentationBundleDto;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiPresentationDto;
import com.tcc.pjb.backend.model.entity.ui.UsuarioAccessibilityPreference;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoAttachmentProperties;
import com.tcc.pjb.backend.platform.hash.CanonicalJsonHasher;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.ui.preferences.UiUserPreferenceService;
import com.tcc.pjb.backend.service.ui.presentation.compiler.UiCssTokenKey;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class UiPresentationServiceChatAttachmentTokensTest {

    private static final long USUARIO_ID = 7L;

    @Test
    void publicaOsDefaultsDeAnexoDoAtendimentoQuandoNadaEConfigurado() {
        UiPresentationBundleDto bundle = servico(new AtendimentoAttachmentProperties()).bundleForUserId(USUARIO_ID);

        assertThat(tokensDeAnexo(bundle.light())).isEqualTo(esperado("0", "10485760", "3"));
        assertThat(tokensDeAnexo(bundle.dark())).isEqualTo(esperado("0", "10485760", "3"));
    }

    @Test
    void publicaALimitacaoConfiguradaParaOAtendimento() {
        AtendimentoAttachmentProperties props = new AtendimentoAttachmentProperties();
        props.setEnabled(true);
        props.setMaxBytes(2_097_152L);
        props.setMaxPerMessage(5);

        UiPresentationBundleDto bundle = servico(props).bundleForUserId(USUARIO_ID);

        assertThat(tokensDeAnexo(bundle.light())).isEqualTo(esperado("1", "2097152", "5"));
        assertThat(tokensDeAnexo(bundle.dark())).isEqualTo(esperado("1", "2097152", "5"));
    }

    @Test
    void limitesNegativosSaoPublicadosComoZero() {
        AtendimentoAttachmentProperties props = new AtendimentoAttachmentProperties();
        props.setEnabled(true);
        props.setMaxBytes(-1L);
        props.setMaxPerMessage(-5);

        UiPresentationBundleDto bundle = servico(props).bundleForUserId(USUARIO_ID);

        assertThat(tokensDeAnexo(bundle.light())).isEqualTo(esperado("1", "0", "0"));
        assertThat(tokensDeAnexo(bundle.dark())).isEqualTo(esperado("1", "0", "0"));
    }

    @Test
    void hashDaApresentacaoAcompanhaALimitacaoDeAnexoPublicada() {
        UiPresentationBundleDto padrao = servico(new AtendimentoAttachmentProperties()).bundleForUserId(USUARIO_ID);
        UiPresentationBundleDto padraoDeNovo = servico(new AtendimentoAttachmentProperties()).bundleForUserId(USUARIO_ID);

        assertThat(padraoDeNovo.light().presentationHash()).isEqualTo(padrao.light().presentationHash());
        assertThat(padraoDeNovo.dark().presentationHash()).isEqualTo(padrao.dark().presentationHash());

        AtendimentoAttachmentProperties habilitada = new AtendimentoAttachmentProperties();
        habilitada.setEnabled(true);
        AtendimentoAttachmentProperties outroLimiteDeBytes = new AtendimentoAttachmentProperties();
        outroLimiteDeBytes.setMaxBytes(2_097_152L);
        AtendimentoAttachmentProperties outroLimitePorMensagem = new AtendimentoAttachmentProperties();
        outroLimitePorMensagem.setMaxPerMessage(5);

        for (AtendimentoAttachmentProperties alterada : List.of(habilitada, outroLimiteDeBytes, outroLimitePorMensagem)) {
            UiPresentationBundleDto bundle = servico(alterada).bundleForUserId(USUARIO_ID);
            assertThat(bundle.light().presentationHash()).isNotEqualTo(padrao.light().presentationHash());
            assertThat(bundle.dark().presentationHash()).isNotEqualTo(padrao.dark().presentationHash());
        }
    }

    private static UiPresentationService servico(AtendimentoAttachmentProperties props) {
        UiUserPreferenceService prefs = mock(UiUserPreferenceService.class);
        when(prefs.loadOrCreate(anyLong())).thenAnswer(inv -> new UsuarioAccessibilityPreference(inv.getArgument(0)));
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        when(usuarios.findById(anyLong())).thenReturn(Optional.empty());
        ObjectMapper mapper = new ObjectMapper();
        return new UiPresentationService(
                mock(CurrentUserService.class),
                prefs,
                new ReadingModeProperties(),
                new CanonicalJsonHasher(mapper, Clock.systemUTC()),
                mapper,
                mock(AuditLedgerService.class),
                mock(OutboxPublisher.class),
                usuarios,
                props);
    }

    private static Map<String, String> tokensDeAnexo(UiPresentationDto dto) {
        List<String> chaves = List.of(
                UiCssTokenKey.CHAT_ATTACH_ENABLED.css(),
                UiCssTokenKey.CHAT_ATTACH_MAX_BYTES.css(),
                UiCssTokenKey.CHAT_ATTACH_MAX_PER_MESSAGE.css());
        return dto.tokens().stream()
                .filter(token -> chaves.contains(token.key()))
                .collect(Collectors.toMap(UiCssTokenDto::key, UiCssTokenDto::value));
    }

    private static Map<String, String> esperado(String enabled, String maxBytes, String maxPerMessage) {
        return Map.of(
                UiCssTokenKey.CHAT_ATTACH_ENABLED.css(), enabled,
                UiCssTokenKey.CHAT_ATTACH_MAX_BYTES.css(), maxBytes,
                UiCssTokenKey.CHAT_ATTACH_MAX_PER_MESSAGE.css(), maxPerMessage);
    }
}
