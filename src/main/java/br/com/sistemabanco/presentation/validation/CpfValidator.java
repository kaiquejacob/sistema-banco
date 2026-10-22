package br.com.sistemabanco.presentation.validation;

import br.com.sistemabanco.domain.util.Validador;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        return valor == null || Validador.validarCpf(valor);
    }
}