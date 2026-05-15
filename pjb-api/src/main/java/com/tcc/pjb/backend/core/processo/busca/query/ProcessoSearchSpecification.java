package com.tcc.pjb.backend.core.processo.busca.query;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class ProcessoSearchSpecification {

    public Specification<Processo> build(ProcessoSearchCriteria criteria) {
        return (Root<Processo> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (criteria == null) return cb.conjunction();
            List<Predicate> predicates = new ArrayList<>();

            if (notBlank(criteria.numero())) {
                String normalized = criteria.numero().trim();
                predicates.add(cb.or(
                        cb.equal(root.get("numeroUnificado"), normalized),
                        cb.equal(root.get("numeroProcesso"), normalized)
                ));
            }

            if (notBlank(criteria.uf())) {
                predicates.add(cb.equal(cb.lower(root.get("uf")), criteria.uf().trim().toLowerCase()));
            }

            if (notBlank(criteria.comarca())) {
                predicates.add(cb.like(cb.lower(root.get("comarca")),
                        "%" + criteria.comarca().trim().toLowerCase() + "%"));
            }

            if (notBlank(criteria.tribunal())) {
                predicates.add(cb.like(cb.lower(root.get("tribunal")),
                        "%" + criteria.tribunal().trim().toLowerCase() + "%"));
            }

            if (notBlank(criteria.assunto())) {
                predicates.add(cb.like(cb.lower(root.get("assunto")),
                        "%" + criteria.assunto().trim().toLowerCase() + "%"));
            }

            if (notBlank(criteria.parteNome())) {
                String pattern = "%" + criteria.parteNome().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("parteAutoraNome")), pattern),
                        cb.like(cb.lower(root.get("parteReuNome")), pattern)
                ));
            }

            if (criteria.ramoDireito() != null) {
                predicates.add(cb.equal(root.get("ramoDireito"), criteria.ramoDireito()));
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("statusProcesso"), criteria.status()));
            }

            if (criteria.fase() != null) {
                predicates.add(cb.equal(root.get("faseAtual"), criteria.fase()));
            }

            if (criteria.distribuicaoApos() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataDistribuicao"), criteria.distribuicaoApos()));
            }

            if (criteria.distribuicaoAntes() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dataDistribuicao"), criteria.distribuicaoAntes()));
            }

            if (criteria.movimentacaoApos() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataUltimaMovimentacao"), criteria.movimentacaoApos()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    public record ProcessoSearchCriteria(
            String numero,
            String uf,
            String comarca,
            String tribunal,
            String assunto,
            String parteNome,
            RamoDireito ramoDireito,
            StatusProcesso status,
            FaseProcessual fase,
            LocalDateTime distribuicaoApos,
            LocalDateTime distribuicaoAntes,
            LocalDateTime movimentacaoApos
    ) {
        public static ProcessoSearchCriteria of(String numero,
                                                 String uf,
                                                 String comarca,
                                                 String tribunal,
                                                 String assunto,
                                                 String parteNome,
                                                 RamoDireito ramoDireito,
                                                 StatusProcesso status,
                                                 FaseProcessual fase,
                                                 LocalDateTime distribuicaoApos,
                                                 LocalDateTime distribuicaoAntes,
                                                 LocalDateTime movimentacaoApos) {
            return new ProcessoSearchCriteria(numero, uf, comarca, tribunal, assunto, parteNome,
                    ramoDireito, status, fase, distribuicaoApos, distribuicaoAntes, movimentacaoApos);
        }
    }
}
