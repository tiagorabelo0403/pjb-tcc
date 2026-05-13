package com.tcc.pjb.backend.model.dto;

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

    
    private byte[] content;         
    private Long size;              
    private Integer numeroPaginas;  

    
    private String hashIntegridade; 
    private String assinaturaDigital; 
    private boolean criptografado;  

    
    private Long usuarioId;         
    private String origemSistema;   
    private LocalDateTime dataUpload; 
    private LocalDateTime ultimaAtualizacao; 

    
    private String resumoIA;        
    private String[] tagsIA;        

    
    private boolean publico;        
    private boolean sigiloso;       
    private boolean valido;         

    
    private String urlDownloadSeguro; 
    private String urlVisualizacao;   
}