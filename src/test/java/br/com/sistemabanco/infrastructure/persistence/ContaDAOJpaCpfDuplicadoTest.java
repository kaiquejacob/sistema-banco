package br.com.sistemabanco.infrastructure.persistence;

import br.com.sistemabanco.domain.exception.CpfJaCadastradoException;
import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.Endereco;
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
class ContaDAOJpaCpfDuplicadoTest {

    private static final String CPF = "15350946056";

    @Autowired
    private ContaDAO contaDAO;

    @Test
    void criacoesSimultaneasDoMesmoCpfDevemTerUmaVencedoraEUmaCpfJaCadastrado() throws Exception {
        Endereco endereco = new Endereco("01001000", "Praca da Se", "Se", "Sao Paulo", "SP");

        int tentativas = 2;
        ExecutorService executor = Executors.newFixedThreadPool(tentativas);
        CountDownLatch prontos = new CountDownLatch(tentativas);
        CountDownLatch largada = new CountDownLatch(1);

        Callable<Throwable> tarefa = () -> {
            ContaBancaria conta = new ContaCorrente(
                    "Cliente Duplicado",
                    new BigDecimal("100.00"),
                    CPF,
                    "duplicado@example.com",
                    endereco
            );
            prontos.countDown();
            largada.await();
            try {
                contaDAO.salvar(conta);
                return null;
            } catch (RuntimeException e) {
                return e;
            }
        };

        int sucessos = 0;
        List<Throwable> falhas = new ArrayList<>();

        try {
            List<Future<Throwable>> resultados = new ArrayList<>();
            for (int i = 0; i < tentativas; i++) {
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

        assertEquals(1, sucessos, "Exatamente uma criação deve vencer");
        assertEquals(1, falhas.size());
        assertInstanceOf(
                CpfJaCadastradoException.class,
                falhas.get(0),
                "A perdedora deve receber CpfJaCadastradoException: " + falhas.get(0)
        );
        assertTrue(contaDAO.buscarPorCpf(CPF).isPresent());
    }
}