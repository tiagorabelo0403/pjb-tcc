package com.tcc.pjb.backend.service.institutional;

import com.tcc.pjb.backend.model.entity.enums.GrauSigilo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class InstituicaoVisibilidadeMatrixService {

    public record MatrixResponse(
            boolean permitido,
            String decisao,
            List<String> motivos,
            List<String> riscos,
            boolean stepUpNecessario,
            String explicacaoDetalhada
    ) {}

    public record ConsultaEntrada(
            UUID usuarioId,
            LotacaoInstitucional lotacao,
            InstituicaoJudicial instituicao,
            GrauSigilo sigiloProcesso
    ) {}

    private final Map<String, MatrixResponse> cache = new ConcurrentHashMap<>(256);

    public MatrixResponse consultar(ConsultaEntrada entrada) {
        String cacheKey = buildCacheKey(entrada);
        return cache.computeIfAbsent(cacheKey, k -> computar(entrada));
    }

    private MatrixResponse computar(ConsultaEntrada entrada) {
        InstituicaoVisibilidadePolicy.Resultado resultado = InstituicaoVisibilidadePolicy.avaliar(
                entrada.lotacao(),
                entrada.instituicao(),
                entrada.sigiloProcesso(),
                LocalDate.now()
        );
        return new MatrixResponse(
                resultado.decisao() != InstituicaoVisibilidadePolicy.Decisao.NEGADO,
                resultado.decisao().name(),
                resultado.motivos(),
                resultado.riscos(),
                resultado.stepUpNecessario(),
                resultado.explicacaoDetalhada()
        );
    }

    public List<MatrixResponse> consultarTodosOsPapeis(
            List<LotacaoInstitucional> lotacoes,
            InstituicaoJudicial instituicao,
            GrauSigilo sigiloProcesso
    ) {
        List<MatrixResponse> resultados = new ArrayList<>(lotacoes.size());
        for (LotacaoInstitucional lotacao : lotacoes) {
            resultados.add(consultar(
                    new ConsultaEntrada(lotacao.usuarioId(), lotacao, instituicao, sigiloProcesso)));
        }
        return resultados;
    }

    public void invalidarCache(UUID instituicaoId) {
        cache.entrySet().removeIf(e -> e.getKey().contains(instituicaoId.toString()));
    }

    private String buildCacheKey(ConsultaEntrada e) {
        return e.usuarioId() + ":" + e.instituicao().id() + ":" +
               e.lotacao().papel().codigo() + ":" + e.sigiloProcesso().name();
    }
}
