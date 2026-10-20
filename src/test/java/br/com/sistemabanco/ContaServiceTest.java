package br.com.sistemabanco;

import br.com.sistemabanco.domain.exception.ContaComSaldoException;
import br.com.sistemabanco.domain.exception.ContaNaoEncontradaException;
import br.com.sistemabanco.domain.exception.SaldoInsuficienteException;
import br.com.sistemabanco.domain.model.*;
import br.com.sistemabanco.domain.repository.ContaDAO;
import br.com.sistemabanco.domain.service.CepService;
import br.com.sistemabanco.application.ContaService;
import br.com.sistemabanco.application.RegistroDeConta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

    @Mock
    private RegistroDeConta registroDeConta;

    @InjectMocks
    private ContaService contaService;

    @Captor
    private ArgumentCaptor<List<Transacao>> transacoesCaptor;

    @Captor
    private ArgumentCaptor<ContaBancaria> contaCaptor;

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

        verify(contaDAO, times(1)).transferirComTransacao(any(), any(), any());
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

        contaService.criarConta(TipoConta.CORRENTE, "Carlos", new BigDecimal("100.00"), "11144477735", "carlos@email.com", "01001-000", "Senha#1234");

        verify(cepService, times(1)).buscarEnderecoPorCep("01001-000");
        verify(registroDeConta, times(1)).registrar(any(ContaBancaria.class), eq("Senha#1234"));
    }

    @Test
    void deveDepositarComSucesso() {
        when(contaDAO.buscarPorCpf("12345678909")).thenReturn(Optional.of(contaOrigem));

        contaService.depositar("12345678909", new BigDecimal("300.00"));

        assertEquals(0, new BigDecimal("1300.00").compareTo(contaOrigem.getSaldo()));
        verify(contaDAO, times(1)).movimentarComTransacao(eq(contaOrigem), any());
    }

    @Test
    void deveSacarComSucesso() {
        when(contaDAO.buscarPorCpf("12345678909")).thenReturn(Optional.of(contaOrigem));

        contaService.sacar("12345678909", new BigDecimal("200.00"));

        assertEquals(0, new BigDecimal("800.00").compareTo(contaOrigem.getSaldo()));
        verify(contaDAO, times(1)).movimentarComTransacao(eq(contaOrigem), any());
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
    void deveRegistrarSaqueETaxaComoTransacoesSeparadasAposAFranquia() {
        when(contaDAO.buscarPorCpf("12345678909")).thenReturn(Optional.of(contaOrigem));

        contaService.sacar("12345678909", new BigDecimal("100.00"));
        contaService.sacar("12345678909", new BigDecimal("100.00"));
        contaService.sacar("12345678909", new BigDecimal("100.00"));

        verify(contaDAO, times(3)).movimentarComTransacao(eq(contaOrigem), transacoesCaptor.capture());

        List<Transacao> primeira = transacoesCaptor.getAllValues().get(0);
        assertEquals(1, primeira.size());
        assertEquals(TipoTransacao.SAQUE, primeira.get(0).getTipo());

        List<Transacao> terceira = transacoesCaptor.getAllValues().get(2);
        assertEquals(2, terceira.size());
        assertEquals(TipoTransacao.SAQUE, terceira.get(0).getTipo());
        assertEquals(0, new BigDecimal("100.00").compareTo(terceira.get(0).getValor()));
        assertEquals(TipoTransacao.TAXA_SAQUE, terceira.get(1).getTipo());
        assertEquals(0, new BigDecimal("10.00").compareTo(terceira.get(1).getValor()));

        // 1000 - 3 saques de 100 - taxa de 10 = 690: o extrato bate com o saldo.
        assertEquals(0, new BigDecimal("690.00").compareTo(contaOrigem.getSaldo()));
    }

    @Test
    void deveEnviarAoDaoApenasAsTransacoesNovasDaOperacao() {
        contaOrigem.depositar(new BigDecimal("50.00"));
        when(contaDAO.buscarPorCpf("12345678909")).thenReturn(Optional.of(contaOrigem));

        contaService.depositar("12345678909", new BigDecimal("300.00"));

        verify(contaDAO).movimentarComTransacao(eq(contaOrigem), transacoesCaptor.capture());
        List<Transacao> enviadas = transacoesCaptor.getValue();
        assertEquals(1, enviadas.size());
        assertEquals(TipoTransacao.DEPOSITO, enviadas.get(0).getTipo());
        assertEquals(0, new BigDecimal("300.00").compareTo(enviadas.get(0).getValor()));
    }

    @Test
    void deveRegistrarAberturaDeContaAoCriarComSaldoInicial() {
        Endereco endereco = new Endereco("01001-000", "Praça da Sé", "Lado ímpar", "Sé", "SP");
        when(cepService.buscarEnderecoPorCep("01001-000")).thenReturn(endereco);

        contaService.criarConta(TipoConta.CORRENTE, "Carlos", new BigDecimal("100.00"),
                "11144477735", "carlos@email.com", "01001-000", "Senha#1234");

        verify(registroDeConta).registrar(contaCaptor.capture(), eq("Senha#1234"));
        ContaBancaria criada = contaCaptor.getValue();
        assertEquals(1, criada.getHistorico().size());
        assertEquals(TipoTransacao.ABERTURA_CONTA, criada.getHistorico().get(0).getTipo());
        assertEquals(0, new BigDecimal("100.00").compareTo(criada.getHistorico().get(0).getValor()));
    }

    @Test
    void devePaginarResumosDeContaPorSaldo() {
        List<ResumoConta> resumos = List.of(
                new ResumoConta("12345678909", "João Silva", TipoConta.CORRENTE, new BigDecimal("1000.00")),
                new ResumoConta("98765432100", "Maria Souza", TipoConta.CORRENTE, new BigDecimal("500.00")));
        when(contaDAO.listarResumosPorSaldo(0, 10)).thenReturn(resumos);
        when(contaDAO.contarContas()).thenReturn(25L);

        PaginaDeContas pagina = contaService.listarPorSaldo(0, 10);

        assertEquals(resumos, pagina.itens());
        assertEquals(25L, pagina.total());
        assertEquals(0, pagina.pagina());
        assertEquals(10, pagina.tamanho());
        assertEquals(3, pagina.totalDePaginas());
    }

    @Test
    void deveRejeitarParametrosDePaginacaoInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> contaService.listarPorSaldo(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> contaService.listarPorSaldo(0, 0));
        assertThrows(IllegalArgumentException.class, () -> contaService.listarPorSaldo(0, 101));

        verify(contaDAO, never()).listarResumosPorSaldo(anyInt(), anyInt());
    }
}