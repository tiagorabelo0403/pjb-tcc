package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.SefazNfeCadastroResolver;
import com.tcc.pjb.backend.model.entity.Processo;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CitacaoSefazCadastroEnrichmentService {

    private final ObjectProvider<SefazNfeCadastroResolver> sefazCadastroResolverProvider;

    public CitacaoSefazCadastroEnrichmentService(ObjectProvider<SefazNfeCadastroResolver> sefazCadastroResolverProvider) {
        this.sefazCadastroResolverProvider = sefazCadastroResolverProvider;
    }

    public Optional<SefazNfeCadastroResolver.CadastroSefazNfe> consultarCadastroSefaz(String cnpj, Processo processo) {
        SefazNfeCadastroResolver resolver = sefazCadastroResolverProvider.getIfAvailable();
        if (resolver == null || processo == null) {
            return Optional.empty();
        }
        String uf = processo.getUf();
        if ((uf == null || uf.isBlank()) && processo.getJurisdicao() != null) {
            uf = processo.getJurisdicao().getUf();
        }
        return resolver.resolver(cnpj, uf, processo.getTribunalCodigoRoteado());
    }

    public void enriquecerExpedicaoComCadastroSefaz(ExpedicaoJudicial expedicao, Processo processo) {
        if (expedicao == null || processo == null || expedicao.getDestinatarioDocumento() == null) {
            return;
        }
        consultarCadastroSefaz(expedicao.getDestinatarioDocumento(), processo).ifPresent(cadastro -> {
            if ((expedicao.getDestinatarioEmail() == null || expedicao.getDestinatarioEmail().isBlank()) && cadastro.possuiEmailOperacional()) {
                expedicao.setDestinatarioEmail(cadastro.emailOperacional());
            }
            if ((expedicao.getDestinatarioTelefone() == null || expedicao.getDestinatarioTelefone().isBlank()) && cadastro.telefoneOperacional() != null && !cadastro.telefoneOperacional().isBlank()) {
                expedicao.setDestinatarioTelefone(cadastro.telefoneOperacional());
            }
            if ((expedicao.getDestinatarioEnderecoEntrega() == null || expedicao.getDestinatarioEnderecoEntrega().isBlank()) && cadastro.possuiEnderecoFisico()) {
                expedicao.setDestinatarioEnderecoEntrega(cadastro.enderecoEstabelecimento());
            }
        });
    }
}
