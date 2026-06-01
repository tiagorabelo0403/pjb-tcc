
package com.tcc.pjb.backend.modules.laiane.dto.legal;

import com.tcc.pjb.backend.modules.laiane.dto.protocol.LaianeProtocolPackageDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Pacote de protocolo de petição gerado pelo Laiane (pré-protocolo no PJe/eProc)")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianePeticaoProtocolPackageResponse {
    @Schema(description = "Pacote de protocolo com metadados de submissão")
    private LaianeProtocolPackageDto protocolPackage;
    private LaianePeticaoAssistResponse preflight;
    @Builder.Default
    private Map<String, Object> strategicEnvelope = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> aiVerifier = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> batchReading = new LinkedHashMap<>();
    @Builder.Default
    private List<String> finalGates = new ArrayList<>();
    @Builder.Default
    private Map<String, Object> multimediaComposition = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> mediaSecurityStatus = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> threatSentinel = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> mediaStorageShield = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> uploadGovernance = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> periciaEvidence = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> mediaPublicationGate = new LinkedHashMap<>();
}
