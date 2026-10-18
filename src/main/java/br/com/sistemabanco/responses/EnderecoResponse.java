package br.com.sistemabanco.responses;

public record EnderecoResponse(String cep, String logradouro, String bairro, String cidade, String estado) {
}