package br.com.sistemabanco;

import br.com.sistemabanco.util.Validador;
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
}