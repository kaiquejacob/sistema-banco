package br.com.sistemabanco.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transacao {
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final TipoTransacao tipo;
    private final BigDecimal valor;
    private final LocalDateTime dataHora;

    public Transacao(TipoTransacao tipo, BigDecimal valor) {
        this(tipo, valor, LocalDateTime.now());
    }

    public Transacao(TipoTransacao tipo, BigDecimal valor, LocalDateTime dataHora) {
        this.tipo = tipo;
        this.valor = valor;
        this.dataHora = dataHora;
    }

    public TipoTransacao getTipo() { return tipo; }
    public BigDecimal getValor() { return valor; }
    public LocalDateTime getDataHora() { return dataHora; }

    @Override
    public String toString() {
        return String.format("[%s] %-20s: R$ %.2f", dataHora.format(FORMATO_DATA), tipo.descricao, valor);
    }

}