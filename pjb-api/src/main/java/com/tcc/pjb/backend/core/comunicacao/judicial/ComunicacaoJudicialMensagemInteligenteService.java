package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;

@Service
public class ComunicacaoJudicialMensagemInteligenteService {

    public record MensagemPortal(String titulo,
                                 String corpo,
                                 String resumoCurto,
                                 String hashMensagem) {
    }

    public record MensagemChat(String corpo,
                               String resumoCurto,
                               String hashMensagem) {
    }

    private record PrazoSintese(NationalPrazoEngine.TipoPrazo tipoPrazo,
                                LocalDate inicio,
                                LocalDate vencimento,
                                boolean estimado,
                                boolean emDiasUteis,
                                int diasBase,
                                String fundamento) {
    }

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

    private final MatrizComunicacaoJudicialResolver matrizResolver;
    private final NationalPrazoEngine prazoEngine;
    private final ObjectProvider<PrazoRespostaPosEntregaEngine> prazoRespostaProvider;

    public ComunicacaoJudicialMensagemInteligenteService(MatrizComunicacaoJudicialResolver matrizResolver,
                                                         NationalPrazoEngine prazoEngine,
                                                         ObjectProvider<PrazoRespostaPosEntregaEngine> prazoRespostaProvider) {
        this.matrizResolver = Objects.requireNonNull(matrizResolver, "matrizResolver");
        this.prazoEngine = Objects.requireNonNull(prazoEngine, "prazoEngine");
        this.prazoRespostaProvider = Objects.requireNonNull(prazoRespostaProvider, "prazoRespostaProvider");
    }

    public MensagemPortal construirPortal(ExpedicaoJudicial expedicao,
                                          Processo processo,
                                          ComunicacaoJudicialPortalNotificationService.EventoPortal evento,
                                          boolean representante) {
        Objects.requireNonNull(expedicao, "expedicao");
        Objects.requireNonNull(evento, "evento");
        ProceduralCommunicationDecision decisao = resolverDecisao(expedicao, processo);
        PrazoSintese prazo = resolverPrazo(expedicao, processo, evento, decisao);
        String numero = numeroProcesso(expedicao, processo);
        String tipo = tipoComunicacao(expedicao);
        String titulo = construirTituloPortal(numero, tipo, evento, representante);
        String corpo = juntarSentencas(
                cabecalhoPortal(numero, tipo, evento, representante),
                sintetizarContexto(processo, decisao),
                sintetizarMaterializacao(expedicao, decisao),
                sintetizarPrazo(prazo, evento, representante),
                sintetizarOrientacao(tipo, evento, representante, prazo, decisao)
        );
        String resumo = juntarSentencas(
                resumoEvento(tipo, evento, representante),
                prazo == null ? resumoSemPrazo(evento) : resumoPrazo(prazo)
        );
        return new MensagemPortal(titulo, corpo, resumo, hashMensagem(expedicao, evento, representante, corpo));
    }

    public MensagemChat construirChat(ExpedicaoJudicial expedicao,
                                      Processo processo,
                                      ComunicacaoJudicialPortalNotificationService.EventoPortal evento,
                                      boolean representante) {
        Objects.requireNonNull(expedicao, "expedicao");
        Objects.requireNonNull(evento, "evento");
        ProceduralCommunicationDecision decisao = resolverDecisao(expedicao, processo);
        PrazoSintese prazo = resolverPrazo(expedicao, processo, evento, decisao);
        String numero = numeroProcesso(expedicao, processo);
        String tipo = tipoComunicacao(expedicao);
        String corpo = juntarSentencas(
                cabecalhoChat(numero, tipo, evento, representante),
                sintetizarContexto(processo, decisao),
                sintetizarMaterializacao(expedicao, decisao),
                sintetizarPrazo(prazo, evento, representante),
                sintetizarOrientacao(tipo, evento, representante, prazo, decisao)
        );
        String resumo = resumoEvento(tipo, evento, representante);
        return new MensagemChat(corpo, resumo, hashMensagem(expedicao, evento, representante, corpo));
    }

