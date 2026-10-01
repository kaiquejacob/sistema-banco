CREATE TABLE IF NOT EXISTS contas (
    cpf VARCHAR(11) PRIMARY KEY,
    titular VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    saldo DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    tipo VARCHAR(50) NOT NULL,
    data_abertura DATE NOT NULL DEFAULT (CURRENT_DATE)
    );

CREATE TABLE IF NOT EXISTS enderecos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cpf_titular VARCHAR(11) NOT NULL,
    cep VARCHAR(9),
    logradouro VARCHAR(150),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    uf VARCHAR(2),
    CONSTRAINT fk_enderecos_contas FOREIGN KEY (cpf_titular) REFERENCES contas(cpf) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS transacoes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cpf_titular VARCHAR(11) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    valor DECIMAL(15, 2) NOT NULL,
    data_hora DATETIME NOT NULL,
    CONSTRAINT fk_transacoes_contas FOREIGN KEY (cpf_titular) REFERENCES contas(cpf) ON DELETE CASCADE
    );