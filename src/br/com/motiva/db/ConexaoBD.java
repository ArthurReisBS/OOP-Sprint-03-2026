package br.com.motiva.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Conexao com o Oracle, em singleton.
 *
 * Uma conexao unica, aberta no inicio e fechada no fim, em vez de uma por
 * operacao: o banco tem limite de sessoes por usuario.
 *
 * A conexao devolvida por getConexao() e COMPARTILHADA: nunca a coloque em
 * try-with-resources dentro de um DAO, ou o primeiro a terminar fecha a dos
 * demais. Os DAOs fecham apenas PreparedStatement e ResultSet.
 *
 * Credenciais vem das variaveis de ambiente DB_USER e DB_PASSWORD.
 */
public class ConexaoBD {

    private static final String HOST = "oracle.fiap.com.br";
    private static final String PORT = "1521";
    private static final String SID  = "ORCL";

    private static final String URL = "jdbc:oracle:thin:@" + HOST + ":" + PORT + ":" + SID;

    /** ORA-01017: invalid username/password. */
    private static final int ORA_CREDENCIAIS_INVALIDAS = 1017;

    private static final int TIMEOUT_VALIDACAO = 5;

    private static ConexaoBD instancia;

    private Connection conexao;

    private ConexaoBD() {
    }

    public static synchronized ConexaoBD getInstancia() {
        if (instancia == null) {
            instancia = new ConexaoBD();
        }
        return instancia;
    }

    /** Abre a conexao, ou reaproveita a atual se ainda estiver viva. */
    public Connection conectar() {
        if (estaConectado()) {
            return conexao;
        }

        String usuario = System.getenv("DB_USER");
        String senha   = System.getenv("DB_PASSWORD");

        if (usuario == null || usuario.isBlank() || senha == null || senha.isBlank()) {
            throw new CredenciaisInvalidasException(
                    "DB_USER e/ou DB_PASSWORD nao definidas no ambiente.");
        }

        try {
            conexao = DriverManager.getConnection(URL, usuario, senha);
            System.out.println("Conectado ao Oracle da FIAP como " + mascarar(usuario) + ".");
            return conexao;

        } catch (SQLException e) {
            if (e.getErrorCode() == ORA_CREDENCIAIS_INVALIDAS) {
                throw new CredenciaisInvalidasException(
                        "Credenciais invalidas para o usuario '" + mascarar(usuario) + "'.", e);
            }
            throw new RuntimeException(
                    "Falha ao conectar em " + URL + ": " + e.getMessage(), e);
        }
    }

    /**
     * A conexao aberta, abrindo-a se necessario.
     *
     * O conectar() automatico permite testar um DAO isoladamente, sem o Main.
     */
    public Connection getConexao() {
        return estaConectado() ? conexao : conectar();
    }

    public boolean estaConectado() {
        try {
            return conexao != null
                    && !conexao.isClosed()
                    && conexao.isValid(TIMEOUT_VALIDACAO);
        } catch (SQLException e) {
            return false;
        }
    }

    /** Fecha a conexao. Idempotente. */
    public void desconectar() {
        if (conexao == null) {
            return;
        }
        try {
            if (!conexao.isClosed()) {
                conexao.close();
                System.out.println("Conexão encerrada.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao fechar a conexão: " + e.getMessage());
        } finally {
            conexao = null;
        }
    }

    public static String getUrl() {
        return URL;
    }

    private static String mascarar(String usuario) {
        return usuario.substring(0, Math.min(3, usuario.length())) + "***";
    }
}
