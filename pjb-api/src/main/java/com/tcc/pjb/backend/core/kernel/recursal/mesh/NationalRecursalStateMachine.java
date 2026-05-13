package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracao;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;

public final class NationalRecursalStateMachine {

    public RecursalStateSnapshot initialSnapshot(String recursoId, RecursalCaseContext context, RecursalSpecies species, RecursalRoutePlan routePlan) {
        return RecursalStateSnapshot.newDraft(recursoId, context);
    }

    public RecursalStateSnapshot transition(RecursalTransitionCommand command, RecursalRoutePlan routePlan) {
        RecursalStateSnapshot snapshot = command.snapshot();
        RecursalLifecycleState nextState = nextState(command, routePlan);
        RecursalTribunal tribunalAtual = snapshot.tribunalAtual();
        RecursalTribunalDetalhado tribunalDetalhadoAtual = snapshot.tribunalDetalhadoAtual();
        InstanceLevel instanciaAtual = snapshot.instanciaAtual();
        RecursalAuthority autoridadeAtual = snapshot.autoridadeAtual();
        boolean preparoSatisfeito = snapshot.preparoSatisfeito();
        boolean admissibilidadePositiva = snapshot.admissibilidadePositiva();
        boolean remetido = snapshot.remetido();
        boolean autuadoDestino = snapshot.autuadoDestino();
        boolean distribuidoDestino = snapshot.distribuidoDestino();
        boolean preparoEmComplementacao = snapshot.preparoEmComplementacao();
        boolean diligenciaPendente = snapshot.diligenciaPendente();
        boolean multaEmbargosProtelatoriosAplicada = snapshot.multaEmbargosProtelatoriosAplicada();
        boolean sobrestadoPorPrecedente = snapshot.sobrestadoPorPrecedente();
        boolean efeitoSuspensivoAtivo = snapshot.efeitoSuspensivoAtivo();
        boolean efeitoAtivoConcedido = snapshot.efeitoAtivoConcedido();
        boolean conhecimentoParcial = snapshot.conhecimentoParcial();
        boolean remessaNecessaria = snapshot.remessaNecessaria();
        boolean requisicaoPublicaPagamento = snapshot.requisicaoPublicaPagamento();
        int iteracoesEmbargosDeclaracao = snapshot.iteracoesEmbargosDeclaracao();
        RecursalRemessaTrace remessaTrace = snapshot.remessaTrace();
        RecursalSustentacaoOralTrace sustentacaoOralTrace = snapshot.sustentacaoOralTrace();
        RecursalPrecedentTrace precedentTrace = snapshot.precedentTrace();
        RecursalCompetenciaTrace competenciaTrace = snapshot.competenciaTrace();
        RecursalPublicPaymentTrace publicPaymentTrace = snapshot.publicPaymentTrace();
        Instant occurredAt = command.occurredAt();

        switch (command.event()) {
            case PROTOCOLAR -> {
                tribunalAtual = command.context().tribunalOrigem();
                tribunalDetalhadoAtual = command.context().tribunalDetalhadoOrigem();
                instanciaAtual = command.context().instanciaAtual();
                autoridadeAtual = command.context().autoridadeAtual();
                remessaNecessaria = command.context().remessaNecessaria();
                requisicaoPublicaPagamento = command.context().demandaRequisicaoPublicaPagamento();
                if (command.species() instanceof ConflitoCompetencia) {
                    competenciaTrace = competenciaTrace.iniciar(command.details());
                }
                if (requisicaoPublicaPagamento) {
                    publicPaymentTrace = publicPaymentTrace.ativar();
                }
            }
            case DISPENSAR_PREPARO, REGISTRAR_PREPARO, COMPLEMENTAR_PREPARO -> {
                preparoSatisfeito = true;
                preparoEmComplementacao = false;
            }
            case INTIMAR_COMPLEMENTACAO_PREPARO -> preparoEmComplementacao = true;
            case RECONHECER_DESERCAO_INSANAVEL, DECLARAR_DESERCAO -> {
                preparoEmComplementacao = false;
                preparoSatisfeito = false;
            }
            case ADMITIR -> {
                admissibilidadePositiva = true;
                autoridadeAtual = nextMeritOrAdmissibilityAuthority(routePlan, nextState);
                if (routePlan.remessa().externa() && nextState == RecursalLifecycleState.REMESSA_EM_CURSO) {
                    remetido = true;
                }
            }
            case NEGAR_SEGUIMENTO -> admissibilidadePositiva = false;
            case APLICAR_PRECEDENTE -> {
                admissibilidadePositiva = false;
                precedentTrace = precedentTrace.aplicar(command.details(), occurredAt);
            }
            case REMETER, REGISTRAR_SAIDA_AUTOS -> {
                remetido = true;
                autoridadeAtual = RecursalAuthority.SECRETARIA_JUDICIARIA;
                remessaTrace = remessaTrace.registrarSaida(command.details(), occurredAt);
            }
            case CONFIRMAR_RECEBIMENTO, AUTUAR_DESTINO -> {
                tribunalAtual = routePlan.tribunalDestino();
                tribunalDetalhadoAtual = routePlan.tribunalDetalhadoDestino();
                instanciaAtual = routePlan.instanciaDestino();
                autoridadeAtual = RecursalAuthority.SECRETARIA_JUDICIARIA;
                autuadoDestino = true;
                remetido = true;
                remessaTrace = remessaTrace.confirmarRecebimento(command.details(), occurredAt);
            }
            case DEVOLVER_REMESSA -> {
                autoridadeAtual = originAuthority(routePlan, command.context());
                remessaTrace = remessaTrace.devolver(command.details(), occurredAt);
            }
            case DISTRIBUIR -> {
                tribunalAtual = routePlan.tribunalDestino();
                tribunalDetalhadoAtual = routePlan.tribunalDetalhadoDestino();
                instanciaAtual = routePlan.instanciaDestino();
                autoridadeAtual = RecursalAuthority.SECRETARIA_JUDICIARIA;
                distribuidoDestino = true;
            }
            case AFETAR_ORGAO_JULGADOR -> autoridadeAtual = nextMeritOrAdmissibilityAuthority(routePlan, nextState);
            case PEDIR_PAUTA_SUSTENTACAO -> {
                autoridadeAtual = nextMeritOrAdmissibilityAuthority(routePlan, nextState);
                sustentacaoOralTrace = sustentacaoOralTrace.solicitar(command.details(), occurredAt);
            }
            case ADIAR_SESSAO -> {
                autoridadeAtual = nextMeritOrAdmissibilityAuthority(routePlan, nextState);
                sustentacaoOralTrace = sustentacaoOralTrace.adiar(command.details(), occurredAt);
            }
            case SUSTENTAR -> {
                autoridadeAtual = nextMeritOrAdmissibilityAuthority(routePlan, nextState);
                sustentacaoOralTrace = sustentacaoOralTrace.sustentar(command.details(), occurredAt);
            }
            case DISPENSAR_SUSTENTACAO -> {
                autoridadeAtual = nextMeritOrAdmissibilityAuthority(routePlan, nextState);
                sustentacaoOralTrace = sustentacaoOralTrace.dispensar(command.details(), occurredAt);
            }
            case DISTINGUIR_CASO -> {
                autoridadeAtual = nextMeritOrAdmissibilityAuthority(routePlan, nextState);
                precedentTrace = precedentTrace.distinguir(command.details(), occurredAt);
            }
            case RECEBER_SUSCITADO -> {
                autoridadeAtual = nextMeritOrAdmissibilityAuthority(routePlan, nextState);
                competenciaTrace = competenciaTrace.receberSuscitado(command.details(), occurredAt);
            }
            case DEFINIR_COMPETENCIA -> {
                autoridadeAtual = nextMeritOrAdmissibilityAuthority(routePlan, nextState);
                competenciaTrace = competenciaTrace.definir(command.details(), occurredAt);
            }
            case REMETER_AUTOS_JUIZO_COMPETENTE -> {
                autoridadeAtual = RecursalAuthority.SECRETARIA_JUDICIARIA;
                competenciaTrace = competenciaTrace.remeterAoJuizoCompetente(command.details(), occurredAt);
            }
            case CONCEDER_EFEITO_SUSPENSIVO -> efeitoSuspensivoAtivo = true;
            case REVOGAR_EFEITO_SUSPENSIVO -> efeitoSuspensivoAtivo = false;
            case CONCEDER_EFEITO_ATIVO -> efeitoAtivoConcedido = true;
            case REVOGAR_EFEITO_ATIVO -> efeitoAtivoConcedido = false;
            case RETRATAR, RETRATAR_POR_PRECEDENTE -> autoridadeAtual = originAuthority(routePlan, command.context());
            case SOBRESTAR -> sobrestadoPorPrecedente = false;
            case SOBRESTAR_POR_PRECEDENTE -> {
                sobrestadoPorPrecedente = true;
                precedentTrace = precedentTrace.sobrestar(command.details());
            }
            case RETOMAR -> {
                sobrestadoPorPrecedente = false;
                precedentTrace = precedentTrace.retomar(command.details(), occurredAt);
            }
            case DETERMINAR_DILIGENCIA -> diligenciaPendente = true;
            case CUMPRIR_DILIGENCIA -> diligenciaPendente = false;
            case APLICAR_MULTA_EMBARGOS_PROTELATORIOS -> multaEmbargosProtelatoriosAplicada = true;
            case ACOLHER_EMBARGOS, REJEITAR_EMBARGOS -> {
                if (command.species() instanceof EmbargosDeclaracao) {
                    iteracoesEmbargosDeclaracao = snapshot.iteracoesEmbargosDeclaracao() + 1;
                }
            }
            case CONHECER_PARCIALMENTE, PROVER_PARCIALMENTE -> conhecimentoParcial = true;
            case EXPEDIR_RPV -> {
                autoridadeAtual = RecursalAuthority.SECRETARIA_JUDICIARIA;
                publicPaymentTrace = publicPaymentTrace.expedir(command.details(), "RPV", occurredAt);
            }
            case EXPEDIR_PRECATORIO -> {
                autoridadeAtual = RecursalAuthority.SECRETARIA_JUDICIARIA;
                publicPaymentTrace = publicPaymentTrace.expedir(command.details(), "PRECATORIO", occurredAt);
            }
            case REGISTRAR_PAGAMENTO_PUBLICO -> {
                autoridadeAtual = RecursalAuthority.SECRETARIA_JUDICIARIA;
                publicPaymentTrace = publicPaymentTrace.liberar(command.details(), occurredAt);
            }
            case VALIDAR_TEMPESTIVIDADE, INTIMAR_CONTRARRAZOES, ENCERRAR_CONTRARRAZOES,
                    ENCAMINHAR_ADMISSIBILIDADE, PROVER, NEGAR_PROVIMENTO,
                    NAO_CONHECER, JULGAR_PREJUDICADO, BAIXAR, CERTIFICAR_TRANSITO -> {
            }
        }

        if (nextState == RecursalLifecycleState.AGUARDANDO_PRECATORIO
                || nextState == RecursalLifecycleState.AGUARDANDO_REQUISICAO_PAGAMENTO_PUBLICO) {
            autoridadeAtual = RecursalAuthority.SECRETARIA_JUDICIARIA;
        }

        return snapshot.advance(
                nextState,
                tribunalAtual,
                tribunalDetalhadoAtual,
                instanciaAtual,
                autoridadeAtual,
                preparoSatisfeito,
                admissibilidadePositiva,
                remetido,
                autuadoDestino,
                distribuidoDestino,
                preparoEmComplementacao,
                diligenciaPendente,
                multaEmbargosProtelatoriosAplicada,
                sobrestadoPorPrecedente,
                efeitoSuspensivoAtivo,
                efeitoAtivoConcedido,
                conhecimentoParcial,
                iteracoesEmbargosDeclaracao,
                remessaNecessaria,
                requisicaoPublicaPagamento,
                remessaTrace,
                sustentacaoOralTrace,
                precedentTrace,
                competenciaTrace,
                publicPaymentTrace,
                occurredAt
        );
    }

