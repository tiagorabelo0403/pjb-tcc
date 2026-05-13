package com.tcc.pjb.backend.service.identity;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaAlias;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaAuditoria;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.model.repository.IdentidadeJuridicaAliasRepository;
import com.tcc.pjb.backend.model.repository.IdentidadeJuridicaAuditoriaRepository;
import com.tcc.pjb.backend.model.repository.IdentidadeJuridicaNacionalRepository;

@SuppressWarnings("ConstantValue")
@Service
public class IdentidadeJuridicaNacionalService {

    private final IdentidadeJuridicaNacionalRepository identidadeRepository;
    private final IdentidadeJuridicaAliasRepository aliasRepository;
    private final IdentidadeJuridicaAuditoriaRepository auditoriaRepository;
    private final DocumentoNacionalValidator documentoValidator;

    public IdentidadeJuridicaNacionalService(IdentidadeJuridicaNacionalRepository identidadeRepository,
                                             IdentidadeJuridicaAliasRepository aliasRepository,
                                             IdentidadeJuridicaAuditoriaRepository auditoriaRepository,
                                             DocumentoNacionalValidator documentoValidator) {
        this.identidadeRepository = Objects.requireNonNull(identidadeRepository);
        this.aliasRepository = Objects.requireNonNull(aliasRepository);
        this.auditoriaRepository = Objects.requireNonNull(auditoriaRepository);
        this.documentoValidator = Objects.requireNonNull(documentoValidator);
    }

    @Transactional
    public IdentidadeJuridicaNacional sincronizarUsuario(Usuario usuario) {
        Objects.requireNonNull(usuario, "Usuario obrigatorio");
        if (usuario.getCpf() == null || usuario.getCpf().isBlank()) {
            throw new IllegalArgumentException("Usuario sem CPF para sincronizacao nacional");
        }

        String documento = documentoValidator.normalizarDocumento(usuario.getCpf());
        documentoValidator.validarDocumento(documento);
        String nomeCanonico = documentoValidator.normalizarNomeCanonico(usuario.getNome());
        String chavePesquisa = documentoValidator.gerarChavePesquisa(usuario.getNome());
        String documentoHash = Hashes.sha256Hex(documento);
        DocumentoNacionalValidator.TipoDocumento tipoDocumento = documentoValidator.detectarTipoDocumento(documento);

        IdentidadeJuridicaNacional identidade = identidadeRepository
                .findByTipoDocumentoAndDocumento(tipoDocumento, documento)
                .orElseGet(() -> criarNovaIdentidade(tipoDocumento, documento, documentoHash, nomeCanonico, chavePesquisa));

        atualizarNomeSeNecessario(identidade, nomeCanonico, chavePesquisa, usuario.getId());
        sincronizarPapeis(identidade, usuario.getTipoUsuario());
        sincronizarOab(identidade, usuario, usuario.getId());
        identidade.atualizarConfianca(calcularNivelConfianca(identidade));

        IdentidadeJuridicaNacional salvo = identidadeRepository.save(identidade);
        registrarAuditoria(salvo,
                "SYNC_USUARIO",
                "USUARIO_SERVICE",
                usuario.getId() != null ? "usuario:" + usuario.getId() : null,
                "Sincronizacao nacional do cadastro de usuario",
                payloadSyncHash(salvo, usuario));
        return salvo;
    }

