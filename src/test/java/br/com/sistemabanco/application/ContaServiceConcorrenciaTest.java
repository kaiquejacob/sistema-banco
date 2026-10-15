package br.com.sistemabanco.application;

import br.com.sistemabanco.domain.exception.ConflitoConcorrenciaException;
import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.domain.model.TipoTransacao;
import br.com.sistemabanco.domain.repository.ContaDAO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ContaServiceConcorrenciaTest {

    private static final int TRANSFERENCIAS = 8;
    private static final String CPF_ORIGEM = "39053344705";
    private static final String CPF_DESTINO = "71428793860";

    @Autowired
    private ContaService contaService;

    @Autowired
    private ContaDAO contaDAO;

    @Test
    void naoDevePerderAtualizacoesEmTransferenciasSimultaneas() throws Exception {
        Endereco endereco = new Endereco(
                "01001000",
                "Praca da Se",
                "Se",
                "Sao Paulo",
                "SP"
        );

        contaDAO.salvar(new ContaCorrente(
                "Concorrencia Origem",
                new BigDecimal("10000.00"),
                CPF_ORIGEM,
                "concorrencia.origem@example.com",
                endereco
        ));

        contaDAO.salvar(new ContaCorrente(
                "Concorrencia Destino",
                new BigDecimal("200.00"),
                CPF_DESTINO,
                "concorrencia.destino@example.com",
                endereco
        ));

        BigDecimal valor = new BigDecimal("100.00");

        ExecutorService executor = Executors.newFixedThreadPool(TRANSFERENCIAS);
        CountDownLatch prontos = new CountDownLatch(TRANSFERENCIAS);
        CountDownLatch largada = new CountDownLatch(1);


        Callable<Throwable> tarefa = () -> {
            prontos.countDown();
            largada.await();
            try {
                contaService.transferir(CPF_ORIGEM, CPF_DESTINO, valor);
                return null;
            } catch (RuntimeException e) {
                return e;
            }
        };

        int sucessos = 0;
        List<Throwable> falhas = new ArrayList<>();

        try {
            List<Future<Throwable>> resultados = new ArrayList<>();
            for (int i = 0; i < TRANSFERENCIAS; i++) {
                resultados.add(executor.submit(tarefa));
            }

            prontos.await();
            largada.countDown();

            for (Future<Throwable> resultado : resultados) {
                Throwable erro = resultado.get(30, TimeUnit.SECONDS);
                if (erro == null) {
                    sucessos++;
                } else {
                    falhas.add(erro);
                }
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(TRANSFERENCIAS, sucessos + falhas.size());
        assertTrue(sucessos >= 1, "Pelo menos uma transferência deveria ter sido concluída");

        for (Throwable falha : falhas) {
            assertInstanceOf(
                    ConflitoConcorrenciaException.class,
                    falha,
                    "Falha inesperada (deveria ser conflito de concorrência): " + falha
            );
        }

        ContaBancaria origem = contaService.buscarConta(CPF_ORIGEM);
        ContaBancaria destino = contaService.buscarConta(CPF_DESTINO);
        BigDecimal movimentado = valor.multiply(BigDecimal.valueOf(sucessos));

        assertEquals(
                0,
                new BigDecimal("10000.00").subtract(movimentado).compareTo(origem.getSaldo()),
                "Saldo da origem inconsistente: " + origem.getSaldo() + " com " + sucessos + " sucessos"
        );
        assertEquals(
                0,
                new BigDecimal("200.00").add(movimentado).compareTo(destino.getSaldo()),
                "Saldo do destino inconsistente: " + destino.getSaldo() + " com " + sucessos + " sucessos"
        );

        long enviadas = origem.getHistorico().stream()
                .filter(t -> t.getTipo() == TipoTransacao.TRANSFERENCIA_ENVIADA)
                .count();
        long recebidas = destino.getHistorico().stream()
                .filter(t -> t.getTipo() == TipoTransacao.TRANSFERENCIA_RECEBIDA)
                .count();

        assertEquals(sucessos, enviadas, "Cada sucesso deve gerar exatamente um lançamento de envio");
        assertEquals(sucessos, recebidas, "Cada sucesso deve gerar exatamente um lançamento de recebimento");
    }

}