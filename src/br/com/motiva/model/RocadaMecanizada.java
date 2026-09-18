package br.com.motiva.model;

import java.time.LocalDate;

/**
 * Corte mecanico da vegetacao.
 *
 * Grava LARGURA_FAIXA_M e ALTURA_CORTE_CM; o CHECK ck_interv_subtipo exige as
 * duas preenchidas quando TIPO = 'ROCADA'.
 */
public record RocadaMecanizada(
        Long id,
        Long trechoId,
        Long equipeId,
        LocalDate dataExecucao,
        double custo,
        double larguraFaixaM,
        double alturaCorteCm
) implements IntervencaoOperacional {

    public RocadaMecanizada {
        if (trechoId == null || equipeId == null) {
            throw new IllegalArgumentException("Trecho e equipe sao obrigatorios.");
        }
        if (dataExecucao == null) {
            throw new IllegalArgumentException("Data de execucao e obrigatoria.");
        }
        if (custo < 0) {
            throw new IllegalArgumentException("Custo negativo: " + custo);
        }
        if (larguraFaixaM <= 0 || alturaCorteCm <= 0) {
            throw new IllegalArgumentException(
                    "Largura e altura de corte devem ser positivas.");
        }
    }

    public static RocadaMecanizada nova(Long trechoId, Long equipeId, LocalDate dataExecucao,
                                        double custo, double larguraFaixaM, double alturaCorteCm) {
        return new RocadaMecanizada(null, trechoId, equipeId, dataExecucao,
                custo, larguraFaixaM, alturaCorteCm);
    }

    @Override
    public TipoIntervencao tipo() {
        return TipoIntervencao.ROCADA;
    }

    @Override
    public String descricaoTecnica() {
        return String.format("Roçada de %.1f m de faixa, corte a %.0f cm",
                larguraFaixaM, alturaCorteCm);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s em %s -- R$ %.2f",
                tipo(), descricaoTecnica(), dataExecucao, custo);
    }
}
