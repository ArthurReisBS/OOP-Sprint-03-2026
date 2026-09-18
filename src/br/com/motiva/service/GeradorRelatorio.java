package br.com.motiva.service;

import br.com.motiva.dao.RelatorioPrioridadeDAO;
import br.com.motiva.model.Prioridade;
import br.com.motiva.model.RelatorioPrioridade;
import br.com.motiva.model.TrechoRodovia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Classifica os trechos por urgencia, imprime o relatorio e o persiste.
 *
 * O DAO e injetado pelo construtor, o que permite testar classificar() sem
 * banco.
 */
public class GeradorRelatorio {

    private static final String LINHA = "=".repeat(78);
    private static final String SUBLINHA = "-".repeat(78);

    private final RelatorioPrioridadeDAO relatorioDAO;

    public GeradorRelatorio() {
        this(new RelatorioPrioridadeDAO());
    }

    /** Construtor com DAO injetado, para teste. */
    public GeradorRelatorio(RelatorioPrioridadeDAO relatorioDAO) {
        this.relatorioDAO = relatorioDAO;
    }

    /**
     * Imprime o relatorio e o persiste.
     *
     * @return o relatorio salvo, com o ID gerado pelo banco
     */
    public RelatorioPrioridade gerarRelatorio(List<TrechoRodovia> trechos) {
        Map<Prioridade, List<TrechoRodovia>> porPrioridade = classificar(trechos);

        imprimirCabecalho(trechos.size());
        imprimirPorPrioridade(porPrioridade);
        imprimirPlanoDeAcao(porPrioridade);

        int qtUrgente = porPrioridade.get(Prioridade.URGENTE).size();
        int qtCritico = porPrioridade.get(Prioridade.CRITICO).size();
        int qtAtencao = porPrioridade.get(Prioridade.ATENCAO).size();
        int qtNormal  = porPrioridade.get(Prioridade.NORMAL).size();

        String resumo = montarResumo(porPrioridade);

        RelatorioPrioridade salvo =
                relatorioDAO.salvarRelatorio(qtUrgente, qtCritico, qtAtencao, qtNormal, resumo);

        System.out.println(SUBLINHA);
        System.out.printf("Relatório #%d gravado em RELATORIO_PRIORIDADE.%n", salvo.id());
        System.out.println(LINHA);

        return salvo;
    }

    /** Sobrecarga que aceita array; a versao com List e a usada pelo DAO. */
    public RelatorioPrioridade gerarRelatorio(TrechoRodovia[] trechos) {
        return gerarRelatorio(Arrays.asList(trechos));
    }

    /** Apenas a classificacao, sem imprimir nem gravar. */
    public Map<Prioridade, List<TrechoRodovia>> classificar(List<TrechoRodovia> trechos) {
        // EnumMap mantem a ordem de declaracao do enum, que e a do relatorio
        Map<Prioridade, List<TrechoRodovia>> mapa = new EnumMap<>(Prioridade.class);

        // todas as faixas entram, mesmo vazias, para o relatorio nao omiti-las
        for (Prioridade p : Prioridade.values()) {
            mapa.put(p, new ArrayList<>());
        }

        for (TrechoRodovia trecho : trechos) {
            mapa.get(trecho.prioridade()).add(trecho);
        }

        // dentro de cada faixa, o mais alto primeiro: ordem de atendimento
        for (List<TrechoRodovia> lista : mapa.values()) {
            lista.sort(Comparator.comparingDouble(TrechoRodovia::alturaEstimadaCm).reversed());
        }

        return mapa;
    }

    // ------------------------------------------------------------------------
    // Saida no console
    // ------------------------------------------------------------------------