    private ProceduralCommunicationDecision resolverDecisao(ExpedicaoJudicial expedicao, Processo processo) {
        return matrizResolver.resolver(processo, expedicao.getTipoComunicacao(), reconstruirPerfil(expedicao));
    }

    private PrazoSintese resolverPrazo(ExpedicaoJudicial expedicao,
                                       Processo processo,
                                       ComunicacaoJudicialPortalNotificationService.EventoPortal evento,
                                       ProceduralCommunicationDecision decisao) {
        PrazoRespostaPosEntregaEngine prazoResposta = prazoRespostaProvider.getIfAvailable();
        if (prazoResposta != null) {
            PrazoRespostaPosEntregaEngine.PrazoResposta existente = prazoResposta.consultarPorExpedicao(expedicao.getExpedicaoUuid()).orElse(null);
            if (existente != null) {
                return new PrazoSintese(
                        existente.tipoPrazo(),
                        existente.inicioEm(),
                        existente.vencimentoEm(),
                        false,
                        existente.emDiasUteis(),
                        existente.diasTotais(),
                        existente.fundamentoLegal()
                );
            }
        }
        if (evento == ComunicacaoJudicialPortalNotificationService.EventoPortal.EXPEDIDA) {
            return null;
        }
        Instant marco = expedicao.getPrazoRespostaInicioEm();
        if (marco == null || decisao == null || decisao.tipoPrazo() == null) {
            return null;
        }
        LocalDate inicio = marco.atZone(ZONE).toLocalDate();
        RamoDireito ramo = processo != null && processo.getRamoDireito() != null
                ? processo.getRamoDireito()
                : safeRamo(expedicao.getRamoDireito());
        GrauJurisdicao grau = processo != null && processo.getJurisdicao() != null && processo.getJurisdicao().getGrau() != null
                ? processo.getJurisdicao().getGrau()
                : safeGrau(expedicao.getGrauJurisdicao());
        String tribunalCodigo = tribunalCodigo(processo, expedicao);
        NationalPrazoEngine.PrazoCalculado calculado = prazoEngine.calcular(
                inicio,
                decisao.tipoPrazo(),
                ramo,
                grau,
                tribunalCodigo
        );
        int multiplicador = multiplicador(expedicao.getTipoDestinatario());
        LocalDate vencimento = calculado.vencimento();
        if (multiplicador > 1 && decisao.tipoPrazo().diasPadrao > 0) {
            int diasBase = Math.max(0, (int) ChronoUnit.DAYS.between(inicio, calculado.vencimento()));
            int diasRecalculados = diasBase * multiplicador;
            vencimento = inicio.plusDays(diasRecalculados);
            if (calculado.tipo().emDiasUteis) {
                vencimento = moverParaDiaUtil(vencimento, ramo);
            }
        }
        return new PrazoSintese(
                decisao.tipoPrazo(),
                inicio,
                vencimento,
                true,
                calculado.tipo().emDiasUteis,
                calculado.tipo().diasPadrao * multiplicador,
                calculado.fundamentoLegal()
        );
    }

    private int multiplicador(ExpedicaoJudicial.TipoDestinatario tipoDestinatario) {
        if (tipoDestinatario == null) {
            return 1;
        }
        return switch (tipoDestinatario) {
            case FAZENDA_PUBLICA, DEFENSOR_PUBLICO -> 2;
            default -> 1;
        };
    }

