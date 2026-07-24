package com.tcc.pjb.backend.service.processual.recursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.prazo.CalendarioUteisService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import com.tcc.pjb.backend.service.recurso.RecursoAdmissibilidadeService;
import com.tcc.pjb.backend.service.recurso.RecursoTempestividadeGuardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = {
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false",
        "spring.jpa.properties.hibernate.format_sql=false",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.tool.schema=ERROR"
})
@ActiveProfiles("test")
class RecursalValidacaoMinimaServiceTest {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    private RecursalValidacaoMinimaService service;

    @BeforeEach
    void setUp() {
        service = new RecursalValidacaoMinimaService(
                new RecursoAdmissibilidadeService(),
                new RecursoTempestividadeGuardService(new CalendarioUteisService()),
                workItemRepository,
                new RepresentacaoProcessualPolicyService()
        );
    }

    @Test
    void cidadaoNoJuizadoEspecialCivelPodeOporEmbargosDeDeclaracaoPorJusPostulandi() {
        Processo processo = salvarProcesso(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, RamoDireito.CIVIL);
        Usuario cidadao = salvarCidadao();

        assertThatCode(() -> service.validar(processo, cidadao, LegalAppealType.EMBARGOS_DECLARACAO, null, false, null))
                .doesNotThrowAnyException();
    }

    @Test
    void cidadaoNoJuizadoEspecialCivelNaoPodeInterporRecursoInominadoSemAdvogado() {
        Processo processo = salvarProcesso(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, RamoDireito.CIVIL);
        Usuario cidadao = salvarCidadao();

        assertThatThrownBy(() -> service.validar(processo, cidadao, LegalAppealType.RECURSO_INOMINADO, null, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("legitimidade");
    }

    @Test
    void cidadaoTrabalhistaPodeInterporRecursoOrdinarioPorJusPostulandi() {
        Processo processo = salvarProcesso(RitoProcessual.TRABALHISTA_ORDINARIO, RamoDireito.TRABALHISTA);
        Usuario cidadao = salvarCidadao();

        assertThatCode(() -> service.validar(processo, cidadao, LegalAppealType.RECURSO_ORDINARIO_TRABALHISTA, null, false, null))
                .doesNotThrowAnyException();
    }

    @Test
    void cidadaoTrabalhistaNaoPodeInterporAgravoRegimentalSemAdvogado() {
        Processo processo = salvarProcesso(RitoProcessual.TRABALHISTA_ORDINARIO, RamoDireito.TRABALHISTA);
        Usuario cidadao = salvarCidadao();

        assertThatThrownBy(() -> service.validar(processo, cidadao, LegalAppealType.AGRAVO_REGIMENTAL, null, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("legitimidade");
    }

    @Test
    void advogadoContinuaLegitimoParaRecursoInominadoNoJuizado() {
        Processo processo = salvarProcesso(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, RamoDireito.CIVIL);
        Usuario advogado = salvarAdvogado();

        assertThatCode(() -> service.validar(processo, advogado, LegalAppealType.RECURSO_INOMINADO, null, false, null))
                .doesNotThrowAnyException();
    }

    private Processo salvarProcesso(RitoProcessual rito, RamoDireito ramo) {
        boolean trabalhista = ramo == RamoDireito.TRABALHISTA;
        String tribunal = trabalhista ? "TRT7" : "TJCE";
        Processo processo = new Processo();
        processo.setNumeroProcesso("0001234-56.2026.8.06.0001");
        processo.setNumeroUnificado(processo.getNumeroProcesso());
        processo.setTribunal(tribunal);
        processo.setTribunalCodigoRoteado(tribunal);
        processo.setVara(trabalhista ? "1 Vara do Trabalho de Fortaleza" : "1 Vara Civel de Fortaleza");
        processo.setComarca("Fortaleza");
        processo.setUf("CE");
        processo.setRamoDireito(ramo);
        processo.setRito(rito);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(StatusProcesso.SENTENCA_PROFERIDA);
        processo.setClasseProcessual("Procedimento");
        processo.setAssunto("Assunto recursal");
        processo.setParteAutoraNome("Autora Recursal");
        processo.setParteReuNome("Reu Recursal");
        processo.setResultadoFinal("Sentenca de improcedencia parcial.");
        return processoRepository.saveAndFlush(processo);
    }

    private Usuario salvarCidadao() {
        Usuario usuario = new Usuario();
        usuario.setNome("Cidadao Recursal");
        usuario.setEmail("cidadao.recursal." + System.nanoTime() + "@test.local");
        usuario.setCpf(String.valueOf(Math.abs(System.nanoTime())).replaceAll("\\D", "").substring(0, 11));
        usuario.setTipoUsuario(TipoUsuario.CIDADAO);
        usuario.setPerfil(TipoUsuario.CIDADAO.name());
        usuario.setAtivo(true);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Usuario salvarAdvogado() {
        Usuario usuario = new Usuario();
        usuario.setNome("Advogada Recursal");
        usuario.setEmail("advogada.recursal." + System.nanoTime() + "@test.local");
        usuario.setCpf(String.valueOf(Math.abs(System.nanoTime())).replaceAll("\\D", "").substring(0, 11));
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setPerfil(TipoUsuario.ADVOGADO.name());
        usuario.setAtivo(true);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setOab("OAB/CE 123456");
        usuario.setOabUf("CE");
        usuario.setOabNormalizada("CE-123456");
        return usuarioRepository.saveAndFlush(usuario);
    }
}