    private void imprimirCabecalho(int totalTrechos) {
        System.out.println();
        System.out.println(LINHA);
        System.out.println("  MOTIVA -- RELATÓRIO DE PRIORIDADE DE INTERVENÇÃO");
        System.out.printf ("  %d trechos monitorados%n", totalTrechos);
        System.out.println(LINHA);
        System.out.println("  Altura estimada = medida + taxa da espécie x dias,"
                + " limitada ao porte");
        System.out.println("  máximo da espécie. É o pior caso, não uma previsão.");
        System.out.printf ("  Faixas: verde ≤ %.0f · amarelo ≤ %.0f · "
                        + "vermelho > %.0f · corte ≥ %.0f cm%n",
                Prioridade.FAIXA_VERDE_MAX_CM, Prioridade.FAIXA_AMARELA_MAX_CM,
                Prioridade.FAIXA_AMARELA_MAX_CM, Prioridade.ALTURA_CORTE_RECOMENDADO_CM);
        System.out.println(SUBLINHA);
    }

    private void imprimirPorPrioridade(Map<Prioridade, List<TrechoRodovia>> porPrioridade) {
        porPrioridade.forEach((prioridade, trechos) -> {
            System.out.printf("%n  %-8s (%d) [%s] -- %s%n",
                    prioridade, trechos.size(), prioridade.corNoMapa(),
                    prioridade.getRecomendacao());

            if (trechos.isEmpty()) {
                System.out.println("    nenhum trecho nesta faixa.");
                return;
            }

            for (TrechoRodovia trecho : trechos) {
                System.out.printf("    %-22s  %-29s  %5.1f cm  (medido %.1f há %d d)%n",
                        trecho.identificacao(),
                        trecho.especiePredominante().getNomeCompleto(),
                        trecho.alturaEstimadaCm(),
                        trecho.alturaVegetacaoCm(), trecho.diasDesdeMedicao());
            }
        });
    }

    /** Lista ordenada do que cortar, com a urgencia de cada trecho. */
    private void imprimirPlanoDeAcao(Map<Prioridade, List<TrechoRodovia>> porPrioridade) {
        List<TrechoRodovia> urgentes = porPrioridade.get(Prioridade.URGENTE);
        List<TrechoRodovia> criticos = porPrioridade.get(Prioridade.CRITICO);

        System.out.println();
        System.out.println(SUBLINHA);
        System.out.println("  PLANO DE AÇÃO");

        if (urgentes.isEmpty() && criticos.isEmpty()) {
            System.out.println("    Nenhuma intervenção necessária nesta semana.");
            return;
        }

        // o rotulo entra como parametro de largura fixa para manter o
        // alinhamento apesar de os rotulos terem tamanhos diferentes
        int ordem = 1;
        for (TrechoRodovia trecho : urgentes) {
            imprimirLinhaDeAcao(ordem++, "IMEDIATO", trecho);
        }
        for (TrechoRodovia trecho : criticos) {
            imprimirLinhaDeAcao(ordem++, "esta semana", trecho);
        }
    }

    private void imprimirLinhaDeAcao(int ordem, String quando, TrechoRodovia trecho) {
        System.out.printf("    %d. %-12s %-22s  %5.1f cm%n",
                ordem, quando, trecho.identificacao(), trecho.alturaEstimadaCm());
    }

    /**
     * Texto da coluna RESUMO.
     *
     * Nomeia os trechos urgentes em vez de so conta-los, para o resumo ser
     * legivel sozinho no historico.
     */
    private String montarResumo(Map<Prioridade, List<TrechoRodovia>> porPrioridade) {
        List<TrechoRodovia> urgentes = porPrioridade.get(Prioridade.URGENTE);
        int total = porPrioridade.values().stream().mapToInt(List::size).sum();

        if (urgentes.isEmpty()) {
            int criticos = porPrioridade.get(Prioridade.CRITICO).size();
            return criticos == 0
                    ? "Malha sob controle: nenhum trecho urgente ou crítico entre os "
                      + total + " monitorados."
                    : "Sem trechos urgentes. " + criticos + " em estado crítico, "
                      + "agendar para esta semana.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(urgentes.size()).append(" de ").append(total)
          .append(" trechos em estado urgente: ");

        for (int i = 0; i < urgentes.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            TrechoRodovia t = urgentes.get(i);
            sb.append(String.format("%s (%.0f cm)", t.identificacao(), t.alturaEstimadaCm()));
        }
        sb.append(". Intervenção imediata recomendada.");

        return sb.toString();
    }
}