    private String construirTituloPortal(String numero,
                                         String tipo,
                                         ComunicacaoJudicialPortalNotificationService.EventoPortal evento,
                                         boolean representante) {
        if (representante) {
            return switch (evento) {
                case EXPEDIDA -> "Cliente representado com " + tipo + " no processo " + numero;
                case ENTREGUE_CONFIRMADA -> "Entrega confirmada de " + tipo + " do cliente no processo " + numero;
                case LIDA_CONFIRMADA -> "Ciência confirmada de " + tipo + " do cliente no processo " + numero;
                case PRESUMIDA_ENTREGUE -> "Presunção de entrega de " + tipo + " do cliente no processo " + numero;
                case PUBLICADA_EDITAL -> "Edital ligado ao cliente representado no processo " + numero;
            };
        }
        return switch (evento) {
            case EXPEDIDA -> "Você recebeu " + tipo + " no processo " + numero;
            case ENTREGUE_CONFIRMADA -> "Entrega confirmada da " + tipo + " no processo " + numero;
            case LIDA_CONFIRMADA -> "Ciência confirmada da " + tipo + " no processo " + numero;
            case PRESUMIDA_ENTREGUE -> "Presunção de entrega da " + tipo + " no processo " + numero;
            case PUBLICADA_EDITAL -> "Edital vinculado à " + tipo + " no processo " + numero;
        };
    }

    private String cabecalhoPortal(String numero,
                                   String tipo,
                                   ComunicacaoJudicialPortalNotificationService.EventoPortal evento,
                                   boolean representante) {
        if (representante) {
            return switch (evento) {
                case EXPEDIDA -> "O PJB registrou nova " + tipo + " para cliente representado por você no processo " + numero + ".";
                case ENTREGUE_CONFIRMADA -> "O PJB confirmou a entrega da " + tipo + " do cliente representado no processo " + numero + ".";
                case LIDA_CONFIRMADA -> "O PJB confirmou a ciência da " + tipo + " do cliente representado no processo " + numero + ".";
                case PRESUMIDA_ENTREGUE -> "O PJB aplicou a presunção legal de entrega da " + tipo + " do cliente representado no processo " + numero + ".";
                case PUBLICADA_EDITAL -> "O PJB registrou a evolução da comunicação para edital no processo " + numero + ".";
            };
        }
        return switch (evento) {
            case EXPEDIDA -> "O PJB registrou nova " + tipo + " ligada ao seu cadastro no processo " + numero + ".";
            case ENTREGUE_CONFIRMADA -> "O PJB confirmou a entrega da " + tipo + " no processo " + numero + ".";
            case LIDA_CONFIRMADA -> "O PJB confirmou a sua ciência da " + tipo + " no processo " + numero + ".";
            case PRESUMIDA_ENTREGUE -> "O PJB reconheceu a presunção legal de entrega da " + tipo + " no processo " + numero + ".";
            case PUBLICADA_EDITAL -> "O PJB registrou publicação editalícia relacionada à " + tipo + " no processo " + numero + ".";
        };
    }

    private String cabecalhoChat(String numero,
                                 String tipo,
                                 ComunicacaoJudicialPortalNotificationService.EventoPortal evento,
                                 boolean representante) {
        if (representante) {
            return switch (evento) {
                case EXPEDIDA -> "PJB informa: houve nova " + tipo + " para cliente representado no processo " + numero + ".";
                case ENTREGUE_CONFIRMADA -> "PJB informa: a entrega da " + tipo + " do cliente foi confirmada no processo " + numero + ".";
                case LIDA_CONFIRMADA -> "PJB informa: a ciência da " + tipo + " do cliente foi confirmada no processo " + numero + ".";
                case PRESUMIDA_ENTREGUE -> "PJB informa: a " + tipo + " do cliente atingiu presunção legal de entrega no processo " + numero + ".";
                case PUBLICADA_EDITAL -> "PJB informa: a comunicação do processo " + numero + " passou a tramitar por edital.";
            };
        }
        return switch (evento) {
            case EXPEDIDA -> "PJB informa: há nova " + tipo + " vinculada ao processo " + numero + ".";
            case ENTREGUE_CONFIRMADA -> "PJB informa: a entrega da " + tipo + " foi confirmada no processo " + numero + ".";
            case LIDA_CONFIRMADA -> "PJB informa: a sua ciência da " + tipo + " foi confirmada no processo " + numero + ".";
            case PRESUMIDA_ENTREGUE -> "PJB informa: a " + tipo + " atingiu presunção legal de entrega no processo " + numero + ".";
            case PUBLICADA_EDITAL -> "PJB informa: a comunicação do processo " + numero + " passou a seguir por edital.";
        };
    }

