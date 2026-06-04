package com.tcc.pjb.backend.core.security.scope;

import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.criminal.BoletimOcorrenciaDigital;
import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import com.tcc.pjb.backend.model.entity.enums.StatusUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DelegaciaInstitucionalScopeService {

    private final UnidadeInstituicaoRepository unidadeInstituicaoRepository;
    private final LotacaoInstituicaoRepository lotacaoInstituicaoRepository;

    public DelegaciaInstitucionalScopeService(UnidadeInstituicaoRepository unidadeInstituicaoRepository,
                                             LotacaoInstituicaoRepository lotacaoInstituicaoRepository) {
        this.unidadeInstituicaoRepository = Objects.requireNonNull(unidadeInstituicaoRepository);
        this.lotacaoInstituicaoRepository = Objects.requireNonNull(lotacaoInstituicaoRepository);
    }

    public UnidadeInstituicao requireDelegaciaRegistroLotada(Usuario usuario, Long unidadeId) {
        if (unidadeId == null) {
            throw new IllegalArgumentException("unidadeRegistroId e obrigatorio para boletim de ocorrencia digital.");
        }
        UnidadeInstituicao unidade = findUnidade(unidadeId);
        if (!isDelegaciaAtivaComTerritorio(unidade)) {
            throw new IllegalStateException("A unidade de registro deve ser uma delegacia institucional ativa com UF e comarca.");
        }
        requireLotacaoDiretaNaDelegaciaComTerritorio(usuario, unidade);
        return unidade;
    }

    public UnidadeInstituicao requireDelegaciaApuracaoLotada(Usuario usuario, Long unidadeId) {
        if (unidadeId == null) {
            throw new IllegalArgumentException("unidadeApuracaoId é obrigatório para inquérito policial digital.");
        }
        UnidadeInstituicao unidade = findUnidade(unidadeId);
        if (!isDelegaciaAtivaComTerritorio(unidade)) {
            throw new IllegalArgumentException("A unidade de apuração deve ser uma delegacia institucional ativa.");
        }
        requireLotacaoDiretaNaUnidadeAtual(usuario, unidade);
        return unidade;
    }

    public UnidadeInstituicao requireDelegaciaDiligenciaLotada(Usuario usuario, Long unidadeId) {
        UnidadeInstituicao unidade = findUnidade(unidadeId);
        if (!isDelegaciaAtivaComTerritorio(unidade)) {
            throw new IllegalStateException("A diligência deve partir de delegacia institucional ativa com UF e comarca.");
        }
        requireLotacaoDiretaNaDelegaciaComTerritorio(usuario, unidade);
        return unidade;
    }

    public void requireDelegaciaAtivaComTerritorio(UnidadeInstituicao unidade) {
        if (!isDelegaciaAtivaComTerritorio(unidade)) {
            throw new IllegalStateException("A unidade deve ser uma delegacia institucional ativa com UF e comarca.");
        }
    }

    public void requireLotacaoDiretaNaDelegaciaComTerritorio(Usuario usuario, UnidadeInstituicao unidade) {
        requireDelegaciaAtivaComTerritorio(unidade);
        boolean lotado = delegaciasAtivasComTerritorioDoUsuario(usuario).stream()
                .anyMatch(ativa -> Objects.equals(ativa.getId(), unidade.getId()));
        if (!lotado) {
            throw new IllegalStateException("Usuario sem lotacao ativa na delegacia informada.");
        }
    }

    public List<UnidadeInstituicao> delegaciasAtivasComTerritorioDoUsuario(Usuario usuario) {
        return lotacaoInstituicaoRepository.findAtivasByUsuario(usuario).stream()
                .map(LotacaoInstituicao::getUnidade)
                .filter(Objects::nonNull)
                .filter(this::isDelegaciaAtivaComTerritorio)
                .toList();
    }

    public boolean hasLotacaoDiretaNaUnidadeAtual(Usuario usuario, UnidadeInstituicao unidade) {
        if (unidade == null || unidade.getId() == null) {
            return false;
        }
        return unidadesAtivasDoUsuarioAtual(usuario).stream()
                .anyMatch(ativa -> Objects.equals(ativa.getId(), unidade.getId()));
    }

    public void requireLotacaoDiretaNaUnidadeAtual(Usuario usuario, UnidadeInstituicao unidade) {
        if (!hasLotacaoDiretaNaUnidadeAtual(usuario, unidade)) {
            throw new IllegalStateException("Usuário sem lotação ativa na delegacia informada.");
        }
    }

    public List<UnidadeInstituicao> unidadesAtivasDoUsuarioAtual(Usuario usuario) {
        return lotacaoInstituicaoRepository.findAtivasByUsuario(usuario).stream()
                .map(LotacaoInstituicao::getUnidade)
                .filter(Objects::nonNull)
                .toList();
    }

    public void requireMesmoRegistroInstitucional(BoletimOcorrenciaDigital boletim, InqueritoPolicialDigital inquerito) {
        UnidadeInstituicao unidadeBoletim = boletim.getUnidadeRegistro();
        UnidadeInstituicao unidadeInquerito = inquerito.getUnidadeApuracao();
        requireMesmaDelegacia(unidadeBoletim, unidadeInquerito, "Boletim e inquerito devem pertencer a mesma delegacia institucional.");
    }

    public void requireInqueritoDaDelegacia(InqueritoPolicialDigital inquerito, UnidadeInstituicao unidadeApuracao) {
        requireMesmaDelegacia(inquerito.getUnidadeApuracao(), unidadeApuracao, "Inquérito não pertence à delegacia informada.");
    }

    public boolean isDelegaciaAtivaComTerritorio(UnidadeInstituicao unidade) {
        return unidade != null
                && unidade.getTipo() == TipoUnidadeInstitucional.DELEGACIA
                && unidade.getStatusUnidade() == StatusUnidadeInstitucional.ATIVA
                && hasText(unidade.getUf())
                && hasText(unidade.getComarca());
    }

    private void requireMesmaDelegacia(UnidadeInstituicao primeira, UnidadeInstituicao segunda, String mensagem) {
        if (primeira == null || segunda == null || !Objects.equals(primeira.getId(), segunda.getId())) {
            throw new IllegalStateException(mensagem);
        }
    }

    private UnidadeInstituicao findUnidade(Long unidadeId) {
        return unidadeInstituicaoRepository.findById(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UnidadeInstituicao", unidadeId));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
