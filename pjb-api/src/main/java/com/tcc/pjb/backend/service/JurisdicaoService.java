package com.tcc.pjb.backend.service;

import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.mapper.JurisdicaoMapper;
import com.tcc.pjb.backend.model.dto.JurisdicaoRequest;
import com.tcc.pjb.backend.model.dto.JurisdicaoResponse;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.repository.ComarcaRepository;
import com.tcc.pjb.backend.model.repository.JurisdicaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Slf4j
@Service
@RequiredArgsConstructor
public class JurisdicaoService {

    private final JurisdicaoRepository jurisdicaoRepository;
    private final ProcessoRepository processoRepository;
    private final ComarcaRepository comarcaRepository;
    private final JurisdicaoMapper jurisdicaoMapper;
    private final AuditoriaInteligenteService auditoriaService;

    

    @PjbTransactionalBudget(operation = "jurisdicao.listar-todas", maxMillis = 3000)
    @Transactional(readOnly = true)
    @Cacheable(value = "jurisdicoes")
    public List<JurisdicaoResponse> listarTodas() {
        return jurisdicaoRepository.findAll().stream()
                .map(jurisdicaoMapper::toResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "jurisdicoes", allEntries = true)
    public JurisdicaoResponse criarJurisdicao(JurisdicaoRequest dto) {
        return criar(dto);
    }

    @Transactional
    @CacheEvict(value = {"jurisdicoes", "jurisdicao_id"}, allEntries = true)
    public JurisdicaoResponse atualizarJurisdicao(Long id, JurisdicaoRequest dto) {
        return atualizar(id, dto);
    }

    @Transactional
    @CacheEvict(value = {"jurisdicoes", "jurisdicao_id"}, allEntries = true)
    public void desativarJurisdicao(Long id) {
        desativar(id);
    }

    

    @Transactional
    @CacheEvict(value = "jurisdicoes", allEntries = true)
    public JurisdicaoResponse criar(JurisdicaoRequest dto) {
        log.info("Criando jurisdição: {}", dto.getNome());

        validarUnicidade(dto.getCodigo(), dto.getSigla());
        validarRegrasCnj(dto);

        Jurisdicao entidade = jurisdicaoMapper.toEntity(dto);
        aplicarComarca(entidade, dto);


        if (dto.getSigla() != null) {
            entidade.setSigla(dto.getSigla().toUpperCase().trim());
        }

        Jurisdicao salvo = jurisdicaoRepository.save(entidade);

        auditoriaService.registrarEventoImutavel(
                "CRIACAO_JURISDICAO",
                salvo.getId(),
                "Nova unidade: " + salvo.getNome()
        );

        return jurisdicaoMapper.toResponse(salvo);
    }

    @PjbTransactionalBudget(operation = "jurisdicao.listar-paginado", maxMillis = 3000)
    @Transactional(readOnly = true)
    public Page<JurisdicaoResponse> listarPaginado(Pageable pageable) {
        return jurisdicaoRepository.findAll(pageable)
                .map(jurisdicaoMapper::toResponse);
    }

    
    @Transactional(readOnly = true)
    @Cacheable(value = "jurisdicao_suggest", key = "(#termo == null ? '' : #termo.trim().toLowerCase()) + ':' + (#limit == null ? 10 : #limit)")
    public List<JurisdicaoResponse> sugerir(String termo, Integer limit) {
        String q = termo == null ? "" : termo.trim();
        if (q.length() < 2) {
            return List.of();
        }

        int lim = limit == null ? 10 : limit;
        if (lim < 1) lim = 1;
        if (lim > 20) lim = 20;

        return jurisdicaoRepository.pesquisar(q, PageRequest.of(0, lim))
                .stream()
                .map(jurisdicaoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "jurisdicao_id", key = "#id")
    public JurisdicaoResponse buscarPorId(Long id) {
        return jurisdicaoMapper.toResponse(buscarOuFalhar(id));
    }

    @Transactional
    @CacheEvict(value = {"jurisdicoes", "jurisdicao_id"}, allEntries = true)
    public JurisdicaoResponse atualizar(Long id, JurisdicaoRequest dto) {
        Jurisdicao entidade = buscarOuFalhar(id);

        
        if (dto.getCodigo() != null
                && !entidade.getCodigo().equals(dto.getCodigo())
                && processoRepository.existsByJurisdicao_Id(id)) {
            throw new RegraNegocioException("Não é permitido alterar Código CNJ de jurisdição com processos ativos.");
        }


        jurisdicaoMapper.updateEntityFromDto(dto, entidade);
        aplicarComarca(entidade, dto);


        if (entidade.getSigla() != null) {
            entidade.setSigla(entidade.getSigla().toUpperCase().trim());
        }

        Jurisdicao salvo = jurisdicaoRepository.save(entidade);

        auditoriaService.registrarEventoImutavel(
                "ATUALIZACAO_JURISDICAO",
                id,
                "Dados alterados"
        );

        return jurisdicaoMapper.toResponse(salvo);
    }

    @Transactional
    @CacheEvict(value = {"jurisdicoes", "jurisdicao_id"}, allEntries = true)
    public void desativar(Long id) {
        Jurisdicao entidade = buscarOuFalhar(id);

        long processosAtivos = processoRepository.countByJurisdicaoIdAndStatusAtivo(id);
        if (processosAtivos > 0) {
            throw new RegraNegocioException(
                    "Impossível desativar: Existem " + processosAtivos + " processos ativos nesta unidade."
            );
        }

        entidade.setAtivo(false);
        jurisdicaoRepository.save(entidade);

        auditoriaService.registrarEventoImutavel(
                "DESATIVACAO_JURISDICAO",
                id,
                "Unidade desativada"
        );
    }

    

    private Jurisdicao buscarOuFalhar(Long id) {
        return jurisdicaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Jurisdição não encontrada ID: " + id));
    }

    private void validarUnicidade(String codigo, String sigla) {
        if (codigo != null && jurisdicaoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new RecursoJaExistenteException("Código CNJ já existe: " + codigo);
        }
        if (sigla != null && jurisdicaoRepository.existsBySiglaIgnoreCase(sigla)) {
            throw new RecursoJaExistenteException("Sigla já existe: " + sigla);
        }
    }

    private void aplicarComarca(Jurisdicao entidade, JurisdicaoRequest dto) {
        String comarcaNome = dto.getComarca();
        String uf = dto.getEstado();
        if (comarcaNome == null || comarcaNome.isBlank() || uf == null || uf.isBlank()) {
            return;
        }
        Optional<Comarca> comarca = comarcaRepository.findByNomeIgnoreCaseAndUf(comarcaNome.trim(), uf.trim().toUpperCase());
        if (comarca.isPresent()) {
            entidade.setComarcaEntidade(comarca.get());
        } else {
            log.warn("Comarca não encontrada no catálogo para nome={} uf={}; comarcaEntidade não alterada", comarcaNome, uf);
        }
    }

    private void validarRegrasCnj(JurisdicaoRequest dto) {
        if (dto.getGrau() == GrauJurisdicao.SUPERIOR
                && dto.getNome() != null
                && !dto.getNome().toUpperCase().contains("TRIBUNAL")) {
            throw new RegraNegocioException("Jurisdição Superior deve conter 'Tribunal' no nome.");
        }
    }
}