    public Set<RecursalTransitionEvent> availableEvents(RecursalStateSnapshot snapshot, RecursalSpecies species, RecursalRoutePlan routePlan) {
        LinkedHashSet<RecursalTransitionEvent> events = new LinkedHashSet<>();
        switch (snapshot.state()) {
            case RASCUNHO -> events.add(RecursalTransitionEvent.PROTOCOLAR);
            case INTERPOSTO -> events.add(RecursalTransitionEvent.VALIDAR_TEMPESTIVIDADE);
            case EM_SANEAMENTO_FORMAL -> {
                if (requiresPreparo(snapshot, routePlan)) {
                    events.add(RecursalTransitionEvent.REGISTRAR_PREPARO);
                    events.add(RecursalTransitionEvent.INTIMAR_COMPLEMENTACAO_PREPARO);
                    if (routePlan.preparo().desercaoPossivel()) {
                        events.add(RecursalTransitionEvent.DECLARAR_DESERCAO);
                        events.add(RecursalTransitionEvent.RECONHECER_DESERCAO_INSANAVEL);
                    }
                } else {
                    events.add(RecursalTransitionEvent.DISPENSAR_PREPARO);
                }
            }
            case PREPARO_EM_COMPLEMENTACAO -> {
                events.add(RecursalTransitionEvent.COMPLEMENTAR_PREPARO);
                if (routePlan.preparo().desercaoPossivel()) {
                    events.add(RecursalTransitionEvent.DECLARAR_DESERCAO);
                }
            }
            case PREPARO_CERTIFICADO -> {
                if (requiresCounterReasons(snapshot, species)) {
                    events.add(RecursalTransitionEvent.INTIMAR_CONTRARRAZOES);
                } else {
                    events.add(RecursalTransitionEvent.ENCAMINHAR_ADMISSIBILIDADE);
                }
            }
            case AGUARDANDO_CONTRARRAZOES -> events.add(RecursalTransitionEvent.ENCERRAR_CONTRARRAZOES);
            case ADMISSIBILIDADE_ORIGEM, ADMISSIBILIDADE_DESTINO -> {
                events.add(RecursalTransitionEvent.ADMITIR);
                events.add(RecursalTransitionEvent.DETERMINAR_DILIGENCIA);
                events.add(RecursalTransitionEvent.NEGAR_SEGUIMENTO);
                if (routePlan.admissibilidade().admiteSobrestamento()) {
                    events.add(RecursalTransitionEvent.SOBRESTAR);
                    events.add(RecursalTransitionEvent.SOBRESTAR_POR_PRECEDENTE);
                }
                if (routePlan.admissibilidade().admiteRetratacao()) {
                    events.add(RecursalTransitionEvent.RETRATAR);
                    events.add(RecursalTransitionEvent.RETRATAR_POR_PRECEDENTE);
                }
            }
            case SOBRESTADO -> events.add(RecursalTransitionEvent.RETOMAR);
            case SOBRESTADO_POR_PRECEDENTE -> events.add(RecursalTransitionEvent.RETOMAR);
            case AGUARDANDO_APLICACAO_PRECEDENTE -> {
                events.add(RecursalTransitionEvent.APLICAR_PRECEDENTE);
                events.add(RecursalTransitionEvent.DISTINGUIR_CASO);
            }
            case PRECEDENTE_APLICADO, CASO_DISTINGUIDO, RETRATACAO, RETRATACAO_POR_PRECEDENTE ->
                    events.add(RecursalTransitionEvent.ENCAMINHAR_ADMISSIBILIDADE);
            case REMESSA_EM_CURSO -> {
                events.add(RecursalTransitionEvent.REGISTRAR_SAIDA_AUTOS);
                if (routePlan.remessa().autuacaoDestino()) {
                    events.add(RecursalTransitionEvent.AUTUAR_DESTINO);
                }
            }
            case AUTOS_EM_TRANSITO -> {
                events.add(RecursalTransitionEvent.CONFIRMAR_RECEBIMENTO);
                events.add(RecursalTransitionEvent.DEVOLVER_REMESSA);
            }
            case REMESSA_DEVOLVIDA -> events.add(RecursalTransitionEvent.ENCAMINHAR_ADMISSIBILIDADE);
            case AUTUADO_NO_DESTINO -> events.add(RecursalTransitionEvent.DISTRIBUIR);
            case DISTRIBUIDO_NO_DESTINO -> events.add(RecursalTransitionEvent.AFETAR_ORGAO_JULGADOR);
            case SUSCITADO -> events.add(RecursalTransitionEvent.RECEBER_SUSCITADO);
            case AGUARDANDO_RESOLUCAO_CONFLITO -> {
                events.add(RecursalTransitionEvent.DETERMINAR_DILIGENCIA);
                events.add(RecursalTransitionEvent.DEFINIR_COMPETENCIA);
            }
            case COMPETENCIA_DEFINIDA -> events.add(RecursalTransitionEvent.REMETER_AUTOS_JUIZO_COMPETENTE);
            case RETORNO_AO_JUIZO_COMPETENTE -> {
                events.add(RecursalTransitionEvent.BAIXAR);
                events.add(RecursalTransitionEvent.CERTIFICAR_TRANSITO);
            }
            case JULGAMENTO_MONOCRATICO -> outcomeEvents(species, events, snapshot);
            case JULGAMENTO_COLEGIADO -> {
                outcomeEvents(species, events, snapshot);
                if (sustentacaoOralDisponivel(snapshot, species)) {
                    events.add(RecursalTransitionEvent.PEDIR_PAUTA_SUSTENTACAO);
                    events.add(RecursalTransitionEvent.SUSTENTAR);
                }
            }
            case PAUTA_SUSTENTACAO_DESIGNADA -> {
                events.add(RecursalTransitionEvent.SUSTENTAR);
                events.add(RecursalTransitionEvent.DISPENSAR_SUSTENTACAO);
                events.add(RecursalTransitionEvent.ADIAR_SESSAO);
            }
            case SUSTENTACAO_REALIZADA -> outcomeEvents(species, events, snapshot);
            case DILIGENCIA_DETERMINADA -> events.add(RecursalTransitionEvent.CUMPRIR_DILIGENCIA);
            case AGUARDANDO_REQUISICAO_PAGAMENTO_PUBLICO -> {
                events.add(RecursalTransitionEvent.EXPEDIR_RPV);
                events.add(RecursalTransitionEvent.EXPEDIR_PRECATORIO);
            }
            case AGUARDANDO_PRECATORIO -> events.add(RecursalTransitionEvent.EXPEDIR_PRECATORIO);
            case RPV_EXPEDIDA, PRECATORIO_EXPEDIDO -> events.add(RecursalTransitionEvent.REGISTRAR_PAGAMENTO_PUBLICO);
            case PAGAMENTO_PUBLICO_LIBERADO -> {
                events.add(RecursalTransitionEvent.BAIXAR);
                events.add(RecursalTransitionEvent.CERTIFICAR_TRANSITO);
            }
            case INADMITIDO, DESERTO, INTEMPESTIVO, PROVIDO, PARCIALMENTE_PROVIDO, IMPROVIDO,
                    NAO_CONHECIDO, ACOLHIDO, REJEITADO, PREJUDICADO -> {
                if (requiresPublicPaymentFlow(snapshot)) {
                    events.add(RecursalTransitionEvent.EXPEDIR_RPV);
                    events.add(RecursalTransitionEvent.EXPEDIR_PRECATORIO);
                } else {
                    events.add(RecursalTransitionEvent.BAIXAR);
                    events.add(RecursalTransitionEvent.CERTIFICAR_TRANSITO);
                }
            }
            case BAIXADO -> events.add(RecursalTransitionEvent.CERTIFICAR_TRANSITO);
            case TRANSITADO_EM_JULGADO -> {
            }
        }
        return Set.copyOf(events);
    }

