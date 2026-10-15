package br.com.sistemabanco.infrastructure.persistence;

import br.com.sistemabanco.domain.exception.CpfJaCadastradoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class PersistenceExceptionTranslator {

    public RuntimeException traduzir(
            DataIntegrityViolationException exception,
            String cpf
    ) {
        if (cpf != null) {
            return new CpfJaCadastradoException(
                    "Não foi possível persistir a conta com CPF: " + cpf
            );
        }

        return new IllegalStateException(
                "Não foi possível concluir a operação no banco de dados.",
                exception
        );
    }
}
