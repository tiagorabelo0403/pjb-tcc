package com.tcc.pjb.backend.model.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.identity.ProntuarioNacionalEntrada;

@Repository
public interface ProntuarioNacionalEntradaRepository extends JpaRepository<ProntuarioNacionalEntrada, UUID> {

    Optional<ProntuarioNacionalEntrada> findByDocumentoHashAndNupnAndPoloAndQualificacaoAndTribunalCodigo(
            String documentoHash,
            String nupn,
            ProntuarioNacionalEntrada.PoloProcessual polo,
            ProntuarioNacionalEntrada.QualificacaoProcessual qualificacao,
            String tribunalCodigo
    );

    List<ProntuarioNacionalEntrada> findAllByDocumentoHashOrderByOcorridoEmDescAtualizadoEmDesc(String documentoHash);

    List<ProntuarioNacionalEntrada> findAllByDocumentoHashAndPoloAndRamoDireitoOrderByOcorridoEmDesc(
            String documentoHash,
            ProntuarioNacionalEntrada.PoloProcessual polo,
            RamoDireito ramoDireito
    );

    List<ProntuarioNacionalEntrada> findAllByNupn(String nupn);

    List<ProntuarioNacionalEntrada> findAllByDocumentoHashInAndRamoDireito(Collection<String> documentoHashes, RamoDireito ramoDireito);
}