    private RecursalLifecycleState nextState(RecursalTransitionCommand command, RecursalRoutePlan routePlan) {
        return switch (command.snapshot().state()) {
            case RASCUNHO -> expect(command.event(), RecursalTransitionEvent.PROTOCOLAR, RecursalLifecycleState.INTERPOSTO);
            case INTERPOSTO -> switch (command.event()) {
                case VALIDAR_TEMPESTIVIDADE -> command.snapshot().remessaNecessaria() || command.context().tempestivo()
                        ? RecursalLifecycleState.EM_SANEAMENTO_FORMAL
                        : RecursalLifecycleState.INTEMPESTIVO;
                default -> reject(command);
            };
            case EM_SANEAMENTO_FORMAL -> switch (command.event()) {
                case DISPENSAR_PREPARO, REGISTRAR_PREPARO -> RecursalLifecycleState.PREPARO_CERTIFICADO;
                case INTIMAR_COMPLEMENTACAO_PREPARO -> RecursalLifecycleState.PREPARO_EM_COMPLEMENTACAO;
                case DECLARAR_DESERCAO, RECONHECER_DESERCAO_INSANAVEL -> RecursalLifecycleState.DESERTO;
                default -> reject(command);
            };
            case PREPARO_EM_COMPLEMENTACAO -> switch (command.event()) {
                case COMPLEMENTAR_PREPARO -> RecursalLifecycleState.PREPARO_CERTIFICADO;
                case DECLARAR_DESERCAO, RECONHECER_DESERCAO_INSANAVEL -> RecursalLifecycleState.DESERTO;
                default -> reject(command);
            };
            case PREPARO_CERTIFICADO -> switch (command.event()) {
                case INTIMAR_CONTRARRAZOES -> RecursalLifecycleState.AGUARDANDO_CONTRARRAZOES;
                case ENCAMINHAR_ADMISSIBILIDADE -> initialAdmissibilityGateway(routePlan, command.species());
                default -> reject(command);
            };
            case AGUARDANDO_CONTRARRAZOES -> switch (command.event()) {
                case ENCERRAR_CONTRARRAZOES -> initialAdmissibilityGateway(routePlan, command.species());
                default -> reject(command);
            };
            case ADMISSIBILIDADE_ORIGEM -> switch (command.event()) {
                case ADMITIR -> postAdmissibilityState(routePlan, command.species());
                case NEGAR_SEGUIMENTO -> RecursalLifecycleState.INADMITIDO;
                case SOBRESTAR -> RecursalLifecycleState.SOBRESTADO;
                case SOBRESTAR_POR_PRECEDENTE -> RecursalLifecycleState.SOBRESTADO_POR_PRECEDENTE;
                case RETRATAR -> RecursalLifecycleState.RETRATACAO;
                case RETRATAR_POR_PRECEDENTE -> RecursalLifecycleState.RETRATACAO_POR_PRECEDENTE;
                default -> reject(command);
            };
            case SOBRESTADO -> switch (command.event()) {
                case RETOMAR -> resumedState(command.snapshot(), routePlan, command.species());
                default -> reject(command);
            };
            case SOBRESTADO_POR_PRECEDENTE -> switch (command.event()) {
                case RETOMAR -> RecursalLifecycleState.AGUARDANDO_APLICACAO_PRECEDENTE;
                default -> reject(command);
            };
            case AGUARDANDO_APLICACAO_PRECEDENTE -> switch (command.event()) {
                case APLICAR_PRECEDENTE -> RecursalLifecycleState.PRECEDENTE_APLICADO;
                case DISTINGUIR_CASO -> resumedState(command.snapshot(), routePlan, command.species());
                default -> reject(command);
            };
            case PRECEDENTE_APLICADO, CASO_DISTINGUIDO, RETRATACAO, RETRATACAO_POR_PRECEDENTE -> switch (command.event()) {
                case ENCAMINHAR_ADMISSIBILIDADE -> resumedState(command.snapshot(), routePlan, command.species());
                default -> reject(command);
            };
            case REMESSA_EM_CURSO -> switch (command.event()) {
                case REGISTRAR_SAIDA_AUTOS -> RecursalLifecycleState.AUTOS_EM_TRANSITO;
                case AUTUAR_DESTINO -> RecursalLifecycleState.AUTUADO_NO_DESTINO;
                default -> reject(command);
            };
            case AUTOS_EM_TRANSITO -> switch (command.event()) {
                case CONFIRMAR_RECEBIMENTO -> RecursalLifecycleState.AUTUADO_NO_DESTINO;
                case DEVOLVER_REMESSA -> RecursalLifecycleState.REMESSA_DEVOLVIDA;
                default -> reject(command);
            };
            case REMESSA_DEVOLVIDA -> switch (command.event()) {
                case ENCAMINHAR_ADMISSIBILIDADE -> remessaDevolvidaState(routePlan, command.species());
                default -> reject(command);
            };
            case AUTUADO_NO_DESTINO -> expect(command.event(), RecursalTransitionEvent.DISTRIBUIR, RecursalLifecycleState.DISTRIBUIDO_NO_DESTINO);
            case DISTRIBUIDO_NO_DESTINO -> switch (command.event()) {
                case AFETAR_ORGAO_JULGADOR -> command.species() instanceof ConflitoCompetencia
                        ? RecursalLifecycleState.SUSCITADO
                        : routePlan.admissibilidade().juizoDestino()
                        ? RecursalLifecycleState.ADMISSIBILIDADE_DESTINO
                        : meritState(routePlan, command.species());
                default -> reject(command);
            };
            case ADMISSIBILIDADE_DESTINO -> switch (command.event()) {
                case ADMITIR -> meritState(routePlan, command.species());
                case DETERMINAR_DILIGENCIA -> RecursalLifecycleState.DILIGENCIA_DETERMINADA;
                case NEGAR_SEGUIMENTO -> RecursalLifecycleState.INADMITIDO;
                case SOBRESTAR -> RecursalLifecycleState.SOBRESTADO;
                case SOBRESTAR_POR_PRECEDENTE -> RecursalLifecycleState.SOBRESTADO_POR_PRECEDENTE;
                case RETRATAR -> RecursalLifecycleState.RETRATACAO;
                case RETRATAR_POR_PRECEDENTE -> RecursalLifecycleState.RETRATACAO_POR_PRECEDENTE;
                default -> reject(command);
            };
            case SUSCITADO -> expect(command.event(), RecursalTransitionEvent.RECEBER_SUSCITADO, RecursalLifecycleState.AGUARDANDO_RESOLUCAO_CONFLITO);
            case AGUARDANDO_RESOLUCAO_CONFLITO -> switch (command.event()) {
                case DETERMINAR_DILIGENCIA -> RecursalLifecycleState.DILIGENCIA_DETERMINADA;
                case DEFINIR_COMPETENCIA -> RecursalLifecycleState.COMPETENCIA_DEFINIDA;
                default -> reject(command);
            };
            case COMPETENCIA_DEFINIDA -> expect(command.event(), RecursalTransitionEvent.REMETER_AUTOS_JUIZO_COMPETENTE, RecursalLifecycleState.RETORNO_AO_JUIZO_COMPETENTE);
            case RETORNO_AO_JUIZO_COMPETENTE -> switch (command.event()) {
                case BAIXAR -> RecursalLifecycleState.BAIXADO;
                case CERTIFICAR_TRANSITO -> RecursalLifecycleState.TRANSITADO_EM_JULGADO;
                default -> reject(command);
            };
            case JULGAMENTO_MONOCRATICO, JULGAMENTO_COLEGIADO -> outcomeState(command);
            case PAUTA_SUSTENTACAO_DESIGNADA -> switch (command.event()) {
                case SUSTENTAR -> RecursalLifecycleState.JULGAMENTO_COLEGIADO;
                case DISPENSAR_SUSTENTACAO, ADIAR_SESSAO -> RecursalLifecycleState.JULGAMENTO_COLEGIADO;
                default -> reject(command);
            };
            case SUSTENTACAO_REALIZADA -> outcomeState(command);
            case DILIGENCIA_DETERMINADA -> switch (command.event()) {
                case CUMPRIR_DILIGENCIA -> command.species() instanceof ConflitoCompetencia
                        ? RecursalLifecycleState.AGUARDANDO_RESOLUCAO_CONFLITO
                        : meritState(routePlan, command.species());
                default -> reject(command);
            };
            case AGUARDANDO_REQUISICAO_PAGAMENTO_PUBLICO -> switch (command.event()) {
                case EXPEDIR_RPV -> RecursalLifecycleState.RPV_EXPEDIDA;
                case EXPEDIR_PRECATORIO -> RecursalLifecycleState.AGUARDANDO_PRECATORIO;
                default -> reject(command);
            };
            case AGUARDANDO_PRECATORIO -> expect(command.event(), RecursalTransitionEvent.EXPEDIR_PRECATORIO, RecursalLifecycleState.PRECATORIO_EXPEDIDO);
            case RPV_EXPEDIDA, PRECATORIO_EXPEDIDO -> switch (command.event()) {
                case REGISTRAR_PAGAMENTO_PUBLICO -> RecursalLifecycleState.PAGAMENTO_PUBLICO_LIBERADO;
                default -> reject(command);
            };
            case PAGAMENTO_PUBLICO_LIBERADO -> switch (command.event()) {
                case BAIXAR -> RecursalLifecycleState.BAIXADO;
                case CERTIFICAR_TRANSITO -> RecursalLifecycleState.TRANSITADO_EM_JULGADO;
                default -> reject(command);
            };
            case INADMITIDO, DESERTO, INTEMPESTIVO, PROVIDO, PARCIALMENTE_PROVIDO, IMPROVIDO,
                    NAO_CONHECIDO, ACOLHIDO, REJEITADO, PREJUDICADO -> switch (command.event()) {
                case EXPEDIR_RPV -> requiresPublicPaymentFlow(command.snapshot()) ? RecursalLifecycleState.RPV_EXPEDIDA : reject(command);
                case EXPEDIR_PRECATORIO -> requiresPublicPaymentFlow(command.snapshot()) ? RecursalLifecycleState.PRECATORIO_EXPEDIDO : reject(command);
                case BAIXAR -> RecursalLifecycleState.BAIXADO;
                case CERTIFICAR_TRANSITO -> RecursalLifecycleState.TRANSITADO_EM_JULGADO;
                default -> reject(command);
            };
            case BAIXADO -> expect(command.event(), RecursalTransitionEvent.CERTIFICAR_TRANSITO, RecursalLifecycleState.TRANSITADO_EM_JULGADO);
            case TRANSITADO_EM_JULGADO -> reject(command);
        };
    }


