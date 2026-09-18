package br.com.motiva.main;

import br.com.motiva.dao.EquipeManutencaoDAO;
import br.com.motiva.dao.IntervencaoOperacionalDAO;
import br.com.motiva.dao.RelatorioPrioridadeDAO;
import br.com.motiva.dao.TrechoRodoviaDAO;
import br.com.motiva.db.ConexaoBD;
import br.com.motiva.db.CredenciaisInvalidasException;
import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.EspecieGrama;
import br.com.motiva.model.IntervencaoOperacional;
import br.com.motiva.model.Pulverizacao;
import br.com.motiva.model.RelatorioPrioridade;
import br.com.motiva.model.RocadaMecanizada;
import br.com.motiva.model.TipoEquipe;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.service.GeradorRelatorio;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Demonstracao das operacoes do sistema, nos 7 passos pedidos.
 *
 * Os passos 2, 3 e 4 apagam os registros que criam, para o Main poder rodar
 * varias vezes sem sujar as tabelas nem alterar os numeros do relatorio.
 *
 * Antes de rodar, exportar DB_USER e DB_PASSWORD e aplicar os dois scripts
 * de sql/ (ver README).
 */
public class Main {

    private static final String LINHA = "=".repeat(78);

    private static final EquipeManutencaoDAO equipeDAO = new EquipeManutencaoDAO();
    private static final TrechoRodoviaDAO trechoDAO = new TrechoRodoviaDAO();
    private static final IntervencaoOperacionalDAO intervencaoDAO = new IntervencaoOperacionalDAO();
    private static final RelatorioPrioridadeDAO relatorioDAO = new RelatorioPrioridadeDAO();

