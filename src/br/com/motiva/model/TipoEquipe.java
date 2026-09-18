package br.com.motiva.model;

/**
 * Servico que a equipe executa. Espelha o CHECK ck_equipe_tipo; os nomes das
 * constantes sao gravados no banco, entao renomear uma quebra os dados salvos.
 */
public enum TipoEquipe {

    ROCADA("Roçada mecanizada"),
    PULVERIZACAO("Pulverização"),
    MISTA("Roçada e pulverização");

    private final String descricao;

    TipoEquipe(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean podeExecutar(TipoIntervencao tipo) {
        return this == MISTA || this.name().equals(tipo.name());
    }
}
