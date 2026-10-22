package br.com.sistemabanco.presentation.responses;

public record EnderecoResponse(String cep, String logradouro, String bairro, String cidade, String estado) {
}