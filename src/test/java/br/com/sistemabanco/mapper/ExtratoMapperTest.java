package br.com.sistemabanco.mapper;

import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.ContaInvestimento;
import br.com.sistemabanco.domain.model.ContaPoupanca;
import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.domain.model.TipoTransacao;
import br.com.sistemabanco.presentation.mapper.ExtratoMapper;
import br.com.sistemabanco.presentation.responses.ExtratoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExtratoMapperTest {

    private final Endereco endereco = new Endereco("01001000", "Praca da Se", "Se", "Sao Paulo", "SP");

    @Test
    void deveMontarExtratoDeContaCorrenteComSaquesRestantes() {
        ContaCorrente conta = new ContaCorrente("Maria Souza", new BigDecimal("1000.00"),
                "12345678909", "maria@email.com", endereco);
        conta.sacar(new BigDecimal("100.00"));

        ExtratoResponse extrato = ExtratoMapper.de(conta);

        assertEquals("12345678909", extrato.cpf());
        assertEquals("Sao Paulo", extrato.endereco().cidade());
        assertEquals(1, extrato.transacoes().size());
        assertEquals(TipoTransacao.SAQUE, extrato.transacoes().get(0).tipo());
        assertEquals("Saque", extrato.transacoes().get(0).descricao());
        assertEquals(1, extrato.detalhes().saquesRealizadosNoMes());
        assertEquals(1, extrato.detalhes().saquesGratuitosRestantes());
        assertNull(extrato.detalhes().rendimentoMensalEstimado());
        assertNull(extrato.detalhes().taxaAdministracao());
    }

    @Test
    void deveMontarExtratoDePoupancaComRendimentoArredondado() {
        ContaBancaria conta = new ContaPoupanca("Maria Souza", new BigDecimal("1000.00"),
                "12345678909", "maria@email.com", endereco);

        ExtratoResponse extrato = ExtratoMapper.de(conta);

        assertEquals(0, new BigDecimal("5.00").compareTo(extrato.detalhes().rendimentoMensalEstimado()));
        assertEquals(2, extrato.detalhes().rendimentoMensalEstimado().scale());
        assertNull(extrato.detalhes().saquesRealizadosNoMes());
    }

    @Test
    void deveMontarExtratoDeInvestimentoComRendimentoETaxaDeAdministracao() {
        ContaBancaria conta = new ContaInvestimento("Maria Souza", new BigDecimal("1000.00"),
                "12345678909", "maria@email.com", endereco);

        ExtratoResponse extrato = ExtratoMapper.de(conta);

        assertEquals(0, new BigDecimal("12.00").compareTo(extrato.detalhes().rendimentoMensalEstimado()));
        assertEquals(0, new BigDecimal("20.00").compareTo(extrato.detalhes().taxaAdministracao()));
    }
}