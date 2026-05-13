package com.tcc.pjb.backend.model.dto.magistratura;

import java.util.List;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoGovernanceMetricas;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

public record MagistraturaOperationalContextResponse(
        Long userId,
        String nome,
        TipoUsuario tipoUsuario,
        String displayPerfil,
        String tratamento,
        String uf,
        String comarca,
        GrauJurisdicao grau,
        EsferaJurisdicao esfera,
        List<String> ritosProvaveis,
        List<String> capacidadesOperacionais,
        List<String> filasPrioritarias,
        List<String> camadasRecursais,
        List<String> canaisExecutivos,
        String modoAtuacao,
        String nivelSegurancaOperacional,
        List<String> widgetsEstrategicos,
        List<RitoOperationalLane> trilhasOperacionaisPorRito,
        boolean localizadorPessoasHabilitado,
        boolean acessoInfojud,
        boolean acessoSisbajud,
        boolean acessoSerasajud,
        List<PerfilDashboardPayload.ExternalSystemStatus> sistemasExternos,
        PessoaLocalizacaoGovernanceMetricas localizadorGovernado
) {
    public record RitoOperationalLane(String rito, List<String> atosPrioritarios, List<String> canaisPreferenciais) {
    }
}
