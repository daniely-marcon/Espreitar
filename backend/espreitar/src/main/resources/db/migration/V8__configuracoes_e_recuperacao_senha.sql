CREATE TABLE configuracao_sistema (
    chave VARCHAR(80) PRIMARY KEY,
    valor VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO configuracao_sistema (chave, valor)
VALUES ('limite_denuncias', '5');

CREATE TABLE token_redefinicao_senha (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    data_expiracao DATETIME NOT NULL,
    CONSTRAINT uk_token_redefinicao_hash UNIQUE (token_hash),
    CONSTRAINT fk_token_redefinicao_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_token_redefinicao_usuario ON token_redefinicao_senha (usuario_id);
