package com.tcc.pjb.backend.model.repository.intelligence;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.intelligence.PessoaLocalizacaoConsultaGovernada;
import com.tcc.pjb.backend.model.entity.intelligence.PessoaLocalizacaoCanalConsulta;

public interface PessoaLocalizacaoConsultaGovernadaRepository extends JpaRepository<PessoaLocalizacaoConsultaGovernada, Long> {

    List<PessoaLocalizacaoConsultaGovernada> findByExecutorUserIdAndCanalConsultaOrderByCreatedAtDesc(Long executorUserId,
                                                                                                      PessoaLocalizacaoCanalConsulta canalConsulta,
                                                                                                      Pageable pageable);

    long countByExecutorUserIdAndCanalConsultaAndCreatedAtGreaterThanEqual(Long executorUserId,
                                                                           PessoaLocalizacaoCanalConsulta canalConsulta,
                                                                           LocalDateTime createdAt);

    long countByExecutorUserIdAndCanalConsultaAndRequerRevisaoTrueAndCreatedAtGreaterThanEqual(Long executorUserId,
                                                                                                PessoaLocalizacaoCanalConsulta canalConsulta,
                                                                                                LocalDateTime createdAt);

    long countByExecutorUserIdAndCanalConsultaAndEnderecoEstritoLiberadoTrueAndCreatedAtGreaterThanEqual(Long executorUserId,
                                                                                                          PessoaLocalizacaoCanalConsulta canalConsulta,
                                                                                                          LocalDateTime createdAt);

    long countByExecutorUserIdAndCanalConsultaAndPossuiContextoFormalFalseAndCreatedAtGreaterThanEqual(Long executorUserId,
                                                                                                        PessoaLocalizacaoCanalConsulta canalConsulta,
                                                                                                        LocalDateTime createdAt);

    long countByExecutorUserIdAndCanalConsultaAndStepUpRequiredTrueAndStepUpSatisfiedFalseAndCreatedAtGreaterThanEqual(Long executorUserId,
                                                                                                                        PessoaLocalizacaoCanalConsulta canalConsulta,
                                                                                                                        LocalDateTime createdAt);
}
