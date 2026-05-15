package com.tcc.pjb.backend.service.inventario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InventarioPartilhaChecklistService {

    public enum ModalidadeInventario { JUDICIAL, EXTRAJUDICIAL, ARROLAMENTO_SUMARIO, ARROLAMENTO_COMUM }

    public record InventarioPartilhaInput(
            LocalDate dataObito,
            LocalDate dataAberturaInventario,
            int quantidadeHerdeiros,
            boolean possuiHerdeiroIncapaz,
            boolean possuiTestamento,
            boolean herdeirosConcordam,
            BigDecimal valorEstimadoEspolio,
            List<String> estadosComImovel
    ) {}

    public record RequisitoInventario(
            String descricao,
            String fundamentoLegal,
            String observacao
    ) {}

    public record InventarioPartilhaResult(
            boolean prazoAberturaExpirado,
            long diasDesdeObito,
            ModalidadeInventario modalidadeIndicada,
            List<RequisitoInventario> requisitosIndicados,
            List<String> pendenciasIdentificadas,
            List<String> requisitosVerificados,
            String sinalizacao
    ) {}

    private static final int DIAS_PRAZO_ABERTURA = 60;
    private static final BigDecimal LIMITE_ARROLAMENTO_SUMARIO = new BigDecimal("1000");

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação jurídica. Não substitui análise do advogado nem decisão judicial.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir com advogado especialista em direito das sucessões antes de qualquer ato cartorário ou judicial.";

    public InventarioPartilhaResult avaliar(InventarioPartilhaInput input) {
        List<RequisitoInventario> requisitos = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        List<String> verificados = new ArrayList<>();

        long diasDesdeObito = calcularDiasDesdeObito(input, pendencias, verificados);
        boolean prazoExpirado = avaliarPrazoAbertura(input, diasDesdeObito, pendencias, verificados);
        ModalidadeInventario modalidade = determinarModalidade(input, requisitos, pendencias, verificados);

        verificarITCMD(input, requisitos, verificados);
        verificarTestamento(input, requisitos, pendencias, verificados);
        verificarImoveis(input, requisitos, pendencias, verificados);
        adicionarDocumentacaoBase(requisitos, verificados);

        return new InventarioPartilhaResult(
                prazoExpirado,
                Math.max(0, diasDesdeObito),
                modalidade,
                List.copyOf(requisitos),
                List.copyOf(pendencias),
                List.copyOf(verificados),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }

    private long calcularDiasDesdeObito(InventarioPartilhaInput input,
            List<String> pendencias, List<String> verificados) {
        if (input.dataObito() == null) {
            pendencias.add("Pendência identificada: data do óbito não informada — necessária para apurar o prazo de abertura do inventário (CPC art. 611).");
            return 0;
        }
        long dias = ChronoUnit.DAYS.between(input.dataObito(), LocalDate.now());
        verificados.add(String.format("Data do óbito: %s (%d dias decorridos).", input.dataObito(), dias));
        return dias;
    }

    private boolean avaliarPrazoAbertura(InventarioPartilhaInput input, long diasDesdeObito,
            List<String> pendencias, List<String> verificados) {
        if (input.dataObito() == null) return false;

        if (input.dataAberturaInventario() != null) {
            long diasAbertura = ChronoUnit.DAYS.between(input.dataObito(), input.dataAberturaInventario());
            if (diasAbertura > DIAS_PRAZO_ABERTURA) {
                pendencias.add(String.format(
                        "Possível requisito a conferir: inventário aberto %d dias após o óbito — prazo legal é de 60 dias" +
                        " (CPC art. 611). Verificar incidência de multa sobre ITCMD no estado respectivo.",
                        diasAbertura));
            } else {
                verificados.add(String.format("Inventário aberto dentro do prazo de 60 dias (CPC art. 611): %d dias após o óbito.", diasAbertura));
            }
            return false;
        }

        if (diasDesdeObito > DIAS_PRAZO_ABERTURA) {
            pendencias.add(String.format(
                    "Pendência identificada: %d dias desde o óbito sem abertura de inventário — prazo de 60 dias" +
                    " expirado (CPC art. 611). Pode haver multa adicional sobre ITCMD. Verificar com urgência.",
                    diasDesdeObito));
            return true;
        }

        verificados.add(String.format("Prazo de abertura: %d de 60 dias — dentro do prazo (CPC art. 611).", diasDesdeObito));
        return false;
    }

    private ModalidadeInventario determinarModalidade(InventarioPartilhaInput input,
            List<RequisitoInventario> requisitos, List<String> pendencias, List<String> verificados) {
        if (input.possuiTestamento()) {
            verificados.add("Testamento informado — inventário judicial obrigatório para abertura e cumprimento (CPC art. 610 §1º).");
            return ModalidadeInventario.JUDICIAL;
        }

        if (input.possuiHerdeiroIncapaz()) {
            verificados.add("Herdeiro incapaz — inventário judicial obrigatório (CPC art. 610 §1º; CC art. 1.775).");
            return ModalidadeInventario.JUDICIAL;
        }

        if (!input.herdeirosConcordam()) {
            pendencias.add("Pendência identificada: herdeiros em desacordo — inventário judicial necessário para resolução do litígio (CPC art. 610 §1º).");
            return ModalidadeInventario.JUDICIAL;
        }

        if (input.valorEstimadoEspolio() != null
                && input.valorEstimadoEspolio().compareTo(LIMITE_ARROLAMENTO_SUMARIO) <= 0) {
            verificados.add(String.format(
                    "Espólio estimado em R$ %s — possível arrolamento sumário (CPC art. 659 §2º) se todos os herdeiros" +
                    " forem capazes e concordes. Sujeito à confirmação judicial.",
                    input.valorEstimadoEspolio().toPlainString()));
            return ModalidadeInventario.ARROLAMENTO_SUMARIO;
        }

        requisitos.add(new RequisitoInventario(
                "Inventário extrajudicial — todos os herdeiros capazes e concordes",
                "Lei 11.441/07; CPC art. 610 §1º",
                "Possível realizar por escritura pública em cartório de notas, sem necessidade de ação judicial" +
                " — desde que todos os herdeiros sejam maiores, capazes, concordes e estejam assistidos por advogado."));
        verificados.add("Condições para inventário extrajudicial presentes — sem testamento, sem incapaz, herdeiros concordes (Lei 11.441/07).");
        return ModalidadeInventario.EXTRAJUDICIAL;
    }

    private void verificarITCMD(InventarioPartilhaInput input,
            List<RequisitoInventario> requisitos, List<String> verificados) {
        requisitos.add(new RequisitoInventario(
                "ITCMD — Imposto sobre Transmissão Causa Mortis e Doação",
                "CF art. 155 I; legislação estadual aplicável",
                "Possível requisito a conferir: ITCMD é imposto estadual; alíquota e base de cálculo variam por estado." +
                " Verificar legislação do estado onde o de cujus era domiciliado e onde estão localizados os bens."));

        if (input.estadosComImovel() != null && input.estadosComImovel().size() > 1) {
            requisitos.add(new RequisitoInventario(
                    "ITCMD em múltiplos estados — possível pluralidade de obrigações fiscais",
                    "CF art. 155 §1º I",
                    String.format("Imóveis em %d estado(s) identificados — cada estado pode ter competência própria para o ITCMD" +
                    " sobre os bens imóveis nele situados. Verificar com contador tributarista.",
                    input.estadosComImovel().size())));
        }

        verificados.add("ITCMD: imposto estadual a ser apurado e recolhido antes da partilha — quitar antes do alvará de transmissão.");
    }

    private void verificarTestamento(InventarioPartilhaInput input,
            List<RequisitoInventario> requisitos, List<String> pendencias, List<String> verificados) {
        if (input.possuiTestamento()) {
            requisitos.add(new RequisitoInventario(
                    "Abertura e cumprimento do testamento",
                    "CPC art. 735; CC art. 1.857",
                    "Possível requisito a conferir: testamento deve ser aberto em juízo e seu conteúdo cumprido" +
                    " antes da partilha (CPC art. 735). Verificar validade formal e se não houve revogação."));
            pendencias.add("Pendência identificada: testamento presente — verificar se há herdeiros necessários com legítima violada" +
                    " (CC art. 1.846 — mínimo de 50% da herança aos herdeiros necessários).");
        }
    }

    private void verificarImoveis(InventarioPartilhaInput input,
            List<RequisitoInventario> requisitos, List<String> pendencias, List<String> verificados) {
        if (input.estadosComImovel() == null || input.estadosComImovel().isEmpty()) {
            verificados.add("Nenhum imóvel informado — verificar se há bens a registrar em cartório de registro de imóveis.");
            return;
        }

        requisitos.add(new RequisitoInventario(
                "Registro da partilha no cartório de imóveis",
                "Lei 6.015/73 art. 167 I 26; CC art. 1.784",
                "Possível requisito a conferir: a transmissão de imóveis por herança só produz efeitos erga omnes" +
                " após registro no Cartório de Registro de Imóveis competente (Lei 6.015/73 art. 167 I 26)."));

        pendencias.add(String.format(
                "Possível requisito a conferir: imóvel(is) localizado(s) em %s — verificar matrícula, ônus reais e" +
                " eventual dívida de IPTU antes da partilha.",
                String.join(", ", input.estadosComImovel())));
    }

    private void adicionarDocumentacaoBase(List<RequisitoInventario> requisitos, List<String> verificados) {
        requisitos.add(new RequisitoInventario(
                "Documentação básica do espólio",
                "CPC art. 615",
                "Possível requisito a conferir: certidão de óbito, documentos do de cujus (CPF, RG, certidão de casamento/nascimento)," +
                " documentos de todos os herdeiros, comprovante de endereço, documentos dos bens (escrituras, CRLV, extratos bancários)."));
        verificados.add("Checklist de documentação básica incluído — conferir lista completa com advogado responsável pelo inventário.");
    }
}