    private RecursalLifecycleState outcomeState(RecursalTransitionCommand command) {
        return switch (command.event()) {
            case CONCEDER_EFEITO_SUSPENSIVO, REVOGAR_EFEITO_SUSPENSIVO,
                    CONCEDER_EFEITO_ATIVO, REVOGAR_EFEITO_ATIVO -> command.snapshot().state();
            case PEDIR_PAUTA_SUSTENTACAO -> RecursalLifecycleState.PAUTA_SUSTENTACAO_DESIGNADA;
            case SUSTENTAR -> RecursalLifecycleState.JULGAMENTO_COLEGIADO;
            case APLICAR_PRECEDENTE -> RecursalLifecycleState.PRECEDENTE_APLICADO;
            case DISTINGUIR_CASO -> RecursalLifecycleState.CASO_DISTINGUIDO;
            case PROVER -> resolvedOutcomeState(command, RecursalLifecycleState.PROVIDO);
            case PROVER_PARCIALMENTE, CONHECER_PARCIALMENTE -> resolvedOutcomeState(command, RecursalLifecycleState.PARCIALMENTE_PROVIDO);
            case NEGAR_PROVIMENTO -> RecursalLifecycleState.IMPROVIDO;
            case NAO_CONHECER -> RecursalLifecycleState.NAO_CONHECIDO;
            case JULGAR_PREJUDICADO -> RecursalLifecycleState.PREJUDICADO;
            case ACOLHER_EMBARGOS -> command.species() instanceof EmbargosDeclaracao ? RecursalLifecycleState.ACOLHIDO : resolvedOutcomeState(command, RecursalLifecycleState.PROVIDO);
            case REJEITAR_EMBARGOS, APLICAR_MULTA_EMBARGOS_PROTELATORIOS -> command.species() instanceof EmbargosDeclaracao ? RecursalLifecycleState.REJEITADO : RecursalLifecycleState.IMPROVIDO;
            case SOBRESTAR -> RecursalLifecycleState.SOBRESTADO;
            case SOBRESTAR_POR_PRECEDENTE -> RecursalLifecycleState.SOBRESTADO_POR_PRECEDENTE;
            case DETERMINAR_DILIGENCIA -> RecursalLifecycleState.DILIGENCIA_DETERMINADA;
            default -> reject(command);
        };
    }