    @Transactional
    public Optional<IdentidadeJuridicaNacional> vincularGovBrPorDocumento(String documentoRaw, String govBrSubject) {
        String documento = documentoValidator.normalizarDocumento(documentoRaw);
        if (documento.isBlank()) {
            return Optional.empty();
        }
        DocumentoNacionalValidator.TipoDocumento tipoDocumento = documentoValidator.detectarTipoDocumento(documento);
        Optional<IdentidadeJuridicaNacional> opt = identidadeRepository.findByTipoDocumentoAndDocumento(tipoDocumento, documento);
        opt.ifPresent(identidade -> {
            String govBrMaterial = documentoValidator.normalizarDocumento(govBrSubject);
            if (govBrMaterial.isBlank()) {
                govBrMaterial = documento;
            }
            String subHash = Hashes.sha256Hex(govBrMaterial);
            identidade.vincularGovBr(subHash, IdentidadeJuridicaNacional.GovBrNivel.BRONZE);
            identidade.atualizarOrigem(IdentidadeJuridicaNacional.OrigemCadastro.GOV_BR);
            identidade.atualizarConfianca(calcularNivelConfianca(identidade));
            identidadeRepository.save(identidade);
            registrarAuditoria(identidade,
                    "GOVBR_LINK",
                    "GOVBR",
                    null,
                    "Vinculo Gov.br confirmado para identidade nacional",
                    Hashes.sha256Hex(identidade.getDocumento() + "|" + identidade.getGovBrNivel()));
        });
        return opt;
    }


    @Transactional
    public IdentidadeJuridicaNacional resolverOuCriarPorDocumento(String documentoRaw,
                                                                  String nome,
                                                                  IdentidadeJuridicaNacional.OrigemCadastro origemCadastro,
                                                                  Collection<IdentidadeJuridicaNacional.PapelNacional> papeis) {
        String documento = documentoValidator.normalizarDocumento(documentoRaw);
        documentoValidator.validarDocumento(documento);
        DocumentoNacionalValidator.TipoDocumento tipoDocumento = documentoValidator.detectarTipoDocumento(documento);
        String documentoHash = Hashes.sha256Hex(documento);
        String nomeBase = nome == null || nome.isBlank() ? "NAO INFORMADO" : nome;
        String nomeCanonico = documentoValidator.normalizarNomeCanonico(nomeBase);
        String chavePesquisa = documentoValidator.gerarChavePesquisa(nomeBase);

        IdentidadeJuridicaNacional identidade = identidadeRepository
                .findByTipoDocumentoAndDocumento(tipoDocumento, documento)
                .orElseGet(() -> criarNovaIdentidade(tipoDocumento, documento, documentoHash, nomeCanonico, chavePesquisa));

        if (!"NAO INFORMADO".equals(nomeCanonico) && !Objects.equals(identidade.getNomeCanonico(), nomeCanonico)) {
            registrarAliasSeAusente(identidade,
                    IdentidadeJuridicaAlias.TipoAlias.NOME_LEGADO,
                    identidade.getNomeCanonico(),
                    identidade.getChavePesquisa());
            identidade.atualizarNomeCanonico(nomeCanonico, chavePesquisa);
        }

        if (devePromoverOrigem(identidade.getOrigemCadastro(), origemCadastro)) {
            identidade.atualizarOrigem(origemCadastro);
        }

        if (papeis != null) {
            for (IdentidadeJuridicaNacional.PapelNacional papel : papeis) {
                if (papel != null) {
                    identidade.adicionarPapel(papel);
                }
            }
        }

        identidade.atualizarConfianca(calcularNivelConfianca(identidade));
        IdentidadeJuridicaNacional salvo = identidadeRepository.save(identidade);
        String origemAuditoria = origemCadastro == null ? "PRONTUARIO" : origemCadastro.name();
        registrarAuditoria(salvo,
                "IDENTIDADE_RESOLVIDA",
                origemAuditoria,
                null,
                "Identidade nacional resolvida para documento processual",
                Hashes.sha256Hex(documento + "|" + salvo.getNomeCanonico()));
        return salvo;
    }

    @Transactional(readOnly = true)
    public Optional<IdentidadeJuridicaNacional> buscarPorDocumento(String documentoRaw) {
        String documento = documentoValidator.normalizarDocumento(documentoRaw);
        if (documento.isBlank()) {
            return Optional.empty();
        }
        DocumentoNacionalValidator.TipoDocumento tipoDocumento = documentoValidator.detectarTipoDocumento(documento);
        return identidadeRepository.findByTipoDocumentoAndDocumento(tipoDocumento, documento);
    }

