package br.com.motiva.ferramentas;

import br.com.motiva.db.ConexaoBD;
import br.com.motiva.db.CredenciaisInvalidasException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostico do banco: versao, usuario e tabelas existentes.
 *
 *     java -cp "bin:lib/ojdbc17.jar" br.com.motiva.ferramentas.Diagnostico
 *
 * Usa o ojdbc17.jar do projeto, dispensando o sqlplus. Ferramenta de apoio,
 * fora da estrutura entregavel.
 */
public class Diagnostico {

    private static final String[] TABELAS_DO_PROJETO = {
            "EQUIPE_MANUTENCAO", "TRECHO_RODOVIA",
            "INTERVENCAO_OPERACIONAL", "RELATORIO_PRIORIDADE"
    };

    /** Versao minima para GENERATED AS IDENTITY e FETCH FIRST. */
    private static final int VERSAO_MINIMA_IDENTITY = 12;

    public static void main(String[] args) {
        try {
            Connection conn = ConexaoBD.getInstancia().conectar();

            int versaoMaior = versaoDoBanco(conn);
            usuarioConectado(conn);
            tabelasDoProjeto(conn);
            todasAsTabelas(conn);
            veredito(versaoMaior);

        } catch (CredenciaisInvalidasException e) {
            System.err.println("\n" + e.getMessage());
            System.err.println(e.getDicaCorrecao());

        } catch (RuntimeException e) {
            System.err.println("\nFalha no diagnostico: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ConexaoBD.getInstancia().desconectar();
        }
    }

    /** Via DatabaseMetaData: a view v$version exige privilegio que o aluno nao tem. */
    private static int versaoDoBanco(Connection conn) {
        titulo("1. VERSAO DO ORACLE");
        try {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("  Produto : " + meta.getDatabaseProductName());
            System.out.println("  Versao  : " + meta.getDatabaseProductVersion());
            System.out.println("  Driver  : " + meta.getDriverVersion());

            int maior = meta.getDatabaseMajorVersion();
            System.out.println("  Major   : " + maior);
            return maior;

        } catch (SQLException e) {
            System.out.println("  nao foi possivel ler a versao: " + e.getMessage());
            return -1;
        }
    }

    private static void usuarioConectado(Connection conn) {
        titulo("2. USUARIO E SCHEMA");
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT USER FROM dual");
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                System.out.println("  Conectado como: " + rs.getString(1));
            }
        } catch (SQLException e) {
            System.out.println("  erro: " + e.getMessage());
        }
    }

    private static void tabelasDoProjeto(Connection conn) {
        titulo("3. TABELAS DO PROJETO");

        List<String> encontradas = new ArrayList<>();
        String sql = "SELECT table_name FROM user_tables WHERE table_name = ?";

        for (String tabela : TABELAS_DO_PROJETO) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, tabela);
                try (ResultSet rs = pstmt.executeQuery()) {
                    boolean existe = rs.next();
                    System.out.printf("  %-26s %s%n", tabela, existe ? "existe" : "NAO EXISTE");
                    if (existe) {
                        encontradas.add(tabela);
                    }
                }
            } catch (SQLException e) {
                System.out.printf("  %-26s erro: %s%n", tabela, e.getMessage());
            }
        }

        System.out.println();
        if (encontradas.isEmpty()) {
            System.out.println("  >> Nenhuma tabela do projeto existe.");
            System.out.println("     Rode o script de criacao:");
            System.out.println("     java -cp \"bin:lib/ojdbc17.jar\" "
                    + "br.com.motiva.ferramentas.ExecutarScript sql/seu-script-criacao.sql");
        } else if (encontradas.size() < TABELAS_DO_PROJETO.length) {
            System.out.println("  >> So " + encontradas.size() + " de "
                    + TABELAS_DO_PROJETO.length + " tabelas existem: o script de criacao");
            System.out.println("     falhou no meio. Rode-o de novo e veja qual statement quebra.");
        } else {
            System.out.println("  >> Todas as tabelas existem. Se o Main ainda der ORA-00942,");
            System.out.println("     o problema e de schema/usuario, nao de criacao.");
        }
    }

    private static void todasAsTabelas(Connection conn) {
        titulo("4. TODAS AS TABELAS DESTE USUARIO");

        String sql = "SELECT table_name FROM user_tables ORDER BY table_name";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            int total = 0;
            while (rs.next()) {
                System.out.println("  " + rs.getString(1));
                total++;
            }
            System.out.println(total == 0 ? "  (nenhuma)" : "\n  total: " + total);

        } catch (SQLException e) {
            System.out.println("  erro: " + e.getMessage());
        }
    }

    private static void veredito(int versaoMaior) {
        titulo("5. VEREDITO SOBRE O SCRIPT DE CRIACAO");

        if (versaoMaior < 0) {
            System.out.println("  Versao desconhecida -- nao da para concluir.");
            return;
        }

        if (versaoMaior >= VERSAO_MINIMA_IDENTITY) {
            System.out.println("  Oracle " + versaoMaior + ": suporta GENERATED AS IDENTITY");
            System.out.println("  e FETCH FIRST ROWS ONLY. O script de criacao esta correto");
            System.out.println("  como esta; se as tabelas nao existem, ele so nao foi rodado.");
        } else {
            System.out.println("  Oracle " + versaoMaior + ": NAO suporta GENERATED AS IDENTITY");
            System.out.println("  (12c+) nem FETCH FIRST ROWS ONLY.");
            System.out.println();
            System.out.println("  O script precisa ser reescrito com SEQUENCE + TRIGGER para");
            System.out.println("  os IDs, e o listarRecentes() do RelatorioPrioridadeDAO");
            System.out.println("  precisa usar ROWNUM. Avise para eu fazer a conversao.");
        }
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("  " + texto);
        System.out.println("=".repeat(70));
    }
}
