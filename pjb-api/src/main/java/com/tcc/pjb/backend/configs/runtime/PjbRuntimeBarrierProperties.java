package com.tcc.pjb.backend.configs.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.runtime.barrier")
public class PjbRuntimeBarrierProperties {

    private final Scheduling scheduling = new Scheduling();
    private final Integrations integrations = new Integrations();
    private final Features features = new Features();

    public Scheduling getScheduling() {
        return scheduling;
    }

    public Integrations getIntegrations() {
        return integrations;
    }

    public Features getFeatures() {
        return features;
    }

    public static class Scheduling {

        private boolean enabled = true;
        private boolean core = true;
        private boolean atendimento = true;
        private boolean comunicacaoJudicial = true;
        private boolean calendar = true;
        private boolean ui = true;
        private boolean govbr = true;
        private boolean integracoesExternas = true;
        private boolean digitalizacao = true;
        private boolean dje = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isCore() { return core; }
        public void setCore(boolean core) { this.core = core; }
        public boolean isAtendimento() { return atendimento; }
        public void setAtendimento(boolean atendimento) { this.atendimento = atendimento; }
        public boolean isComunicacaoJudicial() { return comunicacaoJudicial; }
        public void setComunicacaoJudicial(boolean comunicacaoJudicial) { this.comunicacaoJudicial = comunicacaoJudicial; }
        public boolean isCalendar() { return calendar; }
        public void setCalendar(boolean calendar) { this.calendar = calendar; }
        public boolean isUi() { return ui; }
        public void setUi(boolean ui) { this.ui = ui; }
        public boolean isGovbr() { return govbr; }
        public void setGovbr(boolean govbr) { this.govbr = govbr; }
        public boolean isIntegracoesExternas() { return integracoesExternas; }
        public void setIntegracoesExternas(boolean integracoesExternas) { this.integracoesExternas = integracoesExternas; }
        public boolean isDigitalizacao() { return digitalizacao; }
        public void setDigitalizacao(boolean digitalizacao) { this.digitalizacao = digitalizacao; }
        public boolean isDje() { return dje; }
        public void setDje(boolean dje) { this.dje = dje; }
    }

    public static class Integrations {

        private boolean govbr = true;
        private boolean mni = true;
        private boolean datajud = true;
        private boolean dje = true;
        private boolean digitalizacao = true;
        private boolean judicialFinanceiro = true;
        private boolean judicialSecurity = true;
        private boolean hsm = true;
        private boolean icp = true;

        public boolean isGovbr() { return govbr; }
        public void setGovbr(boolean govbr) { this.govbr = govbr; }
        public boolean isMni() { return mni; }
        public void setMni(boolean mni) { this.mni = mni; }
        public boolean isDatajud() { return datajud; }
        public void setDatajud(boolean datajud) { this.datajud = datajud; }
        public boolean isDje() { return dje; }
        public void setDje(boolean dje) { this.dje = dje; }
        public boolean isDigitalizacao() { return digitalizacao; }
        public void setDigitalizacao(boolean digitalizacao) { this.digitalizacao = digitalizacao; }
        public boolean isJudicialFinanceiro() { return judicialFinanceiro; }
        public void setJudicialFinanceiro(boolean judicialFinanceiro) { this.judicialFinanceiro = judicialFinanceiro; }
        public boolean isJudicialSecurity() { return judicialSecurity; }
        public void setJudicialSecurity(boolean judicialSecurity) { this.judicialSecurity = judicialSecurity; }
        public boolean isHsm() { return hsm; }
        public void setHsm(boolean hsm) { this.hsm = hsm; }
        public boolean isIcp() { return icp; }
        public void setIcp(boolean icp) { this.icp = icp; }
    }

    public static class Features {

        private boolean substituicaoNacional = true;

        public boolean isSubstituicaoNacional() { return substituicaoNacional; }
        public void setSubstituicaoNacional(boolean substituicaoNacional) { this.substituicaoNacional = substituicaoNacional; }
    }
}
