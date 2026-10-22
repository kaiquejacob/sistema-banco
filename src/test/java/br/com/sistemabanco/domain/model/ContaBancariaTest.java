package br.com.sistemabanco.domain.model;

import br.com.sistemabanco.domain.exception.SaldoInsuficienteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void deveRespeitarLimiteDiarioDaModalidade() {
        ContaCorrente conta = new ContaCorrente("Cliente Teste", new BigDecimal("3000.00"),
                "12345678909", "teste@email.com", endereco);

        conta.sacar(new BigDecimal("2000.00"));

        assertThrows(RuntimeException.class, () -> conta.sacar(new BigDecimal("1.00")));
    }

    @Test
    void transferenciaNaoDeveConsumirFranquiaDeSaque() {
        ContaCorrente conta = new ContaCorrente("Cliente Teste", new BigDecimal("1000.00"),
                "12345678909", "teste@email.com", endereco);

        conta.debitarTransferencia(new BigDecimal("100.00"));

        assertEquals(0, conta.getSaquesRealizados());
        assertEquals(0, new BigDecimal("900.00").compareTo(conta.getSaldo()));
    }

    @Test
    void deveRegistrarTaxaComoTransacaoSeparadaAposAFranquia() {
        ContaCorrente conta = contaComSaques("1000.00", 2);

        conta.sacar(new BigDecimal("100.00"));

        assertEquals(0, new BigDecimal("890.00").compareTo(conta.getSaldo()));

        List<Transacao> historico = conta.getHistorico();
        assertEquals(2, historico.size());
        assertEquals(TipoTransacao.SAQUE, historico.get(0).getTipo());
        assertEquals(0, new BigDecimal("100.00").compareTo(historico.get(0).getValor()));
        assertEquals(TipoTransacao.TAXA_SAQUE, historico.get(1).getTipo());
        assertEquals(0, new BigDecimal("10.00").compareTo(historico.get(1).getValor()));
    }

    @Test
    void naoDeveCobrarTaxaDentroDaFranquia() {
        ContaCorrente conta = contaComSaques("1000.00", 0);

        conta.sacar(new BigDecimal("100.00"));

        assertEquals(1, conta.getHistorico().size());
        assertEquals(0, new BigDecimal("900.00").compareTo(conta.getSaldo()));
    }

    @Test
    void naoDeveDebitarNadaQuandoSaldoNaoCobreSaqueMaisTaxa() {
        ContaCorrente conta = contaComSaques("105.00", 2);

        assertThrows(SaldoInsuficienteException.class,
                () -> conta.sacar(new BigDecimal("100.00")));

        assertEquals(0, new BigDecimal("105.00").compareTo(conta.getSaldo()));
        assertTrue(conta.getHistorico().isEmpty());
        assertEquals(2, conta.getSaquesRealizados());
    }

    @Test
    void taxaDeSaqueNaoDeveConsumirLimiteDiario() {
        ContaCorrente conta = contaComSaques("5000.00", 2);

        conta.sacar(new BigDecimal("1990.00"));
        assertEquals(0, new BigDecimal("1990.00").compareTo(conta.getValorSaquesHoje()));

        conta.sacar(new BigDecimal("10.00"));
        assertEquals(0, new BigDecimal("2000.00").compareTo(conta.getValorSaquesHoje()));
    }

    @Test
    void deveRestaurarSaquesDeHojeAoCarregarOHistorico() {
        ContaCorrente conta = new ContaCorrente("Cliente Teste", new BigDecimal("3000.00"),
                "12345678909", "teste@email.com", endereco);

        conta.adicionarTransacaoExistente(
                new Transacao(TipoTransacao.SAQUE, new BigDecimal("2000.00"), LocalDateTime.now()));

        assertEquals(0, new BigDecimal("2000.00").compareTo(conta.getValorSaquesHoje()));
        assertThrows(SaldoInsuficienteException.class,
                () -> conta.sacar(new BigDecimal("1.00")));
    }

    @Test
    void naoDeveContarSaquesDeOntemNemOutrosTipos() {
        ContaCorrente conta = new ContaCorrente("Cliente Teste", new BigDecimal("3000.00"),
                "12345678909", "teste@email.com", endereco);

        conta.adicionarTransacaoExistente(
                new Transacao(TipoTransacao.SAQUE, new BigDecimal("2000.00"), LocalDateTime.now().minusDays(1)));
        conta.adicionarTransacaoExistente(
                new Transacao(TipoTransacao.DEPOSITO, new BigDecimal("500.00"), LocalDateTime.now()));
        conta.adicionarTransacaoExistente(
                new Transacao(TipoTransacao.TRANSFERENCIA_ENVIADA, new BigDecimal("700.00"), LocalDateTime.now()));

        assertEquals(0, BigDecimal.ZERO.compareTo(conta.getValorSaquesHoje()));
        conta.sacar(new BigDecimal("2000.00"));
    }

    @Test
    void deveRegistrarAberturaQuandoHaSaldoInicial() {
        ContaCorrente conta = new ContaCorrente("Cliente Teste", new BigDecimal("250.00"),
                "12345678909", "teste@email.com", endereco);

        conta.registrarAbertura();

        assertEquals(1, conta.getHistorico().size());
        assertEquals(TipoTransacao.ABERTURA_CONTA, conta.getHistorico().get(0).getTipo());
        assertEquals(0, new BigDecimal("250.00").compareTo(conta.getHistorico().get(0).getValor()));
    }

    @Test
    void naoDeveRegistrarAberturaSemSaldoNemDuplicar() {
        ContaCorrente semSaldo = new ContaCorrente("Cliente Teste", BigDecimal.ZERO,
                "12345678909", "teste@email.com", endereco);
        semSaldo.registrarAbertura();
        assertTrue(semSaldo.getHistorico().isEmpty());

        ContaCorrente comSaldo = new ContaCorrente("Cliente Teste", new BigDecimal("250.00"),
                "12345678909", "teste@email.com", endereco);
        comSaldo.registrarAbertura();
        comSaldo.registrarAbertura();
        assertEquals(1, comSaldo.getHistorico().size());
    }

    private ContaCorrente contaComSaques(String saldo, int saquesRealizados) {
        return new ContaCorrente("Cliente Teste", new BigDecimal(saldo), "12345678909",
                "teste@email.com", LocalDate.now(), endereco, saquesRealizados, LocalDate.now());
    }

}