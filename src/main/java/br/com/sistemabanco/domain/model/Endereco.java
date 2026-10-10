package br.com.sistemabanco.domain.model;

public record Endereco(String cep, String logradouro, String bairro, String cidade, String estado) {

    @Override
    public String toString() {
        return logradouro + ", " + bairro + " - " + cidade + "/" + estado + " (CEP: " + cep + ")";
    }
}