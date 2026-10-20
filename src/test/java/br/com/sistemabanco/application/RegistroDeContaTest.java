package br.com.sistemabanco.application;

import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.domain.repository.ContaDAO;
import br.com.sistemabanco.domain.repository.CredencialDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class RegistroDeContaTest {

    @Mock
    private ContaDAO contaDAO;

    @Mock
    private CredencialDAO credencialDAO;

    @Test
    void deveSalvarAContaEDepoisCriarACredencialDoCliente() {
        Endereco endereco = new Endereco("01001000", "Praca da Se", "Se", "Sao Paulo", "SP");
        ContaBancaria conta = new ContaCorrente("Carlos", new BigDecimal("100.00"),
                "11144477735", "carlos@email.com", endereco);

        new RegistroDeConta(contaDAO, credencialDAO).registrar(conta, "Senha#1234");

        InOrder ordem = inOrder(contaDAO, credencialDAO);
        ordem.verify(contaDAO).salvar(conta);
        ordem.verify(credencialDAO).criarCliente("11144477735", "Senha#1234");
    }
}