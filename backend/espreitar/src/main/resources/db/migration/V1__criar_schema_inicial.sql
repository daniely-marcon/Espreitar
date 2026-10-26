-- V1: estrutura inicial do Espreitar (conforme DER do grupo)
-- Requer MySQL 8.0.16+ (para o CHECK ser aplicado)

CREATE TABLE usuario (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    senha         VARCHAR(255) NULL,          -- hash BCrypt; nula em conta Google
    provedor      VARCHAR(10)  NOT NULL DEFAULT 'LOCAL',
    google_id     VARCHAR(50)  NULL,          -- campo "sub" do token do Google
    perfil        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    foto_perfil   VARCHAR(500) NULL,
    data_criacao  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_email     UNIQUE (email),
    CONSTRAINT uk_usuario_google_id UNIQUE (google_id),
    CONSTRAINT ck_usuario_provedor  CHECK (provedor IN ('LOCAL', 'GOOGLE')),
    CONSTRAINT ck_usuario_perfil    CHECK (perfil IN ('USER', 'ADMIN')),
    CONSTRAINT ck_usuario_credencial CHECK (
        (provedor = 'LOCAL'  AND senha IS NOT NULL) OR
        (provedor = 'GOOGLE' AND google_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Atributo multivalorado "notificacao" do usuario
CREATE TABLE usuario_notificacao (
    usuario_id  BIGINT       NOT NULL,
    mensagem    VARCHAR(255) NOT NULL,
    CONSTRAINT fk_notificacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notificacao_usuario ON usuario_notificacao (usuario_id);

CREATE TABLE midia (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_externo        VARCHAR(50)  NULL,      -- nulo em obra cadastrada manualmente
    tipo              VARCHAR(10)  NOT NULL,
    titulo            VARCHAR(255) NOT NULL,
    ano               INT          NULL,
    sinopse           TEXT         NULL,
    cartaz            VARCHAR(500) NULL,      -- URL; vem da API, nulo em obra manual
    nota_media        DOUBLE       NOT NULL DEFAULT 0,  -- derivado de avaliacao
    total_avaliacoes  INT          NOT NULL DEFAULT 0,  -- derivado de avaliacao
    CONSTRAINT uk_midia_externo UNIQUE (id_externo, tipo),
    CONSTRAINT ck_midia_tipo    CHECK (tipo IN ('FILME', 'SERIE')),
    CONSTRAINT ck_midia_nota    CHECK (nota_media BETWEEN 0 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Relacionamento "avalia" (usuario N:N midia), com id proprio
-- para a futura tabela de denuncia apontar para ela
CREATE TABLE avaliacao (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id         BIGINT   NOT NULL,
    midia_id           BIGINT   NOT NULL,
    nota               INT      NOT NULL,
    comentario         TEXT     NULL,
    spoiler            BIT(1)   NOT NULL DEFAULT b'0',
    data_criacao       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao   DATETIME NULL,
    CONSTRAINT fk_avaliacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_avaliacao_midia   FOREIGN KEY (midia_id)   REFERENCES midia (id)   ON DELETE CASCADE,
    CONSTRAINT uk_avaliacao_usuario_midia UNIQUE (usuario_id, midia_id),
    CONSTRAINT ck_avaliacao_nota CHECK (nota BETWEEN 0 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_avaliacao_midia ON avaliacao (midia_id);

-- Pedido de inclusao de obra que nao foi achada na API nem no banco local
CREATE TABLE solicitacao (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id    BIGINT       NOT NULL,
    titulo        VARCHAR(255) NOT NULL,
    ano           INT          NULL,
    tipo          VARCHAR(10)  NOT NULL,
    sinopse       TEXT         NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    nota          INT          NULL,          -- avaliacao inicial (opcional)
    comentario    TEXT         NULL,
    spoiler       BIT(1)       NOT NULL DEFAULT b'0',
    data_criacao  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_solicitacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT ck_solicitacao_tipo   CHECK (tipo IN ('FILME', 'SERIE')),
    CONSTRAINT ck_solicitacao_status CHECK (status IN ('PENDENTE', 'APROVADA', 'REJEITADA')),
    CONSTRAINT ck_solicitacao_nota   CHECK (nota IS NULL OR nota BETWEEN 0 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_solicitacao_status ON solicitacao (status);