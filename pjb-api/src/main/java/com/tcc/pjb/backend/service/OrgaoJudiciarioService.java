package com.tcc.pjb.backend.service;

import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.mapper.OrgaoJudiciarioMapper;
import com.tcc.pjb.backend.model.dto.OrgaoJudiciarioRequest;
import com.tcc.pjb.backend.model.dto.OrgaoJudiciarioResponse;
import com.tcc.pjb.backend.model.entity.OrgaoJudiciario;
import com.tcc.pjb.backend.model.repository.OrgaoJudiciarioRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
@RequiredArgsConstructor
public class OrgaoJudiciarioService {

    private final OrgaoJudiciarioRepository orgaoJudiciarioRepository;
    private final OrgaoJudiciarioMapper orgaoJudiciarioMapper;
    private final ComarcaResolutionService comarcaResolutionService;

    @Transactional
    @CacheEvict(cacheNames = {"orgaos_judiciarios", "orgao_judiciario_id"}, allEntries = true)
    public OrgaoJudiciarioResponse criarOrgaoJudiciario(OrgaoJudiciarioRequest dto) {

        if (orgaoJudiciarioRepository.findBySigla(dto.getSigla()).isPresent()) {
            throw new RecursoJaExistenteException("Sigla do órgão já cadastrada: " + dto.getSigla());
        }

        OrgaoJudiciario entidade = orgaoJudiciarioMapper.requestParaEntidade(dto);
        aplicarComarcaDoCatalogo(entidade);

        OrgaoJudiciario entidadeSalva = orgaoJudiciarioRepository.save(entidade);

        return orgaoJudiciarioMapper.entidadeParaResponse(entidadeSalva);
    }

    private void aplicarComarcaDoCatalogo(OrgaoJudiciario entidade) {
        if (entidade.getComarca() == null || entidade.getComarca().isBlank()) {
            entidade.setComarcaEntidade(null);
            return;
        }
        entidade.setComarcaEntidade(comarcaResolutionService.resolver(entidade.getComarca(), entidade.getEstado())
                .orElse(null));
    }

    @PjbTransactionalBudget(operation = "orgao-judiciario.listar-todos", maxMillis = 3000)
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "orgaos_judiciarios")
    public List<OrgaoJudiciarioResponse> listarTodos() {
        List<OrgaoJudiciario> listaDeEntidades = orgaoJudiciarioRepository.findAll();
        return orgaoJudiciarioMapper.entidadeParaResponseLista(listaDeEntidades);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "orgao_judiciario_id", key = "#id")
    public OrgaoJudiciarioResponse buscarPorId(Long id) {
        return orgaoJudiciarioRepository.findById(id)
                .map(orgaoJudiciarioMapper::entidadeParaResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Órgão Judiciário não encontrado com o ID: " + id));
    }

    @Transactional
    @CacheEvict(cacheNames = {"orgaos_judiciarios", "orgao_judiciario_id"}, allEntries = true)
    public OrgaoJudiciarioResponse atualizarOrgaoJudiciario(Long id, OrgaoJudiciarioRequest dto) {
        OrgaoJudiciario entidadeExistente = orgaoJudiciarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Órgão Judiciário não encontrado com o ID: " + id));

        orgaoJudiciarioMapper.atualizarEntidadeDoRequest(dto, entidadeExistente);
        aplicarComarcaDoCatalogo(entidadeExistente);

        OrgaoJudiciario entidadeSalva = orgaoJudiciarioRepository.save(entidadeExistente);
        return orgaoJudiciarioMapper.entidadeParaResponse(entidadeSalva);
    }

    @Transactional
    @CacheEvict(cacheNames = {"orgaos_judiciarios", "orgao_judiciario_id"}, allEntries = true)
    public void desativarOrgaoJudiciario(Long id) {
        OrgaoJudiciario entidade = orgaoJudiciarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Órgão Judiciário não encontrado com o ID: " + id));

        entidade.setAtivo(false);

        orgaoJudiciarioRepository.save(entidade);
    }
}