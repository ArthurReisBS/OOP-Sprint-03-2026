-- ============================================================================
--  MOTIVA -- Script de dados de teste
--
--  Rodar depois do seu-script-criacao.sql.
--
--  As datas sao relativas a SYSDATE, entao o script vale em qualquer dia.
--  Os trechos cobrem as quatro faixas de prioridade, para o relatorio sair
--  com os quatro contadores diferentes de zero.
--
--      >= 30 cm URGENTE   > 25 cm CRITICO   > 15 cm ATENCAO   <= 15 cm NORMAL
--
--  Taxas de crescimento, em cm/dia:
--      PENNISETUM 14,5 | MEGATHYRSUS 9,5 | BRACHIARIA 4,2 | CYNODON 1,5 | PASPALUM 1,1
-- ============================================================================

-- O ID nunca e informado: as tabelas usam IDENTITY. As FKs abaixo buscam o ID
-- pela chave de negocio, o que permite rodar sem saber os numeros gerados.

-- ----------------------------------------------------------------------------
-- Equipes
-- ----------------------------------------------------------------------------
INSERT INTO equipe_manutencao (nome, tipo, qt_integrantes, disponivel)
     VALUES ('Equipe Alfa - Roçada Norte',   'ROCADA',       6, 1);
INSERT INTO equipe_manutencao (nome, tipo, qt_integrantes, disponivel)
     VALUES ('Equipe Bravo - Roçada Sul',    'ROCADA',       5, 1);
INSERT INTO equipe_manutencao (nome, tipo, qt_integrantes, disponivel)
     VALUES ('Equipe Charlie - Pulverização', 'PULVERIZACAO', 4, 1);
INSERT INTO equipe_manutencao (nome, tipo, qt_integrantes, disponivel)
     VALUES ('Equipe Delta - Mista',         'MISTA',        8, 0);


-- ----------------------------------------------------------------------------
-- Trechos monitorados
-- ----------------------------------------------------------------------------
INSERT INTO trecho_rodovia (rodovia, km_inicial, km_final, especie_predominante,
                            altura_vegetacao_cm, data_ultima_medicao, data_ultima_intervencao)
     VALUES ('SP-021',  12.00,  18.50, 'PASPALUM',    22.0, SYSDATE -  8, SYSDATE - 34);
--                          22,0 + 1,1*8  =  30,8  -> URGENTE

-- especie rapida: 3 dias bastam para passar do corte recomendado
INSERT INTO trecho_rodovia (rodovia, km_inicial, km_final, especie_predominante,
                            altura_vegetacao_cm, data_ultima_medicao, data_ultima_intervencao)
     VALUES ('SP-021',  32.00,  38.40, 'MEGATHYRSUS',  5.0, SYSDATE -  3, SYSDATE -  6);
--                          5,0 + 9,5*3   =  33,5  -> URGENTE

INSERT INTO trecho_rodovia (rodovia, km_inicial, km_final, especie_predominante,
                            altura_vegetacao_cm, data_ultima_medicao, data_ultima_intervencao)
     VALUES ('SP-021',  47.00,  53.00, 'PASPALUM',    20.0, SYSDATE -  6, SYSDATE - 40);
--                          20,0 + 1,1*6  =  26,6  -> CRITICO

INSERT INTO trecho_rodovia (rodovia, km_inicial, km_final, especie_predominante,
                            altura_vegetacao_cm, data_ultima_medicao, data_ultima_intervencao)
     VALUES ('SP-021',  61.00,  68.40, 'CYNODON',     22.0, SYSDATE -  3, SYSDATE - 28);
--                          22,0 + 1,5*3  =  26,5  -> CRITICO

INSERT INTO trecho_rodovia (rodovia, km_inicial, km_final, especie_predominante,
                            altura_vegetacao_cm, data_ultima_medicao, data_ultima_intervencao)
     VALUES ('SP-021',  79.00,  85.00, 'PASPALUM',    14.0, SYSDATE -  5, SYSDATE - 30);
