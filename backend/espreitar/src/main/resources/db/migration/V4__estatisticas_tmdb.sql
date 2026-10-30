ALTER TABLE midia
    ADD COLUMN nota_media_externa DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN total_avaliacoes_externas INT NOT NULL DEFAULT 0;
