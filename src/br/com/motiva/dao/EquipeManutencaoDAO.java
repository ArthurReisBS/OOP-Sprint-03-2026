package br.com.motiva.dao;

import br.com.motiva.db.ConexaoBD;
import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.TipoEquipe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object de EQUIPE_MANUTENCAO.
 *
 * Regras que valem nos quatro DAOs:
 *  1. PreparedStatement sempre, nunca concatenacao de String (SQL Injection).
 *  2. A Connection vem do singleton e nao entra no try-with-resources: e
 *     compartilhada. Fecham-se apenas PreparedStatement e ResultSet.
 */
public class EquipeManutencaoDAO {

    /** Colunas nomeadas em vez de SELECT *. */
    private static final String COLUNAS =
            "id, nome, tipo, qt_integrantes, disponivel";

    private static final String SQL_INSERIR = """
            INSERT INTO equipe_manutencao (nome, tipo, qt_integrantes, disponivel)
            VALUES (?, ?, ?, ?)""";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLUNAS + " FROM equipe_manutencao WHERE id = ?";

    private static final String SQL_LISTAR_TODAS =
            "SELECT " + COLUNAS + " FROM equipe_manutencao ORDER BY id";

    private static final String SQL_LISTAR_DISPONIVEIS =
            "SELECT " + COLUNAS + " FROM equipe_manutencao WHERE disponivel = 1 ORDER BY nome";

    private static final String SQL_ATUALIZAR = """
            UPDATE equipe_manutencao
               SET nome = ?, tipo = ?, qt_integrantes = ?, disponivel = ?
             WHERE id = ?""";

    private static final String SQL_DELETAR =
            "DELETE FROM equipe_manutencao WHERE id = ?";

    /** ORA-02292: integrity constraint violated - child record found. */
    private static final int ORA_FK_COM_DEPENDENTES = 2292;

    public EquipeManutencaoDAO() {
    }

    /** Insere e devolve a equipe com o ID gerado pelo banco. */
    public EquipeManutencao inserir(EquipeManutencao equipe) {
        Connection conn = ConexaoBD.getInstancia().getConexao();

        // no Oracle, getGeneratedKeys() so devolve a chave se a coluna for
        // nomeada em MAIUSCULAS; caso contrario volta o ROWID
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERIR, new String[]{"ID"})) {

            pstmt.setString(1, equipe.nome());
            pstmt.setString(2, equipe.tipo().name());
            pstmt.setInt(3, equipe.quantidadeIntegrantes());
            pstmt.setInt(4, equipe.disponivel() ? 1 : 0);   // Oracle < 23ai nao tem BOOLEAN

            if (pstmt.executeUpdate() == 0) {
                throw new RuntimeException("INSERT nao afetou nenhuma linha.");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                Long idGerado = rs.next() ? rs.getLong(1) : null;
                return new EquipeManutencao(idGerado, equipe.nome(), equipe.tipo(),
                        equipe.quantidadeIntegrantes(), equipe.disponivel());
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao inserir equipe '" + equipe.nome() + "': " + e.getMessage(), e);
        }
    }

    /** A equipe com esse ID, ou null se nao existe. */
    public EquipeManutencao buscarPorId(Long id) {
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? extrair(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar equipe " + id + ": " + e.getMessage(), e);
        }
    }

    public List<EquipeManutencao> listarTodas() {
        return listar(SQL_LISTAR_TODAS, "listar equipes");
    }

    /** Apenas as equipes disponiveis. */
    public List<EquipeManutencao> listarDisponiveis() {
        return listar(SQL_LISTAR_DISPONIVEIS, "listar equipes disponiveis");
    }

    /** True se alguma linha foi atualizada; false se o ID nao existe. */
    public boolean atualizar(EquipeManutencao equipe) {
        if (equipe.id() == null) {
            throw new IllegalArgumentException("Equipe sem ID nao pode ser atualizada.");
        }
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_ATUALIZAR)) {

            pstmt.setString(1, equipe.nome());
            pstmt.setString(2, equipe.tipo().name());
            pstmt.setInt(3, equipe.quantidadeIntegrantes());
            pstmt.setInt(4, equipe.disponivel() ? 1 : 0);
            pstmt.setLong(5, equipe.id());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao atualizar equipe " + equipe.id() + ": " + e.getMessage(), e);
        }
    }

    /** True se deletou; false se o ID nao existe. */
    public boolean deletar(Long id) {
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_DELETAR)) {

            pstmt.setLong(1, id);
            // executeUpdate devolve as linhas afetadas: DELETE sem match nao e erro
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            if (e.getErrorCode() == ORA_FK_COM_DEPENDENTES) {
                throw new RuntimeException("A equipe " + id + " tem intervenções registradas "
                        + "e não pode ser removida. Apague as intervenções primeiro "
                        + "ou marque a equipe como indisponível.", e);
            }
            throw new RuntimeException("Erro ao deletar equipe " + id + ": " + e.getMessage(), e);
        }
    }

    /** Corpo comum das consultas sem parametros que devolvem lista. */
    private List<EquipeManutencao> listar(String sql, String descricaoOperacao) {
        Connection conn = ConexaoBD.getInstancia().getConexao();
        List<EquipeManutencao> equipes = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                equipes.add(extrair(rs));
            }
            return equipes;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao " + descricaoOperacao + ": " + e.getMessage(), e);
        }
    }

    /** ResultSet -> record. */
    private EquipeManutencao extrair(ResultSet rs) throws SQLException {
        return new EquipeManutencao(
                rs.getLong("id"),
                rs.getString("nome"),
                TipoEquipe.valueOf(rs.getString("tipo")),
                rs.getInt("qt_integrantes"),
                rs.getInt("disponivel") == 1
        );
    }
}
