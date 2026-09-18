package br.com.motiva.dao;

import br.com.motiva.db.ConexaoBD;
import br.com.motiva.model.IntervencaoOperacional;
import br.com.motiva.model.Pulverizacao;
import br.com.motiva.model.RocadaMecanizada;
import br.com.motiva.model.TipoIntervencao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object de INTERVENCAO_OPERACIONAL.
 *
 * A tabela guarda as duas subclasses na mesma linha, separadas pela coluna
 * TIPO. Na escrita, um switch sobre o objeto decide quais colunas preencher;
 * na leitura, o valor de TIPO decide qual record construir.
 */
public class IntervencaoOperacionalDAO {

    private static final String COLUNAS = """
            id, trecho_id, equipe_id, tipo, data_execucao, custo,
            largura_faixa_m, altura_corte_cm""";

    private static final String SQL_INSERIR = """
            INSERT INTO intervencao_operacional
                   (trecho_id, equipe_id, tipo, data_execucao, custo,
                    largura_faixa_m, altura_corte_cm)
            VALUES (?, ?, ?, ?, ?, ?, ?)""";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLUNAS + " FROM intervencao_operacional WHERE id = ?";

    private static final String SQL_LISTAR_TODAS =
            "SELECT " + COLUNAS + " FROM intervencao_operacional ORDER BY data_execucao DESC";

    private static final String SQL_LISTAR_POR_TRECHO = "SELECT " + COLUNAS + """
             FROM intervencao_operacional
            WHERE trecho_id = ?
            ORDER BY data_execucao DESC""";

    private static final String SQL_ATUALIZAR = """
            UPDATE intervencao_operacional
               SET trecho_id = ?, equipe_id = ?, tipo = ?, data_execucao = ?, custo = ?,
                   largura_faixa_m = ?, altura_corte_cm = ?
             WHERE id = ?""";

    private static final String SQL_DELETAR =
            "DELETE FROM intervencao_operacional WHERE id = ?";

    /** Recorrencia: quantas vezes cada trecho ja foi atendido e a que custo. */
    private static final String SQL_CONTAR_POR_TRECHO = """
            SELECT trecho_id, COUNT(*) AS total, SUM(custo) AS custo_total
              FROM intervencao_operacional
             GROUP BY trecho_id
             ORDER BY total DESC""";

    /** ORA-02291: integrity constraint violated - parent key not found. */
    private static final int ORA_FK_PAI_INEXISTENTE = 2291;

    /** ORA-02290: check constraint violated. */
    private static final int ORA_CHECK_VIOLADO = 2290;

    public IntervencaoOperacionalDAO() {
    }

