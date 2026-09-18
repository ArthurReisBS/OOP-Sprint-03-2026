package br.com.motiva.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Uma execucao do GeradorRelatorio, com os contadores por faixa.
 *
 * Guarda os totais, e nao a lista de trechos: o detalhe de cada intervencao
 * fica em INTERVENCAO_OPERACIONAL.
 */
public record RelatorioPrioridade(
        Long id,
        LocalDateTime dataGeracao,
        int qtUrgente,
        int qtCritico,
        int qtAtencao,
        int qtNormal,
        String resumo
) {

    /** Limite da coluna RESUMO; o texto e cortado antes do insert. */
    public static final int TAMANHO_MAXIMO_RESUMO = 500;

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public RelatorioPrioridade {
        if (qtUrgente < 0 || qtCritico < 0 || qtAtencao < 0 || qtNormal < 0) {
            throw new IllegalArgumentException("Contadores nao podem ser negativos.");
        }
        if (resumo != null && resumo.length() > TAMANHO_MAXIMO_RESUMO) {
            resumo = resumo.substring(0, TAMANHO_MAXIMO_RESUMO);
        }
    }

    public static RelatorioPrioridade novo(int qtUrgente, int qtCritico,
                                           int qtAtencao, int qtNormal, String resumo) {
        return new RelatorioPrioridade(null, LocalDateTime.now(),
                qtUrgente, qtCritico, qtAtencao, qtNormal, resumo);
    }

    public int totalTrechos() {
        return qtUrgente + qtCritico + qtAtencao + qtNormal;
    }

    /** Trechos que exigem acao: urgentes mais criticos. */
    public int totalAcionaveis() {
        return qtUrgente + qtCritico;
    }

    public double percentualAcionavel() {
        return totalTrechos() == 0 ? 0.0 : (100.0 * totalAcionaveis()) / totalTrechos();
    }

    public int quantidadePara(Prioridade prioridade) {
        return switch (prioridade) {
            case URGENTE -> qtUrgente;
            case CRITICO -> qtCritico;
            case ATENCAO -> qtAtencao;
            case NORMAL  -> qtNormal;
        };
    }

    @Override
    public String toString() {
        return String.format("#%-4s %s | urgente %2d  crítico %2d  atenção %2d  normal %2d"
                        + " | %4.1f%% acionável",
                id == null ? "-" : id,
                dataGeracao == null ? "-" : dataGeracao.format(FORMATO),
                qtUrgente, qtCritico, qtAtencao, qtNormal, percentualAcionavel());
    }
}
