package com.tcc.pjb.backend.core.processo.polo.application;

import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PoloProcessualApplicationService {

    private final PoloProcessualRepository poloRepository;

    @Inject
    public PoloProcessualApplicationService(PoloProcessualRepository poloRepository) {
        this.poloRepository = poloRepository;
    }

    @Transactional
    public PoloProcessual incluir(Long processoId, TipoPolo tipoPolo, TipoParte tipoParte,
                                   String nomeCompleto, String documento, String documentoTipo,
                                   String oabNumero, String oabUf, Long usuarioId,
                                   UUID identidadeId, Long representadoPorId) {
        int ordemPolo = poloRepository.findByProcessoIdAndAtivo(processoId, true)
                .stream()
                .filter(p -> p.getTipoPolo() == tipoPolo)
                .mapToInt(PoloProcessual::getOrdemPolo)
                .max()
                .orElse(-1) + 1;
        PoloProcessual polo = new PoloProcessual(processoId, tipoPolo, tipoParte, nomeCompleto,
                documento, documentoTipo, oabNumero, oabUf, usuarioId, identidadeId,
                representadoPorId, ordemPolo);
        return poloRepository.save(polo);
    }

    @Transactional
    public PoloProcessual incluir(Long processoId, TipoPolo tipoPolo, TipoParte tipoParte,
                                   String nomeCompleto, String documento, String documentoTipo,
                                   String oabNumero, String oabUf, Long usuarioId,
                                   UUID identidadeId, Long representadoPorId,
                                   String ufDomicilio, String comarcaDomicilio, String municipioDomicilio) {
        int ordemPolo = poloRepository.findByProcessoIdAndAtivo(processoId, true)
                .stream()
                .filter(p -> p.getTipoPolo() == tipoPolo)
                .mapToInt(PoloProcessual::getOrdemPolo)
                .max()
                .orElse(-1) + 1;
        PoloProcessual polo = new PoloProcessual(processoId, tipoPolo, tipoParte, nomeCompleto,
                documento, documentoTipo, oabNumero, oabUf, usuarioId, identidadeId,
                representadoPorId, ordemPolo, ufDomicilio, comarcaDomicilio, municipioDomicilio, null);
        return poloRepository.save(polo);
    }

    @Transactional
    public PoloProcessual incluir(Long processoId, TipoPolo tipoPolo, TipoParte tipoParte,
                                   String nomeCompleto, String documento, String documentoTipo,
                                   String oabNumero, String oabUf, Long usuarioId,
                                   UUID identidadeId, Long representadoPorId,
                                   String ufDomicilio, String comarcaDomicilio, String municipioDomicilio,
                                   String razaoSocial) {
        int ordemPolo = poloRepository.findByProcessoIdAndAtivo(processoId, true)
                .stream()
                .filter(p -> p.getTipoPolo() == tipoPolo)
                .mapToInt(PoloProcessual::getOrdemPolo)
                .max()
                .orElse(-1) + 1;
        PoloProcessual polo = new PoloProcessual(processoId, tipoPolo, tipoParte, nomeCompleto,
                documento, documentoTipo, oabNumero, oabUf, usuarioId, identidadeId,
                representadoPorId, ordemPolo, ufDomicilio, comarcaDomicilio, municipioDomicilio, razaoSocial);
        return poloRepository.save(polo);
    }

    @Transactional
    public void excluir(Long poloId, Long operadorId) {
        PoloProcessual polo = poloRepository.findById(poloId)
                .orElseThrow(() -> new EntityNotFoundException("Polo não encontrado: " + poloId));
        polo.desativar();
        poloRepository.save(polo);
    }

    @Transactional(readOnly = true)
    public List<PoloProcessual> listarPorProcesso(Long processoId) {
        return poloRepository.findByProcessoIdAndAtivo(processoId, true);
    }

    @Transactional(readOnly = true)
    public List<PoloProcessual> listarDestinatariosCiencia(Long processoId) {
        return poloRepository.findDestinatariosCiencia(processoId);
    }

    @Transactional(readOnly = true)
    public List<Long> listarUsuarioIdsDestinatarios(Long processoId) {
        return listarDestinatariosCiencia(processoId).stream()
                .filter(p -> p.getUsuarioId() != null)
                .map(PoloProcessual::getUsuarioId)
                .toList();
    }
}