    @Transactional(readOnly = true)
    public Map<UUID, IdentidadeResumo> resumirPorIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<IdentidadeJuridicaNacional> identidades = identidadeRepository.findAllByIdIn(ids);
        Map<UUID, IdentidadeResumo> mapa = new LinkedHashMap<>();
        for (IdentidadeJuridicaNacional identidade : identidades) {
            mapa.put(identidade.getId(), IdentidadeResumo.of(identidade, documentoValidator.mascararDocumento(identidade.getDocumento())));
        }
        return mapa;
    }

    @Transactional(readOnly = true)
    public Optional<IdentidadeResumo> resumirPorId(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return identidadeRepository.findById(id)
                .map(identidade -> IdentidadeResumo.of(identidade, documentoValidator.mascararDocumento(identidade.getDocumento())));
    }

    private IdentidadeJuridicaNacional criarNovaIdentidade(DocumentoNacionalValidator.TipoDocumento tipoDocumento,
                                                           String documento,
                                                           String documentoHash,
                                                           String nomeCanonico,
                                                           String chavePesquisa) {
        IdentidadeJuridicaNacional identidade = new IdentidadeJuridicaNacional(
                UUID.randomUUID(),
                tipoDocumento,
                documento,
                documentoHash,
                documentoValidator.formatarDocumento(documento),
                nomeCanonico,
                chavePesquisa,
                construirProntuarioUri(tipoDocumento, documento),
                IdentidadeJuridicaNacional.OrigemCadastro.USUARIO_SERVICE
        );
        identidade.atualizarConfianca(IdentidadeJuridicaNacional.NivelConfiancaIdentidade.AUTODECLARADA);
        identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.PARTE);
        return identidade;
    }

    private void atualizarNomeSeNecessario(IdentidadeJuridicaNacional identidade,
                                           String nomeCanonico,
                                           String chavePesquisa,
                                           Long usuarioId) {
        if (identidade.getNomeCanonico().equals(nomeCanonico)) {
            return;
        }
        registrarAliasSeAusente(identidade, IdentidadeJuridicaAlias.TipoAlias.NOME_LEGADO, identidade.getNomeCanonico(), identidade.getChavePesquisa());
        identidade.atualizarNomeCanonico(nomeCanonico, chavePesquisa);
        registrarAuditoria(identidade,
                "NOME_ATUALIZADO",
                "USUARIO_SERVICE",
                usuarioId != null ? "usuario:" + usuarioId : null,
                "Nome canonico atualizado na identidade nacional",
                Hashes.sha256Hex(nomeCanonico));
    }

    private void sincronizarOab(IdentidadeJuridicaNacional identidade, Usuario usuario, Long usuarioId) {
        if (usuario.getTipoUsuario() != null && (usuario.getTipoUsuario() == TipoUsuario.ADVOGADO || usuario.getTipoUsuario() == TipoUsuario.OAB_PRESIDENTE_SECCIONAL)) {
            if (usuario.getOabNumero() != null && !usuario.getOabNumero().isBlank() && usuario.getOabUf() != null && !usuario.getOabUf().isBlank()) {
                identidade.registrarOab(usuario.getOabNumero(), usuario.getOabUf().toUpperCase(Locale.ROOT), IdentidadeJuridicaNacional.OabStatus.PENDENTE);
                registrarAliasSeAusente(identidade,
                        IdentidadeJuridicaAlias.TipoAlias.OAB_LEGADA,
                        usuario.getOabNormalizada() != null ? usuario.getOabNormalizada() : usuario.getOab(),
                        normalizeOptional(usuario.getOabNormalizada() != null ? usuario.getOabNormalizada() : usuario.getOab()));
                registrarAuditoria(identidade,
                        "OAB_SYNC",
                        "USUARIO_SERVICE",
                        usuarioId != null ? "usuario:" + usuarioId : null,
                        "Inscricao OAB sincronizada para identidade nacional",
                        Hashes.sha256Hex((usuario.getOabUf() + "|" + usuario.getOabNumero()).toUpperCase(Locale.ROOT)));
            }
        } else {
            identidade.removerOabPendente();
        }
    }

    private void sincronizarPapeis(IdentidadeJuridicaNacional identidade, TipoUsuario tipoUsuario) {
        identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.SUJEITO_PROCESSUAL);
        identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.PARTE);
        if (tipoUsuario == null) {
            return;
        }
        if (tipoUsuario.isAdvocacia() || tipoUsuario == TipoUsuario.OAB_PRESIDENTE_SECCIONAL) {
            identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.ADVOGADO);
            identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.REPRESENTANTE_LEGAL);
        }
        if (tipoUsuario.isMagistratura()) {
            identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.MAGISTRADO);
        }
        if (tipoUsuario.isMinisterioPublico()) {
            identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.MEMBRO_MP);
        }
        if (tipoUsuario.isDefensoriaPublica()) {
            identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.DEFENSOR_PUBLICO);
        }
        if (tipoUsuario.isPerito() || tipoUsuario.isAuxiliarJustica()) {
            identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.PERITO);
        }
        if (tipoUsuario.isServidorJudiciario() || tipoUsuario == TipoUsuario.OFICIAL_JUSTICA || tipoUsuario.isAdministradorSistema()) {
            identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.SERVENTUARIO);
        }
        if (tipoUsuario == TipoUsuario.DELEGADO_POLICIA || tipoUsuario == TipoUsuario.AGENTE_POLICIAL) {
            identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.AUTORIDADE_POLICIAL);
        }
        if (tipoUsuario.isProcuradoria()) {
            identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.PROCURADOR_PUBLICO);
            identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.REPRESENTANTE_LEGAL);
        }
        if (tipoUsuario.isSaude()) {
            identidade.adicionarPapel(IdentidadeJuridicaNacional.PapelNacional.ORGAO_PUBLICO);
        }
    }

    private IdentidadeJuridicaNacional.NivelConfiancaIdentidade calcularNivelConfianca(IdentidadeJuridicaNacional identidade) {
        Set<IdentidadeJuridicaNacional.NivelConfiancaIdentidade> niveis = EnumSet.of(IdentidadeJuridicaNacional.NivelConfiancaIdentidade.AUTODECLARADA);
        if (identidade.getReceitaStatus() == IdentidadeJuridicaNacional.ReceitaStatus.VALIDADO) {
            niveis.add(IdentidadeJuridicaNacional.NivelConfiancaIdentidade.DOCUMENTAL);
        }
        if (identidade.getGovBrNivel() != IdentidadeJuridicaNacional.GovBrNivel.NAO_VINCULADO) {
            niveis.add(IdentidadeJuridicaNacional.NivelConfiancaIdentidade.FEDERADA);
        }
        if (identidade.getOabStatus() == IdentidadeJuridicaNacional.OabStatus.ATIVO || identidade.getOabStatus() == IdentidadeJuridicaNacional.OabStatus.PENDENTE) {
            niveis.add(IdentidadeJuridicaNacional.NivelConfiancaIdentidade.PROFISSIONAL_VALIDADA);
        }
        if (identidade.getReceitaStatus() == IdentidadeJuridicaNacional.ReceitaStatus.VALIDADO
                && identidade.getGovBrNivel() != IdentidadeJuridicaNacional.GovBrNivel.NAO_VINCULADO) {
            niveis.add(IdentidadeJuridicaNacional.NivelConfiancaIdentidade.MULTIFONTE);
        }
        if (identidade.getOrigemCadastro() == IdentidadeJuridicaNacional.OrigemCadastro.CNJ
                || identidade.getOrigemCadastro() == IdentidadeJuridicaNacional.OrigemCadastro.TRIBUNAL
                || identidade.getOrigemCadastro() == IdentidadeJuridicaNacional.OrigemCadastro.API_INSTITUCIONAL
                || identidade.getOrigemCadastro() == IdentidadeJuridicaNacional.OrigemCadastro.MIGRACAO_HOMOLOGADA) {
            niveis.add(IdentidadeJuridicaNacional.NivelConfiancaIdentidade.INSTITUCIONAL);
        }
        return niveis.stream().max(Enum::compareTo).orElse(IdentidadeJuridicaNacional.NivelConfiancaIdentidade.AUTODECLARADA);
    }

    private void registrarAliasSeAusente(IdentidadeJuridicaNacional identidade,
                                         IdentidadeJuridicaAlias.TipoAlias tipoAlias,
                                         String valor,
                                         String valorNormalizado) {
        if (valor == null || valor.isBlank() || valorNormalizado == null || valorNormalizado.isBlank()) {
            return;
        }
        boolean exists = aliasRepository.findByIdentidade_IdAndTipoAliasAndValorNormalizado(identidade.getId(), tipoAlias, valorNormalizado).isPresent();
        if (!exists) {
            aliasRepository.save(new IdentidadeJuridicaAlias(identidade, tipoAlias, valor, valorNormalizado));
        }
    }

    private void registrarAuditoria(IdentidadeJuridicaNacional identidade,
                                    String evento,
                                    String origem,
                                    String ator,
                                    String descricao,
                                    String payloadHash) {
        auditoriaRepository.save(new IdentidadeJuridicaAuditoria(identidade, evento, origem, ator, descricao, payloadHash));
    }


    private static boolean devePromoverOrigem(IdentidadeJuridicaNacional.OrigemCadastro atual,
                                              IdentidadeJuridicaNacional.OrigemCadastro novaOrigem) {
        if (novaOrigem == null || atual == novaOrigem) {
            return false;
        }
        if (atual == null) {
            return true;
        }
        return switch (novaOrigem) {
            case CNJ, TRIBUNAL, API_INSTITUCIONAL, MIGRACAO_HOMOLOGADA, RECEITA_FEDERAL, OAB_NACIONAL -> true;
            case GOV_BR -> atual == IdentidadeJuridicaNacional.OrigemCadastro.AUTOCADASTRO
                    || atual == IdentidadeJuridicaNacional.OrigemCadastro.USUARIO_SERVICE;
            case USUARIO_SERVICE -> atual == IdentidadeJuridicaNacional.OrigemCadastro.AUTOCADASTRO;
            case AUTOCADASTRO -> false;
        };
    }

    private static String construirProntuarioUri(DocumentoNacionalValidator.TipoDocumento tipoDocumento, String documento) {
        return "pjb://prontuario/" + tipoDocumento.name().toLowerCase(Locale.ROOT) + "/" + documento;
    }

    private static String payloadSyncHash(IdentidadeJuridicaNacional identidade, Usuario usuario) {
        return Hashes.sha256Hex(
                identidade.getDocumento() + "|"
                        + identidade.getNomeCanonico() + "|"
                        + String.valueOf(usuario.getTipoUsuario()) + "|"
                        + String.valueOf(usuario.getId())
        );
    }

    private static String normalizeOptional(String valor) {
        if (valor == null) {
            return null;
        }
        String normalized = valor.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    public record IdentidadeResumo(
            UUID id,
            String nomeCanonico,
            String documentoMascarado,
            String prontuarioNacionalUri,
            String nivelConfianca,
            String receitaStatus,
            String oabStatus,
            boolean govBrVinculado
    ) {
        public static IdentidadeResumo of(IdentidadeJuridicaNacional identidade, String documentoMascarado) {
            return new IdentidadeResumo(
                    identidade.getId(),
                    identidade.getNomeCanonico(),
                    documentoMascarado,
                    identidade.getProntuarioNacionalUri(),
                    identidade.getNivelConfianca().name(),
                    identidade.getReceitaStatus().name(),
                    identidade.getOabStatus().name(),
                    identidade.getGovBrNivel() != IdentidadeJuridicaNacional.GovBrNivel.NAO_VINCULADO
            );
        }
    }
}