    private String sintetizarContexto(Processo processo, ProceduralCommunicationDecision decisao) {
        List<String> partes = new ArrayList<>();
        if (processo != null && processo.getRito() != null) {
            partes.add("rito " + humanizarEnum(processo.getRito().name()));
        }
        if (processo != null && processo.getFaseAtual() != null) {
            partes.add("fase " + humanizarEnum(processo.getFaseAtual().name()));
        }
        if (processo != null && processo.getClasseProcessual() != null && !processo.getClasseProcessual().isBlank()) {
            partes.add("classe " + processo.getClasseProcessual().trim());
        }
        if (processo != null && processo.getObjetoProcessual() != null && !processo.getObjetoProcessual().isBlank()) {
            partes.add("objeto " + resumoLivre(processo.getObjetoProcessual(), 96));
        }
        if (partes.isEmpty() && decisao != null && decisao.eixoProcedimental() != null && !decisao.eixoProcedimental().isBlank()) {
            partes.add("eixo procedimental " + decisao.eixoProcedimental().replace('/', ' '));
        }
        if (partes.isEmpty()) {
            return null;
        }
        return "Contexto processual: " + String.join("; ", partes) + ".";
    }

    private String sintetizarMaterializacao(ExpedicaoJudicial expedicao, ProceduralCommunicationDecision decisao) {
        String materializacao = materializacao(decisao);
        String modalidade = expedicao.getModalidade() != null ? expedicao.getModalidade().getLabel() : "modalidade não definida";
        if (materializacao == null) {
            return "Forma de ciência monitorada pelo PJB: " + modalidade + ".";
        }
        return "Forma de ciência priorizada: " + materializacao + " por " + modalidade + ".";
    }

    private String sintetizarPrazo(PrazoSintese prazo,
                                   ComunicacaoJudicialPortalNotificationService.EventoPortal evento,
                                   boolean representante) {
        if (prazo == null) {
            if (evento == ComunicacaoJudicialPortalNotificationService.EventoPortal.EXPEDIDA) {
                return representante
                        ? "O prazo processual ainda não foi iniciado; ele nasce com a ciência válida do ato pelo cliente ou por seu representante legalmente apto."
                        : "O prazo processual ainda não foi iniciado; ele começa com a ciência válida do ato no PJB ou pela forma legal correspondente.";
            }
            return "O PJB ainda está consolidando a data final do prazo; acompanhe o painel do processo para a fixação definitiva.";
        }
        if (prazo.tipoPrazo().diasPadrao == 0) {
            return "Prazo identificado: " + humanizarPrazo(prazo.tipoPrazo()) + ". Nesta hipótese o sistema trata o ato como resposta sem contagem automática padronizada, exigindo atenção imediata ao painel e ao comando judicial específico.";
        }
        String modo = prazo.emDiasUteis() ? "em dias úteis" : "em dias corridos";
        String origem = prazo.estimado() ? "estimado" : "apurado";
        String fundamento = prazo.fundamento() != null && !prazo.fundamento().isBlank()
                ? " Fundamento-base: " + resumoLivre(prazo.fundamento(), 140) + "."
                : "";
        return "Prazo identificado: " + humanizarPrazo(prazo.tipoPrazo())
                + ", com início em " + formatarData(prazo.inicio())
                + " e vencimento " + origem + " para " + formatarData(prazo.vencimento())
                + ", contado " + modo
                + "."
                + fundamento;
    }

