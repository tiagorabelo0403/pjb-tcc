package com.tcc.pjb.backend.model.repository.professional;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessBasis;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantApprovalStatus;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import({
        com.tcc.pjb.backend.core.infra.spring.SpringContext.class,
        com.tcc.pjb.backend.core.security.crypto.CryptoVaultService.class,
        com.tcc.pjb.backend.core.security.crypto.UsuarioBlindIndexService.class
})
@ActiveProfiles("test")
class ProfessionalInstitutionalAccessGrantVigentesNoProcessoTest {

    private static final String VARA = "TJCE-VARA-01";
    private static final EnumSet<ProfessionalAccessGrantType> TITULOS = EnumSet.of(
            ProfessionalAccessGrantType.LOTACAO_UNIDADE,
            ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL,
            ProfessionalAccessGrantType.SUBSTITUICAO,
            ProfessionalAccessGrantType.RELATORIA_PROCESSO);

    @Autowired
    private ProfessionalInstitutionalAccessGrantRepository grantRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void trazSoAcessosDeMagistraturaAprovadosVigentesDaUnidadeOuDoProcesso() {
        LocalDateTime agora = LocalDateTime.now();
        Processo processo = salvarProcesso("0007001-11.2026.8.06.0001");
        Processo outroProcesso = salvarProcesso("0007002-11.2026.8.06.0001");
        Usuario juiz = salvarUsuario(TipoUsuario.JUIZ, "titular");

        ProfessionalInstitutionalAccessGrant lotacao = salvar(juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, VARA, null, agora);
        ProfessionalInstitutionalAccessGrant relatoria = salvar(juiz, ProfessionalAccessGrantType.RELATORIA_PROCESSO, null, processo, agora);
        salvar(juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, "TJCE-VARA-02", null, agora);
        salvar(juiz, ProfessionalAccessGrantType.RELATORIA_PROCESSO, null, outroProcesso, agora);
        salvar(juiz, ProfessionalAccessGrantType.SUBSTITUICAO, VARA, outroProcesso, agora);
        salvar(juiz, ProfessionalAccessGrantType.PLANTAO, VARA, null, agora);
        ProfessionalInstitutionalAccessGrant pendente = salvar(juiz, ProfessionalAccessGrantType.SUBSTITUICAO, VARA, null, agora);
        pendente.setApprovalStatus(ProfessionalGrantApprovalStatus.PENDING);
        ProfessionalInstitutionalAccessGrant revogada = salvar(juiz, ProfessionalAccessGrantType.SUBSTITUICAO, VARA, null, agora);
        revogada.setAtivo(false);
        ProfessionalInstitutionalAccessGrant vencida = salvar(juiz, ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL, VARA, null, agora);
        vencida.setFimVigencia(agora.minusDays(1));
        ProfessionalInstitutionalAccessGrant futura = salvar(juiz, ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL, VARA, null, agora);
        futura.setInicioVigencia(agora.plusDays(1));
        ProfessionalInstitutionalAccessGrant outraClasse = salvar(juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, VARA, null, agora);
        outraClasse.setActorClass(ProfessionalActorClass.DEFENSORIA);
        grantRepository.saveAllAndFlush(List.of(pendente, revogada, vencida, futura, outraClasse));

        List<ProfessionalInstitutionalAccessGrant> vigentes = grantRepository.findVigentesNoProcessoOuNaUnidade(
                ProfessionalActorClass.MAGISTRATURA, TITULOS, ProfessionalGrantApprovalStatus.APPROVED, agora, processo.getId(), VARA);

        assertThat(vigentes).extracting(ProfessionalInstitutionalAccessGrant::getId)
                .containsExactly(lotacao.getId(), relatoria.getId());
        assertThat(vigentes).allSatisfy(grant -> assertThat(grant.getUsuario().getId()).isEqualTo(juiz.getId()));
    }

