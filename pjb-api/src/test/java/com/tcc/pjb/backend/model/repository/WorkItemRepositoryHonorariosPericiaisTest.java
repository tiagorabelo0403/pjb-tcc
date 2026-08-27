package com.tcc.pjb.backend.model.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@org.springframework.context.annotation.Import({
        com.tcc.pjb.backend.core.infra.spring.SpringContext.class,
        com.tcc.pjb.backend.core.security.crypto.CryptoVaultService.class,
        com.tcc.pjb.backend.core.security.crypto.UsuarioBlindIndexService.class
})
@ActiveProfiles("test")
class WorkItemRepositoryHonorariosPericiaisTest {

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void encontraSomenteHonorariosDoSolicitanteMesmoComIdsQuePoderiamColidirComoSufixo() {
        Processo processo1 = salvarProcesso("0001111-11.2026.8.06.0001");
        Processo processo2 = salvarProcesso("0002222-22.2026.8.06.0001");

        Usuario perito5 = salvarUsuario(TipoUsuario.PERITO_MEDICO, "perito5");
        Usuario perito25 = salvarUsuario(TipoUsuario.PERITO_MEDICO, "perito25");


        WorkItem honorarioPerito5 = salvarHonorario(processo1, perito5);

        WorkItem honorarioPerito25 = salvarHonorario(processo2, perito25);

        List<WorkItem> resultadoPerito5 = workItemRepository.findHonorariosPericiaisPorSolicitante(perito5.getId());
        List<WorkItem> resultadoPerito25 = workItemRepository.findHonorariosPericiaisPorSolicitante(perito25.getId());

        assertThat(resultadoPerito5).extracting(WorkItem::getId).containsExactly(honorarioPerito5.getId());
        assertThat(resultadoPerito25).extracting(WorkItem::getId).containsExactly(honorarioPerito25.getId());
    }

    private WorkItem salvarHonorario(Processo processo, Usuario solicitante) {
        WorkItem honorario = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("HONORARIO:" + processo.getId() + ":" + solicitante.getId())
                .type(WorkItemType.PETICAO)
                .titulo("Solicitação de Honorários Periciais — " + processo.getNumeroProcesso())
                .descricao("Valor: R$ 100.00 | justificativa de teste")
                .status(WorkItemStatus.PENDENTE)
                .prioridade(3)
                .uf("CE")
                .comarca("Fortaleza")
                .dueAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();
        return workItemRepository.saveAndFlush(honorario);
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
        processo.setAssunto("Pericia medica");
        processo.setParteAutoraNome("Autor Honorarios");
        processo.setParteReuNome("Reu Honorarios");
        processo.setUf("CE");
        processo.setComarca("Fortaleza");
        processo.setTribunal("TJCE");
        processo.setVara("1 Vara Civel de Fortaleza");
        processo.setDataCriacao(LocalDateTime.now());
        processo.setDataDistribuicao(LocalDateTime.now());
        return processoRepository.saveAndFlush(processo);
    }

    private Usuario salvarUsuario(TipoUsuario tipo, String label) {
        Usuario usuario = new Usuario();
        usuario.setNome("Perito " + label);
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
        int d1 = calcularDigitoCpf(digits, 10);
        int[] withD1 = java.util.Arrays.copyOf(digits, 10);
        withD1[9] = d1;
        int d2 = calcularDigitoCpf(withD1, 11);
        return base + d1 + d2;
    }

    private int calcularDigitoCpf(int[] digits, int peso) {
        int soma = 0;
        for (int i = 0; i < peso - 1; i++) {
            soma += digits[i] * (peso - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