    public static void main(String[] args) {
        // fixa o locale da saida para o formato nao variar com o do sistema
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));

        try {
            passo1TestarConexao();
            passo2CrudEquipe();
            passo3CrudTrecho();
            passo4CrudIntervencao();
            passo5GerarRelatorio();
            passo6ConsultarHistorico();

        } catch (CredenciaisInvalidasException e) {
            System.err.println("\n" + e.getMessage());
            System.err.println(e.getDicaCorrecao());

        } catch (RuntimeException e) {
            System.err.println("\nFalha na execução: " + e.getMessage());
            e.printStackTrace();

        } finally {
            // no finally: a conexao fecha mesmo se um passo estourar
            passo7Desconectar();
        }
    }

    // ------------------------------------------------------------------------
    // Passo 1 -- conexao
    // ------------------------------------------------------------------------
    private static void passo1TestarConexao() {
        titulo("PASSO 1 -- TESTE DE CONEXÃO");

        ConexaoBD.getInstancia().conectar();

        System.out.println("URL: " + ConexaoBD.getUrl());
        System.out.println("Conexão ativa: " + ConexaoBD.getInstancia().estaConectado());
    }

    // ------------------------------------------------------------------------
    // Passo 2 -- CRUD de EquipeManutencao
    // ------------------------------------------------------------------------
    private static void passo2CrudEquipe() {
        titulo("PASSO 2 -- CRUD DE EQUIPE DE MANUTENÇÃO");

        System.out.println("[R] Equipes já cadastradas:");
        equipeDAO.listarTodas().forEach(e -> System.out.println("    " + e));

        System.out.println("\n[C] Inserindo equipe de demonstração...");
        EquipeManutencao nova = equipeDAO.inserir(
                EquipeManutencao.nova("Equipe Echo - Demo", TipoEquipe.MISTA, 5, true));
        System.out.println("    inserida com ID " + nova.id() + ": " + nova);

        System.out.println("\n[R] Buscando pelo ID " + nova.id() + "...");
        System.out.println("    " + equipeDAO.buscarPorId(nova.id()));

        System.out.println("\n[U] Marcando como indisponível e reduzindo para 3 integrantes...");
        EquipeManutencao alterada = new EquipeManutencao(
                nova.id(), nova.nome(), nova.tipo(), 3, false);
        System.out.println("    atualizou? " + equipeDAO.atualizar(alterada));
        System.out.println("    " + equipeDAO.buscarPorId(nova.id()));

        System.out.println("\n[R] Só as disponíveis (a de demo sumiu, está indisponível):");
        equipeDAO.listarDisponiveis().forEach(e -> System.out.println("    " + e));

        System.out.println("\n[D] Removendo a equipe de demonstração...");
        System.out.println("    deletou? " + equipeDAO.deletar(nova.id()));
        System.out.println("    busca após delete: " + equipeDAO.buscarPorId(nova.id()));
    }

    // ------------------------------------------------------------------------
    // Passo 3 -- CRUD de TrechoRodovia
    // ------------------------------------------------------------------------
    private static void passo3CrudTrecho() {
        titulo("PASSO 3 -- CRUD DE TRECHO DE RODOVIA");

        System.out.println("[R] Trechos monitorados:");
        trechoDAO.listarTodas().forEach(t -> System.out.println("    " + t));

        System.out.println("\n[C] Inserindo trecho de demonstração...");
        TrechoRodovia novo = trechoDAO.inserir(TrechoRodovia.novo(
                "SP-999", 1.0, 5.0, EspecieGrama.CYNODON, 8.0,
                LocalDate.now().minusDays(4), null));
        System.out.println("    inserido com ID " + novo.id() + ": " + novo);
        System.out.printf ("    nunca intervindo? %s%n", novo.diasDesdeIntervencao().isEmpty());

        System.out.println("\n[R] Buscando pelo ID " + novo.id() + "...");
        System.out.println("    " + trechoDAO.buscarPorId(novo.id()));

        System.out.println("\n[U] Nova medição do sensor: 31 cm hoje"
                + " (passa do corte recomendado)...");
        TrechoRodovia medido = novo.comMedicao(31.0, LocalDate.now());
        System.out.println("    atualizou? " + trechoDAO.atualizar(medido));
        TrechoRodovia relido = trechoDAO.buscarPorId(novo.id());
        System.out.println("    " + relido);
        System.out.println("    prioridade recalculada: " + relido.prioridade()
                + " -- " + relido.prioridade().getRecomendacao());

        System.out.println("\n[R] Trechos da SP-999:");
        trechoDAO.listarPorRodovia("SP-999").forEach(t -> System.out.println("    " + t));

        System.out.println("\n[D] Removendo o trecho de demonstração...");
        System.out.println("    deletou? " + trechoDAO.deletar(novo.id()));
        System.out.println("    busca após delete: " + trechoDAO.buscarPorId(novo.id()));
    }

    // ------------------------------------------------------------------------
    // Passo 4 -- CRUD de IntervencaoOperacional
    // ------------------------------------------------------------------------
    private static void passo4CrudIntervencao() {
        titulo("PASSO 4 -- CRUD DE INTERVENÇÃO OPERACIONAL");

        // as FKs recusam IDs inexistentes (ORA-02291)
        List<TrechoRodovia> trechos = trechoDAO.listarTodas();
        List<EquipeManutencao> equipes = equipeDAO.listarTodas();

        if (trechos.isEmpty() || equipes.isEmpty()) {
            System.out.println("Sem trechos ou equipes cadastrados. "
                    + "Rode o sql/seu-script-dados.sql antes.");
            return;
        }

        Long trechoId = trechos.get(0).id();
        Long equipeId = equipes.get(0).id();

        System.out.println("[R] Histórico completo (" + intervencaoDAO.listarTodas().size()
                + " intervenções). As 5 mais recentes:");
        intervencaoDAO.listarTodas().stream().limit(5)
                .forEach(i -> System.out.println("    " + i));

        System.out.println("\n[C] Inserindo uma roçada de demonstração...");
        IntervencaoOperacional rocada = intervencaoDAO.inserir(RocadaMecanizada.nova(
                trechoId, equipeId, LocalDate.now(), 4200.00, 3.0, 10.0));
        System.out.println("    inserida com ID " + rocada.id() + ": " + rocada);

        System.out.println("\n[C] E uma pulverização, para exercitar o outro ramo do switch...");
        IntervencaoOperacional pulverizacao = intervencaoDAO.inserir(Pulverizacao.nova(
                trechoId, equipeId, LocalDate.now(), 2600.00));
        System.out.println("    inserida com ID " + pulverizacao.id() + ": " + pulverizacao);

        System.out.println("\n[R] Buscando pelo ID " + rocada.id()
                + " -- o TIPO da linha decide qual record volta:");
        IntervencaoOperacional lida = intervencaoDAO.buscarPorId(rocada.id());
        System.out.println("    " + lida);
        System.out.println("    classe reconstruída: " + lida.getClass().getSimpleName());
        System.out.printf ("    custo por km: R$ %.2f%n",
                lida.custoPorKm(trechos.get(0).extensaoKm()));

        System.out.println("\n[U] Corrigindo o custo da roçada para R$ 4.500,00...");
        RocadaMecanizada corrigida = new RocadaMecanizada(rocada.id(), trechoId, equipeId,
                rocada.dataExecucao(), 4500.00, 3.0, 10.0);
        System.out.println("    atualizou? " + intervencaoDAO.atualizar(corrigida));
        System.out.println("    " + intervencaoDAO.buscarPorId(rocada.id()));

        System.out.println("\n[D] Removendo as duas intervenções de demonstração...");
        System.out.println("    roçada deletada?       " + intervencaoDAO.deletar(rocada.id()));
        System.out.println("    pulverização deletada? "
                + intervencaoDAO.deletar(pulverizacao.id()));

        // depois do delete: antes, contaria as intervencoes de demonstracao
        System.out.println("\n[R] Recorrência por trecho (agregação no banco):");
        Map<Long, String> nomePorId = trechos.stream()
                .collect(Collectors.toMap(TrechoRodovia::id, TrechoRodovia::identificacao));

        intervencaoDAO.contarPorTrecho().stream().limit(5).forEach(r ->
                System.out.printf("    %-22s  %d %s  R$ %.2f no total  (média R$ %.2f)%n",
                        nomePorId.getOrDefault(r.trechoId(), "trecho " + r.trechoId()),
                        r.totalIntervencoes(),
                        r.totalIntervencoes() == 1 ? "intervenção " : "intervenções",
                        r.custoTotal(), r.custoMedio()));
    }

    // ------------------------------------------------------------------------
    // Passo 5 -- relatorio COM persistencia
    // ------------------------------------------------------------------------
    private static void passo5GerarRelatorio() {
        titulo("PASSO 5 -- RELATÓRIO DE PRIORIDADE (console + banco)");

        List<TrechoRodovia> trechos = trechoDAO.listarTodas();

        if (trechos.isEmpty()) {
            System.out.println("Nenhum trecho cadastrado. Rode o sql/seu-script-dados.sql antes.");
            return;
        }

        GeradorRelatorio gerador = new GeradorRelatorio(relatorioDAO);
        RelatorioPrioridade salvo = gerador.gerarRelatorio(trechos);

        System.out.println("\nO que foi gravado:");
        System.out.println("    " + salvo);
        System.out.println("    resumo: " + salvo.resumo());
    }

    // ------------------------------------------------------------------------
    // Passo 6 -- historico de relatorios
    // ------------------------------------------------------------------------
    private static void passo6ConsultarHistorico() {
        titulo("PASSO 6 -- HISTÓRICO DE RELATÓRIOS");

        List<RelatorioPrioridade> historico = relatorioDAO.listarRecentes(10);

        if (historico.isEmpty()) {
            System.out.println("Nenhum relatório no histórico ainda.");
            return;
        }

        System.out.println("Os " + historico.size() + " relatórios mais recentes:\n");
        historico.forEach(r -> System.out.println("    " + r));

        // a comparacao so e possivel porque ha mais de um relatorio salvo
        if (historico.size() >= 2) {
            RelatorioPrioridade atual = historico.get(0);
            RelatorioPrioridade anterior = historico.get(1);
            int variacao = atual.totalAcionaveis() - anterior.totalAcionaveis();

            System.out.printf("%n    Tendência: %d trechos acionáveis agora contra %d no "
                            + "relatório anterior (%+d).%n",
                    atual.totalAcionaveis(), anterior.totalAcionaveis(), variacao);
            System.out.println("    " + (variacao > 0
                    ? "A malha piorou desde o último relatório."
                    : variacao < 0
                        ? "A malha melhorou desde o último relatório."
                        : "Situação estável desde o último relatório."));
        }
    }

    // ------------------------------------------------------------------------
    // Passo 7 -- desconectar
    // ------------------------------------------------------------------------
    private static void passo7Desconectar() {
        titulo("PASSO 7 -- ENCERRAMENTO");

        ConexaoBD.getInstancia().desconectar();
        System.out.println("Conexão ativa: " + ConexaoBD.getInstancia().estaConectado());
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println(LINHA);
        System.out.println("  " + texto);
        System.out.println(LINHA);
    }
}