--                          14,0 + 1,1*5  =  19,5  -> ATENCAO

INSERT INTO trecho_rodovia (rodovia, km_inicial, km_final, especie_predominante,
                            altura_vegetacao_cm, data_ultima_medicao, data_ultima_intervencao)
     VALUES ('SP-021',  94.00, 101.20, 'BRACHIARIA',   8.0, SYSDATE -  3, SYSDATE - 17);
--                          8,0 + 4,2*3   =  20,6  -> ATENCAO

INSERT INTO trecho_rodovia (rodovia, km_inicial, km_final, especie_predominante,
                            altura_vegetacao_cm, data_ultima_medicao, data_ultima_intervencao)
     VALUES ('SP-021', 112.00, 118.00, 'CYNODON',      6.0, SYSDATE -  4, SYSDATE - 12);
--                          6,0 + 1,5*4   =  12,0  -> NORMAL

INSERT INTO trecho_rodovia (rodovia, km_inicial, km_final, especie_predominante,
                            altura_vegetacao_cm, data_ultima_medicao, data_ultima_intervencao)
     VALUES ('SP-021', 128.00, 134.70, 'PASPALUM',     5.0, SYSDATE -  3, SYSDATE -  9);
--                          5,0 + 1,1*3   =   8,3  -> NORMAL

-- DATA_ULTIMA_INTERVENCAO nula: caso valido que o codigo precisa suportar
INSERT INTO trecho_rodovia (rodovia, km_inicial, km_final, especie_predominante,
                            altura_vegetacao_cm, data_ultima_medicao, data_ultima_intervencao)
     VALUES ('SP-021', 145.00, 149.50, 'BRACHIARIA',   2.0, SYSDATE -  2, NULL);
--                          2,0 + 4,2*2   =  10,4  -> NORMAL


-- ----------------------------------------------------------------------------
-- Historico de intervencoes
--
-- As datas batem com DATA_ULTIMA_INTERVENCAO dos trechos acima.
-- ----------------------------------------------------------------------------

-- km 12,0: mesmo a especie mais lenta passa do corte em 34 dias
INSERT INTO intervencao_operacional
       (trecho_id, equipe_id, tipo, data_execucao, custo,
        largura_faixa_m, altura_corte_cm)
     VALUES (
        (SELECT id FROM trecho_rodovia    WHERE rodovia = 'SP-021' AND km_inicial = 12.00),
        (SELECT id FROM equipe_manutencao WHERE nome = 'Equipe Alfa - Roçada Norte'),
        'ROCADA', SYSDATE - 34, 4850.00,
        3.50, 10.00);

INSERT INTO intervencao_operacional
       (trecho_id, equipe_id, tipo, data_execucao, custo,
        largura_faixa_m, altura_corte_cm)
     VALUES (
        (SELECT id FROM trecho_rodovia    WHERE rodovia = 'SP-021' AND km_inicial = 32.00),
        (SELECT id FROM equipe_manutencao WHERE nome = 'Equipe Alfa - Roçada Norte'),
        'ROCADA', SYSDATE - 6, 5120.00,
        3.50, 12.00);

INSERT INTO intervencao_operacional
       (trecho_id, equipe_id, tipo, data_execucao, custo,
        largura_faixa_m, altura_corte_cm)
     VALUES (
        (SELECT id FROM trecho_rodovia    WHERE rodovia = 'SP-021' AND km_inicial = 47.00),
        (SELECT id FROM equipe_manutencao WHERE nome = 'Equipe Charlie - Pulverização'),
        'PULVERIZACAO', SYSDATE - 40, 3200.00,
        NULL, NULL);

