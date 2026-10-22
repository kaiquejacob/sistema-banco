package br.com.sistemabanco.domain.validation;

import br.com.sistemabanco.domain.util.Validador;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidadorTest {

    @Test
    void deveRetornarTrueParaCpfValido() {
        String cpfValido = "12345678909";
        boolean resultado = Validador.validarCpf(cpfValido);

        assertTrue(resultado, "O CPF deveria ser considerado válido");
    }

    @Test
    void deveRetornarFalseParaCpfInvalido() {
        String cpfInvalido = "11111111111";
        boolean resultado = Validador.validarCpf(cpfInvalido);

        assertFalse(resultado, "CPF com dígitos repetidos deveria ser inválido");
    }

    @Test
    void deveAceitarEmailsComDominioDeVariosNiveis() {
        assertTrue(Validador.validarEmail("joao@email.com"));
        assertTrue(Validador.validarEmail("joao@empresa.com.br"));
        assertTrue(Validador.validarEmail("joao.silva+banco@sub.empresa.com.br"));
    }

    @Test
    void deveRejeitarEmailsInvalidos() {
        assertFalse(Validador.validarEmail(null));
        assertFalse(Validador.validarEmail(""));
        assertFalse(Validador.validarEmail("joao"));
        assertFalse(Validador.validarEmail("joao@"));
        assertFalse(Validador.validarEmail("@email.com"));
        assertFalse(Validador.validarEmail("joao@email"));
        assertFalse(Validador.validarEmail("joao@email.c"));
        assertFalse(Validador.validarEmail("joao silva@email.com"));
        assertFalse(Validador.validarEmail("a".repeat(95) + "@x.com"));
    }

    @Test
    void deveAceitarNomesComAcentoApostrofoHifenEPonto() {
        assertTrue(Validador.validarNome("João da Silva"));
        assertTrue(Validador.validarNome("Maria-Clara Souza"));
        assertTrue(Validador.validarNome("Ana D'Ávila"));
        assertTrue(Validador.validarNome("Carlos Pereira Jr."));
    }

    @Test
    void deveRejeitarNomesInvalidos() {
        assertFalse(Validador.validarNome(null));
        assertFalse(Validador.validarNome(""));
        assertFalse(Validador.validarNome("   "));
        assertFalse(Validador.validarNome("João123"));
        assertFalse(Validador.validarNome("-João"));
        assertFalse(Validador.validarNome("João@Silva"));
        assertFalse(Validador.validarNome("a".repeat(101)));
    }

}