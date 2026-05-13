package com.tcc.pjb.backend.service.profile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCertidaoTipo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;

@Service
public class DiligenceInstitutionalTemplateService {

    public String title(TelemetriaOperacionalCanal canal,
                        DiligenciaCertidaoTipo tipo) {
        String prefix = switch (canal) {
            case OFICIAL_JUSTICA -> "Mandado";
            case DELEGADO -> "Diligência";
        };
        String suffix = switch (tipo) {
            case CHEGADA_CONFIRMADA -> "certidão automática de chegada confirmada";
            case TENTATIVA_FRUSTRADA -> "certidão automática de tentativa frustrada";
            case CUMPRIMENTO_POSITIVO -> "certidão automática de cumprimento positivo";
            case DILIGENCIA_OPERACIONAL -> "certidão automática operacional";
        };
        return prefix + " - " + suffix + " - v3";
    }

    public String narrative(Usuario actor,
                            TelemetriaOperacionalCanal canal,
                            String diligenceReference,
                            DiligenciaOperadorCheckpointEvento checkpoint,
                            DiligenceReferenceResolverService.ResolvedDiligenceReference resolved,
                            DiligenciaCertidaoTipo tipo,
                            String evidence,
                            String observations,
                            String attemptTrailDigest) {
        Objects.requireNonNull(actor);
        Objects.requireNonNull(canal);
        Objects.requireNonNull(diligenceReference);
        Objects.requireNonNull(checkpoint);
        Objects.requireNonNull(tipo);
        String perfil = actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil();
        String nome = actor.getNome() == null || actor.getNome().isBlank() ? perfil : actor.getNome().trim();
        String processNumber = checkpoint.getProcessoNumero() != null
                ? checkpoint.getProcessoNumero()
                : resolved != null ? resolved.processoNumero() : null;
        Map<String, String> block = new LinkedHashMap<>();
        block.put("template", templateId(canal, tipo));
        block.put("titulo", title(canal, tipo));
        block.put("instituicao", institutionLabel(canal));
        block.put("unidade_operacional", unitLabel(actor));
        block.put("ente_federativo", actor.getEnteFederativo() != null ? actor.getEnteFederativo().name() : "-");
        block.put("operador_nome", nome);
        block.put("operador_perfil", perfil);
        block.put("canal", canal.name());
        block.put("referencia", diligenceReference);
        block.put("work_item_id", nv(checkpoint.getWorkItemId()));
        block.put("processo_id", nv(checkpoint.getProcessoId()));
        block.put("processo_numero", nv(processNumber));
        block.put("checkpoint_id", nv(checkpoint.getId()));
        block.put("checkpoint_tipo", checkpoint.getCheckpointTipo().name());
        block.put("classificacao", checkpoint.getClassification());
        block.put("sequencia_tentativa", nv(checkpoint.getTentativaSequencia()));
        block.put("situacao_campo", situacaoCampo(tipo, checkpoint.isInsideGeofence()));
        block.put("destino_latitude", Double.toString(checkpoint.getTargetLatitude()));
        block.put("destino_longitude", Double.toString(checkpoint.getTargetLongitude()));
        block.put("observada_latitude", Double.toString(checkpoint.getObservedLatitude()));
        block.put("observada_longitude", Double.toString(checkpoint.getObservedLongitude()));
        block.put("distancia_metros", Double.toString(checkpoint.getDistanceMeters()));
        block.put("raio_metros", Double.toString(checkpoint.getGeofenceRadiusMeters()));
        block.put("dentro_da_cerca", Boolean.toString(checkpoint.isInsideGeofence()));
        block.put("fonte", checkpoint.getSource());
        block.put("assinatura_localizacao_sha256", nv(checkpoint.getLocationSignatureSha256()));
        block.put("capturado_em", String.valueOf(checkpoint.getOccurredAt()));
        block.put("registrado_em", String.valueOf(checkpoint.getCreatedAt()));
        block.put("trilha_tentativas_digest_sha256", attemptTrailDigest);
        block.put("evidence_chave_custodia", nv(evidence));
        block.put("bloco_institucional", institutionalBody(canal, tipo, checkpoint, resolved));
        block.put("observacoes", nv(observations));
        StringJoiner joiner = new StringJoiner("\n");
        block.forEach((k, v) -> joiner.add(k + "=" + v));
        return joiner.toString();
    }

