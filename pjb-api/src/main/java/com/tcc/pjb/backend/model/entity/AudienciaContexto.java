package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.model.entity.enums.CategoriaAudiencia;
import com.tcc.pjb.backend.model.entity.enums.FormaRealizacao;
import com.tcc.pjb.backend.model.entity.enums.MeioAudiencia;
import com.tcc.pjb.backend.model.entity.enums.ModalidadeAudiencia;
import com.tcc.pjb.backend.model.entity.enums.ResultadoValidacaoAudiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusAudiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoAudiencia;
import com.tcc.pjb.backend.model.entity.enums.NivelSegurancaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.ModuloProcesso;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.service.audiencia.AudienciaValidator;
import java.util.Objects;
import java.util.Optional;

public class AudienciaContexto {

    

    private final TipoAudiencia tipoAudiencia;
    private final ModalidadeAudiencia modalidade;
    private final CategoriaAudiencia categoria;

    

    private final RitoProcessual rito;
    private final FaseProcessual fase;
    private final ModuloProcesso modulo;
    private final StatusProcesso statusProcesso;

    

    private final EsferaJurisdicao esfera;
    private final MateriaJurisdicao materia;
    private final NivelSegurancaInstitucional nivelSeguranca;

    

    private final MeioAudiencia meio;
    private final FormaRealizacao formaRealizacao;
    private final StatusAudiencia statusAudiencia;

    

    public AudienciaContexto(
            TipoAudiencia tipoAudiencia,
            ModalidadeAudiencia modalidade,
            RitoProcessual rito,
            FaseProcessual fase,
            ModuloProcesso modulo,
            StatusProcesso statusProcesso,
            EsferaJurisdicao esfera,
            MateriaJurisdicao materia,
            NivelSegurancaInstitucional nivelSeguranca,
            MeioAudiencia meio,
            FormaRealizacao formaRealizacao,
            StatusAudiencia statusAudiencia
    ) {
        this.tipoAudiencia = Objects.requireNonNull(tipoAudiencia);
        this.modalidade = Objects.requireNonNull(modalidade);
        this.categoria = modalidade.getCategoria();

        this.rito = Objects.requireNonNull(rito);
        this.fase = Objects.requireNonNull(fase);
        this.modulo = Objects.requireNonNull(modulo);
        this.statusProcesso = Objects.requireNonNull(statusProcesso);

        this.esfera = Objects.requireNonNull(esfera);
        this.materia = Objects.requireNonNull(materia);
        this.nivelSeguranca = Objects.requireNonNull(nivelSeguranca);

        this.meio = Objects.requireNonNull(meio);
        this.formaRealizacao = Objects.requireNonNull(formaRealizacao);
        this.statusAudiencia = Objects.requireNonNull(statusAudiencia);
    }

    

    public ResultadoValidacaoAudiencia validar() {

        
        if (nivelSeguranca == NivelSegurancaInstitucional.MAXIMO_CONSTITUCIONAL
                && categoria == CategoriaAudiencia.CONSENSUAL) {
            return ResultadoValidacaoAudiencia.NAO_ADMITIDA_NO_STF;
        }

        
        if (modalidade == ModalidadeAudiencia.UNA
                && rito != RitoProcessual.TRABALHISTA_SUMARISSIMO
                && rito != RitoProcessual.TRABALHISTA_ORDINARIO
                && rito != RitoProcessual.TRABALHISTA_SUMARIO_ALCADA) {
            return ResultadoValidacaoAudiencia.INCOMPATIVEL_COM_O_RITO;
        }

        
        if (modalidade.permiteAcordo()
                && fase == FaseProcessual.EXECUCAO) {
            return ResultadoValidacaoAudiencia.INCOMPATIVEL_COM_A_FASE_PROCESSUAL;
        }

        
        if (meio == MeioAudiencia.VIRTUAL
                && formaRealizacao == FormaRealizacao.PRESENCIAL_OBRIGATORIA) {
            return ResultadoValidacaoAudiencia.INCOMPATIVEL_COM_O_MEIO_DE_REALIZACAO;
        }

        
        if (!AudienciaValidator.validar(this)) {
            return ResultadoValidacaoAudiencia.INCOMPATIVEL_COM_O_MODULO_PROCESSUAL;
        }

        
        if (nivelSeguranca.exigeRegistroImutavel()
                && statusAudiencia == StatusAudiencia.CANCELADA) {
            return ResultadoValidacaoAudiencia.VALIDA_COM_RESTRICOES_REGIMENTAIS;
        }

        
        return ResultadoValidacaoAudiencia.VALIDA_CONSTITUCIONALMENTE;
    }

    

    public Optional<String> fundamentacaoLegal() {
        return Optional.ofNullable(
                AudienciaValidator.fundamentacao(this.tipoAudiencia)
        );
    }

    

    public TipoAudiencia getTipo() { return tipoAudiencia; }
    public ModalidadeAudiencia getModalidade() { return modalidade; }
    public CategoriaAudiencia getCategoria() { return categoria; }

    public RitoProcessual getRito() { return rito; }
    public FaseProcessual getFase() { return fase; }
    public ModuloProcesso getModulo() { return modulo; }
    public StatusProcesso getStatusProcesso() { return statusProcesso; }

    public EsferaJurisdicao getEsfera() { return esfera; }
    public MateriaJurisdicao getMateria() { return materia; }
    public NivelSegurancaInstitucional getNivelSeguranca() { return nivelSeguranca; }

    public MeioAudiencia getMeio() { return meio; }
    public FormaRealizacao getFormaRealizacao() { return formaRealizacao; }
    public StatusAudiencia getStatusAudiencia() { return statusAudiencia; }

    

    @Override
    public String toString() {
        return """
                AudiênciaContexto {
                  tipo=%s,
                  modalidade=%s,
                  categoria=%s,
                  rito=%s,
                  fase=%s,
                  módulo=%s,
                  esfera=%s,
                  matéria=%s,
                  meio=%s,
                  forma=%s,
                  segurança=%s,
                  statusAudiência=%s
                }
                """.formatted(
                tipoAudiencia,
                modalidade,
                categoria,
                rito,
                fase,
                modulo,
                esfera,
                materia,
                meio,
                formaRealizacao,
                nivelSeguranca,
                statusAudiencia
        );
    }
}
