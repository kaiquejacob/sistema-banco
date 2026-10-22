package br.com.sistemabanco.integration;

import br.com.sistemabanco.domain.exception.ServicoCepException;
import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.domain.repository.ContaDAO;
import br.com.sistemabanco.domain.repository.CredencialDAO;
import br.com.sistemabanco.domain.service.CepService;
import br.com.sistemabanco.infrastructure.security.Papel;
import br.com.sistemabanco.infrastructure.security.UsuarioEntity;
import br.com.sistemabanco.infrastructure.security.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class FluxoBancarioIntegracaoTest {

    private static final String CPF_DEPOSITO = "52998224725";
    private static final String CPF_SAQUE = "11144477735";
    private static final String CPF_TAXA = "12345678909";
    private static final String CPF_ORIGEM = "39053344705";
    private static final String CPF_DESTINO = "98765432100";
    private static final String CPF_SEM_HISTORICO = "15350946056";
    private static final String CPF_NOVO = "20000000027";

    private static final String SENHA = "SenhaTeste#1";
    private static final String SENHA_ADMIN = "SenhaAdmin#1";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private ContaDAO contaDAO;

    @Autowired
    private CredencialDAO credencialDAO;

    @Autowired
    private UsuarioJpaRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CepService cepService;

    private MockMvc mvc;

    @BeforeEach
    void prepararCenario() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .apply(springSecurity())
                .build();

        when(cepService.buscarEnderecoPorCep(any()))
                .thenReturn(new Endereco("01001000", "Praca da Se", "Se", "Sao Paulo", "SP"));

        criarConta(CPF_DEPOSITO, "Cliente Deposito");
        criarConta(CPF_SAQUE, "Cliente Saque");
        criarConta(CPF_TAXA, "Cliente Taxa");
        criarConta(CPF_ORIGEM, "Cliente Origem");
        criarConta(CPF_DESTINO, "Cliente Destino");
        criarConta(CPF_SEM_HISTORICO, "Cliente Sem Historico", BigDecimal.ZERO);

        usuarioRepository.save(
                new UsuarioEntity("admin", passwordEncoder.encode(SENHA_ADMIN), Papel.ADMIN, null)
        );
    }

    @Test
    void depositoDeveAumentarSaldoECriarTransacao() throws Exception {
        mvc.perform(post("/contas/me/depositos")
                        .with(httpBasic(CPF_DEPOSITO, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":250.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF_DEPOSITO))
                .andExpect(jsonPath("$.saldo").value(1250.00));
    }

    @Test
    void saqueDeveDiminuirSaldoECriarTransacao() throws Exception {
        mvc.perform(post("/contas/me/saques")
                        .with(httpBasic(CPF_SAQUE, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF_SAQUE))
                .andExpect(jsonPath("$.saldo").value(900.00));
    }

    @Test
    void saqueComSaldoInsuficienteDeveRetornar422() throws Exception {
        mvc.perform(post("/contas/me/saques")
                        .with(httpBasic(CPF_SAQUE, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":999999.00}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void taxaDeManutencaoDeveSerCobradaPeloAdmin() throws Exception {
        mvc.perform(post("/contas/admin/" + CPF_TAXA + "/taxa-manutencao")
                        .with(httpBasic("admin", SENHA_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF_TAXA));
    }

    @Test
    void transferenciaDeveDebitarOrigemECreditarDestino() throws Exception {
        mvc.perform(post("/transferencias")
                        .with(httpBasic(CPF_ORIGEM, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpfDestino\":\"" + CPF_DESTINO + "\",\"valor\":150.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF_ORIGEM))
                .andExpect(jsonPath("$.saldo").value(850.00));

        mvc.perform(get("/contas/me")
                        .with(httpBasic(CPF_DESTINO, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(1150.00));
    }

    @Test
    void transferenciaParaAPropriaContaDeveRetornar400() throws Exception {
        mvc.perform(post("/transferencias")
                        .with(httpBasic(CPF_ORIGEM, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpfDestino\":\"" + CPF_ORIGEM + "\",\"valor\":10.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferenciaComSaldoInsuficienteDeveRetornar422() throws Exception {
        mvc.perform(post("/transferencias")
                        .with(httpBasic(CPF_ORIGEM, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpfDestino\":\"" + CPF_DESTINO + "\",\"valor\":999999.00}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void transferenciaParaContaInexistenteDeveRetornar404() throws Exception {
        mvc.perform(post("/transferencias")
                        .with(httpBasic(CPF_ORIGEM, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpfDestino\":\"00000000191\",\"valor\":10.00}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void remocaoDeContaComHistoricoDeveRetornar409() throws Exception {
        mvc.perform(post("/contas/me/depositos")
                        .with(httpBasic(CPF_DEPOSITO, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":10.00}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/contas/admin/" + CPF_DEPOSITO)
                        .with(httpBasic("admin", SENHA_ADMIN)))
                .andExpect(status().isConflict());
    }

    @Test
    void remocaoDeContaSemHistoricoDeveRetornar204() throws Exception {
        mvc.perform(delete("/contas/admin/" + CPF_SEM_HISTORICO)
                        .with(httpBasic("admin", SENHA_ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void cadastroComCpfDuplicadoDeveRetornar409() throws Exception {
        mvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titular": "Duplicado",
                                  "cpf": "%s",
                                  "email": "duplicado@example.com",
                                  "cep": "01001000",
                                  "tipo": "CORRENTE",
                                  "saldoInicial": 100.00,
                                  "senha": "SenhaNova#1"
                                }
                                """.formatted(CPF_DEPOSITO)))
                .andExpect(status().isConflict());
    }

    @Test
    void cadastroComEmailInvalidoDeveRetornar400() throws Exception {
        mvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titular": "Cliente Invalido",
                                  "cpf": "%s",
                                  "email": "email-invalido",
                                  "cep": "01001000",
                                  "tipo": "CORRENTE",
                                  "saldoInicial": 100.00,
                                  "senha": "SenhaNova#1"
                                }
                                """.formatted(CPF_NOVO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadastroComViaCepIndisponivelDeveRetornar502() throws Exception {
        when(cepService.buscarEnderecoPorCep(any()))
                .thenThrow(new ServicoCepException("ViaCEP indisponível"));

        mvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titular": "Cliente Cep Falho",
                                  "cpf": "%s",
                                  "email": "cep.falho@example.com",
                                  "cep": "01001000",
                                  "tipo": "CORRENTE",
                                  "saldoInicial": 100.00,
                                  "senha": "SenhaNova#1"
                                }
                                """.formatted(CPF_NOVO)))
                .andExpect(status().isBadGateway());
    }

    private void criarConta(String cpf, String titular) {
        criarConta(cpf, titular, new BigDecimal("1000.00"));
    }

    private void criarConta(String cpf, String titular, BigDecimal saldo) {
        Endereco endereco = new Endereco("01001000", "Praca da Se", "Se", "Sao Paulo", "SP");

        contaDAO.salvar(
                new ContaCorrente(
                        titular,
                        saldo,
                        cpf,
                        titular.replace(" ", ".").toLowerCase(Locale.ROOT) + "@example.com",
                        endereco
                )
        );

        credencialDAO.criarCliente(cpf, SENHA);
    }
}