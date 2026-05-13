package com.tcc.pjb.backend.model.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMensagemRequest {

    
    @NotNull(message = "O ID do Processo é obrigatório")
    private Long processoId;

    @NotNull(message = "O ID do Usuário (remetente) é obrigatório")
    private Long usuarioId;

    @NotBlank(message = "O conteúdo não pode ser vazio")
    
    @Size(max = 5000, message = "Mensagem muito longa (máx 5000 caracteres)")
    private String conteudo;

    private UUID mensagemUuid;              
    private LocalDateTime dataEnvio;       
    private String canal;                   
    private String ipOrigem;                
    private String deviceInfo;              
    private String idioma;                  
    private boolean sigiloso;               
    private int nivelSigilo;                
    private boolean urgente;                
    private boolean prioridadeAlta;         
    private String tipoMensagem;            
    private String mimeType;                
    private Long tamanhoBytes;              
    private List<String> anexosUrls;        
    private String hashIntegridade;         
    private boolean criptografado;          
    private boolean precisaRevisaoHumana;   
    private boolean permitidoIA;            
    private String resumoIA;                
    private List<String> tagsIA;            
    private double confiancaIA;             
    private boolean detectouOfensa;         
    private boolean detectouSpam;           
    private boolean detectouIronia;         
    private String respostaModeracao;       
    private boolean podeRecorrer;           
    private int tentativasViolacao;         
    private boolean suspenderConta;         
    private String referenciaExterna;       
    private String origemSistema;           
    private LocalDateTime ultimaAtualizacao;
}