    private RecursalLifecycleState resolvedOutcomeState(RecursalTransitionCommand command, RecursalLifecycleState directOutcome) {
        if (!requiresPublicPaymentFlow(command.context(), directOutcome)) {
            return directOutcome;
        }
        return prefersPrecatorio(command)
                ? RecursalLifecycleState.AGUARDANDO_PRECATORIO
                : RecursalLifecycleState.AGUARDANDO_REQUISICAO_PAGAMENTO_PUBLICO;
    }

    private RecursalLifecycleState initialAdmissibilityGateway(RecursalRoutePlan routePlan, RecursalSpecies species) {
        if (routePlan.admissibilidade().juizoOrigem()) {
            return RecursalLifecycleState.ADMISSIBILIDADE_ORIGEM;
        }
        if (routePlan.remessa().externa()) {
            return RecursalLifecycleState.REMESSA_EM_CURSO;
        }
        return meritState(routePlan, species);
    }

    private RecursalLifecycleState resumedState(RecursalStateSnapshot snapshot, RecursalRoutePlan routePlan, RecursalSpecies species) {
        if (!snapshot.remetido() && !snapshot.autuadoDestino() && !snapshot.distribuidoDestino()) {
            return initialAdmissibilityGateway(routePlan, species);
        }
        if (routePlan.admissibilidade().juizoDestino() && !snapshot.admissibilidadePositiva()) {
            return RecursalLifecycleState.ADMISSIBILIDADE_DESTINO;
        }
        return meritState(routePlan, species);
    }

