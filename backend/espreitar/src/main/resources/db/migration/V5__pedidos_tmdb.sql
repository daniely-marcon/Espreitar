ALTER TABLE solicitacao
    ADD COLUMN id_externo VARCHAR(50) NULL,
    ADD COLUMN cartaz VARCHAR(500) NULL,
    ADD COLUMN nota_media_externa DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN total_avaliacoes_externas INT NOT NULL DEFAULT 0;
