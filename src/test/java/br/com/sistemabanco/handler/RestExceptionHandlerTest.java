package br.com.sistemabanco.handler;

import br.com.sistemabanco.application.ContaService;
import br.com.sistemabanco.presentation.controller.ContaController;
import br.com.sistemabanco.presentation.controller.TransferenciaController;
import br.com.sistemabanco.domain.exception.ConflitoConcorrenciaException;
import br.com.sistemabanco.domain.exception.ContaComHistoricoException;
import br.com.sistemabanco.domain.exception.ContaComSaldoException;
import br.com.sistemabanco.domain.exception.ContaNaoEncontradaException;
import br.com.sistemabanco.domain.exception.CpfInvalidoException;
import br.com.sistemabanco.domain.exception.CpfJaCadastradoException;
import br.com.sistemabanco.domain.exception.SaldoInsuficienteException;
import br.com.sistemabanco.domain.exception.ServicoCepException;
import br.com.sistemabanco.presentation.handler.RestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.security.Principal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RestExceptionHandlerTest {

    private static final String CPF = "12345678909";

    // Simula o cliente logado: em produção o Spring Security preenche o Principal.
    private static final Principal CLIENTE = () -> CPF;

    private static final String CORPO_CRIAR_CONTA = """
            {"titular":"Maria Souza","cpf":"12345678909","email":"maria@email.com",
             "cep":"01001000","tipo":"CORRENTE","saldoInicial":100.00,"senha":"Senha#1234"}
            """;

    @Mock
    private ContaService contaService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new ContaController(contaService),
                        new TransferenciaController(contaService))
                .setControllerAdvice(new RestExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ---- 400 ----

    @Test
    void deveRetornar400QuandoARegraDeValorDoDominioFalha() throws Exception {
        doThrow(new IllegalArgumentException("O valor do depósito deve ser positivo."))
                .when(contaService).depositar(eq(CPF), any(BigDecimal.class));

        mockMvc.perform(post("/contas/me/depositos")
                        .principal(CLIENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":50.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.detail").value("O valor do depósito deve ser positivo."));
    }

    @Test
    void deveRetornar400QuandoOCpfEInvalido() throws Exception {
        when(contaService.buscarConta("123"))
                .thenThrow(new CpfInvalidoException("CPF inválido: 123"));

        mockMvc.perform(get("/contas/admin/{cpf}", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("CPF inválido: 123"));
    }

    @Test
    void deveListarOsCamposInvalidosNaValidacaoDoCorpo() throws Exception {
        mockMvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titular":"","cpf":"12345678909","email":"maria@email.com",
                                 "cep":"01001000","tipo":"CORRENTE","saldoInicial":100.00,"senha":"Senha#1234"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("titular")));
    }

    @Test
    void deveRetornar400QuandoOCorpoEMalFormado() throws Exception {
        mockMvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar405QuandoOMetodoNaoEPermitido() throws Exception {
        mockMvc.perform(get("/transferencias"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---- 404 ----

    @Test
    void deveRetornar404QuandoAContaNaoExiste() throws Exception {
        when(contaService.buscarConta(CPF))
                .thenThrow(new ContaNaoEncontradaException("Conta não encontrada para o CPF: " + CPF));

        mockMvc.perform(get("/contas/me").principal(CLIENTE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Conta não encontrada"))
                .andExpect(jsonPath("$.detail").value("Conta não encontrada para o CPF: " + CPF));
    }

    // ---- 409 ----

    @Test
    void deveRetornar409QuandoOCpfJaEstaCadastrado() throws Exception {
        when(contaService.criarConta(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new CpfJaCadastradoException("Já existe uma conta cadastrada para o CPF: " + CPF));

        mockMvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_CRIAR_CONTA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito"));
    }

    @Test
    void deveRetornar409AoRemoverContaComSaldo() throws Exception {
        doThrow(new ContaComSaldoException("Não é possível remover conta com saldo positivo."))
                .when(contaService).removerConta(CPF);

        mockMvc.perform(delete("/contas/admin/{cpf}", CPF))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Não é possível remover conta com saldo positivo."));
    }

    @Test
    void deveRetornar409AoRemoverContaComHistorico() throws Exception {
        doThrow(new ContaComHistoricoException("Não é possível excluir uma conta que possui transações."))
                .when(contaService).removerConta(CPF);

        mockMvc.perform(delete("/contas/admin/{cpf}", CPF))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRetornar409QuandoHaConflitoDeConcorrencia() throws Exception {
        doThrow(new ConflitoConcorrenciaException("A conta foi alterada por outra operação."))
                .when(contaService).depositar(eq(CPF), any(BigDecimal.class));

        mockMvc.perform(post("/contas/me/depositos")
                        .principal(CLIENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":50.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRetornar409QuandoOBancoFalhaPorLock() throws Exception {
        doThrow(new CannotAcquireLockException("deadlock detectado em contas"))
                .when(contaService).transferir(eq(CPF), any(), any(BigDecimal.class));

        mockMvc.perform(post("/transferencias")
                        .principal(CLIENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpfDestino":"11144477735","valor":10.00}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(not(containsString("deadlock"))));
    }

    // ---- 422 ----

    @Test
    void deveRetornar422QuandoOSaldoEInsuficiente() throws Exception {
        doThrow(new SaldoInsuficienteException("Saldo insuficiente. Saldo atual: R$10, valor solicitado: R$50.00"))
                .when(contaService).sacar(eq(CPF), any(BigDecimal.class));

        mockMvc.perform(post("/contas/me/saques")
                        .principal(CLIENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":50.00}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.title").value("Operação não permitida"));
    }

    // ---- 502 e 500: nunca vazam a mensagem interna ----

    @Test
    void deveRetornar502SemVazarDetalhesInternosDoServicoDeCep() throws Exception {
        when(contaService.criarConta(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ServicoCepException("timeout em http://interno:8080/segredo",
                        new RuntimeException("causa")));

        mockMvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_CRIAR_CONTA))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Serviço de CEP indisponível"))
                .andExpect(jsonPath("$.detail").value(not(containsString("segredo"))));
    }

    @Test
    void deveRetornar500SemVazarAMensagemDaExcecao() throws Exception {
        when(contaService.buscarConta(CPF))
                .thenThrow(new RuntimeException("senha do banco: 12345"));

        mockMvc.perform(get("/contas/me").principal(CLIENTE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail").value(not(containsString("senha"))));
    }
}