    private RecursalLifecycleState postAdmissibilityState(RecursalRoutePlan routePlan, RecursalSpecies species) {
        if (routePlan.remessa().externa()) {
            return RecursalLifecycleState.REMESSA_EM_CURSO;
        }
        if (routePlan.admissibilidade().juizoDestino()) {
            return RecursalLifecycleState.ADMISSIBILIDADE_DESTINO;
        }
        return meritState(routePlan, species);
    }

    private RecursalLifecycleState remessaDevolvidaState(RecursalRoutePlan routePlan, RecursalSpecies species) {
        return routePlan.admissibilidade().juizoOrigem()
                ? RecursalLifecycleState.ADMISSIBILIDADE_ORIGEM
                : initialAdmissibilityGateway(routePlan, species);
    }

    private RecursalLifecycleState meritState(RecursalRoutePlan routePlan, RecursalSpecies species) {
        if (species instanceof ConflitoCompetencia) {
            return RecursalLifecycleState.AGUARDANDO_RESOLUCAO_CONFLITO;
        }
        return routePlan.julgamentoColegiado() ? RecursalLifecycleState.JULGAMENTO_COLEGIADO : RecursalLifecycleState.JULGAMENTO_MONOCRATICO;
    }

    private boolean sustentacaoOralDisponivel(RecursalStateSnapshot snapshot, RecursalSpecies species) {
        if (!supportsSustentacaoOral(species)) {
            return false;
        }
        RecursalSustentacaoOralTrace trace = snapshot.sustentacaoOralTrace();
        return trace == null || !trace.solicitada() && !trace.realizada() && !trace.dispensada();
    }

