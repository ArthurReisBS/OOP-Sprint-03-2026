package br.com.motiva.model;

/**
 * Discriminador da tabela INTERVENCAO_OPERACIONAL: define qual subclasse o DAO
 * constroi ao ler a coluna TIPO.
 */
public enum TipoIntervencao {

    ROCADA("Roçada mecanizada"),
    PULVERIZACAO("Pulverização");

    private final String descricao;

    TipoIntervencao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
