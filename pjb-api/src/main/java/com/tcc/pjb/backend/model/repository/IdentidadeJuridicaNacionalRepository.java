package com.tcc.pjb.backend.model.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;

@Repository
public interface IdentidadeJuridicaNacionalRepository extends JpaRepository<IdentidadeJuridicaNacional, UUID> {

    Optional<IdentidadeJuridicaNacional> findByTipoDocumentoAndDocumento(DocumentoNacionalValidator.TipoDocumento tipoDocumento, String documento);

    Optional<IdentidadeJuridicaNacional> findByDocumentoHash(String documentoHash);

    List<IdentidadeJuridicaNacional> findAllByIdIn(Collection<UUID> ids);
}
