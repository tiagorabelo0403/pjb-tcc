package com.tcc.pjb.backend.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Prova, com Postgres real e chave mestra real (não o fallback de teste), que {@code Usuario.cpf}/
 * {@code email} saem cifrados de verdade em repouso (V344) — não só "parecem diferentes", a coluna
 * bruta no banco não contém o CPF em texto puro — e que a leitura pelo índice cego
 * ({@code findByCpf}/{@code findByEmail}) e o cruzamento com {@code Processo} continuam funcionando,
 * de ponta a ponta, sem nenhum dos call sites originais mudar.
 */
@TestPropertySource(properties = {
        "pjb.security.master-key=" + UsuarioSensitiveDataEncryptionIT.CHAVE_TESTE_BASE64,
        "pjb.security.crypto.allow-plaintext-fallback=false"
})
class UsuarioSensitiveDataEncryptionIT extends PjbIntegrationTestBase {

    static final String CHAVE_TESTE_BASE64 = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private Usuario salvarUsuario(String sufixo, String cpf, String email) {
        Usuario u = new Usuario();
        u.setNome("Fulano de Tal " + sufixo);
        u.setEmail(email);
        u.setCpf(cpf);
        u.setTipoUsuario(TipoUsuario.ADVOGADO);
        u.setPerfil(TipoUsuario.ADVOGADO.name());
        u.setSenha("hash-fake");
        u.setAtivo(true);
        return usuarioRepository.save(u);
    }

    @Test
    void cpfEEmailSaoCifradosDeVerdadeNoBancoENuncaEmTextoPuro() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String cpf = "111222333" + sufixo.substring(0, 2).replaceAll("\\D", "1");
        String email = "fulano." + sufixo + "@example.com";
        Usuario salvo = salvarUsuario(sufixo, cpf, email);

        Map<String, Object> raw = jdbc.queryForMap(
                "SELECT cpf, email, cpf_hash, email_hash FROM tb_usuario WHERE id = ?", salvo.getId());

        assertThat((String) raw.get("cpf")).isNotEqualTo(cpf).doesNotContain(cpf);
        assertThat((String) raw.get("email")).isNotEqualTo(email).doesNotContain(email);
        assertThat((String) raw.get("cpf_hash")).hasSize(64);
        assertThat((String) raw.get("email_hash")).hasSize(64);
        // ciphertext é base64 de IV(12) + tag(16) + payload — bem maior que o texto puro
        assertThat(Base64.getDecoder().decode((String) raw.get("cpf")).length).isGreaterThanOrEqualTo(28);
    }

    @Test
    void leituraPorEntidadeDevolveOTextoPuroOriginalTransparente() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String cpf = "22233344" + sufixo.replaceAll("\\D", "1").substring(0, 3);
        String email = "ciclano." + sufixo + "@example.com";
        Usuario salvo = salvarUsuario(sufixo, cpf, email);

        Usuario recarregado = usuarioRepository.findById(salvo.getId()).orElseThrow();

        assertThat(recarregado.getCpf()).isEqualTo(cpf);
        assertThat(recarregado.getEmail()).isEqualTo(email);
    }

    @Test
    void findByCpfEFindByEmailFuncionamPeloIndiceCego() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String cpf = "33344455" + sufixo.replaceAll("\\D", "2").substring(0, 3);
        String email = "beltrano." + sufixo + "@example.com";
        Usuario salvo = salvarUsuario(sufixo, cpf, email);

        assertThat(usuarioRepository.findByCpf(cpf)).isPresent().get()
                .extracting(Usuario::getId).isEqualTo(salvo.getId());
        assertThat(usuarioRepository.findByEmail(email)).isPresent().get()
                .extracting(Usuario::getId).isEqualTo(salvo.getId());
        assertThat(usuarioRepository.findByCpf("00000000000")).isEmpty();
        assertThat(usuarioRepository.findByEmail("naoexiste@example.com")).isEmpty();
    }

    @Test
    void findByCpfIgnoraFormatacaoDiferenteDaUsadaNoCadastro() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String cpfDigitos = "44455566" + sufixo.replaceAll("\\D", "3").substring(0, 3);
        Usuario salvo = salvarUsuario(sufixo, cpfDigitos, "formatado." + sufixo + "@example.com");
        String cpfFormatado = cpfDigitos.substring(0, 3) + "." + cpfDigitos.substring(3, 6) + "."
                + cpfDigitos.substring(6, 9) + "-" + cpfDigitos.substring(9);

        assertThat(usuarioRepository.findByCpf(cpfFormatado)).isPresent().get()
                .extracting(Usuario::getId).isEqualTo(salvo.getId());
    }

    @Test
    void findAllByPartesCpfEncontraProcessoPeloUsuarioVinculadoComCpfCifrado() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String cpf = "55566677" + sufixo.replaceAll("\\D", "4").substring(0, 3);
        Usuario advogado = salvarUsuario(sufixo, cpf, "cruzamento." + sufixo + "@example.com");

        Processo processo = Processo.builder()
                .numeroProcesso("CROSS-" + sufixo)
                .numeroUnificado("CROSS-" + sufixo)
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.CIVIL)
                .tribunal("TJCE")
                .comarca("Fortaleza")
                .uf("CE")
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .usuario(advogado)
                .build();
        processoRepository.save(processo);

        List<Processo> encontrados = processoRepository.findAllByPartesCpf(cpf);

        assertThat(encontrados).extracting(Processo::getNumeroProcesso).contains("CROSS-" + sufixo);
    }
}
