package br.com.sistemabanco.infrastructure.persistence;

import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.domain.repository.ContaDAO;
import br.com.sistemabanco.infrastructure.persistence.repository.ContaJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContaDAOJpaTest {

    @Autowired
    private ContaDAO contaDAO;

    @Autowired
    private ContaJpaRepository contaRepository;

    @Test
    void deveSalvarEConsultarContaCorrente() throws Exception {
        Endereco endereco = new Endereco(
                "01001000",
                "Praça da Sé",
                "Sé",
                "São Paulo",
                "SP"
        );

        ContaBancaria conta = new ContaCorrente(
                "Cliente Teste",
                new BigDecimal("500.00"),
                "52998224725",
                "cliente.teste@example.com",
                endereco
        );

        contaDAO.salvar(conta);

        Optional<ContaBancaria> resultado =
                contaDAO.buscarPorCpf("52998224725");

        assertTrue(resultado.isPresent());

        ContaBancaria contaSalva = resultado.orElseThrow();

        assertEquals("Cliente Teste", contaSalva.getTitular());
        assertEquals(0, new BigDecimal("500.00")
                .compareTo(contaSalva.getSaldo()));
        assertEquals("52998224725", contaSalva.getCpf());
        assertEquals("cliente.teste@example.com", contaSalva.getEmail());
        assertNotNull(contaSalva.getEndereco());

        assertEquals("01001000", contaSalva.getEndereco().cep());
        assertEquals("São Paulo", contaSalva.getEndereco().cidade());
    }

    @Test
    void devePersistirDepositoEHistoricoDeTransacao() throws Exception {
        Endereco endereco = new Endereco(
                "01001000",
                "Praça da Sé",
                "Sé",
                "São Paulo",
                "SP"
        );

        ContaBancaria conta = new ContaCorrente(
                "Cliente Deposito",
                new BigDecimal("500.00"),
                "11144477735",
                "cliente.deposito@example.com",
                endereco
        );

        contaDAO.salvar(conta);

        BigDecimal saldoAnterior = conta.getSaldo();
        BigDecimal valorDeposito = new BigDecimal("150.00");

        conta.depositar(valorDeposito);

        br.com.sistemabanco.domain.model.Transacao transacao =
                new br.com.sistemabanco.domain.model.Transacao(
                        br.com.sistemabanco.domain.model.TipoTransacao.DEPOSITO,
                        valorDeposito
                );

        contaDAO.movimentarComTransacao(
                conta,
                transacao,
                saldoAnterior
        );

        ContaBancaria contaAtualizada =
                contaDAO.buscarPorCpf("11144477735").orElseThrow();

        assertEquals(
                0,
                new BigDecimal("650.00").compareTo(contaAtualizada.getSaldo())
        );

        assertFalse(contaAtualizada.getHistorico().isEmpty());

        assertEquals(
                br.com.sistemabanco.domain.model.TipoTransacao.DEPOSITO,
                contaAtualizada.getHistorico()
                        .get(contaAtualizada.getHistorico().size() - 1)
                        .getTipo()
        );

        assertEquals(
                0,
                valorDeposito.compareTo(
                        contaAtualizada.getHistorico()
                                .get(contaAtualizada.getHistorico().size() - 1)
                                .getValor()
                )
        );
    }

    @Test
    void devePersistirTransferenciaEntreDuasContas() throws Exception {
        Endereco endereco = new Endereco(
                "01001000",
                "Praça da Sé",
                "Sé",
                "São Paulo",
                "SP"
        );

        ContaBancaria origem = new ContaCorrente(
                "Cliente Origem",
                new BigDecimal("1000.00"),
                "11144477735",
                "origem@example.com",
                endereco
        );

        ContaBancaria destino = new ContaCorrente(
                "Cliente Destino",
                new BigDecimal("200.00"),
                "12345678909",
                "destino@example.com",
                endereco
        );

        contaDAO.salvar(origem);
        contaDAO.salvar(destino);

        BigDecimal saldoOrigemAnterior = origem.getSaldo();
        BigDecimal saldoDestinoAnterior = destino.getSaldo();
        BigDecimal valor = new BigDecimal("100.00");

        origem.debitarTransferencia(valor);
        destino.depositar(
                valor,
                br.com.sistemabanco.domain.model.TipoTransacao.TRANSFERENCIA_RECEBIDA
        );

        contaDAO.transferirComTransacao(
                origem,
                destino,
                valor,
                saldoOrigemAnterior,
                saldoDestinoAnterior
        );

        ContaBancaria origemAtualizada =
                contaDAO.buscarPorCpf("11144477735").orElseThrow();

        ContaBancaria destinoAtualizada =
                contaDAO.buscarPorCpf("12345678909").orElseThrow();

        assertEquals(
                0,
                new BigDecimal("900.00").compareTo(origemAtualizada.getSaldo())
        );

        assertEquals(
                0,
                new BigDecimal("300.00").compareTo(destinoAtualizada.getSaldo())
        );

        assertTrue(origemAtualizada.getHistorico().stream().anyMatch(
                t -> t.getTipo()
                        == br.com.sistemabanco.domain.model.TipoTransacao.TRANSFERENCIA_ENVIADA
                        && t.getValor().compareTo(valor) == 0
        ));

        assertTrue(destinoAtualizada.getHistorico().stream().anyMatch(
                t -> t.getTipo()
                        == br.com.sistemabanco.domain.model.TipoTransacao.TRANSFERENCIA_RECEBIDA
                        && t.getValor().compareTo(valor) == 0
        ));
    }

    @Test
    void deveRejeitarOperacaoQuandoSaldoAnteriorEstiverDesatualizado()
            throws Exception {

        Endereco endereco = new Endereco(
                "01001000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                "SP"
        );

        ContaBancaria conta = new ContaCorrente(
                "Cliente Concorrencia",
                new BigDecimal("1000.00"),
                "52998224725",
                "concorrencia@example.com",
                endereco
        );

        contaDAO.salvar(conta);

        // Simula o saldo que uma operação teria lido anteriormente.
        BigDecimal saldoAnteriorDesatualizado =
                new BigDecimal("900.00");

        BigDecimal valorDeposito = new BigDecimal("100.00");

        conta.depositar(valorDeposito);

        br.com.sistemabanco.domain.model.Transacao transacao =
                new br.com.sistemabanco.domain.model.Transacao(
                        br.com.sistemabanco.domain.model.TipoTransacao.DEPOSITO,
                        valorDeposito
                );

        assertThrows(
                br.com.sistemabanco.domain.exception.ConflitoConcorrenciaException.class,
                () -> contaDAO.movimentarComTransacao(
                        conta,
                        transacao,
                        saldoAnteriorDesatualizado
                )
        );

        ContaBancaria contaPersistida =
                contaDAO.buscarPorCpf("52998224725").orElseThrow();

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(contaPersistida.getSaldo())
        );
    }

    @Test
    void deveExcluirContaSemTransacoesESeuEndereco() throws Exception {
        Endereco endereco = new Endereco(
                "01001000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                "SP"
        );

        ContaBancaria conta = new ContaCorrente(
                "Cliente Exclusao",
                new BigDecimal("500.00"),
                "11144477735",
                "exclusao@example.com",
                endereco
        );

        contaDAO.salvar(conta);

        assertTrue(contaDAO.buscarPorCpf("11144477735").isPresent());

        contaDAO.deletar("11144477735");

        assertTrue(
                contaDAO.buscarPorCpf("11144477735").isEmpty(),
                "A conta deveria ter sido excluída"
        );
    }

    @Test
    void naoDeveExcluirContaQuePossuiTransacoes() throws Exception {
        Endereco endereco = new Endereco(
                "01001000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                "SP"
        );

        ContaBancaria conta = new ContaCorrente(
                "Cliente Com Historico",
                new BigDecimal("500.00"),
                "12345678909",
                "historico@example.com",
                endereco
        );

        contaDAO.salvar(conta);

        BigDecimal valorDeposito = new BigDecimal("100.00");

        conta.depositar(valorDeposito);

        br.com.sistemabanco.domain.model.Transacao transacao =
                new br.com.sistemabanco.domain.model.Transacao(
                        br.com.sistemabanco.domain.model.TipoTransacao.DEPOSITO,
                        valorDeposito
                );

        contaDAO.movimentarComTransacao(
                conta,
                transacao,
                new BigDecimal("500.00")
        );

        assertThrows(
                RuntimeException.class,
                () -> {
                    contaDAO.deletar("12345678909");
                    contaRepository.flush();
                }
        );

        assertTrue(
                contaDAO.buscarPorCpf("12345678909").isPresent(),
                "A conta deve continuar existindo"
        );

        ContaBancaria contaPersistida =
                contaDAO.buscarPorCpf("12345678909").orElseThrow();

        assertEquals(
                0,
                new BigDecimal("600.00")
                        .compareTo(contaPersistida.getSaldo())
        );

        assertFalse(
                contaPersistida.getHistorico().isEmpty(),
                "O histórico deve ser preservado"
        );
    }



}
