INSERT INTO midia (id_externo, tipo, titulo, ano, sinopse, cartaz, nota_media, total_avaliacoes)
VALUES
    ('demo-espreitar-001', 'FILME', 'Amostra de Filme', 2024,
     'Obra fictícia para testar a listagem e as avaliações do catálogo.', NULL, 0, 0),
    ('demo-espreitar-002', 'SERIE', 'Amostra de Série', 2023,
     'Série fictícia para testar os filtros de tipo e busca.', NULL, 0, 0),
    ('demo-espreitar-003', 'FILME', 'Amostra de Aventura', 2022,
     'Segundo filme fictício para testar a busca por título.', NULL, 0, 0)
ON DUPLICATE KEY UPDATE
    titulo = VALUES(titulo),
    ano = VALUES(ano),
    sinopse = VALUES(sinopse);
