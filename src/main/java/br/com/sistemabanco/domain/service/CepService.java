package br.com.sistemabanco.domain.service;

import br.com.sistemabanco.domain.model.Endereco;

public interface CepService {
    Endereco buscarEnderecoPorCep(String cep);
}