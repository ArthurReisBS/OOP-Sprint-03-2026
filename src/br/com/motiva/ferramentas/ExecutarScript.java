package br.com.motiva.ferramentas;

import br.com.motiva.db.ConexaoBD;
import br.com.motiva.db.CredenciaisInvalidasException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Executa um arquivo .sql pelo JDBC, dispensando o sqlplus.
 *
 *     java -cp "bin:lib/ojdbc17.jar" \
 *          br.com.motiva.ferramentas.ExecutarScript sql/seu-script-criacao.sql
 *
 * Cobre SQL separado por ponto e virgula. Nao interpreta diretivas de sqlplus
 * nem blocos PL/SQL. Ferramenta de apoio, fora da estrutura entregavel.
 */
public class ExecutarScript {

    /** ORA-00942: table or view does not exist. */
    private static final int ORA_TABELA_NAO_EXISTE = 942;

    /** ORA-00955: name is already used by an existing object. */
    private static final int ORA_OBJETO_JA_EXISTE = 955;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("uso: ExecutarScript <arquivo.sql>");
            System.exit(2);
        }

        Path arquivo = Path.of(args[0]);
        String conteudo;
        try {
            conteudo = Files.readString(arquivo);
        } catch (IOException e) {
            System.err.println("Nao foi possivel ler " + arquivo + ": " + e.getMessage());
            System.exit(2);
            return;
        }

        List<String> statements = separarStatements(conteudo);
        System.out.println("Arquivo : " + arquivo);
        System.out.println("Comandos: " + statements.size());
        System.out.println("=".repeat(70));

        int ok = 0;
        int tolerados = 0;
        List<String> falhas = new ArrayList<>();

        try {
            Connection conn = ConexaoBD.getInstancia().conectar();

            for (int i = 0; i < statements.size(); i++) {
                String sql = statements.get(i);
                String rotulo = String.format("[%02d] %s", i + 1, resumir(sql));

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                    System.out.println("  ok       " + rotulo);
                    ok++;

                } catch (SQLException e) {
                    if (eToleravel(e, sql)) {
                        System.out.println("  ignorado " + rotulo
                                + "  (ORA-" + e.getErrorCode() + ", esperado)");
                        tolerados++;
                    } else {
                        System.out.println("  FALHOU   " + rotulo);
                        System.out.println("           ORA-" + e.getErrorCode()
                                + ": " + primeiraLinha(e.getMessage()));
                        falhas.add(rotulo + " -> ORA-" + e.getErrorCode());
                    }
                }
            }

            // DDL no Oracle ja faz commit sozinho; isto cobre o DML
            if (!conn.getAutoCommit()) {
                conn.commit();
            }

        } catch (CredenciaisInvalidasException e) {
            System.err.println("\n" + e.getMessage());
            System.err.println(e.getDicaCorrecao());
            System.exit(1);

        } catch (SQLException e) {
            System.err.println("Erro de conexao: " + e.getMessage());
            System.exit(1);
        } finally {
            ConexaoBD.getInstancia().desconectar();
        }

        System.out.println("=".repeat(70));
        System.out.printf("ok: %d   ignorados: %d   falhas: %d%n", ok, tolerados, falhas.size());

        if (!falhas.isEmpty()) {
            System.out.println("\nFalhas:");
            falhas.forEach(f -> System.out.println("  " + f));
            System.exit(1);
        }
        System.out.println("\nScript aplicado com sucesso.");
    }

    /**
     * Quebra o script em comandos.
     *
     * Percorre caractere a caractere em vez de split(";") para nao quebrar em
     * ponto e virgula dentro de string literal ou comentario.
     */
    static List<String> separarStatements(String conteudo) {
        List<String> statements = new ArrayList<>();
        StringBuilder atual = new StringBuilder();

        boolean emString = false;
        boolean emComentarioLinha = false;

        for (int i = 0; i < conteudo.length(); i++) {
            char c = conteudo.charAt(i);
            char proximo = i + 1 < conteudo.length() ? conteudo.charAt(i + 1) : '\0';

            if (emComentarioLinha) {
                if (c == '\n') {
                    emComentarioLinha = false;
                    atual.append(c);
                }
                continue;
            }

            if (!emString && c == '-' && proximo == '-') {
                emComentarioLinha = true;
                i++;
                continue;
            }

            if (c == '\'') {
                // '' e aspa escapada, nao fim de string
                if (emString && proximo == '\'') {
                    atual.append(c).append(proximo);
                    i++;
                    continue;
                }
                emString = !emString;
                atual.append(c);
                continue;
            }

            if (c == ';' && !emString) {
                adicionarSeUtil(statements, atual.toString());
                atual.setLength(0);
                continue;
            }

            atual.append(c);
        }

        adicionarSeUtil(statements, atual.toString());   // ultimo, se veio sem ';'
        return statements;
    }

    private static void adicionarSeUtil(List<String> statements, String bruto) {
        String limpo = bruto.strip();
        if (limpo.isEmpty()) {
            return;
        }
        // diretivas de sqlplus nao rodam por JDBC
        String primeira = limpo.split("\\s+", 2)[0].toUpperCase();
        if (primeira.equals("SET") || primeira.equals("PROMPT")
                || primeira.equals("EXIT") || primeira.equals("SPOOL")) {
            return;
        }
        statements.add(limpo);
    }

    /** DROP do inexistente e CREATE do que ja existe sao esperados. */
    private static boolean eToleravel(SQLException e, String sql) {
        String inicio = sql.stripLeading().toUpperCase();

        if (e.getErrorCode() == ORA_TABELA_NAO_EXISTE && inicio.startsWith("DROP")) {
            return true;
        }
        return e.getErrorCode() == ORA_OBJETO_JA_EXISTE && inicio.startsWith("CREATE");
    }

    /** Resume o comando para o log. */
    private static String resumir(String sql) {
        String uma = sql.replaceAll("\\s+", " ").strip();
        return uma.length() <= 62 ? uma : uma.substring(0, 59) + "...";
    }

    private static String primeiraLinha(String mensagem) {
        if (mensagem == null) {
            return "(sem mensagem)";
        }
        int quebra = mensagem.indexOf('\n');
        return quebra < 0 ? mensagem : mensagem.substring(0, quebra);
    }
}
