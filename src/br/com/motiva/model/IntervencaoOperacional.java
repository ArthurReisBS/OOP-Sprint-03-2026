package br.com.motiva.model;

import java.time.LocalDate;

/**
 * Intervencao executada em um trecho.
 *
 * Interface sealed em vez de classe abstrata porque as implementacoes sao
 * records, que nao estendem classe. Como o compilador conhece as duas unicas
 * implementacoes, um switch sobre elas dispensa default e passa a falhar na
 * compilacao se um terceiro tipo for acrescentado.
 */
public sealed interface IntervencaoOperacional
        permits RocadaMecanizada, Pulverizacao {

    Long id();

    Long trechoId();

    Long equipeId();

    LocalDate dataExecucao();

    double custo();

    /** Valor gravado na coluna TIPO. */
    TipoIntervencao tipo();

    /** Linha unica descrevendo o servico, para console e relatorio. */
    String descricaoTecnica();

    default double custoPorKm(double extensaoKm) {
        if (extensaoKm <= 0) {
            throw new IllegalArgumentException("Extensao deve ser positiva: " + extensaoKm);
        }
        return custo() / extensaoKm;
    }
}
