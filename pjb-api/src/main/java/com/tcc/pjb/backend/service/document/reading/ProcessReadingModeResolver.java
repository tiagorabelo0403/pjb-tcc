package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProcessReadingModeResolver {

    public ProcessReadingModeProfile resolve(Processo processo,
                                             Usuario usuario,
                                             List<DocumentoProcessual> documentos,
                                             List<DocumentoPagina> paginas) {
        long totalDocumentos = documentos == null ? 0L : documentos.size();
        long totalPaginas = paginas == null ? 0L : paginas.size();
        long paginasComTexto = paginas == null ? 0L : paginas.stream().filter(p -> !blank(p.getTextoExtraido())).count();
        return resolve(processo, usuario, totalDocumentos, totalPaginas, paginasComTexto);
    }

    public ProcessReadingModeProfile resolve(Processo processo,
                                             Usuario usuario,
                                             long totalDocumentos,
                                             long totalPaginas,
                                             long paginasComTexto) {
        int coberturaTextual = totalPaginas == 0L ? 0 : (int) Math.round((paginasComTexto * 100.0d) / totalPaginas);
        boolean volumeExtenso = totalPaginas >= 180L || totalDocumentos >= 18L;
        boolean recursal = processo != null && processo.getFaseAtual() == FaseProcessual.RECURSAL;
        boolean sigiloReforcado = processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial();
        TipoUsuario tipo = usuario != null ? usuario.getTipoUsuario() : null;
        String profileCode = resolveProfileCode(tipo, processo, volumeExtenso, recursal);
        String visualTheme = resolveVisualTheme(tipo, sigiloReforcado, volumeExtenso);
        String glareControlMode = volumeExtenso ? "AMBAR_PROGRESSIVO" : "SUAVIZACAO_NEUTRA";
        String contrastMode = sigiloReforcado ? "CONTRASTE_REFORCADO" : "CONTRASTE_EQUILIBRADO";
        String fontScale = volumeExtenso ? "112" : recursal ? "108" : "100";
        String lineSpacing = volumeExtenso ? "EXPANDIDO" : "PADRAO_LIMPO";
        String segmentationMode = totalPaginas >= 80L ? "AGRUPAMENTO_POR_PECA_E_BLOCO" : "AGRUPAMENTO_POR_DOCUMENTO";
        String navigationMode = recursal ? "MAPA_RECURSAL_E_PECA_CHAVE" : volumeExtenso ? "MAPA_DE_PECAS_E_MARCADORES" : "ROLAGEM_ASSISTIDA";
        String evidenceMode = resolveEvidenceMode(processo);
        String recursalMode = recursal ? "TRILHA_DECISAO_RECURSO_CONTRARRAZOES" : "TRILHA_LINEAR";
        String supportDeskMode = resolveSupportDeskMode(tipo, processo);
        String noteMode = tipo != null && (tipo.isMagistratura() || tipo.isAssessor()) ? "ANOTACAO_LATERAL_E_FIXACAO" : "MARCADOR_SEMANTICO";
        String fatigueShieldMode = volumeExtenso ? "BLOCOS_CURTOS_COM_RESPIRACAO_VISUAL" : "LEITURA_CONTINUA_SUAVE";
        String summaryMode = totalPaginas >= 40L ? "SINOPSE_PROGRESSIVA_POR_BLOCO" : "SINOPSE_DIRETA";
        List<String> alerts = new ArrayList<>();
        if (coberturaTextual < 65 && totalPaginas > 0L) {
            alerts.add("Cobertura textual baixa: destacar páginas sem texto extraído e priorizar leitura assistida.");
        }
        if (volumeExtenso) {
            alerts.add("Volume processual alto: ativar segmentação por peças e navegação lateral fixa.");
        }
        if (sigiloReforcado) {
            alerts.add("Sigilo reforçado: reduzir distrações, impedir cache local e privilegiar trilha mínima de exposição.");
        }
        if (recursal) {
            alerts.add("Fase recursal ativa: destacar decisão atacada, razões, contrarrazões e precedentes centrais.");
        }
        if (tipo != null && tipo.isServidorJudiciario()) {
            alerts.add("Perfil servidor: manter faixa operacional visível com pendências, prazos e peça-alvo do próximo impulso.");
        }
        return new ProcessReadingModeProfile(
                profileCode,
                visualTheme,
                glareControlMode,
                contrastMode,
                fontScale,
                lineSpacing,
                segmentationMode,
                navigationMode,
                evidenceMode,
                recursalMode,
                supportDeskMode,
                noteMode,
                fatigueShieldMode,
                summaryMode,
                totalDocumentos,
                totalPaginas,
                coberturaTextual,
                sigiloReforcado,
                recursal,
                volumeExtenso,
                alerts
        );
    }

    private static String resolveProfileCode(TipoUsuario tipo, Processo processo, boolean volumeExtenso, boolean recursal) {
        if (tipo != null && (tipo.isMagistratura() || tipo.isAssessor())) {
            return recursal ? "GABINETE_RECURSAL_INTENSIVO" : "GABINETE_DECISOR_ESTRUTURADO";
        }
        if (tipo != null && (tipo.isMinisterioPublico() || tipo.isDefensoriaPublica() || tipo.isProcuradoria() || tipo.isAdvocacia())) {
            return recursal ? "TESE_PROVA_RECURSO" : "TESE_E_PROVA";
        }
        if (tipo != null && tipo.isServidorJudiciario()) {
            return "TRIAGEM_OPERACIONAL_ASSISTIDA";
        }
        if (volumeExtenso) {
            return "LEITURA_VOLUMOSA_ASSISTIDA";
        }
        if (processo != null && processo.getRamoDireito() == RamoDireito.PENAL) {
            return "LEITURA_CRONO_PROBATORIA";
        }
        return "LEITURA_EQUILIBRADA";
    }

    private static String resolveVisualTheme(TipoUsuario tipo, boolean sigiloReforcado, boolean volumeExtenso) {
        if (sigiloReforcado) {
            return "AMBAR_RESERVADO";
        }
        if (tipo != null && (tipo.isMagistratura() || tipo.isAssessor() || tipo.isServidorJudiciario())) {
            return volumeExtenso ? "AMBAR_JURIDICO" : "MARFIM_SUAVE";
        }
        return volumeExtenso ? "AMBAR_JURIDICO" : "NEUTRO_ESTATUARIO";
    }

    private static String resolveEvidenceMode(Processo processo) {
        if (processo == null || processo.getRamoDireito() == null) {
            return "PROVA_GERAL";
        }
        return switch (processo.getRamoDireito()) {
            case PENAL -> "PROVA_CRONOLOGICA_E_ELEMENTOS_DE_AUTORIA";
            case TRABALHISTA -> "PROVA_DOCUMENTAL_E_CALCULO";
            case PREVIDENCIARIO -> "PROVA_MEDICA_SOCIAL_E_DOCUMENTAL";
            case FAMILIA, INFANCIA_JUVENTUDE -> "PROVA_SENSIVEL_E_ESCUTA_PROTEGIDA";
            case EMPRESARIAL, TRIBUTARIO -> "PROVA_CONTABIL_E_DOCUMENTAL";
            default -> "PROVA_GERAL";
        };
    }

    private static String resolveSupportDeskMode(TipoUsuario tipo, Processo processo) {
        if (tipo != null && tipo.isServidorJudiciario()) {
            return "PENDENCIA_PRAZO_E_MOVIMENTACAO";
        }
        if (tipo != null && tipo.isAssessor()) {
            return "MINUTA_PRECEDENTE_E_PECA_CHAVE";
        }
        if (tipo != null && tipo.isMagistratura()) {
            return "TRILHA_DECISORIA_E_CHECKLIST_DE_ENFRENTAMENTO";
        }
        if (processo != null && processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            return "RAZOES_CONTRARRAZOES_E_TEMA";
        }
        return "PECA_E_MOVIMENTACAO";
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
