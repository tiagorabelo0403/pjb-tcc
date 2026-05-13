package com.tcc.pjb.backend.model.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMensagemResponse {

    
    private Long id;
    private Long processoId;
    private LocalDateTime dataEnvio;
    private String conteudo;
    private String nomeUsuario;
    private String perfilUsuario;

    private UUID mensagemUuid;              
    private LocalDateTime dataRecebimento;  
    private LocalDateTime ultimaAtualizacao;
    private boolean editada;                
    private boolean excluida;               
    private String motivoExclusao;          
    private boolean sigilosa;               
    private int nivelSigilo;                
    private boolean urgente;                
    private boolean prioridadeAlta;         
    private String tipoMensagem;            
    private String mimeType;                
    private Long tamanhoBytes;              
    private int numeroPaginas;              
    private List<String> anexosUrls;        
    private String hashIntegridade;         
    private boolean criptografada;          
    private String assinaturaDigital;       
    private boolean validaAssinatura;       
    private String ipOrigem;                
    private String deviceInfo;              
    private String canal;                   
    private String idioma;                  
    private boolean moderada;               
    private List<String> motivosModeracao;  
    private boolean precisaRevisaoHumana;   
    private boolean detectouOfensa;         
    private boolean detectouSpam;           
    private boolean detectouIronia;         
    private double confiancaIA;             
    private String resumoIA;                
    private List<String> tagsIA;            
    private String respostaModeracao;       
    private boolean podeRecorrer;           
    private int tentativasViolacao;         
    private boolean suspenderConta;         
    private String referenciaExterna;       
    private String origemSistema;           
    private Long usuarioId;                 
    private String advogadoOab;             
    private String orgaoJudiciario;         
    private String comarca;                 
    private String jurisdicao;              
    private String esfera;                  
    private boolean publico;                
    private boolean arquivada;              
    private LocalDateTime dataArquivamento; 
    private boolean marcadaFavorita;        
    private boolean mencionouUsuario;       
    private List<Long> usuariosMencionados; 
    private boolean acordoRelacionado;
    private String statusAcordo;
    private boolean acordoPendenteHomologacao;
    private Integer rodadaNegocial;
    private String versaoNegocial;
    private String tipoAnexoNegocial;
    private String rotuloAnexoNegocial;
    private String decisaoJudicialAcordo;
    private String faseCanalNegocial;
    private boolean bloqueadaAlteracaoTermos;
}
