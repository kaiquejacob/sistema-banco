package br.com.sistemabanco.service.impl;

import br.com.sistemabanco.exception.ServicoCepException;
import br.com.sistemabanco.model.Endereco;
import br.com.sistemabanco.service.CepService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CepServiceImpl implements CepService {

    private static final String VIA_CEP_URL = "https://viacep.com.br/ws/%s/json/";

    @Override
    public Endereco buscarEnderecoPorCep(String cep) {
        String cepLimpo = cep.replaceAll("\\D", "");
        if (cepLimpo.length() != 8) {
            throw new IllegalArgumentException("CEP deve ter exatamente 8 dígitos.");
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(VIA_CEP_URL, cepLimpo)))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 || response.body().contains("\"erro\":")) {
                throw new IllegalArgumentException("CEP não encontrado ou inválido: " + cep);
            }

            String json = response.body();
            String logradouro = extrairCampo(json, "logradouro");
            String bairro = extrairCampo(json, "bairro");
            String cidade = extrairCampo(json, "localidade");
            String estado = extrairCampo(json, "uf");

            return new Endereco(cepLimpo, logradouro, bairro, cidade, estado);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new ServicoCepException("Falha ao consultar serviço de CEP no ViaCEP: " + e.getMessage(), e);
        }
    }

    private String extrairCampo(String json, String campo) {
        Pattern pattern = Pattern.compile("\"" + campo + "\":\\s*\"(.*?)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}