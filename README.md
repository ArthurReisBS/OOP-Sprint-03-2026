# MOTIVA — Persistência em Oracle

Sistema de priorização de roçada em rodovias, com os dados persistidos em Oracle via
**JDBC puro** (sem ORM) e padrão **DAO**.

Requer **Java 21** (usa `record`, `sealed` e pattern matching) e o `ojdbc17.jar` em `lib/`.

## Integrantes

| Integrante | RM |
| --- | --- |
| Arthur | RM562181 |
| Isabelle | RM566464 |
| Carol | RM564651 |
| Léo | RM563663 |
| Manoella | RM564469 |
| Júlia | RM565010 |

---

## 1. Como rodar

### 1.1 Credenciais

Saem de variáveis de ambiente — nunca do código.

```bash
export DB_USER=seu_rm_da_fiap
export DB_PASSWORD=sua_data_de_nascimento_DDMMAA
```

No Eclipse: *Run Configurations → aba Environment → Add*.

### 1.2 Compilar

```bash
javac -encoding UTF-8 -d bin -cp "lib/ojdbc17.jar" $(find src -name "*.java")
```

No Windows, o separador do classpath é `;` em vez de `:`.

### 1.3 Criar as tabelas

Sem isso, todo DAO devolve **ORA-00942**.

```bash
java -cp "bin:lib/ojdbc17.jar" br.com.motiva.ferramentas.ExecutarScript sql/seu-script-criacao.sql
java -cp "bin:lib/ojdbc17.jar" br.com.motiva.ferramentas.ExecutarScript sql/seu-script-dados.sql
```

`ExecutarScript` aplica o `.sql` pelo JDBC, comando a comando, e dispensa o `sqlplus`
(que exige o Oracle Instant Client instalado à parte). Quem tiver o `sqlplus` pode usar:

```bash
sqlplus $DB_USER/$DB_PASSWORD@oracle.fiap.com.br:1521:ORCL @sql/seu-script-criacao.sql
```

Os `DROP TABLE` do topo do script falham na primeira execução — é esperado, e a
ferramenta os marca como `ignorado`.

### 1.4 Executar

```bash
java -cp "bin:lib/ojdbc17.jar" br.com.motiva.main.Main
```

### 1.5 Diagnóstico

```bash
java -cp "bin:lib/ojdbc17.jar" br.com.motiva.ferramentas.Diagnostico
```

Mostra a versão do Oracle, o usuário conectado e quais tabelas existem. É o primeiro
comando a rodar diante de qualquer erro.

---

## 2. Estrutura

```
sql/
├── seu-script-criacao.sql    as 4 tabelas
└── seu-script-dados.sql      dados de teste
src/br/com/motiva/
├── db/            ConexaoBD (singleton) e a exceção de credenciais
├── model/         entidades, como records
├── dao/           os 4 DAOs
├── service/       GeradorRelatorio
├── main/          Main — os 7 passos
└── ferramentas/   apoio, fora da estrutura entregável
lib/ojdbc17.jar
```

---

## 3. Modelo

| Classe | Tabela |
| --- | --- |
| `EquipeManutencao` | `EQUIPE_MANUTENCAO` |
| `TrechoRodovia` | `TRECHO_RODOVIA` |
| `IntervencaoOperacional` → `RocadaMecanizada`, `Pulverizacao` | `INTERVENCAO_OPERACIONAL` |
| `RelatorioPrioridade` | `RELATORIO_PRIORIDADE` |

**A herança é uma interface `sealed`.** As implementações são `record`s, que não estendem
classe. Como o compilador conhece as duas únicas implementações, o `switch` do DAO
dispensa `default` e passa a falhar na compilação se um terceiro tipo for acrescentado.

**As duas subclasses dividem uma tabela**, separadas pela coluna `TIPO` (*single table
inheritance*). A alternativa — uma tabela por subclasse — exigiria `JOIN` ou `UNION` em
toda listagem do histórico. Só a roçada tem colunas próprias; o `CHECK ck_interv_subtipo`
garante que a pulverização as deixe nulas.

---

## 4. Regra de priorização

A prioridade **não é persistida**: é derivada em tempo de execução, porque envelhece
sozinha.

```
altura estimada = altura medida + taxa da espécie × dias desde a medição,
                  limitada ao porte máximo da espécie
```

