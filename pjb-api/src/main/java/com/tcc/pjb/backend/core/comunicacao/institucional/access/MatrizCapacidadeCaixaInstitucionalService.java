package com.tcc.pjb.backend.core.comunicacao.institucional.access;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;

@Service
public class MatrizCapacidadeCaixaInstitucionalService {

    private final Map<FuncaoOperacionalInstitucional, Set<CapacidadeCaixaInstitucional>> capacidadePorFuncao;

    public MatrizCapacidadeCaixaInstitucionalService() {
        EnumMap<FuncaoOperacionalInstitucional, Set<CapacidadeCaixaInstitucional>> map = new EnumMap<>(FuncaoOperacionalInstitucional.class);
        map.put(FuncaoOperacionalInstitucional.MEMBRO_TITULAR, EnumSet.of(
                CapacidadeCaixaInstitucional.VISUALIZAR,
                CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                CapacidadeCaixaInstitucional.DAR_CIENCIA,
                CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE,
                CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO,
                CapacidadeCaixaInstitucional.PREPARAR_MINUTA,
                CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO,
                CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO,
                CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
                CapacidadeCaixaInstitucional.DEVOLVER_PARA_FILA,
                CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR,
                CapacidadeCaixaInstitucional.PEDIR_RETORNO_SECRETARIA,
                CapacidadeCaixaInstitucional.REGISTRAR_IMPEDIMENTO,
                CapacidadeCaixaInstitucional.REGISTRAR_SUBSTITUICAO,
                CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS,
                CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA,
                CapacidadeCaixaInstitucional.REGISTRAR_TERMO_AUDIENCIA,
                CapacidadeCaixaInstitucional.EMITIR_PARECER,
                CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL
        ));
        map.put(FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE, EnumSet.of(
                CapacidadeCaixaInstitucional.VISUALIZAR,
                CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE,
                CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO,
                CapacidadeCaixaInstitucional.PREPARAR_MINUTA,
                CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
                CapacidadeCaixaInstitucional.DEVOLVER_PARA_FILA,
                CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR,
                CapacidadeCaixaInstitucional.PEDIR_RETORNO_SECRETARIA,
                CapacidadeCaixaInstitucional.REGISTRAR_IMPEDIMENTO,
                CapacidadeCaixaInstitucional.REGISTRAR_SUBSTITUICAO,
                CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS,
                CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA,
                CapacidadeCaixaInstitucional.REGISTRAR_TERMO_AUDIENCIA,
                CapacidadeCaixaInstitucional.EMITIR_PARECER,
                CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL
        ));
        map.put(FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM, EnumSet.of(
                CapacidadeCaixaInstitucional.VISUALIZAR,
                CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE,
                CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO,
                CapacidadeCaixaInstitucional.PREPARAR_MINUTA,
                CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
                CapacidadeCaixaInstitucional.DEVOLVER_PARA_FILA,
                CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR,
                CapacidadeCaixaInstitucional.PEDIR_RETORNO_SECRETARIA,
                CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS,
                CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL
        ));
        map.put(FuncaoOperacionalInstitucional.ASSESSOR_INSTITUCIONAL, EnumSet.of(
                CapacidadeCaixaInstitucional.VISUALIZAR,
                CapacidadeCaixaInstitucional.PREPARAR_MINUTA,
                CapacidadeCaixaInstitucional.DEVOLVER_PARA_FILA,
                CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR,
                CapacidadeCaixaInstitucional.PEDIR_RETORNO_SECRETARIA,
                CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS,
                CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL
        ));
        map.put(FuncaoOperacionalInstitucional.APOIO_TECNICO_SETORIAL, EnumSet.of(
                CapacidadeCaixaInstitucional.VISUALIZAR,
                CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                CapacidadeCaixaInstitucional.DAR_CIENCIA,
                CapacidadeCaixaInstitucional.PREPARAR_MINUTA,
                CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
                CapacidadeCaixaInstitucional.DEVOLVER_PARA_FILA,
                CapacidadeCaixaInstitucional.REGISTRAR_IMPEDIMENTO,
                CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS
        ));
        map.put(FuncaoOperacionalInstitucional.GESTOR_CAIXA, EnumSet.allOf(CapacidadeCaixaInstitucional.class));
        map.put(FuncaoOperacionalInstitucional.SUBSTITUTO, EnumSet.of(
                CapacidadeCaixaInstitucional.VISUALIZAR,
                CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                CapacidadeCaixaInstitucional.DAR_CIENCIA,
                CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE,
                CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO,
                CapacidadeCaixaInstitucional.PREPARAR_MINUTA,
                CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO,
                CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO,
                CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
                CapacidadeCaixaInstitucional.DEVOLVER_PARA_FILA,
                CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR,
                CapacidadeCaixaInstitucional.PEDIR_RETORNO_SECRETARIA,
                CapacidadeCaixaInstitucional.REGISTRAR_IMPEDIMENTO,
                CapacidadeCaixaInstitucional.REGISTRAR_SUBSTITUICAO,
                CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS,
                CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.EMITIR_PARECER,
                CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL
        ));
        map.put(FuncaoOperacionalInstitucional.PLANTONISTA, EnumSet.of(
                CapacidadeCaixaInstitucional.VISUALIZAR,
                CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                CapacidadeCaixaInstitucional.DAR_CIENCIA,
                CapacidadeCaixaInstitucional.PREPARAR_MINUTA,
                CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO,
                CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO,
                CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
                CapacidadeCaixaInstitucional.REGISTRAR_IMPEDIMENTO,
                CapacidadeCaixaInstitucional.REGISTRAR_SUBSTITUICAO,
                CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS,
                CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA,
                CapacidadeCaixaInstitucional.REGISTRAR_TERMO_AUDIENCIA,
                CapacidadeCaixaInstitucional.EMITIR_PARECER,
                CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL
        ));
        this.capacidadePorFuncao = Map.copyOf(map);
    }

    public Set<CapacidadeCaixaInstitucional> capacidades(FuncaoOperacionalInstitucional funcaoOperacional) {
        Objects.requireNonNull(funcaoOperacional, "funcaoOperacional");
        return capacidadePorFuncao.getOrDefault(funcaoOperacional, Set.of());
    }
}
