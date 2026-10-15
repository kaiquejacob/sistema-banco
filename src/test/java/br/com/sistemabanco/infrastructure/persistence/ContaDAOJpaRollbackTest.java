
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    void deveReverterTransferenciaQuandoFalharAoSalvarSegundaTransacao()
            throws Exception {

        Endereco endereco = new Endereco(
                "01001000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                "SP"
        );

        ContaBancaria origem = new ContaCorrente(
                "Conta Origem",
                new BigDecimal("1000.00"),
                "11144477735",
                "origem.rollback@example.com",
                endereco
        );

        ContaBancaria destino = new ContaCorrente(
                "Conta Destino",
                new BigDecimal("200.00"),
                "12345678909",
                "destino.rollback@example.com",
                endereco
        );

        contaDAO.salvar(origem);
        contaDAO.salvar(destino);

        doAnswer(invocation -> invocation.callRealMethod())
                .when(transacaoRepository)
                .save(any());

        doAnswer(invocation -> {
            throw new RuntimeException("Falha simulada no segundo salvamento");
        }).when(transacaoRepository).save(any());

        BigDecimal saldoOrigemAnterior = origem.getSaldo();
        BigDecimal saldoDestinoAnterior = destino.getSaldo();
        BigDecimal valor = new BigDecimal("100.00");

        origem.debitarTransferencia(valor);
        destino.depositar(
                valor,
                TipoTransacao.TRANSFERENCIA_RECEBIDA
        );

        RuntimeException excecao = assertThrows(
                RuntimeException.class,
                () -> contaDAO.transferirComTransacao(
                        origem,
                        destino,
                        valor,
                        saldoOrigemAnterior,
                        saldoDestinoAnterior
                )
        );

        assertEquals(
                "Falha simulada no segundo salvamento",
                excecao.getMessage()
        );

        ContaEntity origemPersistida =
                contaRepository.findById(origem.getCpf()).orElseThrow();

        ContaEntity destinoPersistida =
                contaRepository.findById(destino.getCpf()).orElseThrow();

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(origemPersistida.getSaldo())
        );

        assertEquals(
                0,
                new BigDecimal("200.00")
                        .compareTo(destinoPersistida.getSaldo())
        );
    }
}
