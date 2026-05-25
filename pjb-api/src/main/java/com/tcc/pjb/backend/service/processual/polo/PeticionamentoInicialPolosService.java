package com.tcc.pjb.backend.service.processual.polo;

import com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PeticionamentoInicialPolosService {

    private final PoloProcessualApplicationService poloProcessualApplicationService;

    public PeticionamentoInicialPolosService(PoloProcessualApplicationService poloProcessualApplicationService) {
        this.poloProcessualApplicationService = Objects.requireNonNull(poloProcessualApplicationService);
    }

    @Transactional
    public void registrar(Processo processo, Usuario peticionante) {
        if (processo == null || processo.getId() == null) {
            return;
        }
        TipoUsuario tipoUsuario = peticionante == null ? null : peticionante.getTipoUsuario();
        String autora = trimToNull(processo.getParteAutoraNome());
        String reu = trimToNull(processo.getParteReuNome());
        if (autora != null) {
            poloProcessualApplicationService.incluir(
                    processo.getId(),
                    TipoPolo.ATIVO,
                    TipoParte.AUTOR,
                    autora,
                    documentoAutor(peticionante, tipoUsuario, autora),
                    documentoTipo(documentoAutor(peticionante, tipoUsuario, autora)),
                    oabNumero(peticionante, tipoUsuario),
                    oabUf(peticionante, tipoUsuario),
                    usuarioIdRepresentante(peticionante, tipoUsuario),
                    null,
                    null
            );
        }
        if (reu != null) {
            poloProcessualApplicationService.incluir(
                    processo.getId(),
                    TipoPolo.PASSIVO,
                    TipoParte.REU,
                    reu,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
        registrarInstitucional(processo, peticionante, tipoUsuario);
    }

    private void registrarInstitucional(Processo processo, Usuario peticionante, TipoUsuario tipoUsuario) {
        TipoPolo tipoPolo = tipoPoloInstitucional(tipoUsuario);
        if (tipoPolo == null || peticionante == null) {
            return;
        }
        poloProcessualApplicationService.incluir(
                processo.getId(),
                tipoPolo,
                tipoPolo == TipoPolo.MINISTERIO_PUBLICO ? TipoParte.MINISTERIO_PUBLICO : TipoParte.TERCEIRO_INTERESSADO,
                firstNonBlank(peticionante.getNome(), tipoPolo.label()),
                documentoInstitucional(peticionante),
                documentoTipo(documentoInstitucional(peticionante)),
                null,
                null,
                peticionante.getId(),
                null,
                null
        );
    }

    private TipoPolo tipoPoloInstitucional(TipoUsuario tipoUsuario) {
        if (tipoUsuario == null) {
            return null;
        }
        if (tipoUsuario.isDefensoriaPublica()) {
            return TipoPolo.DEFENSORIA;
        }
        if (tipoUsuario.isMinisterioPublico()) {
            return TipoPolo.MINISTERIO_PUBLICO;
        }
        if (tipoUsuario.isProcuradoria()) {
            return TipoPolo.PROCURADORIA;
        }
        return null;
    }

    private Long usuarioIdRepresentante(Usuario peticionante, TipoUsuario tipoUsuario) {
        if (peticionante == null || tipoUsuario == null) {
            return null;
        }
        if (tipoUsuario.isAdvocacia() || tipoUsuario.isDefensoriaPublica() || tipoUsuario.isMinisterioPublico() || tipoUsuario.isProcuradoria()) {
            return peticionante.getId();
        }
        return null;
    }

    private String documentoAutor(Usuario peticionante, TipoUsuario tipoUsuario, String autora) {
        if (peticionante == null || tipoUsuario == null || tipoUsuario.isAdvocacia()) {
            return null;
        }
        if (!sameName(autora, peticionante.getNome())) {
            return null;
        }
        return documentoInstitucional(peticionante);
    }

    private String documentoInstitucional(Usuario peticionante) {
        return digits(trimToNull(peticionante == null ? null : peticionante.getCpf()));
    }

    private String oabNumero(Usuario peticionante, TipoUsuario tipoUsuario) {
        if (peticionante == null || tipoUsuario == null || !tipoUsuario.isAdvocacia()) {
            return null;
        }
        return digits(firstNonBlank(peticionante.getOabNormalizada(), peticionante.getOab()));
    }

    private String oabUf(Usuario peticionante, TipoUsuario tipoUsuario) {
        if (peticionante == null || tipoUsuario == null || !tipoUsuario.isAdvocacia()) {
            return null;
        }
        return trimToNull(peticionante.getOabUf());
    }

    private String documentoTipo(String documento) {
        if (documento == null) {
            return null;
        }
        return documento.length() == 14 ? "CNPJ" : documento.length() == 11 ? "CPF" : null;
    }

    private boolean sameName(String first, String second) {
        String a = normalizeName(first);
        String b = normalizeName(second);
        return a != null && a.equals(b);
    }

    private String normalizeName(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String digits(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        String digits = normalized.replaceAll("\\D+", "");
        return digits.isBlank() ? null : digits;
    }

    private String firstNonBlank(String first, String second) {
        String a = trimToNull(first);
        return a == null ? trimToNull(second) : a;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