    @Test
    void semUnidadeSoTrazAcessosVinculadosAoProcesso() {
        LocalDateTime agora = LocalDateTime.now();
        Processo processo = salvarProcesso("0007003-11.2026.8.06.0001");
        Usuario juiz = salvarUsuario(TipoUsuario.DESEMBARGADOR, "relator");
        salvar(juiz, ProfessionalAccessGrantType.LOTACAO_UNIDADE, VARA, null, agora);
        ProfessionalInstitutionalAccessGrant relatoria = salvar(juiz, ProfessionalAccessGrantType.RELATORIA_PROCESSO, null, processo, agora);

        List<ProfessionalInstitutionalAccessGrant> vigentes = grantRepository.findVigentesNoProcessoOuNaUnidade(
                ProfessionalActorClass.MAGISTRATURA, TITULOS, ProfessionalGrantApprovalStatus.APPROVED, agora, processo.getId(), null);

        assertThat(vigentes).extracting(ProfessionalInstitutionalAccessGrant::getId).containsExactly(relatoria.getId());
    }

    private ProfessionalInstitutionalAccessGrant salvar(Usuario usuario, ProfessionalAccessGrantType tipo, String unidade,
                                                        Processo processo, LocalDateTime agora) {
        return grantRepository.saveAndFlush(ProfessionalInstitutionalAccessGrant.builder()
                .usuario(usuario)
                .processo(processo)
                .actorClass(ProfessionalActorClass.MAGISTRATURA)
                .grantType(tipo)
                .accessBasis(ProfessionalAccessBasis.MAGISTRATURA_COMPETENCIA_TERRITORIAL)
                .unidadeJudiciariaCodigo(unidade)
                .approvalStatus(ProfessionalGrantApprovalStatus.APPROVED)
                .approvedByUserId(1L)
                .inicioVigencia(agora.minusDays(10))
                .build());
    }

    private Processo salvarProcesso(String numero) {
        Processo processo = new Processo();
        processo.setNumeroProcesso(numero);
        processo.setNumeroUnificado(numero);
        processo.setTipoJustica(TipoJustica.ESTADUAL);
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setStatusProcesso(StatusProcesso.DISTRIBUIDO);
        processo.setNivelSigilo(NivelSigilo.PUBLICO);
        processo.setClasseProcessual("Procedimento Comum Civel");
        processo.setAssunto("Acordo");
        processo.setParteAutoraNome("Autor Acordo");
        processo.setParteReuNome("Reu Acordo");
        processo.setUf("CE");
        processo.setComarca("Fortaleza");
        processo.setTribunal("TJCE");
        processo.setUnidadeJudiciariaCodigo(VARA);
        processo.setDataCriacao(LocalDateTime.now());
        processo.setDataDistribuicao(LocalDateTime.now());
        return processoRepository.saveAndFlush(processo);
    }

    private Usuario salvarUsuario(TipoUsuario tipo, String label) {
        Usuario usuario = new Usuario();
        usuario.setNome("Magistrado " + label);
        usuario.setEmail(label.toLowerCase(Locale.ROOT) + "." + System.nanoTime() + "@test.local");
        usuario.setCpf(cpfValido());
        usuario.setTipoUsuario(tipo);
        usuario.setPerfil(tipo.name());
        usuario.setAtivo(true);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        return usuarioRepository.saveAndFlush(usuario);
    }

    private String cpfValido() {
        long unique = Math.abs(System.nanoTime() % 900000000L);
        String base = String.format("%09d", unique);
        int[] digits = base.chars().map(c -> c - '0').toArray();
        int d1 = digitoCpf(digits, 10);
        int[] comD1 = Arrays.copyOf(digits, 10);
        comD1[9] = d1;
        int d2 = digitoCpf(comD1, 11);
        return base + d1 + d2;
    }

    private int digitoCpf(int[] digits, int peso) {
        int soma = 0;
        for (int i = 0; i < peso - 1; i++) {
            soma += digits[i] * (peso - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
