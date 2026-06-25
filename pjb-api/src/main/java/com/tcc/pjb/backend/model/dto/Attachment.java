package com.tcc.pjb.backend.model.dto;

import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {

    
    private UUID uuid;              
    private String name;            
    private String contentType;

    /**
     * Tipo canônico do documento declarado pelo remetente, quando informado.
     * <p>
     * Nullable: nenhum caminho de decisão lê este campo na data desta fatia (1b).
     * O leitor — gate de completude documental por rito/classe — será introduzido
     * na fatia 1c; até lá o campo é inerte. Quando o gate existir, tipo ausente
     * passa a ser rejeição explícita no gate, nunca null silencioso.
     */
    private TipoDocumento tipoDocumento;


    private byte[] content;         
    private Long size;              
    private Integer numeroPaginas;  

    
    private String hashIntegridade; 
    private String assinaturaDigital; 
    private boolean criptografado;  

    
    private Long usuarioId;         
    private String origemSistema;
    @Schema(description = "Data e hora do upload do documento processual", format = "date-time",
            example = "2026-06-01T10:00:00-03:00")
    private LocalDateTime dataUpload;
    @Schema(description = "Data e hora da última atualização dos metadados do anexo", format = "date-time",
            example = "2026-06-01T10:05:00-03:00")
    private LocalDateTime ultimaAtualizacao; 

    
    private String resumoIA;        
    private String[] tagsIA;        

    
    private boolean publico;        
    private boolean sigiloso;       
    private boolean valido;         

    
    private String urlDownloadSeguro; 
    private String urlVisualizacao;   
}