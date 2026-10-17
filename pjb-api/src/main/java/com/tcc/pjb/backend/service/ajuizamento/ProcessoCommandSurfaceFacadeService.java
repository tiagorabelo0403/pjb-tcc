package com.tcc.pjb.backend.service.ajuizamento;

import com.tcc.pjb.backend.command.AjuizarProcessoCommand;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.mapper.ProcessoMapper;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.dto.ProcessoResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.ProcessoResponseAssemblerService;
import com.tcc.pjb.backend.service.document.SmartFileSplitter;
import com.tcc.pjb.backend.service.processual.representacao.PeticaoInicialSemAdvogadoCabimentoService;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProcessoCommandSurfaceFacadeService {

    private final AjuizarProcessoCommand ajuizarProcessoCommand;
    private final SmartFileSplitter smartFileSplitter;
    private final ProcessoMapper processoMapper;
    private final ProcessoResponseAssemblerService processoResponseAssemblerService;
    private final CurrentUserService currentUserService;
    private final PeticaoInicialSemAdvogadoCabimentoService cabimentoSemAdvogadoService;

    public ProcessoCommandSurfaceFacadeService(AjuizarProcessoCommand ajuizarProcessoCommand,
                                               SmartFileSplitter smartFileSplitter,
                                               ProcessoMapper processoMapper,
                                               ProcessoResponseAssemblerService processoResponseAssemblerService,
                                               CurrentUserService currentUserService,
                                               PeticaoInicialSemAdvogadoCabimentoService cabimentoSemAdvogadoService) {
        this.ajuizarProcessoCommand = Objects.requireNonNull(ajuizarProcessoCommand);
        this.smartFileSplitter = Objects.requireNonNull(smartFileSplitter);
        this.processoMapper = Objects.requireNonNull(processoMapper);
        this.processoResponseAssemblerService = Objects.requireNonNull(processoResponseAssemblerService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.cabimentoSemAdvogadoService = Objects.requireNonNull(cabimentoSemAdvogadoService);
    }

    public ProcessoResponse ajuizar(ProcessoRequest request, List<MultipartFile> arquivos) {
        Objects.requireNonNull(request, "request");

        Usuario advogado = currentUserService.getRequired();
        List<Attachment> anexosSeguros = smartFileSplitter.processarArquivos(
                normalizeFiles(arquivos), request.getAnexosDeclarados());
        Processo processo = processoMapper.toEntity(request);
        exigirCabimentoDoPeticionantePessoal(advogado, processo);
        Processo salvo = ajuizarProcessoCommand.execute(processo, advogado, request, anexosSeguros, request.isJuizo100Digital());
        return processoResponseAssemblerService.toResponse(salvo, request);
    }

    private void exigirCabimentoDoPeticionantePessoal(Usuario usuario, Processo processo) {
        if (PeticaoInicialSemAdvogadoCabimentoService.peticionaSemAdvogado(usuario)) {
            cabimentoSemAdvogadoService.exigirCabimento(processo.getRito(), processo.getValorCausa(), processo.getTipoJustica());
        }
    }

    private List<MultipartFile> normalizeFiles(List<MultipartFile> arquivos) {
        if (arquivos == null || arquivos.isEmpty()) {
            return List.of();
        }
        return arquivos.stream()
                .filter(Objects::nonNull)
                .filter(arquivo -> !arquivo.isEmpty())
                .toList();
    }
}
