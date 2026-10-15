package br.com.sistemabanco.infrastructure.persistence;

import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.domain.model.TipoTransacao;
import br.com.sistemabanco.domain.repository.ContaDAO;
import br.com.sistemabanco.infrastructure.persistence.entity.ContaEntity;
import br.com.sistemabanco.infrastructure.persistence.repository.ContaJpaRepository;
import br.com.sistemabanco.infrastructure.persistence.repository.TransacaoJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@SpringBootTest
@ActiveProfiles("test")
class ContaDAOJpaRollbackTest {

    @Autowired
    private ContaDAO contaDAO;

    @Autowired
    private ContaJpaRepository contaRepository;

    @MockitoBean
    private TransacaoJpaRepository transacaoRepository;

    @Test
    void deveReverterAlteracoesDeSaldoQuandoFalharAoSalvarSegundaTransacao() {
        ContaBancaria origem = novaContaCorrente(
                "Conta Origem", "1000.00", "11144477735", "origem.rollback@example.com");
        ContaBancaria destino = novaContaCorrente(
                "Conta Destino", "200.00", "12345678909", "destino.rollback@example.com");

        contaDAO.salvar(origem);
        contaDAO.salvar(destino);

        AtomicInteger chamadas = new AtomicInteger();
        doAnswer(invocation -> {
            if (chamadas.incrementAndGet() == 1) {
                contaRepository.flush();
                return invocation.getArgument(0);
            }
            throw new RuntimeException("Falha simulada no segundo salvamento");
        }).when(transacaoRepository).save(any());

        BigDecimal valor = new BigDecimal("100.00");

        origem.debitarTransferencia(valor);
        destino.depositar(valor, TipoTransacao.TRANSFERENCIA_RECEBIDA);

        RuntimeException excecao = assertThrows(
                RuntimeException.class,
                () -> contaDAO.transferirComTransacao(
                        origem,
                        destino,
                                valor
                )
        );

        assertEquals("Falha simulada no segundo salvamento", excecao.getMessage());

        verify(transacaoRepository, times(2)).save(any());

        assertSaldo("1000.00", contaRepository.findById(origem.getCpf()).orElseThrow());
        assertSaldo("200.00", contaRepository.findById(destino.getCpf()).orElseThrow());
    }

    @Test
    void deveConfirmarAlteracoesDeSaldoQuandoNaoHaFalha() {
        ContaBancaria origem = novaContaCorrente(
                "Controle Origem", "1000.00", "39053344705", "controle.origem@example.com");
        ContaBancaria destino = novaContaCorrente(
                "Controle Destino", "200.00", "16899535009", "controle.destino@example.com");

        contaDAO.salvar(origem);
        contaDAO.salvar(destino);

        BigDecimal valor = new BigDecimal("100.00");

        origem.debitarTransferencia(valor);
        destino.depositar(valor, TipoTransacao.TRANSFERENCIA_RECEBIDA);

        contaDAO.transferirComTransacao(
                origem,
                destino,
                valor
        );

        verify(transacaoRepository, times(2)).save(any());

        assertSaldo("900.00", contaRepository.findById(origem.getCpf()).orElseThrow());
        assertSaldo("300.00", contaRepository.findById(destino.getCpf()).orElseThrow());
    }

    private ContaBancaria novaContaCorrente(String titular, String saldo, String cpf, String email) {
        Endereco endereco = new Endereco(
                "01001000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                "SP"
        );

        return new ContaCorrente(titular, new BigDecimal(saldo), cpf, email, endereco);
    }

    private void assertSaldo(String esperado, ContaEntity entity) {
        assertEquals(
                0,
                new BigDecimal(esperado).compareTo(entity.getSaldo()),
                "Saldo esperado " + esperado + " mas foi " + entity.getSaldo()
        );
    }

}