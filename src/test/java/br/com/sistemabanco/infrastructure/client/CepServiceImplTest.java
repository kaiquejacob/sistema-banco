package br.com.sistemabanco.infrastructure.client;

import br.com.sistemabanco.domain.exception.ServicoCepException;
import br.com.sistemabanco.domain.model.Endereco;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CepServiceImplTest {

    private static final String URL_BASE = "https://viacep.test/ws";
    private static final String URL_CEP = URL_BASE + "/01001000/json/";

    private static final String RESPOSTA_OK = """
            {"cep":"01001-000","logradouro":"Praça da Sé","complemento":"lado ímpar",
             "bairro":"Sé","localidade":"São Paulo","uf":"SP","ibge":"3550308","ddd":"11"}
            """;

    private MockRestServiceServer server;
    private CepServiceImpl service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(URL_BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        service = new CepServiceImpl(builder.build());
    }

    @Test
    void deveBuscarOEnderecoIgnorandoCamposDesconhecidos() {
        server.expect(requestTo(URL_CEP))
                .andRespond(withSuccess(RESPOSTA_OK, MediaType.APPLICATION_JSON));

        Endereco endereco = service.buscarEnderecoPorCep("01001-000");

        assertEquals("01001000", endereco.cep());
        assertEquals("Praça da Sé", endereco.logradouro());
        assertEquals("Sé", endereco.bairro());
        assertEquals("São Paulo", endereco.cidade());
        assertEquals("SP", endereco.estado());
        server.verify();
    }

    @Test
    void deveRejeitarCepInexistenteComErroEmTexto() {
        server.expect(requestTo(URL_CEP))
                .andRespond(withSuccess("{\"erro\": \"true\"}", MediaType.APPLICATION_JSON));

        assertThrows(IllegalArgumentException.class,
                () -> service.buscarEnderecoPorCep("01001000"));
    }

    @Test
    void deveRejeitarCepInexistenteComErroBooleano() {
        server.expect(requestTo(URL_CEP))
                .andRespond(withSuccess("{\"erro\": true}", MediaType.APPLICATION_JSON));

        assertThrows(IllegalArgumentException.class,
                () -> service.buscarEnderecoPorCep("01001000"));
    }

    @Test
    void deveConverterFalhaDoServicoEmServicoCepException() {
        server.expect(requestTo(URL_CEP))
                .andRespond(withServerError());

        assertThrows(ServicoCepException.class,
                () -> service.buscarEnderecoPorCep("01001000"));
    }

    @Test
    void deveRejeitarCepForaDoFormatoSemChamarOServico() {
        assertThrows(IllegalArgumentException.class, () -> service.buscarEnderecoPorCep("123"));
        assertThrows(IllegalArgumentException.class, () -> service.buscarEnderecoPorCep(null));

        server.verify();
    }

    @Test
    void deveUsarOCacheNaSegundaConsultaIndependenteDaFormatacao() {
        server.expect(requestTo(URL_CEP))
                .andRespond(withSuccess(RESPOSTA_OK, MediaType.APPLICATION_JSON));

        Endereco primeira = service.buscarEnderecoPorCep("01001-000");
        Endereco segunda = service.buscarEnderecoPorCep("01001000");

        assertEquals(primeira, segunda);
        server.verify();
    }

    @Test
    void naoDeveGuardarFalhasNoCache() {
        server.expect(requestTo(URL_CEP))
                .andRespond(withServerError());
        server.expect(requestTo(URL_CEP))
                .andRespond(withSuccess(RESPOSTA_OK, MediaType.APPLICATION_JSON));

        assertThrows(ServicoCepException.class,
                () -> service.buscarEnderecoPorCep("01001000"));

        Endereco endereco = service.buscarEnderecoPorCep("01001000");

        assertEquals("Praça da Sé", endereco.logradouro());
        server.verify();
    }
}