    private String sintetizarOrientacao(String tipo,
                                        ComunicacaoJudicialPortalNotificationService.EventoPortal evento,
                                        boolean representante,
                                        PrazoSintese prazo,
                                        ProceduralCommunicationDecision decisao) {
        if (evento == ComunicacaoJudicialPortalNotificationService.EventoPortal.PUBLICADA_EDITAL) {
            return representante
                    ? "Providência sugerida: conferir imediatamente a publicação, validar a regularidade da representação e preparar a resposta processual cabível sem aguardar nova lembrança."
                    : "Providência sugerida: abrir o painel do processo, ler o ato integralmente e providenciar orientação jurídica imediata, porque a comunicação já está em estágio editalício.";
        }
        if (evento == ComunicacaoJudicialPortalNotificationService.EventoPortal.EXPEDIDA) {
            return representante
                    ? "Providência sugerida: revisar a íntegra do ato, alinhar a estratégia com o cliente e manter a pasta recursal pronta para a confirmação da ciência."
                    : "Providência sugerida: acessar a íntegra do ato no PJB e verificar se haverá confirmação de entrega, leitura ou outra forma legal de ciência.";
        }
        if (prazo != null && prazo.vencimento() != null && prazo.tipoPrazo().diasPadrao > 0) {
            return representante
                    ? "Providência sugerida: tratar o vencimento de " + formatarData(prazo.vencimento()) + " como marco operacional prioritário, salvo superveniência de suspensão, prazo em dobro ou determinação judicial mais específica."
                    : "Providência sugerida: considerar o vencimento de " + formatarData(prazo.vencimento()) + " como marco central do ato e buscar orientação jurídica antes do último dia útil.";
        }
        if (decisao != null && decisao.bloquearPresuncao()) {
            return "Providência sugerida: este fluxo possui sensibilidade processual reforçada; acompanhe cada atualização no painel e não presuma regularidade sem leitura integral do ato.";
        }
        return representante
                ? "Providência sugerida: acompanhe o painel de comunicações, o módulo de prazos e a trilha do chat para não perder a janela processual seguinte."
                : "Providência sugerida: acompanhe o painel de comunicações e leia a íntegra do ato para entender a próxima providência processual.";
    }

    private String resumoEvento(String tipo,
                                ComunicacaoJudicialPortalNotificationService.EventoPortal evento,
                                boolean representante) {
        if (representante) {
            return switch (evento) {
                case EXPEDIDA -> "Nova " + tipo + " do cliente representado registrada.";
                case ENTREGUE_CONFIRMADA -> "Entrega da " + tipo + " do cliente confirmada.";
                case LIDA_CONFIRMADA -> "Ciência da " + tipo + " do cliente confirmada.";
                case PRESUMIDA_ENTREGUE -> "Presunção de entrega da " + tipo + " do cliente aplicada.";
                case PUBLICADA_EDITAL -> "Edital relacionado ao cliente representado publicado.";
            };
        }
        return switch (evento) {
            case EXPEDIDA -> "Nova " + tipo + " registrada no seu cadastro.";
            case ENTREGUE_CONFIRMADA -> "Entrega da " + tipo + " confirmada.";
            case LIDA_CONFIRMADA -> "Ciência da " + tipo + " confirmada.";
            case PRESUMIDA_ENTREGUE -> "Presunção de entrega da " + tipo + " aplicada.";
            case PUBLICADA_EDITAL -> "Edital relacionado à " + tipo + " publicado.";
        };
    }

    private String resumoSemPrazo(ComunicacaoJudicialPortalNotificationService.EventoPortal evento) {
        if (evento == ComunicacaoJudicialPortalNotificationService.EventoPortal.EXPEDIDA) {
            return "Prazo ainda não iniciado.";
        }
        return "Prazo em consolidação no painel.";
    }

