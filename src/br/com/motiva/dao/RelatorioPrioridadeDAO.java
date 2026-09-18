package br.com.motiva.dao;

import br.com.motiva.db.ConexaoBD;
import br.com.motiva.model.RelatorioPrioridade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object de RELATORIO_PRIORIDADE.
 *
 * Alem do CRUD, expoe salvarRelatorio(), que recebe os contadores soltos e
 * monta a entidade: e o metodo que o GeradorRelatorio chama.
 */
public class RelatorioPrioridadeDAO {

    private static final String COLUNAS = """
            id, data_geracao, qt_urgente, qt_critico, qt_atencao, qt_normal, resumo""";

    private static final String SQL_INSERIR = """
            INSERT INTO relatorio_prioridade
                   (data_geracao, qt_urgente, qt_critico, qt_atencao, qt_normal, resumo)
            VALUES (?, ?, ?, ?, ?, ?)""";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLUNAS + " FROM relatorio_prioridade WHERE id = ?";

    private static final String SQL_LISTAR_TODAS =
            "SELECT " + COLUNAS + " FROM relatorio_prioridade ORDER BY data_geracao DESC";

    /** Os N mais recentes. FETCH FIRST e Oracle 12c+; antes disso seria ROWNUM. */
    private static final String SQL_LISTAR_RECENTES = "SELECT " + COLUNAS + """
             FROM relatorio_prioridade
            ORDER BY data_geracao DESC
            FETCH FIRST ? ROWS ONLY""";

    private static final String SQL_ATUALIZAR = """
            UPDATE relatorio_prioridade
               SET data_geracao = ?, qt_urgente = ?, qt_critico = ?,
                   qt_atencao = ?, qt_normal = ?, resumo = ?
             WHERE id = ?""";

    private static final String SQL_DELETAR =
            "DELETE FROM relatorio_prioridade WHERE id = ?";

    public RelatorioPrioridadeDAO() {
    }

    /** Monta a entidade a partir dos contadores e persiste; devolve com ID. */
    public RelatorioPrioridade salvarRelatorio(int qtUrgente, int qtCritico,
                                               int qtAtencao, int qtNormal, String resumo) {
        return inserir(RelatorioPrioridade.novo(qtUrgente, qtCritico, qtAtencao, qtNormal, resumo));
    }

    public RelatorioPrioridade inserir(RelatorioPrioridade relatorio) {
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERIR, new String[]{"ID"})) {

            // a data vem do objeto, e nao do DEFAULT SYSTIMESTAMP: o relatorio
            // carrega o instante em que foi gerado, nao o da gravacao
            LocalDateTime quando = relatorio.dataGeracao() == null
                    ? LocalDateTime.now()
                    : relatorio.dataGeracao();

            preencherParametros(pstmt, relatorio, quando);

            if (pstmt.executeUpdate() == 0) {
                throw new RuntimeException("INSERT nao afetou nenhuma linha.");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                Long idGerado = rs.next() ? rs.getLong(1) : null;
                return new RelatorioPrioridade(idGerado, quando,
                        relatorio.qtUrgente(), relatorio.qtCritico(),
                        relatorio.qtAtencao(), relatorio.qtNormal(), relatorio.resumo());
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar relatório: " + e.getMessage(), e);
        }
    }

    /** O relatorio com esse ID, ou null se nao existe. */
    public RelatorioPrioridade buscarPorId(Long id) {
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? extrair(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar relatório " + id
                    + ": " + e.getMessage(), e);
        }
    }

    /** Historico completo, do mais recente para o mais antigo. */
    public List<RelatorioPrioridade> listarTodas() {
        Connection conn = ConexaoBD.getInstancia().getConexao();
        List<RelatorioPrioridade> relatorios = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_LISTAR_TODAS);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                relatorios.add(extrair(rs));
            }
            return relatorios;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar relatórios: " + e.getMessage(), e);
        }
    }

    /** Os N relatorios mais recentes. */
    public List<RelatorioPrioridade> listarRecentes(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva: " + quantidade);
        }
        Connection conn = ConexaoBD.getInstancia().getConexao();
        List<RelatorioPrioridade> relatorios = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_LISTAR_RECENTES)) {

            pstmt.setInt(1, quantidade);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    relatorios.add(extrair(rs));
                }
                return relatorios;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar relatórios recentes: "
                    + e.getMessage(), e);
        }
    }

    public boolean atualizar(RelatorioPrioridade relatorio) {
        if (relatorio.id() == null) {
            throw new IllegalArgumentException("Relatorio sem ID nao pode ser atualizado.");
        }
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_ATUALIZAR)) {

            preencherParametros(pstmt, relatorio, relatorio.dataGeracao());
            pstmt.setLong(7, relatorio.id());        // o 7o e o WHERE, so existe no UPDATE

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar relatório " + relatorio.id()
                    + ": " + e.getMessage(), e);
        }
    }

    public boolean deletar(Long id) {
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_DELETAR)) {

            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar relatório " + id
                    + ": " + e.getMessage(), e);
        }
    }

    /** Preenche os 6 parametros comuns ao INSERT e ao UPDATE. */
    private void preencherParametros(PreparedStatement pstmt, RelatorioPrioridade relatorio,
                                     LocalDateTime quando) throws SQLException {

        pstmt.setTimestamp(1, Timestamp.valueOf(quando));
        pstmt.setInt(2, relatorio.qtUrgente());
        pstmt.setInt(3, relatorio.qtCritico());
        pstmt.setInt(4, relatorio.qtAtencao());
        pstmt.setInt(5, relatorio.qtNormal());
        pstmt.setString(6, relatorio.resumo());
    }

    /** ResultSet -> record. */
    private RelatorioPrioridade extrair(ResultSet rs) throws SQLException {
        Timestamp dataGeracao = rs.getTimestamp("data_geracao");

        return new RelatorioPrioridade(
                rs.getLong("id"),
                dataGeracao == null ? null : dataGeracao.toLocalDateTime(),
                rs.getInt("qt_urgente"),
                rs.getInt("qt_critico"),
                rs.getInt("qt_atencao"),
                rs.getInt("qt_normal"),
                rs.getString("resumo")
        );
    }
}
