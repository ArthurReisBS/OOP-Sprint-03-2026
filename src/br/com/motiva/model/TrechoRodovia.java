package br.com.motiva.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.OptionalLong;

/**
 * Trecho de rodovia monitorado.
 *
 * O ID e null enquanto o trecho nao foi inserido: quem gera e o banco.
 */
public record TrechoRodovia(
        Long id,
        String rodovia,
        double kmInicial,
        double kmFinal,
        EspecieGrama especiePredominante,
        double alturaVegetacaoCm,
        LocalDate dataUltimaMedicao,
        LocalDate dataUltimaIntervencao   // null = nunca intervindo
) {

    /** Altura que sobra depois de uma roçada. */
    public static final double ALTURA_APOS_CORTE_CM = 2.0;

    public TrechoRodovia {
        if (rodovia == null || rodovia.isBlank()) {
            throw new IllegalArgumentException("Rodovia e obrigatoria.");
        }
        if (especiePredominante == null) {
            throw new IllegalArgumentException("Especie predominante e obrigatoria.");
        }
        if (kmFinal <= kmInicial) {
            throw new IllegalArgumentException(
                    "km final (" + kmFinal + ") deve ser maior que o inicial (" + kmInicial + ").");
        }
        if (alturaVegetacaoCm < 0) {
            throw new IllegalArgumentException("Altura negativa: " + alturaVegetacaoCm);
        }
        if (dataUltimaMedicao == null) {
            throw new IllegalArgumentException("Data da ultima medicao e obrigatoria.");
        }
    }

    /** Trecho ainda nao persistido, sem ID. */
    public static TrechoRodovia novo(String rodovia, double kmInicial, double kmFinal,
                                     EspecieGrama especiePredominante, double alturaVegetacaoCm,
                                     LocalDate dataUltimaMedicao,
                                     LocalDate dataUltimaIntervencao) {
        return new TrechoRodovia(null, rodovia, kmInicial, kmFinal, especiePredominante,
                alturaVegetacaoCm, dataUltimaMedicao, dataUltimaIntervencao);
    }

    public double extensaoKm() {
        return kmFinal - kmInicial;
    }

    public long diasDesdeMedicao() {
        return ChronoUnit.DAYS.between(dataUltimaMedicao, LocalDate.now());
    }

    /** Vazio se o trecho nunca recebeu intervencao. */
    public OptionalLong diasDesdeIntervencao() {
        return dataUltimaIntervencao == null
                ? OptionalLong.empty()
                : OptionalLong.of(ChronoUnit.DAYS.between(dataUltimaIntervencao, LocalDate.now()));
    }

    /**
     * Altura provavel hoje, no pior caso: medida + taxa da especie x dias,
     * limitada ao porte maximo da especie.
     *
     * O teto evita a extrapolacao linear absurda em trechos sem medicao
     * recente.
     */
    public double alturaEstimadaCm() {
        double crescimento = especiePredominante.getTaxaBaseCmDia() * diasDesdeMedicao();
        return Math.min(alturaVegetacaoCm + crescimento,
                especiePredominante.getAlturaMaximaCm());
    }

    public Prioridade prioridade() {
        return Prioridade.paraAltura(alturaEstimadaCm());
    }

    public String corNoMapa() {
        return prioridade().corNoMapa();
    }

    /** Dias restantes ate a altura de corte recomendada. Zero se ja passou. */
    public long diasAteCorte() {
        return especiePredominante.diasAteCorte(alturaEstimadaCm());
    }

    /** Novo estado apos leitura do sensor; o original fica intacto. */
    public TrechoRodovia comMedicao(double novaAlturaCm, LocalDate quando) {
        return new TrechoRodovia(id, rodovia, kmInicial, kmFinal, especiePredominante,
                novaAlturaCm, quando, dataUltimaIntervencao);
    }

    /** Novo estado apos um corte: a altura cai para ALTURA_APOS_CORTE_CM. */
    public TrechoRodovia comIntervencao(LocalDate quando) {
        return new TrechoRodovia(id, rodovia, kmInicial, kmFinal, especiePredominante,
                ALTURA_APOS_CORTE_CM, quando, quando);
    }

    public String identificacao() {
        return String.format("%s km %.1f-%.1f", rodovia, kmInicial, kmFinal);
    }

    @Override
    public String toString() {
        return String.format("%-22s  %-29s  medido %5.1f cm há %2d dias  ->  %5.1f cm  [%s]",
                identificacao(), especiePredominante.getNomeCompleto(),
                alturaVegetacaoCm, diasDesdeMedicao(), alturaEstimadaCm(), prioridade());
    }
}
