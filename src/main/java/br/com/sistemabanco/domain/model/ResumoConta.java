package br.com.sistemabanco.domain.model;

import java.math.BigDecimal;

public record ResumoConta(String cpf, String titular, TipoConta tipo, BigDecimal saldo) {
}