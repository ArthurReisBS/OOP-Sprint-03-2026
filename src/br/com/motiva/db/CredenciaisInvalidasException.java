package br.com.motiva.db;

/**
 * Traduz o ORA-01017 e a ausencia de credenciais para o dominio da aplicacao.
 */
public class CredenciaisInvalidasException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CredenciaisInvalidasException(String message) {
        super(message);
    }

    public CredenciaisInvalidasException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getDicaCorrecao() {
        return """

        ERRO DE AUTENTICACAO NO BANCO DE DADOS

          export DB_USER=seu_RM_da_FIAP
          export DB_PASSWORD=sua_data_de_nascimento_DDMMAA

          java -cp "bin:lib/ojdbc17.jar" br.com.motiva.main.Main

        A senha padrao da FIAP e a data de nascimento em DDMMAA, seis digitos,
        sem barra e sem espaco.
        """;
    }
}
