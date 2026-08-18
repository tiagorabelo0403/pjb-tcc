package com.tcc.pjb.backend.core.processo.polo.application;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.secretariat.institucional.PoloInstitucionalComposicaoEvent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PoloProcessualApplicationService {

    private final PoloProcessualRepository poloRepository;
    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Inject
    public PoloProcessualApplicationService(PoloProcessualRepository poloRepository,
                                             ProcessoRepository processoRepository,
                                             UsuarioRepository usuarioRepository,
                                             ApplicationEventPublisher eventPublisher) {
        this.poloRepository = poloRepository;
        this.processoRepository = processoRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventPublisher = eventPublisher;
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
        PoloProcessual salvo = poloRepository.save(polo);
        publicarEventoInstitucionalSeAplicavel(salvo);
        return salvo;
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
        PoloProcessual salvo = poloRepository.save(polo);
        publicarEventoInstitucionalSeAplicavel(salvo);
        return salvo;
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
        PoloProcessual salvo = poloRepository.save(polo);
        publicarEventoInstitucionalSeAplicavel(salvo);
        return salvo;
    }

    private void publicarEventoInstitucionalSeAplicavel(PoloProcessual polo) {
        TipoUnidadeInstitucional tipo = resolverTipoUnidadeInstitucional(polo.getTipoPolo());
        if (tipo == null) {
            tipo = resolverTipoUnidadeInstitucionalPorRepresentante(polo.getUsuarioId());
        }
        if (tipo == null) {
            return;
        }
        Processo processo = processoRepository.findById(polo.getProcessoId()).orElse(null);
        if (processo == null) {
            return;
        }
        eventPublisher.publishEvent(new PoloInstitucionalComposicaoEvent(processo.getId(), processo.getComarca(), tipo));
    }

    private static TipoUnidadeInstitucional resolverTipoUnidadeInstitucional(TipoPolo tipoPolo) {
        return switch (tipoPolo) {
            case MINISTERIO_PUBLICO -> TipoUnidadeInstitucional.PROMOTORIA;
            case DEFENSORIA -> TipoUnidadeInstitucional.NUCLEO_DEFENSORIA;
            case PROCURADORIA -> TipoUnidadeInstitucional.PROCURADORIA_PUBLICA;
            default -> null;
        };
    }

    private TipoUnidadeInstitucional resolverTipoUnidadeInstitucionalPorRepresentante(Long usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return null;
        }
        TipoUsuario tipoUsuario = usuario.getTipoUsuario();
        if (tipoUsuario.isMinisterioPublico()) {
            return TipoUnidadeInstitucional.PROMOTORIA;
        }
        if (tipoUsuario.isDefensoriaPublica()) {
            return TipoUnidadeInstitucional.NUCLEO_DEFENSORIA;
        }
        if (tipoUsuario.isProcuradoria()) {
            return TipoUnidadeInstitucional.PROCURADORIA_PUBLICA;
        }
        return null;
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
