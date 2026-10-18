package br.com.sistemabanco.validation;

import br.com.sistemabanco.util.Validador;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        return valor == null || Validador.validarCpf(valor);
    }
}