INSERT INTO intervencao_operacional
       (trecho_id, equipe_id, tipo, data_execucao, custo,
        largura_faixa_m, altura_corte_cm)
     VALUES (
        (SELECT id FROM trecho_rodovia    WHERE rodovia = 'SP-021' AND km_inicial = 61.00),
        (SELECT id FROM equipe_manutencao WHERE nome = 'Equipe Bravo - Roçada Sul'),
        'ROCADA', SYSDATE - 28, 4400.00,
        3.00, 10.00);

INSERT INTO intervencao_operacional
       (trecho_id, equipe_id, tipo, data_execucao, custo,
        largura_faixa_m, altura_corte_cm)
     VALUES (
        (SELECT id FROM trecho_rodovia    WHERE rodovia = 'SP-021' AND km_inicial = 79.00),
        (SELECT id FROM equipe_manutencao WHERE nome = 'Equipe Bravo - Roçada Sul'),
        'ROCADA', SYSDATE - 30, 3980.00,
        3.00, 8.00);

INSERT INTO intervencao_operacional
       (trecho_id, equipe_id, tipo, data_execucao, custo,
        largura_faixa_m, altura_corte_cm)
     VALUES (
        (SELECT id FROM trecho_rodovia    WHERE rodovia = 'SP-021' AND km_inicial = 94.00),
        (SELECT id FROM equipe_manutencao WHERE nome = 'Equipe Charlie - Pulverização'),
        'PULVERIZACAO', SYSDATE - 17, 2750.00,
        NULL, NULL);

-- km 112,0: duas intervencoes no mesmo trecho, para exercitar a recorrencia
INSERT INTO intervencao_operacional
       (trecho_id, equipe_id, tipo, data_execucao, custo,
        largura_faixa_m, altura_corte_cm)
     VALUES (
        (SELECT id FROM trecho_rodovia    WHERE rodovia = 'SP-021' AND km_inicial = 112.00),
        (SELECT id FROM equipe_manutencao WHERE nome = 'Equipe Alfa - Roçada Norte'),
        'ROCADA', SYSDATE - 45, 3100.00,
        2.50, 8.00);

INSERT INTO intervencao_operacional
       (trecho_id, equipe_id, tipo, data_execucao, custo,
        largura_faixa_m, altura_corte_cm)
     VALUES (
        (SELECT id FROM trecho_rodovia    WHERE rodovia = 'SP-021' AND km_inicial = 112.00),
        (SELECT id FROM equipe_manutencao WHERE nome = 'Equipe Alfa - Roçada Norte'),
        'ROCADA', SYSDATE - 12, 3250.00,
        2.50, 8.00);

INSERT INTO intervencao_operacional
       (trecho_id, equipe_id, tipo, data_execucao, custo,
        largura_faixa_m, altura_corte_cm)
     VALUES (
        (SELECT id FROM trecho_rodovia    WHERE rodovia = 'SP-021' AND km_inicial = 128.00),
        (SELECT id FROM equipe_manutencao WHERE nome = 'Equipe Bravo - Roçada Sul'),
        'ROCADA', SYSDATE - 9, 2900.00,
        2.50, 8.00);


-- ----------------------------------------------------------------------------
-- Relatorio anterior, para o historico ter com o que comparar.
-- ----------------------------------------------------------------------------
INSERT INTO relatorio_prioridade
       (data_geracao, qt_urgente, qt_critico, qt_atencao, qt_normal, resumo)
     VALUES (SYSTIMESTAMP - 7, 1, 2, 3, 3,
             'Relatorio da semana anterior: 1 trecho urgente no km 12 da SP-021. '
             || 'Roçada priorizada para a Equipe Alfa.');

COMMIT;

-- Conferencia
SELECT 'equipes'       AS tabela, COUNT(*) AS linhas FROM equipe_manutencao
UNION ALL
SELECT 'trechos',       COUNT(*) FROM trecho_rodovia
UNION ALL
SELECT 'intervencoes',  COUNT(*) FROM intervencao_operacional
UNION ALL
SELECT 'relatorios',    COUNT(*) FROM relatorio_prioridade;
