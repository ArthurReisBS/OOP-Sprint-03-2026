package br.com.motiva.model;

import java.time.LocalDate;

/**
 * Intervencao por pulverizacao.
 *
 * Nao tem atributos proprios: a linha se distingue apenas por TIPO =
 * 'PULVERIZACAO', e o CHECK ck_interv_subtipo exige as colunas da rocada nulas.
 */
public record Pulverizacao(
        Long id,
        Long trechoId,
        Long equipeId,
        LocalDate dataExecucao,
        double custo
) implements IntervencaoOperacional {

    public Pulverizacao {
        if (trechoId == null || equipeId == null) {
            throw new IllegalArgumentException("Trecho e equipe sao obrigatorios.");
        }
        if (dataExecucao == null) {
            throw new IllegalArgumentException("Data de execucao e obrigatoria.");
        }
        if (custo < 0) {
            throw new IllegalArgumentException("Custo negativo: " + custo);
        }
    }

    public static Pulverizacao nova(Long trechoId, Long equipeId,
                                    LocalDate dataExecucao, double custo) {
        return new Pulverizacao(null, trechoId, equipeId, dataExecucao, custo);
    }

    @Override
    public TipoIntervencao tipo() {
        return TipoIntervencao.PULVERIZACAO;
    }

    @Override
    public String descricaoTecnica() {
        return "Pulverização";
    }

    @Override
    public String toString() {
        return String.format("[%s] %s em %s -- R$ %.2f",
                tipo(), descricaoTecnica(), dataExecucao, custo);
    }
}
