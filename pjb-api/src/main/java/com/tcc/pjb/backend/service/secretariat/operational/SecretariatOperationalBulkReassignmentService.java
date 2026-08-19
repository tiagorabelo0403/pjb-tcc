package com.tcc.pjb.backend.service.secretariat.operational;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecretariatOperationalBulkReassignmentService {

    private static final int LIMITE_ITENS = 500;

    private final UsuarioRepository usuarioRepository;
    private final WorkItemRepository workItemRepository;
    private final SecretariatQueueProjectionService projectionService;

    public SecretariatOperationalBulkReassignmentService(UsuarioRepository usuarioRepository,
                                                          WorkItemRepository workItemRepository,
                                                          SecretariatQueueProjectionService projectionService) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.projectionService = Objects.requireNonNull(projectionService);
    }

    @PjbTransactionalBudget(operation = "secretariat.operational.reatribuir-carga-afastamento", maxMillis = 3000)
    @Transactional
    public BulkReassignmentSnapshot reatribuirCargaPorAfastamento(Long servidorAfastadoId, Usuario actor) {
        Usuario afastado = usuarioRepository.findById(servidorAfastadoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", servidorAfastadoId));
        List<WorkItem> itens = workItemRepository.inboxByUser(servidorAfastadoId, PageRequest.of(0, LIMITE_ITENS)).getContent();
        Map<Long, Long> cargaAtual = new HashMap<>();
        List<BulkReassignmentItem> reatribuidos = new ArrayList<>();
        int semCandidato = 0;
        for (WorkItem item : itens) {
            Usuario candidato = melhorCandidato(item, afastado, cargaAtual);
            if (candidato == null) {
                semCandidato++;
                continue;
            }
            String suffix = "\nReatribuído por afastamento de " + afastado.getNome() + " (#" + afastado.getId()
                    + ") para " + candidato.getNome() + " (#" + candidato.getId() + ") por " + actor.getNome()
                    + " (#" + actor.getId() + ')';
            item.setAssignedUser(candidato);
            item.setDescricao((item.getDescricao() == null ? "" : item.getDescricao()) + suffix);
            workItemRepository.save(item);
            projectionService.upsert(item, item.getPrioridade() == null ? 60 : 60 + Math.max(0, (6 - item.getPrioridade()) * 8),
                    List.of("REATRIBUICAO_AFASTAMENTO", String.valueOf(afastado.getId()), candidato.getNome()));
            cargaAtual.merge(candidato.getId(), 1L, Long::sum);
            reatribuidos.add(new BulkReassignmentItem(
                    item.getId(),
                    item.getProcesso() == null ? null : item.getProcesso().getId(),
                    item.getProcesso() == null ? null : item.getProcesso().getNumeroProcesso(),
                    item.getTitulo(),
                    candidato.getId(),
                    candidato.getNome()));
        }
        return new BulkReassignmentSnapshot(afastado.getId(), afastado.getNome(), reatribuidos.size(), semCandidato, List.copyOf(reatribuidos));
    }

    private Usuario melhorCandidato(WorkItem item, Usuario afastado, Map<Long, Long> cargaAtual) {
        if (item.getAssignedRole() == null) {
            return null;
        }
        List<Usuario> mesmaComarca = usuarioRepository.findByTipoUsuario(item.getAssignedRole()).stream()
                .filter(Usuario::isAtivo)
                .filter(candidato -> !Objects.equals(candidato.getId(), afastado.getId()))
                .filter(candidato -> item.getComarca() == null || item.getComarca().equalsIgnoreCase(candidato.getComarca()))
                .toList();
        List<Usuario> elegiveis = mesmaComarca.isEmpty()
                ? usuarioRepository.findByTipoUsuario(item.getAssignedRole()).stream()
                        .filter(Usuario::isAtivo)
                        .filter(candidato -> !Objects.equals(candidato.getId(), afastado.getId()))
                        .toList()
                : mesmaComarca;
        return elegiveis.stream()
                .min(Comparator.comparingLong((Usuario candidato) -> cargaAtualDe(candidato, cargaAtual)).thenComparing(Usuario::getNome))
                .orElse(null);
    }

    private long cargaAtualDe(Usuario candidato, Map<Long, Long> cargaAtual) {
        return cargaAtual.computeIfAbsent(candidato.getId(),
                id -> workItemRepository.inboxByUser(id, PageRequest.of(0, 1)).getTotalElements());
    }

    public record BulkReassignmentItem(
            Long workItemId,
            Long processoId,
            String numeroProcesso,
            String titulo,
            Long novoResponsavelId,
            String novoResponsavelNome
    ) {
    }

    public record BulkReassignmentSnapshot(
            Long servidorAfastadoId,
            String servidorAfastadoNome,
            int totalReatribuido,
            int semCandidatoDisponivel,
            List<BulkReassignmentItem> itens
    ) {
    }
}
