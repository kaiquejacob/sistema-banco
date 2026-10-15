package br.com.sistemabanco.infrastructure.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ContaEntityTest {

    @Test
    void deveAssociarEnderecoAConta() {
        ContaEntity conta = new ContaEntity();
        EnderecoEntity endereco = new EnderecoEntity();

        conta.setEndereco(endereco);

        assertSame(endereco, conta.getEndereco());
    }
}
