package com.tcc.pjb.backend.modules.advocacia.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.modules.advocacia.entity.Cliente;
import com.tcc.pjb.backend.modules.advocacia.entity.util.CriptografiaPJB;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;

@Service
public class AdvClienteCanonicalizeSensitiveService {

    private final ClienteRepository clienteRepository;

    public AdvClienteCanonicalizeSensitiveService(ClienteRepository clienteRepository) {
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
    }

    public record BatchResult(long processed,
                              long updated,
                              long duplicates,
                              long lastId,
                              boolean done) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchResult canonicalizeBatch(long afterId, Long untilId, int batchSize, boolean dryRun) {
        int size = Math.max(1, Math.min(batchSize, 2000));
        List<Cliente> batch = clienteRepository.findBatchForCanonicalize(afterId, untilId, PageRequest.of(0, size));
        if (batch.isEmpty()) {
            return new BatchResult(0, 0, 0, afterId, true);
        }

        Map<Long, Set<String>> seen = new HashMap<>();
        List<Cliente> changed = new ArrayList<>();

        long processed = 0;
        long updated = 0;
        long duplicates = 0;
        long lastId = afterId;

        for (Cliente c : batch) {
            if (c == null || c.getId() == null) {
                continue;
            }
            lastId = Math.max(lastId, c.getId());
            processed++;

            String rawCpf = c.getCpfCriptografado();
            String cpfDigits = CriptografiaPJB.normalizarDocumentoNumerico(rawCpf);

            String rawEmail = c.getEmailCriptografado();
            String emailNorm = normalizeEmail(rawEmail);

            String newHash = cpfDigits != null ? CriptografiaPJB.hashCpfCnpj(cpfDigits) : null;

            Long advogadoId = c.getAdvogado() != null ? c.getAdvogado().getId() : null;
            boolean duplicate = false;

            if (advogadoId != null && newHash != null) {
                Set<String> s = seen.computeIfAbsent(advogadoId, k -> new HashSet<>());
                if (s.contains(newHash)) {
                    duplicate = true;
                } else {
                    Long min = clienteRepository.minIdByAdvogadoAndCpfHash(advogadoId, newHash);
                    if (min != null && !Objects.equals(min, c.getId())) {
                        duplicate = true;
                    } else {
                        s.add(newHash);
                    }
                }
            }

            DesiredUpdate desired = desiredUpdate(c, cpfDigits, emailNorm, newHash, duplicate);
            if (!desired.shouldChange) {
                continue;
            }

            if (duplicate) {
                duplicates++;
            } else {
                updated++;
            }

            if (!dryRun) {
                apply(c, desired);
                changed.add(c);
            }
        }

        if (!dryRun && !changed.isEmpty()) {
            clienteRepository.saveAll(changed);
        }

        return new BatchResult(processed, updated, duplicates, lastId, false);
    }

    @Transactional(readOnly = true)
    public long countTotal(long afterId, Long untilId) {
        return clienteRepository.countForCanonicalize(afterId, untilId);
    }

    private static String normalizeEmail(String email) {
        if (email == null) return null;
        String v = email.trim();
        if (v.isBlank()) return null;
        return v.toLowerCase();
    }

    private record DesiredUpdate(boolean shouldChange,
                                 String cpfDigits,
                                 String emailNorm,
                                 String cpfHash,
                                 boolean duplicate) {
    }

    private static DesiredUpdate desiredUpdate(Cliente c,
                                             String cpfDigits,
                                             String emailNorm,
                                             String cpfHash,
                                             boolean duplicate) {
        boolean change = false;

        String currentCpf = c.getCpfCriptografado();
        String currentCpfDigits = CriptografiaPJB.normalizarDocumentoNumerico(currentCpf);
        if (!Objects.equals(currentCpfDigits, cpfDigits)) {
            change = true;
        }

        String currentEmail = normalizeEmail(c.getEmailCriptografado());
        if (!Objects.equals(currentEmail, emailNorm)) {
            change = true;
        }

        String currentHash = c.getCpfHash();
        if (duplicate) {
            if (currentHash != null) {
                change = true;
            }
            if (c.getStatus() != StatusCliente.ARQUIVADO && c.getStatus() != StatusCliente.EM_ANALISE) {
                change = true;
            }
        } else {
            if (!Objects.equals(currentHash, cpfHash)) {
                change = true;
            }
        }

        return new DesiredUpdate(change, cpfDigits, emailNorm, cpfHash, duplicate);
    }

    private static void apply(Cliente c, DesiredUpdate desired) {
        c.setCpfCriptografado(desired.cpfDigits);
        c.setEmailCriptografado(desired.emailNorm);

        if (desired.duplicate) {
            c.setCpfHash(null);
            if (c.getStatus() != StatusCliente.ARQUIVADO) {
                c.setStatus(StatusCliente.EM_ANALISE);
            }
            return;
        }

        c.setCpfHash(desired.cpfHash);
    }
}
