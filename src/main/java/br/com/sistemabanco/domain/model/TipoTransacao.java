package br.com.sistemabanco.domain.model;

public enum TipoTransacao {
    ABERTURA_CONTA("Saldo Inicial"),
    DEPOSITO("Depósito"),
    SAQUE("Saque"),
    TAXA_MANUTENCAO("Taxa de manutenção"),
    TRANSFERENCIA_ENVIADA("Transf. Enviada"),
    TRANSFERENCIA_RECEBIDA("Transf. Recebida");

    public final String descricao;

    TipoTransacao(String descricao) {
        this.descricao = descricao;
    }
}