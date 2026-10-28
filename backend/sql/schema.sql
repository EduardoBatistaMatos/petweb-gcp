-- Estrutura de referência da tabela já existente no banco "petshop".
-- A API não executa este script e não altera a estrutura do banco.
-- Use-o apenas para recriar o ambiente em outra máquina, se necessário.

CREATE TABLE IF NOT EXISTS pet (
    id                SERIAL PRIMARY KEY,
    nome              VARCHAR(100) NOT NULL,
    especie           VARCHAR(50)  NOT NULL,
    raca              VARCHAR(100),
    porte             VARCHAR(30),
    responsavel       VARCHAR(150) NOT NULL,
    data_nascimento   DATE,
    data_insercao_img TIMESTAMP,
    detalhes          TEXT,
    imagem_url        TEXT,
    fotos             JSONB NOT NULL DEFAULT '[]'::jsonb,
    img_principal     TEXT
);
