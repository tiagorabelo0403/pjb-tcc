package com.tcc.pjb.backend.platform.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class PjbRuntimeDrainCoordinatorTest {

    private AnnotationConfigApplicationContext context;
    private AnnotationConfigApplicationContext child;

    @AfterEach
    void fechar() {
        if (child != null && child.isActive()) {
            child.close();
        }
        if (context != null && context.isActive()) {
            context.close();
        }
    }

    @Test
    void pausaEReinicioDoContextoVoltamAAceitarTrafegoETarefas() throws Exception {
        context = contextoComCoordenador();

        context.pause();
        context.restart();

        assertThat(context.getBean(PjbRuntimeDrainService.class).isDraining())
                .as("pausa de contexto nao e desligamento; ao reiniciar a instancia volta a aceitar trafego")
                .isFalse();
        assertThat(context.getBean(PjbBoundedExecutorService.class).acceptingTasks()).isTrue();
        assertThat(agendaEExecuta(context.getBean("pjbTimeoutScheduler", ScheduledExecutorService.class)))
                .as("o agendador de timeout precisa seguir vivo depois da pausa")
                .isTrue();
    }

    @Test
    void stopEStartDoContextoVoltamAAceitarTrafegoETarefas() throws Exception {
        context = contextoComCoordenador();

        context.stop();
        context.start();

        assertThat(context.getBean(PjbRuntimeDrainService.class).isDraining()).isFalse();
        assertThat(context.getBean(PjbBoundedExecutorService.class).acceptingTasks()).isTrue();
        assertThat(agendaEExecuta(context.getBean("pjbTimeoutScheduler", ScheduledExecutorService.class))).isTrue();
    }

    @Test
    void stopSemRestartDrenaAInstanciaSemDesligarOAgendador() {
        context = contextoComCoordenador();

        context.stop();

        assertThat(context.getBean(PjbRuntimeDrainService.class).isDraining()).isTrue();
        assertThat(context.getBean(PjbBoundedExecutorService.class).acceptingTasks()).isFalse();
        assertThat(context.getBean("pjbTimeoutScheduler", ScheduledExecutorService.class).isShutdown()).isFalse();
    }

    @Test
    void drenagemPedidaPeloOperadorSobreviveAPausaEReinicio() {
        context = contextoComCoordenador();
        PjbRuntimeDrainService drainService = context.getBean(PjbRuntimeDrainService.class);
        drainService.beginDrain("manutencao-operador");

        context.pause();
        context.restart();

        assertThat(drainService.isDraining())
                .as("reinicio de contexto so desfaz a drenagem que o proprio stop iniciou")
                .isTrue();
        assertThat(drainService.reason()).isEqualTo("manutencao-operador");
    }

    @Test
    void drenagemPedidaPeloOperadorDuranteAPausaSobreviveAoReinicio() {
        context = contextoComCoordenador();
        PjbRuntimeDrainService drainService = context.getBean(PjbRuntimeDrainService.class);

        context.pause();
        drainService.beginDrain("manutencao-operador");
        context.restart();

        assertThat(drainService.isDraining())
                .as("o operador trocou o motivo da drenagem durante a pausa; o reinicio nao pode desfaze-la")
                .isTrue();
        assertThat(drainService.reason()).isEqualTo("manutencao-operador");
    }

    @Test
    void fechamentoDoContextoDrenaEDesligaAgendadorEExecutores() {
        context = contextoComCoordenador();
        PjbRuntimeDrainService drainService = context.getBean(PjbRuntimeDrainService.class);
        PjbBoundedExecutorService executor = context.getBean(PjbBoundedExecutorService.class);
        ScheduledExecutorService scheduler = context.getBean("pjbTimeoutScheduler", ScheduledExecutorService.class);

        context.close();

        assertThat(drainService.isDraining()).isTrue();
        assertThat(drainService.reason()).isEqualTo("context-shutdown");
        assertThat(executor.isShutdown()).isTrue();
        assertThat(scheduler.isShutdown()).isTrue();
    }

    @Test
    void fechamentoDeContextoFilhoNaoConvertePausaDoPaiEmDesligamento() throws Exception {
        context = contextoComCoordenador();
        child = new AnnotationConfigApplicationContext();
        child.setParent(context);
        child.refresh();
        child.close();

        context.pause();
        context.restart();

        assertThat(context.getBean(PjbRuntimeDrainService.class).isDraining()).isFalse();
        assertThat(agendaEExecuta(context.getBean("pjbTimeoutScheduler", ScheduledExecutorService.class)))
                .as("o ContextClosedEvent do filho propaga para o pai e nao pode ser lido como fechamento do pai")
                .isTrue();
    }

    private static AnnotationConfigApplicationContext contextoComCoordenador() {
        PjbRuntimeLifecycleProperties properties = new PjbRuntimeLifecycleProperties();
        properties.setDrainQuietPeriod(Duration.ofMillis(10));
        properties.setShutdownAwaitTimeout(Duration.ofSeconds(2));
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(PjbRuntimeLifecycleProperties.class, () -> properties);
        ctx.registerBean(PjbRuntimeDrainService.class);
        ctx.registerBean("pjbTestExecutor", PjbBoundedExecutorService.class,
                () -> new PjbBoundedExecutorService("pjb-coord-test-", 1, true, Duration.ofSeconds(2), Duration.ofMillis(20)));
        ctx.registerBean("pjbTimeoutScheduler", ScheduledExecutorService.class,
                () -> Executors.newSingleThreadScheduledExecutor(), bd -> bd.setDestroyMethodName("shutdown"));
        ctx.registerBean(PjbRuntimeDrainCoordinator.class);
        ctx.refresh();
        ctx.getBean(PjbRuntimeDrainService.class).markAccepting("teste");
        return ctx;
    }

    private static boolean agendaEExecuta(ScheduledExecutorService scheduler) throws InterruptedException {
        if (scheduler.isShutdown()) {
            return false;
        }
        CountDownLatch executou = new CountDownLatch(1);
        scheduler.schedule(executou::countDown, 1, TimeUnit.MILLISECONDS);
        return executou.await(2, TimeUnit.SECONDS);
    }
}