    private String resumoPrazo(PrazoSintese prazo) {
        if (prazo.tipoPrazo().diasPadrao == 0) {
            return "Sem contagem automática padronizada.";
        }
        return "Vencimento " + formatarData(prazo.vencimento()) + ".";
    }

    private CitacaoIntimacaoEngine.PerfilDestinatario reconstruirPerfil(ExpedicaoJudicial expedicao) {
        return switch (expedicao.getTipoDestinatario()) {
            case PESSOA_FISICA -> new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica(
                    expedicao.getDestinatarioDocumento(),
                    expedicao.getDestinatarioNome(),
                    null,
                    expedicao.getDestinatarioEmail(),
                    expedicao.getDestinatarioTelefone(),
                    expedicao.getModalidade() == ModalidadeExpedicaoJudicial.DIGITAL_GOVBR_PUSH,
                    expedicao.getModalidade() == ModalidadeExpedicaoJudicial.DIGITAL_DOMICILIO_ELETRONICO_MNI
            );
            case PESSOA_JURIDICA -> new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaJuridica(
                    expedicao.getDestinatarioDocumento(),
                    expedicao.getDestinatarioNome(),
                    expedicao.getDestinatarioEmail(),
                    expedicao.getDestinatarioTelefone(),
                    expedicao.getDestinatarioEnderecoEntrega(),
                    expedicao.getModalidade() == ModalidadeExpedicaoJudicial.PORTAL_EMPRESA_CNPJ,
                    false,
                    false,
                    false,
                    true
            );
            case ADVOGADO_OAB -> new CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab(
                    expedicao.getDestinatarioDocumento(),
                    expedicao.getDestinatarioNome(),
                    null,
                    expedicao.getDestinatarioEmail(),
                    expedicao.getModalidade() == ModalidadeExpedicaoJudicial.DIGITAL_DOMICILIO_ELETRONICO_MNI,
                    "PJB"
            );
            case DEFENSOR_PUBLICO -> new CitacaoIntimacaoEngine.PerfilDestinatario.DefensorPublico(
                    expedicao.getDestinatarioDocumento(),
                    expedicao.getDestinatarioNome(),
                    expedicao.getDestinatarioEmail()
            );
            case MINISTERIO_PUBLICO -> new CitacaoIntimacaoEngine.PerfilDestinatario.MinisterioPublico(
                    expedicao.getDestinatarioDocumento(),
                    expedicao.getDestinatarioNome(),
                    expedicao.getDestinatarioEmail()
            );
            case FAZENDA_PUBLICA -> new CitacaoIntimacaoEngine.PerfilDestinatario.FazendaPublica(
                    expedicao.getDestinatarioDocumento(),
                    expedicao.getDestinatarioNome(),
                    expedicao.getDestinatarioEmail(),
                    expedicao.getInstanciaExpedidora()
            );
            case JUIZO_DEPRECADO -> new CitacaoIntimacaoEngine.PerfilDestinatario.JuizoDeprecado(
                    expedicao.getInstanciaExpedidora(),
                    expedicao.getDestinatarioNome(),
                    null,
                    expedicao.getDestinatarioEmail()
            );
        };
    }

    private String numeroProcesso(ExpedicaoJudicial expedicao, Processo processo) {
        if (processo != null && processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()) {
            return processo.getNumeroUnificado();
        }
        if (expedicao.getNumeroUnificado() != null && !expedicao.getNumeroUnificado().isBlank()) {
            return expedicao.getNumeroUnificado();
        }
        return String.valueOf(expedicao.getProcessoId());
    }

