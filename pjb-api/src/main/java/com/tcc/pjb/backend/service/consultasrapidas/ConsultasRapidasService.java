package com.tcc.pjb.backend.service.consultasrapidas;

import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.consultasrapidas.QuickBuscaResponse;
import com.tcc.pjb.backend.model.dto.consultasrapidas.QuickConsultaResponse;
import com.tcc.pjb.backend.model.dto.consultasrapidas.QuickDocumentoPublicoDTO;
import com.tcc.pjb.backend.model.dto.consultasrapidas.QuickProcessoResumoDTO;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConsultasRapidasService {

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final DocumentoPaginaRepository paginaRepository;
    private final PjbAuthorizationService authorizationService;
    private final CurrentUserService currentUserService;

    
    @Transactional(readOnly = true)
    public QuickConsultaResponse consultarPorNumero(String numero) {
        if (numero == null || numero.isBlank()) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "numero");
        }

        Processo p = processoRepository.findByNumeroUnificado(numero)
                .or(() -> processoRepository.findByNumeroProcesso(numero))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", numero));

        QuickProcessoResumoDTO resumo = toResumo(p);
        NivelSigilo sigilo = p.getNivelSigilo() == null ? NivelSigilo.PUBLICO : p.getNivelSigilo();

        
        if (!sigilo.exigeCredencial()) {
            List<QuickDocumentoPublicoDTO> docs = documentoRepository.findByProcessoId(p.getId()).stream()
                    .filter(d -> {
                        NivelSigilo ds = d.getNivelSigilo() == null ? NivelSigilo.PUBLICO : d.getNivelSigilo();
                        DocumentoCategoria c = d.getCategoria() == null ? DocumentoCategoria.PUBLICO : d.getCategoria();
                        return !ds.exigeCredencial() && c == DocumentoCategoria.PUBLICO;
                    })
                    .map(d -> new QuickDocumentoPublicoDTO(
                            d.getId().toString(),
                            d.getTitulo(),
                            d.getNomeOriginal(),
                            d.getContentType(),
                            d.getTamanhoBytes(),
                            (int) paginaRepository.countByDocumentoId(d.getId()),
                            d.getCriadoEm(),
                            "/api/v1/public/processos/documentos/" + d.getId() + "/pdf"
                    ))
                    .toList();

            return new QuickConsultaResponse(resumo, null, docs);
        }

        
        
        AuthzDecision d = authorizationService.canReadProcesso(p);

        
        if (sigilo.getNivel() >= 4 && !d.allowed()) {
            throw new RecursoNaoEncontradoException("Processo", numero);
        }
        String msg;
        if (d.allowed()) {
            msg = "Processo em segredo de justiça (acesso autorizado). Use a área completa para visualizar a íntegra.";
        } else {
            msg = "Processo em segredo de justiça. Conteúdo restrito às partes, advogados habilitados e perfis institucionais.";
        }
        return new QuickConsultaResponse(resumo, msg, List.of());
    }

    
    @Transactional(readOnly = true)
    public QuickBuscaResponse buscar(String cpf, String nome, String numero, Integer page, Integer size) {
        Usuario u = currentUserService.getRequired();

        String cpfNorm = normalizeCpf(cpf);
        String nomeNorm = normalizeNome(nome);
        String numeroNorm = numero != null ? numero.trim() : null;

        if ((cpfNorm == null || cpfNorm.isBlank()) && (nomeNorm == null || nomeNorm.isBlank()) && (numeroNorm == null || numeroNorm.isBlank())) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "cpf|nome|numero")
                    .addMetadado("motivo", "informe ao menos um parâmetro");
        }

        
        if (u.getTipoUsuario() == TipoUsuario.CIDADAO) {
            if (cpfNorm == null || cpfNorm.isBlank()) {
                throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "cpf")
                        .addMetadado("motivo", "cidadão deve consultar pelo próprio CPF");
            }
            if (u.getCpf() == null || !cpfNorm.equals(normalizeCpf(u.getCpf()))) {
                throw new AccessDeniedPjbException("Consulta por CPF de terceiro não é permitida para perfil CIDADÃO.");
            }
            
            nomeNorm = null;
        }

        int p = page == null ? 0 : Math.max(0, page);
        int s = size == null ? 10 : Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(p, s);

        Page<Processo> result = processoRepository.searchQuick(cpfNorm, nomeNorm, numeroNorm, pageable);

        
        List<QuickProcessoResumoDTO> itens = result.getContent().stream()
                .filter(proc -> {
                    NivelSigilo ns = proc.getNivelSigilo() == null ? NivelSigilo.PUBLICO : proc.getNivelSigilo();
                    if (!ns.exigeCredencial()) return true;
                    return authorizationService.canReadProcesso(proc).allowed();
                })
                .map(this::toResumo)
                .toList();

        return new QuickBuscaResponse(itens, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private QuickProcessoResumoDTO toResumo(Processo p) {
        NivelSigilo sigilo = p.getNivelSigilo() == null ? NivelSigilo.PUBLICO : p.getNivelSigilo();
        boolean isSigiloso = sigilo.exigeCredencial();
        String numero = (p.getNumeroUnificado() != null && !p.getNumeroUnificado().isBlank())
                ? p.getNumeroUnificado()
                : p.getNumeroProcesso();
        return new QuickProcessoResumoDTO(
                p.getId(),
                numero,
                p.getClasseProcessual(),
                p.getAssunto(),
                sigilo,
                isSigiloso,
                p.getDataUltimaMovimentacao()
        );
    }

    private static String normalizeCpf(String cpf) {
        if (cpf == null) return null;
        String digits = cpf.replaceAll("\\D+", "");
        if (digits.isBlank()) return null;
        if (digits.length() != 11) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CPF_INVALIDO, "cpf")
                    .addMetadado("motivo", "CPF deve ter 11 dígitos")
                    .addMetadado("recebido", cpf);
        }
        if (!isValidCpf(digits)) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CPF_INVALIDO, "cpf")
                    .addMetadado("motivo", "CPF não passa na validação de dígitos verificadores")
                    .addMetadado("recebido", cpf);
        }
        return digits;
    }

    private static boolean isValidCpf(String cpf11) {
        
        boolean allSame = true;
        for (int i = 1; i < cpf11.length(); i++) {
            if (cpf11.charAt(i) != cpf11.charAt(0)) {
                allSame = false;
                break;
            }
        }
        if (allSame) return false;

        int d1 = calcCpfDigit(cpf11, 9, 10);
        int d2 = calcCpfDigit(cpf11, 10, 11);
        return (cpf11.charAt(9) - '0') == d1 && (cpf11.charAt(10) - '0') == d2;
    }

    private static int calcCpfDigit(String cpf, int len, int weightStart) {
        int sum = 0;
        int weight = weightStart;
        for (int i = 0; i < len; i++) {
            sum += (cpf.charAt(i) - '0') * weight--;
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }

    private static String normalizeNome(String nome) {
        if (nome == null) return null;
        String n = nome.trim();
        if (n.isBlank()) return null;
        
        n = n.replaceAll("\\s+", " ");
        
        if (n.length() < 2) {
            throw new ErroDeValidacaoException(TipoErroValidacao.FORMATO_INVALIDO, "nome")
                    .addMetadado("motivo", "termo muito curto");
        }
        return n.toLowerCase(Locale.ROOT);
    }
}
