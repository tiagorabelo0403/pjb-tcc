package com.tcc.pjb.backend.modules.advocacia.service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.modules.advocacia.dto.ClienteDTO;
import com.tcc.pjb.backend.modules.advocacia.entity.Cliente;
import com.tcc.pjb.backend.modules.advocacia.entity.util.CriptografiaPJB;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;

@Component
public class ClienteSpecification {

    public Specification<Cliente> build(ClienteDTO.ClienteQuery query) {
        return (Root<Cliente> root, CriteriaQuery<?> q, CriteriaBuilder cb) -> {
            if (query == null) return cb.conjunction();

            List<Predicate> predicates = new ArrayList<>();

            Long advogadoId = query.getAdvogadoId();
            String nome = query.getNome();
            String cpfCnpj = query.getCpfCnpj();
            StatusCliente status = query.getStatus();
            LocalDateTime cadastradoDepois = query.getCadastradoDepois();

            if (advogadoId != null) {
                predicates.add(cb.equal(root.get("advogado").get("id"), advogadoId));
            }

            if (nome != null && !nome.isBlank()) {
                String nomeNormalizado = normalizar(nome);
                predicates.add(cb.like(cb.lower(root.get("nomeNormalizado")), "%" + nomeNormalizado + "%"));
            }

            if (cpfCnpj != null && !cpfCnpj.isBlank()) {
                List<String> hashes = CriptografiaPJB.candidateCpfHashes(cpfCnpj);
                if (!hashes.isEmpty()) {
                    predicates.add(root.get("cpfHash").in(hashes));
                }
            }

            if (status == null && (nome == null || nome.isBlank()) && (cpfCnpj == null || cpfCnpj.isBlank())) {
                status = StatusCliente.ATIVO;
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (cadastradoDepois != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataCriacao"), cadastradoDepois));
            }

            aplicarPoliticasDeRisco(root, cb, predicates, status);
            aplicarJanelaTemporal(root, cb, predicates, status);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void aplicarPoliticasDeRisco(Root<Cliente> root,
                                        CriteriaBuilder cb,
                                        List<Predicate> predicates,
                                        StatusCliente status) {
        if (status == null) return;

        var confiabilidade = cb.coalesce(root.get("nivelConfiabilidade"), 0.0);

        if (status == StatusCliente.ATIVO) {
            predicates.add(cb.greaterThanOrEqualTo(confiabilidade, 0.7));
        }
        if (status == StatusCliente.INATIVO) {
            predicates.add(cb.lessThanOrEqualTo(confiabilidade, 0.5));
        }
    }

    private void aplicarJanelaTemporal(Root<Cliente> root,
                                      CriteriaBuilder cb,
                                      List<Predicate> predicates,
                                      StatusCliente status) {
        LocalDateTime agora = LocalDateTime.now();

        if (status == null) {
            predicates.add(cb.greaterThan(root.get("dataAtualizacao"), agora.minusMonths(18)));
            return;
        }

        if (status == StatusCliente.ATIVO) {
            predicates.add(cb.greaterThan(root.get("dataAtualizacao"), agora.minusMonths(24)));
            return;
        }

        if (status == StatusCliente.INATIVO) {
            predicates.add(cb.lessThan(root.get("dataAtualizacao"), agora.minusMonths(36)));
        }
    }

    private String normalizar(String texto) {
        if (texto == null) return null;
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }
}
