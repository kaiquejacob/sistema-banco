package br.com.sistemabanco.infrastructure.client;

import br.com.sistemabanco.domain.exception.ServicoCepException;
import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.domain.service.CepService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CepServiceImpl implements CepService {

    private static final int TAMANHO_MAXIMO_CACHE = 1000;

    private final RestClient restClient;
    private final Map<String, Endereco> cache = new ConcurrentHashMap<>();

    public CepServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Endereco buscarEnderecoPorCep(String cep) {
        String cepLimpo = normalizar(cep);

        Endereco emCache = cache.get(cepLimpo);
        if (emCache != null) {
            return emCache;
        }

        ViaCepResponse resposta;
        try {
            resposta = restClient.get()
                    .uri("/{cep}/json/", cepLimpo)
                    .retrieve()
                    .body(ViaCepResponse.class);
        } catch (RestClientException e) {
            throw new ServicoCepException("Falha ao consultar o serviço de CEP: " + e.getMessage(), e);
        }

        if (resposta == null || resposta.erro() != null) {
            throw new IllegalArgumentException("CEP não encontrado: " + cep);
        }

        Endereco endereco = new Endereco(
                cepLimpo,
                valorOuVazio(resposta.logradouro()),
                valorOuVazio(resposta.bairro()),
                valorOuVazio(resposta.localidade()),
                valorOuVazio(resposta.uf())
        );

        if (cache.size() < TAMANHO_MAXIMO_CACHE) {
            cache.put(cepLimpo, endereco);
        }
        return endereco;
    }

    private String normalizar(String cep) {
        if (cep == null) {
            throw new IllegalArgumentException("CEP não pode ser nulo.");
        }
        String cepLimpo = cep.replaceAll("\\D", "");
        if (cepLimpo.length() != 8) {
            throw new IllegalArgumentException("CEP deve ter exatamente 8 dígitos.");
        }
        return cepLimpo;
    }

    private String valorOuVazio(String valor) {
        return valor != null ? valor : "";
    }
}