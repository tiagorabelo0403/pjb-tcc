package com.tcc.pjb.backend.core.comunicacao.institucional.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalDelegationAssignmentStateRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.AbrangenciaVinculoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class AutorizacaoCaixaInstitucionalService {

    private final CurrentUserService currentUserService;
    private final CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService;
    private final EstruturaCaixaInstitucionalService estruturaCaixaInstitucionalService;
    private final VinculoUsuarioCaixaInstitucionalResolver vinculoUsuarioCaixaInstitucionalResolver;
    private final InstitutionalDelegationAssignmentStateRepository delegationAssignmentStateRepository;

    public AutorizacaoCaixaInstitucionalService(CurrentUserService currentUserService,
                                                CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService,
                                                EstruturaCaixaInstitucionalService estruturaCaixaInstitucionalService,
                                                VinculoUsuarioCaixaInstitucionalResolver vinculoUsuarioCaixaInstitucionalResolver,
                                                InstitutionalDelegationAssignmentStateRepository delegationAssignmentStateRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.catalogoInstitucionalUnificadoService = Objects.requireNonNull(catalogoInstitucionalUnificadoService);
        this.estruturaCaixaInstitucionalService = Objects.requireNonNull(estruturaCaixaInstitucionalService);
        this.vinculoUsuarioCaixaInstitucionalResolver = Objects.requireNonNull(vinculoUsuarioCaixaInstitucionalResolver);
        this.delegationAssignmentStateRepository = Objects.requireNonNull(delegationAssignmentStateRepository);
    }

    public ResultadoAutorizacaoCaixaInstitucional autorizar(String unidadeCodigo,
                                                            String caixaCodigo,
                                                            CapacidadeCaixaInstitucional capacidade) {
        return autorizar(null, unidadeCodigo, caixaCodigo, capacidade);
    }

    public ResultadoAutorizacaoCaixaInstitucional autorizar(String expedicaoUuid,
                                                            String unidadeCodigo,
                                                            String caixaCodigo,
                                                            CapacidadeCaixaInstitucional capacidade) {
        Usuario usuario = currentUserService.getRequired();
        UnidadeInstitucional unidade = localizarUnidade(unidadeCodigo);
        CaixaInstitucional caixa = localizarCaixa(unidade, caixaCodigo);
        List<VinculoUsuarioCaixaInstitucional> vinculos = new ArrayList<>(vinculoUsuarioCaixaInstitucionalResolver.resolver(usuario, unidade, caixa));
        vinculos.addAll(resolveDelegatedLinks(usuario, expedicaoUuid, unidade, caixa));
        List<String> justificativas = new ArrayList<>();
        boolean autorizado = vinculos.stream().anyMatch(v -> v.permite(capacidade));
        if (autorizado) {
            justificativas.add("autorizado_por_vinculo_institucional");
        } else {
            justificativas.add("capacidade_nao_concedida_para_usuario_atual");
        }
        if (vinculos.isEmpty()) {
            justificativas.add("nenhum_vinculo_elegivel_encontrado");
        } else {
            justificativas.add("vinculos=" + vinculos.size());
        }
        return new ResultadoAutorizacaoCaixaInstitucional(autorizado, unidade.codigo(), caixa.codigo(), capacidade, justificativas, vinculos);
    }

    public void require(String unidadeCodigo, String caixaCodigo, CapacidadeCaixaInstitucional capacidade) {
        require(null, unidadeCodigo, caixaCodigo, capacidade);
    }

    public void require(String expedicaoUuid, String unidadeCodigo, String caixaCodigo, CapacidadeCaixaInstitucional capacidade) {
        ResultadoAutorizacaoCaixaInstitucional resultado = autorizar(expedicaoUuid, unidadeCodigo, caixaCodigo, capacidade);
        if (!resultado.autorizado()) {
            throw new AccessDeniedPjbException("Usuário atual não possui capacidade institucional para operar a caixa " + caixaCodigo);
        }
    }


    private List<VinculoUsuarioCaixaInstitucional> resolveDelegatedLinks(Usuario usuario,
                                                                          String expedicaoUuid,
                                                                          UnidadeInstitucional unidade,
                                                                          CaixaInstitucional caixa) {
        if (usuario.getId() == null) {
            return List.of();
        }
        return delegationAssignmentStateRepository.findByDelegadoUsuarioId(usuario.getId()).stream()
                .filter(assignment -> expedicaoUuid == null || assignment.expedicaoUuid().equalsIgnoreCase(expedicaoUuid))
                .filter(assignment -> assignment.unidadeCodigo().equalsIgnoreCase(unidade.codigo()))
                .filter(assignment -> assignment.caixaCodigo().equalsIgnoreCase(caixa.codigo()))
                .filter(assignment -> assignment.ativaEm(java.time.Instant.now()))
                .map(assignment -> new VinculoUsuarioCaixaInstitucional(
                        usuario.getId(),
                        unidade,
                        caixa,
                        assignment.tipoFluxo().isSubstituicao() ? FuncaoOperacionalInstitucional.SUBSTITUTO : FuncaoOperacionalInstitucional.ASSESSOR_INSTITUCIONAL,
                        AbrangenciaVinculoInstitucional.UNIDADE,
                        assignment.capacidades(),
                        "autorizado_por_delegacao_institucional=" + assignment.assignmentId()
                ))
                .toList();
    }
    private UnidadeInstitucional localizarUnidade(String unidadeCodigo) {
        return catalogoInstitucionalUnificadoService.listarPorTipo(null).stream()
                .filter(unidade -> unidade.codigo().equalsIgnoreCase(unidadeCodigo))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("UnidadeInstitucional", unidadeCodigo));
    }

    private CaixaInstitucional localizarCaixa(UnidadeInstitucional unidade, String caixaCodigo) {
        return estruturaCaixaInstitucionalService.expandir(unidade).stream()
                .filter(caixa -> caixa.codigo().equalsIgnoreCase(caixaCodigo))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("CaixaInstitucional", caixaCodigo));
    }
}