    private RecursalAuthority nextMeritOrAdmissibilityAuthority(RecursalRoutePlan routePlan, RecursalLifecycleState nextState) {
        return switch (nextState) {
            case ADMISSIBILIDADE_ORIGEM -> routePlan.admissibilidade().autoridadeOrigem();
            case ADMISSIBILIDADE_DESTINO -> routePlan.admissibilidade().autoridadeDestino();
            case JULGAMENTO_MONOCRATICO, JULGAMENTO_COLEGIADO, PAUTA_SUSTENTACAO_DESIGNADA,
                    SUSCITADO, AGUARDANDO_RESOLUCAO_CONFLITO, COMPETENCIA_DEFINIDA -> routePlan.autoridadeJulgamentoMerito();
            case REMESSA_EM_CURSO, AUTOS_EM_TRANSITO, REMESSA_DEVOLVIDA,
                    AGUARDANDO_REQUISICAO_PAGAMENTO_PUBLICO, AGUARDANDO_PRECATORIO,
                    RPV_EXPEDIDA, PRECATORIO_EXPEDIDO, PAGAMENTO_PUBLICO_LIBERADO,
                    RETORNO_AO_JUIZO_COMPETENTE -> RecursalAuthority.SECRETARIA_JUDICIARIA;
            default -> routePlan.autoridadeJulgamentoMerito();
        };
    }

