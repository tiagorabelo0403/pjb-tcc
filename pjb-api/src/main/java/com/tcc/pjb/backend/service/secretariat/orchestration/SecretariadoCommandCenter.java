package com.tcc.pjb.backend.service.secretariat.orchestration;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "pjb.secretariat.enabled", havingValue = "true")
public class SecretariadoCommandCenter {

  public record CpfCnpj(String valor) {
    public CpfCnpj {
      if (valor == null) {
        throw new IllegalArgumentException("documento inválido");
      }
      String v = valor.replaceAll("\\D+", "");
      if (!v.matches("\\d{11}|\\d{14}")) {
        throw new IllegalArgumentException("documento inválido");
      }
      valor = v;
    }
  }

  public record InfoSisbajud(double saldoBloqueado, boolean possuiContasAtivas) {
  }

  public record InfoRenajud(int quantidadeVeiculos, boolean possuiRestricaoJudicial) {
  }

  public record InfoInfojud(String ultimaDeclaracaoIrpf, double bensDeclarados) {
  }

  public record DossiePatrimonial(
      Long processoId,
      CpfCnpj alvo,
      InfoSisbajud dadosBancarios,
      InfoRenajud dadosVeiculares,
      InfoInfojud dadosFiscais,
      String statusConsolidado,
      boolean achouIndicioPatrimonial,
      Instant geradoEm,
      long latencyMs
  ) {
    public Optional<InfoSisbajud> dadosBancariosOptional() {
      return Optional.ofNullable(dadosBancarios);
    }

    public Optional<InfoRenajud> dadosVeicularesOptional() {
      return Optional.ofNullable(dadosVeiculares);
    }

    public Optional<InfoInfojud> dadosFiscaisOptional() {
      return Optional.ofNullable(dadosFiscais);
    }
  }

  public interface SisbajudClient {
    InfoSisbajud buscarContas(CpfCnpj alvo) throws Exception;
  }

  public interface RenajudClient {
    InfoRenajud buscarVeiculos(CpfCnpj alvo) throws Exception;
  }

  public interface InfojudClient {
    InfoInfojud buscarDeclaracoes(CpfCnpj alvo) throws Exception;
  }

  private final SisbajudClient sisbajudClient;
  private final RenajudClient renajudClient;
  private final InfojudClient infojudClient;
  private final PjbExecutionOrchestrator executionOrchestrator;

  public SecretariadoCommandCenter(ObjectProvider<SisbajudClient> s,
                                   ObjectProvider<RenajudClient> r,
                                   ObjectProvider<InfojudClient> i,
                                   PjbExecutionOrchestrator executionOrchestrator) {
    this.sisbajudClient = s.getIfAvailable(() -> alvo -> new InfoSisbajud(0, false));
    this.renajudClient = r.getIfAvailable(() -> alvo -> new InfoRenajud(0, false));
    this.infojudClient = i.getIfAvailable(() -> alvo -> new InfoInfojud(null, 0));
    this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator);
  }

  public DossiePatrimonial gerarDossiePatrimonial(Long processoId, String cpfCnpj) {
    Objects.requireNonNull(processoId, "processoId");
    CpfCnpj doc = new CpfCnpj(cpfCnpj);

    long start = System.nanoTime();
    Duration budget = Duration.ofSeconds(3);

    CompletableFuture<InfoSisbajud> f1 = call("secretariat-sisbajud-dossier", () -> sisbajudClient.buscarContas(doc), budget);
    CompletableFuture<InfoRenajud> f2 = call("secretariat-renajud-dossier", () -> renajudClient.buscarVeiculos(doc), budget);
    CompletableFuture<InfoInfojud> f3 = call("secretariat-infojud-dossier", () -> infojudClient.buscarDeclaracoes(doc), budget);

    awaitAll(List.of(f1, f2, f3), budget);

    long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    InfoSisbajud bancarios = f1.getNow(null);
    InfoRenajud veiculares = f2.getNow(null);
    InfoInfojud fiscais = f3.getNow(null);
    boolean indicio = (bancarios != null && bancarios.saldoBloqueado() >= 1000)
        || (veiculares != null && veiculares.quantidadeVeiculos() >= 3)
        || (fiscais != null && fiscais.bensDeclarados() >= 10000);
    String status = indicio ? "INDICIOS_ENCONTRADOS" : "SEM_INDICIOS_RELEVANTES";

    return new DossiePatrimonial(
        processoId,
        doc,
        bancarios,
        veiculares,
        fiscais,
        status,
        indicio,
        Instant.now(),
        latencyMs
    );
  }

  private void awaitAll(List<? extends CompletableFuture<?>> futures, Duration timeout) {
    CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    try {
      all.get(timeout.toMillis() + 250L, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      cancelPending(futures);
    } catch (TimeoutException | ExecutionException e) {
      cancelPending(futures);
    }
  }

  private void cancelPending(List<? extends CompletableFuture<?>> futures) {
    futures.stream()
        .filter(future -> !future.isDone())
        .forEach(future -> future.cancel(true));
  }

  private <T> CompletableFuture<T> call(String operationName, Callable<T> fn, Duration timeout) {
    return executionOrchestrator.supply(PjbExecutionDescriptor.io(operationName, timeout), () -> {
          try {
            return fn.call();
          } catch (Exception e) {
            return null;
          }
        })
        .exceptionally(ex -> null);
  }
}
