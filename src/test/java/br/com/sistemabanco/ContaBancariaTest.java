package br.com.sistemabanco;

import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.Endereco;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContaBancariaTest {

    private Endereco endereco;

    @BeforeEach
    void setUp() {
        endereco = new Endereco("01001-000", "Rua Teste", "Apto 1", "Centro", "SP");
    }

    @Test
    void deveIncrementarSaldoAoDepositar() throws Exception {
        ContaCorrente conta = new ContaCorrente("Cliente Teste", new BigDecimal("100.00"), "12345678909", "teste@email.com", endereco);
        conta.depositar(new BigDecimal("50.00"));

        assertEquals(0, new BigDecimal("150.00").compareTo(conta.getSaldo()));
    }

    @Test
    void deveDecrementarSaldoAoSacar() throws Exception {
        ContaCorrente conta = new ContaCorrente("Cliente Teste", new BigDecimal("100.00"), "12345678909", "teste@email.com", endereco);
        conta.sacar(new BigDecimal("40.00"));

        assertEquals(0, new BigDecimal("60.00").compareTo(conta.getSaldo()));
    }
}