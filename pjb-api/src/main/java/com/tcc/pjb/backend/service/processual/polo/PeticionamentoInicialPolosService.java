package com.tcc.pjb.backend.service.processual.polo;

import com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService;
import com.tcc.pjb.backend.core.processo.polo.motor.PoloCompositionPolicy;
import com.tcc.pjb.backend.core.processo.polo.motor.PoloComposto;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.core.validation.document.DocumentoValidado;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PeticionamentoInicialPolosService {

    private final PoloProcessualApplicationService poloProcessualApplicationService;
    private final PoloCompositionPolicy poloCompositionPolicy;
    private final DocumentoNacionalValidator documentoNacionalValidator;

    public PeticionamentoInicialPolosService(PoloProcessualApplicationService poloProcessualApplicationService,
                                              PoloCompositionPolicy poloCompositionPolicy,
                                              DocumentoNacionalValidator documentoNacionalValidator) {
        this.poloProcessualApplicationService = Objects.requireNonNull(poloProcessualApplicationService);
        this.poloCompositionPolicy = Objects.requireNonNull(poloCompositionPolicy);
        this.documentoNacionalValidator = Objects.requireNonNull(documentoNacionalValidator);
    }

    @Transactional
    public void registrar(Processo processo, Usuario peticionante) {
        if (processo == null || processo.getId() == null) {
            return;
        }
        TipoUsuario tipoUsuario = peticionante == null ? null : peticionante.getTipoUsuario();
        List<PoloComposto> composicao = poloCompositionPolicy.compor(processo);
        for (PoloComposto pc : composicao) {
            poloProcessualApplicationService.incluir(
                    processo.getId(),
                    pc.tipoPolo(),
                    pc.tipoParte(),
                    pc.nome(),
                    pc.cpf(),
                    documentoNacionalValidator.validar(pc.cpf()) instanceof DocumentoValidado.Valido v ? v.tipo().name() : null,
                    pc.tipoPolo() == TipoPolo.ATIVO ? oabNumero(peticionante, tipoUsuario) : null,
                    pc.tipoPolo() == TipoPolo.ATIVO ? oabUf(peticionante, tipoUsuario) : null,
                    pc.tipoPolo() == TipoPolo.ATIVO ? usuarioIdRepresentante(peticionante, tipoUsuario) : null,
                    null,
                    null,
                    pc.ufDomicilio(),
                    pc.comarcaDomicilio(),
                    pc.municipioDomicilio()
            );
        }
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