    private String materializacao(ProceduralCommunicationDecision decisao) {
        if (decisao == null || decisao.marcadores() == null) {
            return null;
        }
        for (String marcador : decisao.marcadores()) {
            if (!marcador.startsWith("materializacao=")) {
                continue;
            }
            String valor = marcador.substring("materializacao=".length());
            return switch (valor) {
                case "representante_digital" -> "ciência por representante habilitado em trilha digital";
                case "oficial_justica" -> "cumprimento presencial por oficial de justiça";
                case "digital_direta" -> "ciência digital direta pelo PJB";
                case "hora_certa" -> "hora certa como trilha subsidiária";
                case "cooperacao_externa" -> "cooperação judicial especializada";
                case "executiva" -> "execução ou cumprimento com reforço patrimonial e coercitivo";
                default -> humanizarEnum(valor);
            };
        }
        return null;
    }

    private String tipoComunicacao(ExpedicaoJudicial expedicao) {
        TipoComunicacaoJudicial tipo = expedicao.getTipoComunicacao();
        if (tipo == null) {
            return "comunicação judicial";
        }
        if (tipo.isCitacao()) {
            return "citação";
        }
        if (tipo.isIntimacao()) {
            return "intimação";
        }
        if (tipo.isMandado()) {
            return "mandado";
        }
        if (tipo.isEdital()) {
            return "edital";
        }
        return tipo.getDescricao() != null && !tipo.getDescricao().isBlank()
                ? tipo.getDescricao().toLowerCase(Locale.ROOT)
                : humanizarEnum(tipo.name());
    }

    private String humanizarPrazo(NationalPrazoEngine.TipoPrazo tipoPrazo) {
        return humanizarEnum(tipoPrazo.name());
    }

    private String humanizarEnum(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        return normalized;
    }

    private String resumoLivre(String source, int limite) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String compacta = source.trim().replaceAll("\\s+", " ");
        if (compacta.length() <= limite) {
            return compacta;
        }
        return compacta.substring(0, Math.max(0, limite - 1)).trim() + "…";
    }

    private String juntarSentencas(String... partes) {
        List<String> validas = new ArrayList<>();
        for (String parte : partes) {
            if (parte != null && !parte.isBlank()) {
                validas.add(parte.trim());
            }
        }
        return validas.isEmpty() ? "" : String.join(" ", validas);
    }

    private String formatarData(LocalDate data) {
        return data == null ? "não definida" : DATA.format(data);
    }

    private String hashMensagem(ExpedicaoJudicial expedicao,
                                ComunicacaoJudicialPortalNotificationService.EventoPortal evento,
                                boolean representante,
                                String corpo) {
        return Integer.toHexString(Objects.hash(
                expedicao.getExpedicaoUuid(),
                evento,
                representante,
                corpo,
                expedicao.getStatus(),
                expedicao.getHashIntegridade()
        ));
    }

    private String tribunalCodigo(Processo processo, ExpedicaoJudicial expedicao) {
        if (processo != null) {
            Jurisdicao jurisdicao = processo.getJurisdicao();
            if (jurisdicao != null && jurisdicao.getCodigo() != null && !jurisdicao.getCodigo().isBlank()) {
                return jurisdicao.getCodigo();
            }
            if (processo.getTribunalCodigoRoteado() != null && !processo.getTribunalCodigoRoteado().isBlank()) {
                return processo.getTribunalCodigoRoteado();
            }
        }
        return expedicao.getInstanciaExpedidora();
    }

    private RamoDireito safeRamo(String value) {
        try {
            return value == null || value.isBlank() ? RamoDireito.CIVIL : RamoDireito.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return RamoDireito.CIVIL;
        }
    }

    private GrauJurisdicao safeGrau(String value) {
        try {
            return value == null || value.isBlank() ? GrauJurisdicao.PRIMEIRO_GRAU : GrauJurisdicao.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return GrauJurisdicao.PRIMEIRO_GRAU;
        }
    }

    private LocalDate moverParaDiaUtil(LocalDate data, RamoDireito ramo) {
        LocalDate cursor = data;
        while (!prazoEngine.ehDiaUtil(cursor, java.util.Set.of(), ramo)) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }
}