    private String templateId(TelemetriaOperacionalCanal canal,
                              DiligenciaCertidaoTipo tipo) {
        return canal.name() + ":" + tipo.name() + ":V3";
    }

    private String institutionLabel(TelemetriaOperacionalCanal canal) {
        return switch (canal) {
            case OFICIAL_JUSTICA -> "PJB-CUMPRIMENTO-JUDICIAL-MALHA-V3";
            case DELEGADO -> "PJB-OPERACAO-INVESTIGATIVA-MALHA-V3";
        };
    }

    private String unitLabel(Usuario actor) {
        String comarca = actor.getComarca() == null || actor.getComarca().isBlank() ? "CENTRAL" : actor.getComarca().trim().toUpperCase();
        String uf = actor.getUf() == null || actor.getUf().isBlank() ? "BR" : actor.getUf().trim().toUpperCase();
        return comarca + "/" + uf;
    }

    private String institutionalBody(TelemetriaOperacionalCanal canal,
                                     DiligenciaCertidaoTipo tipo,
                                     DiligenciaOperadorCheckpointEvento checkpoint,
                                     DiligenceReferenceResolverService.ResolvedDiligenceReference resolved) {
        String workItemType = resolved != null && resolved.workItemType() != null ? resolved.workItemType() : checkpoint.getWorkItemType();
        String status = resolved != null && resolved.workItemStatus() != null ? resolved.workItemStatus() : checkpoint.getWorkItemStatus();
        return switch (canal) {
            case OFICIAL_JUSTICA -> switch (tipo) {
                case CHEGADA_CONFIRMADA -> "Confirmação georreferenciada de chegada ao destino operacional do mandado, apta à certificação automática, à juntada controlada e à anexação institucional externa auditável.";
                case TENTATIVA_FRUSTRADA -> "Registro validado de tentativa frustrada em campo, com preservação da trilha geográfica, lastro temporal, trilha de tentativa assinada e insumo para nova deliberação judicial.";
                case CUMPRIMENTO_POSITIVO -> "Cumprimento materialmente confirmado em campo, com captura da trilha operacional, aderência territorial, lastro para baixa do item executivo e preparação para remessa institucional.";
                case DILIGENCIA_OPERACIONAL -> "Registro operacional intermediário de diligência judicial, preservando integridade, territorialidade, cadeia de decisão e aptidão para anexação interoperável.";
            };
            case DELEGADO -> switch (tipo) {
                case CHEGADA_CONFIRMADA -> "Chegada investigativa validada com georreferenciamento, apta a compor cadeia narrativa operacional, trilha de auditoria institucional e intercâmbio controlado com a malha judicial.";
                case TENTATIVA_FRUSTRADA -> "Tentativa investigativa sem êxito material imediato, com preservação do contexto espacial, temporal, da referência do item operacional e da janela de replay transacional.";
                case CUMPRIMENTO_POSITIVO -> "Execução investigativa confirmada com aderência espacial, prova de execução, aptidão para providências subsequentes e remessa institucional versionada.";
                case DILIGENCIA_OPERACIONAL -> "Marco operacional de diligência investigativa, conectado ao item de trabalho, preservado para governança, supervisão, replay institucional e anexação interoperável.";
            };
        } + " work_item_tipo=" + nv(workItemType) + "; work_item_status=" + nv(status);
    }

    private String situacaoCampo(DiligenciaCertidaoTipo tipo,
                                 boolean insideGeofence) {
        if (tipo == DiligenciaCertidaoTipo.CUMPRIMENTO_POSITIVO) {
            return "RESULTADO_POSITIVO_CONTROLADO";
        }
        if (tipo == DiligenciaCertidaoTipo.TENTATIVA_FRUSTRADA) {
            return "RESULTADO_NEGATIVO_CONTROLADO";
        }
        return insideGeofence ? "PRESENCA_CONFIRMADA" : "PRESENCA_NAO_CONFIRMADA";
    }

    private String nv(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
