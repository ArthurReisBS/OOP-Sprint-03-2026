package br.com.motiva.dao;

import br.com.motiva.db.ConexaoBD;
import br.com.motiva.model.EspecieGrama;
import br.com.motiva.model.TrechoRodovia;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object de TRECHO_RODOVIA.
 *
 * DATA_ULTIMA_INTERVENCAO aceita null (trecho nunca intervindo), o que exige
 * setNull() na escrita e teste de null na leitura.
 *
 * Nao grava a prioridade: ela e derivada da altura e dos dias decorridos, e
 * persisti-la criaria uma copia que envelhece errado.
 */
public class TrechoRodoviaDAO {

    private static final String COLUNAS = """
            id, rodovia, km_inicial, km_final, especie_predominante,
            altura_vegetacao_cm, data_ultima_medicao, data_ultima_intervencao""";

    private static final String SQL_INSERIR = """
            INSERT INTO trecho_rodovia
                   (rodovia, km_inicial, km_final, especie_predominante,
                    altura_vegetacao_cm, data_ultima_medicao, data_ultima_intervencao)
            VALUES (?, ?, ?, ?, ?, ?, ?)""";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLUNAS + " FROM trecho_rodovia WHERE id = ?";

    private static final String SQL_LISTAR_TODAS =
            "SELECT " + COLUNAS + " FROM trecho_rodovia ORDER BY rodovia, km_inicial";

    private static final String SQL_LISTAR_POR_RODOVIA =
            "SELECT " + COLUNAS + " FROM trecho_rodovia WHERE rodovia = ? ORDER BY km_inicial";

    private static final String SQL_ATUALIZAR = """
            UPDATE trecho_rodovia
               SET rodovia = ?, km_inicial = ?, km_final = ?, especie_predominante = ?,
                   altura_vegetacao_cm = ?, data_ultima_medicao = ?,
                   data_ultima_intervencao = ?
             WHERE id = ?""";

    private static final String SQL_DELETAR =
            "DELETE FROM trecho_rodovia WHERE id = ?";

    /** ORA-02292: integrity constraint violated - child record found. */
    private static final int ORA_FK_COM_DEPENDENTES = 2292;

    /** ORA-00001: unique constraint violated. */
    private static final int ORA_CHAVE_DUPLICADA = 1;

    public TrechoRodoviaDAO() {
    }

    public TrechoRodovia inserir(TrechoRodovia trecho) {
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERIR, new String[]{"ID"})) {

            preencherParametros(pstmt, trecho);

            if (pstmt.executeUpdate() == 0) {
                throw new RuntimeException("INSERT nao afetou nenhuma linha.");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                Long idGerado = rs.next() ? rs.getLong(1) : null;
                return new TrechoRodovia(idGerado, trecho.rodovia(),
                        trecho.kmInicial(), trecho.kmFinal(), trecho.especiePredominante(),
                        trecho.alturaVegetacaoCm(), trecho.dataUltimaMedicao(),
                        trecho.dataUltimaIntervencao());
            }

        } catch (SQLException e) {
            if (e.getErrorCode() == ORA_CHAVE_DUPLICADA) {
                throw new RuntimeException("Já existe um trecho cadastrado em "
                        + trecho.identificacao() + ".", e);
            }
            throw new RuntimeException("Erro ao inserir trecho " + trecho.identificacao()
                    + ": " + e.getMessage(), e);
        }
    }

    /** O trecho com esse ID, ou null se nao existe. */
    public TrechoRodovia buscarPorId(Long id) {
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? extrair(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar trecho " + id + ": " + e.getMessage(), e);
        }
    }

    /** Todos os trechos monitorados; entrada do GeradorRelatorio. */
    public List<TrechoRodovia> listarTodas() {
        Connection conn = ConexaoBD.getInstancia().getConexao();
        List<TrechoRodovia> trechos = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_LISTAR_TODAS);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                trechos.add(extrair(rs));
            }
            return trechos;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar trechos: " + e.getMessage(), e);
        }
    }

    public List<TrechoRodovia> listarPorRodovia(String rodovia) {
        Connection conn = ConexaoBD.getInstancia().getConexao();
        List<TrechoRodovia> trechos = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_LISTAR_POR_RODOVIA)) {

            pstmt.setString(1, rodovia);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    trechos.add(extrair(rs));
                }
                return trechos;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar trechos da " + rodovia
                    + ": " + e.getMessage(), e);
        }
    }

    public boolean atualizar(TrechoRodovia trecho) {
        if (trecho.id() == null) {
            throw new IllegalArgumentException("Trecho sem ID nao pode ser atualizado.");
        }
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_ATUALIZAR)) {

            preencherParametros(pstmt, trecho);
            pstmt.setLong(8, trecho.id());          // o 8o e o WHERE, so existe no UPDATE

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar trecho " + trecho.id()
                    + ": " + e.getMessage(), e);
        }
    }

    public boolean deletar(Long id) {
        Connection conn = ConexaoBD.getInstancia().getConexao();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_DELETAR)) {

            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            if (e.getErrorCode() == ORA_FK_COM_DEPENDENTES) {
                throw new RuntimeException("O trecho " + id + " tem intervenções no histórico "
                        + "e não pode ser removido. Apague as intervenções dele primeiro.", e);
            }
            throw new RuntimeException("Erro ao deletar trecho " + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * Preenche os 7 parametros comuns ao INSERT e ao UPDATE.
     *
     * As duas queries listam as colunas na mesma ordem de proposito; alterar
     * uma sem a outra quebra em silencio, porque os tipos sao compativeis.
     */
    private void preencherParametros(PreparedStatement pstmt, TrechoRodovia trecho)
            throws SQLException {

        pstmt.setString(1, trecho.rodovia());
        pstmt.setDouble(2, trecho.kmInicial());
        pstmt.setDouble(3, trecho.kmFinal());
        pstmt.setString(4, trecho.especiePredominante().name());
        pstmt.setDouble(5, trecho.alturaVegetacaoCm());
        pstmt.setDate(6, Date.valueOf(trecho.dataUltimaMedicao()));

        // setDate nao aceita null com LocalDate: precisa de setNull tipado
        LocalDate ultimaIntervencao = trecho.dataUltimaIntervencao();
        if (ultimaIntervencao == null) {
            pstmt.setNull(7, Types.DATE);
        } else {
            pstmt.setDate(7, Date.valueOf(ultimaIntervencao));
        }
    }

    /** ResultSet -> record. */
    private TrechoRodovia extrair(ResultSet rs) throws SQLException {
        Date ultimaIntervencao = rs.getDate("data_ultima_intervencao");

        return new TrechoRodovia(
                rs.getLong("id"),
                rs.getString("rodovia"),
                rs.getDouble("km_inicial"),
                rs.getDouble("km_final"),
                EspecieGrama.valueOf(rs.getString("especie_predominante")),
                rs.getDouble("altura_vegetacao_cm"),
                rs.getDate("data_ultima_medicao").toLocalDate(),
                ultimaIntervencao == null ? null : ultimaIntervencao.toLocalDate()
        );
    }
}
