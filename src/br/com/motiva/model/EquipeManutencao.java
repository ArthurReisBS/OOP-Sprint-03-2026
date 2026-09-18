package br.com.motiva.model;

/**
 * Equipe de campo que executa as intervencoes.
 */
public record EquipeManutencao(
        Long id,
        String nome,
        TipoEquipe tipo,
        int quantidadeIntegrantes,
        boolean disponivel
) {

    public EquipeManutencao {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da equipe e obrigatorio.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo da equipe e obrigatorio.");
        }
        if (quantidadeIntegrantes <= 0) {
            throw new IllegalArgumentException(
                    "Equipe precisa de ao menos 1 integrante, veio " + quantidadeIntegrantes + ".");
        }
    }

    /** Equipe ainda nao persistida, sem ID. */
    public static EquipeManutencao nova(String nome, TipoEquipe tipo,
                                        int quantidadeIntegrantes, boolean disponivel) {
        return new EquipeManutencao(null, nome, tipo, quantidadeIntegrantes, disponivel);
    }

    /** True se a equipe esta livre e sabe executar o servico. */
    public boolean podeAtender(TipoIntervencao tipoIntervencao) {
        return disponivel && tipo.podeExecutar(tipoIntervencao);
    }

    public EquipeManutencao comDisponibilidade(boolean novaDisponibilidade) {
        return new EquipeManutencao(id, nome, tipo, quantidadeIntegrantes, novaDisponibilidade);
    }

    @Override
    public String toString() {
        return String.format("%-30s  %-12s  %d integrantes  %s",
                nome, tipo, quantidadeIntegrantes, disponivel ? "disponível" : "indisponível");
    }
}
