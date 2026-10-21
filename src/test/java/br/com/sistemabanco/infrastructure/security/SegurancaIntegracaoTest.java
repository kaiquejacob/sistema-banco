package br.com.sistemabanco.infrastructure.security;

import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.domain.repository.ContaDAO;
import br.com.sistemabanco.domain.repository.CredencialDAO;
import br.com.sistemabanco.domain.service.CepService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SegurancaIntegracaoTest {

    private static final String CPF_A = "52998224725";   // só leitura
    private static final String CPF_B = "11144477735";   // só leitura e destino de transferência
    private static final String CPF_C = "12345678909";   // recebe um depósito
    private static final String CPF_D = "39053344705";   // origem de uma transferência
    private static final String CPF_NOVO = "15350946056"; // criado pelo cadastro público

    private static final String SENHA_CLIENTE = "SenhaCliente#1";
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

    @BeforeAll
    void prepararDados() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .apply(springSecurity())
                .build();

        criarCliente(CPF_A, "Cliente A", "a@example.com");
        criarCliente(CPF_B, "Cliente B", "b@example.com");
        criarCliente(CPF_C, "Cliente C", "c@example.com");
        criarCliente(CPF_D, "Cliente D", "d@example.com");

        usuarioRepository.save(
                new UsuarioEntity("admin", passwordEncoder.encode(SENHA_ADMIN), Papel.ADMIN, null));
    }

    // ---- autenticação ----

    @Test
    void semCredencialDeveRetornar401() throws Exception {
        mvc.perform(get("/contas/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void comSenhaErradaDeveRetornar401() throws Exception {
        mvc.perform(get("/contas/me").with(httpBasic(CPF_A, "senha-errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void comUsuarioInexistenteDeveRetornarOMesmo401() throws Exception {
        // Mesma resposta de "senha errada": quem tenta não descobre quais CPFs têm conta.
        mvc.perform(get("/contas/me").with(httpBasic("00000000191", "qualquer-senha")))
                .andExpect(status().isUnauthorized());
    }

    // ---- o cliente só enxerga a própria conta ----

    @Test
    void clienteSoEnxergaAPropriaConta() throws Exception {
        mvc.perform(get("/contas/me").with(httpBasic(CPF_A, SENHA_CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF_A));

        mvc.perform(get("/contas/me").with(httpBasic(CPF_B, SENHA_CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF_B));
    }

    @Test
    void rotasAntigasPorCpfEstaoNegadas() throws Exception {
        // Não existe mais rota em que o cliente informe o CPF na URL.
        mvc.perform(get("/contas/" + CPF_B).with(httpBasic(CPF_A, SENHA_CLIENTE)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/contas/" + CPF_B + "/saques")
                        .with(httpBasic(CPF_A, SENHA_CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":10.00}"))
                .andExpect(status().isForbidden());
    }

    // ---- papéis ----

    @Test
    void clienteNaoAcessaRotasDeAdmin() throws Exception {
        mvc.perform(get("/contas").with(httpBasic(CPF_A, SENHA_CLIENTE)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/contas/admin/" + CPF_B).with(httpBasic(CPF_A, SENHA_CLIENTE)))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/contas/admin/" + CPF_B).with(httpBasic(CPF_A, SENHA_CLIENTE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminNaoUsaRotasDoCliente() throws Exception {
        mvc.perform(get("/contas/me").with(httpBasic("admin", SENHA_ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListaContas() throws Exception {
        mvc.perform(get("/contas").with(httpBasic("admin", SENHA_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isArray());
    }

    @Test
    void adminConsultaContaPorCpf() throws Exception {
        mvc.perform(get("/contas/admin/" + CPF_B).with(httpBasic("admin", SENHA_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF_B));
    }

    // ---- operações do cliente ----

    @Test
    void clienteDepositaNaPropriaConta() throws Exception {
        mvc.perform(post("/contas/me/depositos")
                        .with(httpBasic(CPF_C, SENHA_CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF_C))
                .andExpect(jsonPath("$.saldo").value(1100.00));
    }

    @Test
    void transferenciaDebitaOClienteLogado() throws Exception {
        mvc.perform(post("/transferencias")
                        .with(httpBasic(CPF_D, SENHA_CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpfDestino\":\"" + CPF_B + "\",\"valor\":100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF_D))
                .andExpect(jsonPath("$.saldo").value(900.00));
    }

    // ---- cadastro público ----

    @Test
    void cadastroPublicoCriaAContaEACredencial() throws Exception {
        when(cepService.buscarEnderecoPorCep(any()))
                .thenReturn(new Endereco("01001000", "Praca da Se", "Se", "Sao Paulo", "SP"));

        mvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titular":"Cliente Novo","cpf":"15350946056","email":"novo@example.com",
                                 "cep":"01001000","tipo":"CORRENTE","saldoInicial":50.00,"senha":"SenhaNova#1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value(CPF_NOVO));

        // A senha informada no cadastro já serve para entrar.
        mvc.perform(get("/contas/me").with(httpBasic(CPF_NOVO, "SenhaNova#1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(50.00));
    }

    // ---- o que é público e o que é negado por padrão ----

    @Test
    void healthEPublico() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void rotaNaoListadaEhNegada() throws Exception {
        mvc.perform(get("/qualquer-coisa").with(httpBasic(CPF_A, SENHA_CLIENTE)))
                .andExpect(status().isForbidden());
    }

    private void criarCliente(String cpf, String titular, String email) {
        Endereco endereco = new Endereco("01001000", "Praca da Se", "Se", "Sao Paulo", "SP");
        contaDAO.salvar(new ContaCorrente(titular, new BigDecimal("1000.00"), cpf, email, endereco));
        credencialDAO.criarCliente(cpf, SENHA_CLIENTE);
    }
}