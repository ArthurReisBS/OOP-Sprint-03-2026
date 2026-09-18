package br.com.motiva.model;

/**
 * Faixa de urgencia de um trecho, derivada da altura estimada da vegetacao.
 */
public enum Prioridade {

    URGENTE("Passou da altura de corte recomendada: intervir imediatamente"),
    CRITICO("Corte necessário: agendar para esta semana"),
    ATENCAO("Dentro do limite, mas subindo: monitorar"),
    NORMAL ("Sem necessidade de corte");

    public static final double FAIXA_VERDE_MAX_CM = 15.0;
    public static final double FAIXA_AMARELA_MAX_CM = 25.0;
    public static final double ALTURA_CORTE_RECOMENDADO_CM = 30.0;

    private final String recomendacao;

    Prioridade(String recomendacao) {
        this.recomendacao = recomendacao;
    }

    public String getRecomendacao() {
        return recomendacao;
    }

    /** Altura a partir da qual esta faixa comeca. */
    public double getAlturaMinimaCm() {
        return switch (this) {
            case URGENTE -> ALTURA_CORTE_RECOMENDADO_CM;
            case CRITICO -> FAIXA_AMARELA_MAX_CM;
            case ATENCAO -> FAIXA_VERDE_MAX_CM;
            case NORMAL  -> 0.0;
        };
    }

    /** Cor equivalente no mapa operacional. */
    public String corNoMapa() {
        return switch (this) {
            case URGENTE, CRITICO -> "vermelho";
            case ATENCAO          -> "amarelo";
            case NORMAL           -> "verde";
        };
    }

    /** True se o trecho entra no cronograma da semana. */
    public boolean exigeIntervencao() {
        return this == URGENTE || this == CRITICO;
    }

    /**
     * Classifica uma altura em cm.
     *
     * Os limiares intermediarios sao exclusivos: 15,0 cm ainda e NORMAL e
     * 25,0 cm ainda e ATENCAO. So o de corte recomendado e inclusivo.
     */
    public static Prioridade paraAltura(double alturaCm) {
        if (alturaCm < 0) {
            throw new IllegalArgumentException("Altura negativa: " + alturaCm);
        }
        if (alturaCm >= ALTURA_CORTE_RECOMENDADO_CM) {
            return URGENTE;
        }
        if (alturaCm > FAIXA_AMARELA_MAX_CM) {
            return CRITICO;
        }
        if (alturaCm > FAIXA_VERDE_MAX_CM) {
            return ATENCAO;
        }
        return NORMAL;
    }
}
