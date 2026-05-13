package com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade;

import java.time.LocalDate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshContextRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesRequest;

public record RecursalAdmissibilityRequest(
        @Valid @NotNull RecursalMeshContextRequest context,
        @Valid @NotNull RecursalMeshSpeciesRequest species,
        @NotBlank @Size(max = 160) String recursoId,
        LocalDate dataIntimacao,
        LocalDate dataProtocolo,
        @NotBlank @Size(max = 16) String tribunalCodigo,
        @Size(min = 2, max = 2) String uf,
        @Size(max = 120) String comarca,
        boolean preparoRecolhido,
        boolean preparoDispensado,
        boolean aceitouDecisaoOuPraticouAtoIncompativel,
        boolean recursoAnteriorMesmaEspecieInterposto,
        boolean pedidoEfeitoSuspensivo,
        boolean tutelaUrgenciaRecursal,
        boolean segredoJustica,
        boolean priorizaIdosoOuSaude) {
}
