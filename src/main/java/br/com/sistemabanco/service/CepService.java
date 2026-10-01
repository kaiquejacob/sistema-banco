package br.com.sistemabanco.service;

import br.com.sistemabanco.model.Endereco;

public interface CepService {
    Endereco buscarEnderecoPorCep(String cep);
}