| Prioridade | Altura estimada | Cor |
| --- | --- | --- |
| `NORMAL` | ≤ 15 cm | verde |
| `ATENCAO` | 16 a 25 cm | amarelo |
| `CRITICO` | 26 a 29 cm | vermelho |
| `URGENTE` | ≥ 30 cm | vermelho |

Os limiares intermediários são exclusivos: 15,0 cm ainda é `NORMAL` e 25,0 cm ainda é
`ATENCAO`.

**A espécie define a taxa de crescimento**, e a diferença entre elas é de 13x:

| Espécie | Taxa | Porte máximo |
| --- | --- | --- |
| Pennisetum (capim-elefante) | 14,5 cm/dia | 250 cm |
| Megathyrsus (capim-colonião) | 9,5 | 180 |
| Brachiaria (Urochloa) | 4,2 | 90 |
| Cynodon (grama-seda) | 1,5 | 45 |
| Paspalum (grama-batatais) | 1,1 | 40 |

A taxa é o crescimento em condições ideais, sem modulação por clima — a altura estimada
é um **limite superior, não uma previsão**. Para decidir a ordem de atendimento isso
basta e erra para o lado seguro. O teto por espécie evita a extrapolação absurda em
trechos sem medição recente.

---

## 5. Os 4 DAOs

Todos têm construtor padrão, `inserir()`, `buscarPorId()`, `listarTodas()`,
`atualizar()` e `deletar()`, com o SQL em constantes.

| DAO | Além do CRUD |
| --- | --- |
| `EquipeManutencaoDAO` | `listarDisponiveis()` |
| `TrechoRodoviaDAO` | `listarPorRodovia()` |
| `IntervencaoOperacionalDAO` | `listarPorTrecho()`, `contarPorTrecho()` |
| `RelatorioPrioridadeDAO` | `salvarRelatorio()`, `listarRecentes()` |

Duas regras valem nos quatro:

**`PreparedStatement` sempre**, nunca concatenação de String — risco de SQL Injection.

**A `Connection` é compartilhada e não entra no `try-with-resources`.** Ela vem do
singleton `ConexaoBD`; fechá-la dentro de um DAO derrubaria os demais. Cada DAO fecha
apenas o `PreparedStatement` e o `ResultSet` — o `try-with-resources` compila para um
`finally`. A conexão é responsabilidade do `Main`, que chama `desconectar()` no passo 7.

---

## 6. O `Main`

| Passo | O que faz |
| --- | --- |
| 1 | Testa a conexão |
| 2 | CRUD de `EquipeManutencao` |
| 3 | CRUD de `TrechoRodovia`, com recálculo da prioridade após nova medição |
| 4 | CRUD de `IntervencaoOperacional` — insere uma roçada e uma pulverização, exercitando os dois ramos do `switch` |
| 5 | Gera o relatório no console e o persiste |
| 6 | Consulta o histórico e compara com o relatório anterior |
| 7 | `desconectar()` |

Os passos 2, 3 e 4 apagam os registros que criam, então o `Main` pode ser rodado várias
vezes sem sujar as tabelas. O passo 7 fica num `finally`: a conexão fecha mesmo se um
passo estourar.

O `GeradorRelatorio` imprime no console **e** grava em `RELATORIO_PRIORIDADE`. O
histórico é o que permite comparar execuções e identificar trechos que aparecem como
urgentes com frequência.

---

## 7. Erros comuns

| Erro | Causa |
| --- | --- |
| `Driver not found` | `ojdbc17.jar` fora do classpath |
| `ORA-01017` | usuário ou senha errados |
| `ORA-00942` | rodar `seu-script-criacao.sql` primeiro |
| `ORA-02291` | trecho ou equipe informados não existem |
| `ORA-02292` | há registros dependentes — apague as intervenções antes do trecho |
| `ORA-02290` | combinação de colunas inválida para o `TIPO` |

Os três últimos são traduzidos para português pelos DAOs.

---

## 8. Testar um DAO isoladamente

`getConexao()` abre a conexão sozinho, então um DAO funciona sem o `Main`:

```java
public class TesteEquipeDAO {
    public static void main(String[] args) {
        new EquipeManutencaoDAO().listarTodas().forEach(System.out::println);
        ConexaoBD.getInstancia().desconectar();
    }
}
```