    public IntervencaoOperacional inserir(IntervencaoOperacional intervencao) {
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERIR, new String[]{"ID"})) {

            preencherParametros(pstmt, intervencao);

            if (pstmt.executeUpdate() == 0) {
                throw new RuntimeException("INSERT nao afetou nenhuma linha.");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                Long idGerado = rs.next() ? rs.getLong(1) : null;
                return comId(intervencao, idGerado);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir intervenção: " + traduzir(e), e);
        }
    }

    /** A intervencao com esse ID, ou null se nao existe. */
    public IntervencaoOperacional buscarPorId(Long id) {
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? extrair(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar intervenção " + id
                    + ": " + e.getMessage(), e);
        }
    }

    public List<IntervencaoOperacional> listarTodas() {
        Connection conn = ConexaoBD.getInstancia().getConexao();
        List<IntervencaoOperacional> intervencoes = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_LISTAR_TODAS);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                intervencoes.add(extrair(rs));
            }
            return intervencoes;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar intervenções: " + e.getMessage(), e);
        }
    }

    /** Historico de um trecho, do mais recente para o mais antigo. */
    public List<IntervencaoOperacional> listarPorTrecho(Long trechoId) {
        Connection conn = ConexaoBD.getInstancia().getConexao();
        List<IntervencaoOperacional> intervencoes = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_LISTAR_POR_TRECHO)) {

            pstmt.setLong(1, trechoId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    intervencoes.add(extrair(rs));
                }
                return intervencoes;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar intervenções do trecho " + trechoId
                    + ": " + e.getMessage(), e);
        }
    }

    /** Agregacao feita no banco, e nao em memoria, para escalar com o historico. */
    public List<ResumoPorTrecho> contarPorTrecho() {
        Connection conn = ConexaoBD.getInstancia().getConexao();
        List<ResumoPorTrecho> resumos = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_CONTAR_POR_TRECHO);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                resumos.add(new ResumoPorTrecho(
                        rs.getLong("trecho_id"),
                        rs.getInt("total"),
                        rs.getDouble("custo_total")));
            }
            return resumos;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar intervenções por trecho: "
                    + e.getMessage(), e);
        }
    }

    public boolean atualizar(IntervencaoOperacional intervencao) {
        if (intervencao.id() == null) {
            throw new IllegalArgumentException("Intervencao sem ID nao pode ser atualizada.");
        }
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_ATUALIZAR)) {

            preencherParametros(pstmt, intervencao);
            pstmt.setLong(8, intervencao.id());      // o 8o e o WHERE, so existe no UPDATE

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar intervenção " + intervencao.id()
                    + ": " + traduzir(e), e);
        }
    }

    public boolean deletar(Long id) {
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_DELETAR)) {

            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar intervenção " + id
                    + ": " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------------
    // Objeto <-> linha
    // ------------------------------------------------------------------------

    /**
     * Preenche os 7 parametros comuns ao INSERT e ao UPDATE.
     *
     * O switch dispensa default porque a interface e sealed: um terceiro tipo
     * passaria a quebrar a compilacao aqui.
     */
    private void preencherParametros(PreparedStatement pstmt, IntervencaoOperacional intervencao)
            throws SQLException {

        pstmt.setLong(1, intervencao.trechoId());
        pstmt.setLong(2, intervencao.equipeId());
        pstmt.setString(3, intervencao.tipo().name());
        pstmt.setDate(4, Date.valueOf(intervencao.dataExecucao()));
        pstmt.setDouble(5, intervencao.custo());

        switch (intervencao) {
            case RocadaMecanizada rocada -> {
                pstmt.setDouble(6, rocada.larguraFaixaM());
                pstmt.setDouble(7, rocada.alturaCorteCm());
            }
            case Pulverizacao ignorada -> {
                pstmt.setNull(6, Types.NUMERIC);     // largura_faixa_m
                pstmt.setNull(7, Types.NUMERIC);     // altura_corte_cm
            }
        }
    }

    /**
     * ResultSet -> record, escolhendo a subclasse pela coluna TIPO.
     *
     * Aqui o switch e sobre um valor vindo do banco, nao sobre o tipo sealed:
     * um TIPO desconhecido precisa falhar explicitamente.
     */
    private IntervencaoOperacional extrair(ResultSet rs) throws SQLException {
        Long id        = rs.getLong("id");
        Long trechoId  = rs.getLong("trecho_id");
        Long equipeId  = rs.getLong("equipe_id");
        var dataExec   = rs.getDate("data_execucao").toLocalDate();
        double custo   = rs.getDouble("custo");

        TipoIntervencao tipo = TipoIntervencao.valueOf(rs.getString("tipo"));

        return switch (tipo) {
            case ROCADA -> new RocadaMecanizada(id, trechoId, equipeId, dataExec, custo,
                    rs.getDouble("largura_faixa_m"),
                    rs.getDouble("altura_corte_cm"));

            case PULVERIZACAO -> new Pulverizacao(id, trechoId, equipeId, dataExec, custo);
        };
    }

    /** Recria o record com o ID gerado; records sao imutaveis. */
    private IntervencaoOperacional comId(IntervencaoOperacional intervencao, Long id) {
        return switch (intervencao) {
            case RocadaMecanizada r -> new RocadaMecanizada(id, r.trechoId(), r.equipeId(),
                    r.dataExecucao(), r.custo(), r.larguraFaixaM(), r.alturaCorteCm());

            case Pulverizacao p -> new Pulverizacao(id, p.trechoId(), p.equipeId(),
                    p.dataExecucao(), p.custo());
        };
    }

    /** Traduz os erros de integridade mais comuns desta tabela. */
    private String traduzir(SQLException e) {
        return switch (e.getErrorCode()) {
            case ORA_FK_PAI_INEXISTENTE ->
                    "o trecho ou a equipe informados não existem. Cadastre-os antes.";
            case ORA_CHECK_VIOLADO ->
                    "combinação de colunas inválida para o tipo. Uma roçada precisa de largura "
                    + "e altura de corte; uma pulverização deve deixar as duas nulas.";
            default -> e.getMessage();
        };
    }

    /** Linha do agregado de contarPorTrecho(); nao e entidade, nao tem tabela. */
    public record ResumoPorTrecho(Long trechoId, int totalIntervencoes, double custoTotal) {

        public double custoMedio() {
            return totalIntervencoes == 0 ? 0.0 : custoTotal / totalIntervencoes;
        }
    }
}
