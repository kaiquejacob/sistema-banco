package br.com.sistemabanco.responses;

import br.com.sistemabanco.domain.model.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoResponse(TipoTransacao tipo, String descricao, BigDecimal valor, LocalDateTime dataHora) {
}