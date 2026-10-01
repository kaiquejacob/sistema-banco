package br.com.sistemabanco;

import br.com.sistemabanco.exception.ContaComSaldoException;
import br.com.sistemabanco.exception.ContaNaoEncontradaException;
import br.com.sistemabanco.exception.SaldoInsuficienteException;
import br.com.sistemabanco.model.*;
import br.com.sistemabanco.repository.ContaDAO;
import br.com.sistemabanco.service.CepService;
import br.com.sistemabanco.service.ContaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock
    private ContaDAO contaDAO;

    @Mock
    private CepService cepService;

    @InjectMocks
    private ContaService contaService;

    private ContaBancaria contaOrigem;
    private ContaBancaria contaDestino;

    @BeforeEach
    void setUp() {
        Endereco endereco = new Endereco("01001-000", "Praça da Sé", "Lado ímpar", "Sé",  "SP");
        contaOrigem = new ContaCorrente("João Silva", new BigDecimal("1000.00"), "12345678909", "joao@email.com", endereco);
        contaDestino = new ContaCorrente("Maria Souza", new BigDecimal("500.00"), "98765432100", "maria@email.com", endereco);
    }

    @Test
    void deveTransferirComSucesso() {
        when(contaDAO.buscarPorCpf("12345678909")).thenReturn(Optional.of(contaOrigem));
        when(contaDAO.buscarPorCpf("98765432100")).thenReturn(Optional.of(contaDestino));

        contaService.transferir("12345678909", "98765432100", new BigDecimal("200.00"));

        assertEquals(0, new BigDecimal("800.00").compareTo(contaOrigem.getSaldo()));
        assertEquals(0, new BigDecimal("700.00").compareTo(contaDestino.getSaldo()));

        verify(contaDAO, times(1)).transferirComTransacao(
                any(ContaBancaria.class),
                any(ContaBancaria.class),
                any(BigDecimal.class)
        );
    }

    @Test
    void deveLancarExcecaoQuandoSaldoForInsuficienteNaTransferencia() {
        when(contaDAO.buscarPorCpf("12345678909")).thenReturn(Optional.of(contaOrigem));
        when(contaDAO.buscarPorCpf("98765432100")).thenReturn(Optional.of(contaDestino));

        assertThrows(SaldoInsuficienteException.class, () ->
                contaService.transferir("12345678909", "98765432100", new BigDecimal("2000.00"))
        );

        verify(contaDAO, never()).transferirComTransacao(any(), any(), any());
    }

    @Test
    void deveLancarExcecaoQuandoContaOrigemNaoExistir() {
        when(contaDAO.buscarPorCpf("12345678909")).thenReturn(Optional.empty());

        assertThrows(ContaNaoEncontradaException.class, () ->
                contaService.transferir("12345678909", "98765432100", new BigDecimal("100.00"))
        );
    }

    @Test
    void deveCriarContaComSucessoBuscandoEndereco() {
        Endereco endereco = new Endereco("01001-000", "Praça da Sé", "Lado ímpar", "Sé",  "SP");
        when(cepService.buscarEnderecoPorCep("01001-000")).thenReturn(endereco);

        contaService.criarConta(TipoConta.CORRENTE, "Carlos", new BigDecimal("100.00"), "11144477735", "carlos@email.com", "01001-000");

        verify(cepService, times(1)).buscarEnderecoPorCep("01001-000");
        verify(contaDAO, times(1)).salvar(any(ContaBancaria.class));
    }

    @Test
    void deveDepositarComSucesso() {
        when(contaDAO.buscarPorCpf("12345678909")).thenReturn(Optional.of(contaOrigem));

        contaService.depositar("12345678909", new BigDecimal("300.00"));

        assertEquals(0, new BigDecimal("1300.00").compareTo(contaOrigem.getSaldo()));
        verify(contaDAO, times(1)).atualizarSaldoESaques(contaOrigem);
        verify(contaDAO, times(1)).registrarTransacao(eq("12345678909"), any());
    }

    @Test
    void deveSacarComSucesso() {
        when(contaDAO.buscarPorCpf("12345678909")).thenReturn(Optional.of(contaOrigem));

        contaService.sacar("12345678909", new BigDecimal("200.00"));

        assertEquals(0, new BigDecimal("800.00").compareTo(contaOrigem.getSaldo()));
        verify(contaDAO, times(1)).atualizarSaldoESaques(contaOrigem);
        verify(contaDAO, times(1)).registrarTransacao(eq("12345678909"), any());
    }

    @Test
    void deveLancarExcecaoQuandoContaDestinoNaoExistirNaTransferencia() {
        when(contaDAO.buscarPorCpf("12345678909")).thenReturn(Optional.of(contaOrigem));
        when(contaDAO.buscarPorCpf("12312312387")).thenReturn(Optional.empty());

        assertThrows(ContaNaoEncontradaException.class, () ->
                contaService.transferir("12345678909", "12312312387", new BigDecimal("100.00"))
        );

        verify(contaDAO, never()).transferirComTransacao(any(), any(), any());
    }

    @Test
    void deveLancarExcecaoAoRemoverContaComSaldoPositivo() {
        when(contaDAO.buscarPorCpf("12345678909")).thenReturn(Optional.of(contaOrigem));

        assertThrows(ContaComSaldoException.class, () ->
                contaService.removerConta("12345678909")
        );

        verify(contaDAO, never()).deletar(anyString());
    }

    @Test
    void deveListarContasOrdenadasPorSaldoDecrescente() {
        when(contaDAO.buscarTodas()).thenReturn(List.of(contaDestino, contaOrigem));

        List<ContaBancaria> resultado = contaService.listarTodasOrdenadasPorSaldo();

        assertEquals(2, resultado.size());
        assertEquals("12345678909", resultado.get(0).getCpf());
        assertEquals("98765432100", resultado.get(1).getCpf());
    }
}