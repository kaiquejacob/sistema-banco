package br.com.sistemabanco.domain.model;

import java.math.BigDecimal;

public enum TipoConta {
    CORRENTE("Conta Corrente") {
        @Override
        public BigDecimal calcularTaxaManutencao() {
            return new BigDecimal("12.00");
        }

        @Override
        public BigDecimal obterLimiteSaqueDiario() {
            return new BigDecimal("2000.00");
        }
    },
    POUPANCA("Conta Poupança") {
        @Override
        public BigDecimal calcularTaxaManutencao() {
            return BigDecimal.ZERO;
        }

        @Override
        public BigDecimal obterLimiteSaqueDiario() {
            return new BigDecimal("1000.00");
        }
    },
    INVESTIMENTO("Conta Investimento") {
        @Override
        public BigDecimal calcularTaxaManutencao() {
            return new BigDecimal("25.00");
        }

        @Override
        public BigDecimal obterLimiteSaqueDiario() {
            return new BigDecimal("5000.00");
        }
    };

    public final String nome;

    TipoConta(String nome) {
        this.nome = nome;
    }

    public abstract BigDecimal calcularTaxaManutencao();
    public abstract BigDecimal obterLimiteSaqueDiario();
}