    private void outcomeEvents(RecursalSpecies species, LinkedHashSet<RecursalTransitionEvent> events, RecursalStateSnapshot snapshot) {
        if (!snapshot.efeitoSuspensivoAtivo()) {
            events.add(RecursalTransitionEvent.CONCEDER_EFEITO_SUSPENSIVO);
        } else {
            events.add(RecursalTransitionEvent.REVOGAR_EFEITO_SUSPENSIVO);
        }
        if (!snapshot.efeitoAtivoConcedido()) {
            events.add(RecursalTransitionEvent.CONCEDER_EFEITO_ATIVO);
        } else {
            events.add(RecursalTransitionEvent.REVOGAR_EFEITO_ATIVO);
        }
        events.add(RecursalTransitionEvent.DETERMINAR_DILIGENCIA);
        events.add(RecursalTransitionEvent.PROVER);
        events.add(RecursalTransitionEvent.PROVER_PARCIALMENTE);
        events.add(RecursalTransitionEvent.CONHECER_PARCIALMENTE);
        events.add(RecursalTransitionEvent.NEGAR_PROVIMENTO);
        events.add(RecursalTransitionEvent.NAO_CONHECER);
        events.add(RecursalTransitionEvent.JULGAR_PREJUDICADO);
        events.add(RecursalTransitionEvent.SOBRESTAR);
        events.add(RecursalTransitionEvent.SOBRESTAR_POR_PRECEDENTE);
        if (species instanceof EmbargosDeclaracao) {
            events.add(RecursalTransitionEvent.ACOLHER_EMBARGOS);
            events.add(RecursalTransitionEvent.REJEITAR_EMBARGOS);
            events.add(RecursalTransitionEvent.APLICAR_MULTA_EMBARGOS_PROTELATORIOS);
        }
    }

    private boolean requiresPreparo(RecursalStateSnapshot snapshot, RecursalRoutePlan routePlan) {
        return !snapshot.remessaNecessaria() && routePlan.preparo().exigido();
    }

    private boolean requiresCounterReasons(RecursalStateSnapshot snapshot, RecursalSpecies species) {
        return !snapshot.remessaNecessaria() && species.requiresCounterReasons();
    }

    private boolean supportsSustentacaoOral(RecursalSpecies species) {
        return !(species instanceof EmbargosDeclaracao);
    }

    private RecursalAuthority originAuthority(RecursalRoutePlan routePlan, RecursalCaseContext context) {
        return routePlan.admissibilidade().autoridadeOrigem() == null ? context.autoridadeAtual() : routePlan.admissibilidade().autoridadeOrigem();
    }

    private boolean requiresPublicPaymentFlow(RecursalStateSnapshot snapshot) {
        return snapshot != null && requiresPublicPaymentFlow(snapshot.requisicaoPublicaPagamento(), snapshot.state());
    }

    private boolean requiresPublicPaymentFlow(RecursalCaseContext context, RecursalLifecycleState outcomeState) {
        return context != null && requiresPublicPaymentFlow(context.demandaRequisicaoPublicaPagamento(), outcomeState);
    }

    private boolean requiresPublicPaymentFlow(boolean requisicaoPublicaPagamento, RecursalLifecycleState outcomeState) {
        return requisicaoPublicaPagamento && (outcomeState == RecursalLifecycleState.PROVIDO || outcomeState == RecursalLifecycleState.PARCIALMENTE_PROVIDO);
    }

    private boolean prefersPrecatorio(RecursalTransitionCommand command) {
        String modalidade = command == null || command.details() == null ? null : command.details().modalidadePagamento();
        return modalidade != null && modalidade.equalsIgnoreCase("PRECATORIO");
    }

    private RecursalLifecycleState expect(RecursalTransitionEvent actual, RecursalTransitionEvent expected, RecursalLifecycleState nextState) {
        if (actual != expected) {
            throw new RecursalTransitionRejectedException("Transição inválida para evento " + actual + "; esperado " + expected);
        }
        return nextState;
    }

    private RecursalLifecycleState reject(RecursalTransitionCommand command) {
        throw new RecursalTransitionRejectedException("Evento " + command.event() + " não permitido em " + command.snapshot().state());
    }
}
