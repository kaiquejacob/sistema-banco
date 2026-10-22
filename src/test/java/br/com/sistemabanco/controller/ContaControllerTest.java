package br.com.sistemabanco.controller;

import br.com.sistemabanco.application.ContaService;
import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.domain.model.PaginaDeContas;
import br.com.sistemabanco.domain.model.ResumoConta;
import br.com.sistemabanco.domain.model.TipoConta;
import br.com.sistemabanco.presentation.controller.ContaController;
import br.com.sistemabanco.presentation.controller.TransferenciaController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContaControllerTest {

    private static final String CPF = "12345678909";

    // Simula o cliente logado: em produção o Spring Security preenche o Principal.
    private static final Principal CLIENTE = () -> CPF;

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
                .setValidator(validator)
                .build();
    }

    // ---- cadastro (público) ----

    @Test
    void deveCriarContaERetornar201ComLocation() throws Exception {
        when(contaService.criarConta(eq(TipoConta.CORRENTE), eq("Maria Souza"), any(BigDecimal.class),
                eq(CPF), eq("maria@email.com"), eq("01001000"), eq("Senha#1234")))
                .thenReturn(conta("100.00"));

        mockMvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titular":"Maria Souza","cpf":"12345678909","email":"maria@email.com",
                                 "cep":"01001000","tipo":"CORRENTE","saldoInicial":100.00,"senha":"Senha#1234"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/contas/me"))
                .andExpect(jsonPath("$.cpf").value(CPF))
                .andExpect(jsonPath("$.tipo").value("CORRENTE"))
                .andExpect(jsonPath("$.saldo").value(100.00));
    }

    @Test
    void deveRejeitarCriacaoComCamposInvalidos() throws Exception {
        mockMvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titular":"","cpf":"12345678909","email":"maria@email.com",
                                 "cep":"123","tipo":"CORRENTE","saldoInicial":-1,"senha":"Senha#1234"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contaService);
    }

    @Test
    void deveRejeitarCriacaoComCpfInvalido() throws Exception {
        mockMvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titular":"Maria Souza","cpf":"11111111111","email":"maria@email.com",
                                 "cep":"01001000","tipo":"CORRENTE","saldoInicial":100.00,"senha":"Senha#1234"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contaService);
    }

    @Test
    void deveRejeitarCriacaoComSenhaCurta() throws Exception {
        mockMvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titular":"Maria Souza","cpf":"12345678909","email":"maria@email.com",
                                 "cep":"01001000","tipo":"CORRENTE","saldoInicial":100.00,"senha":"123"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contaService);
    }

    // ---- cliente: o CPF vem do Principal ----

    @Test
    void deveMostrarAMinhaConta() throws Exception {
        when(contaService.buscarConta(CPF)).thenReturn(conta("500.00"));

        mockMvc.perform(get("/contas/me").principal(CLIENTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titular").value("Maria Souza"))
                .andExpect(jsonPath("$.saldo").value(500.00));
    }

    @Test
    void deveDepositarNaMinhaContaERetornarContaAtualizada() throws Exception {
        when(contaService.buscarConta(CPF)).thenReturn(conta("150.00"));

        mockMvc.perform(post("/contas/me/depositos")
                        .principal(CLIENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":50.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(150.00));

        verify(contaService).depositar(eq(CPF), any(BigDecimal.class));
    }

    @Test
    void deveRejeitarDepositoComValorNaoPositivo() throws Exception {
        mockMvc.perform(post("/contas/me/depositos")
                        .principal(CLIENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":-5}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contaService);
    }

    @Test
    void deveSacarDaMinhaConta() throws Exception {
        when(contaService.buscarConta(CPF)).thenReturn(conta("80.00"));

        mockMvc.perform(post("/contas/me/saques")
                        .principal(CLIENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":20.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(80.00));

        verify(contaService).sacar(eq(CPF), any(BigDecimal.class));
    }

    @Test
    void deveMostrarMeuExtratoEstruturado() throws Exception {
        ContaBancaria conta = conta("100.00");
        conta.depositar(new BigDecimal("50.00"));
        when(contaService.buscarConta(CPF)).thenReturn(conta);

        mockMvc.perform(get("/contas/me/extrato").principal(CLIENTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endereco.cidade").value("Sao Paulo"))
                .andExpect(jsonPath("$.transacoes[0].tipo").value("DEPOSITO"))
                .andExpect(jsonPath("$.detalhes.saquesGratuitosRestantes").value(2));
    }

    @Test
    void deveTransferirUsandoOClienteLogadoComoOrigem() throws Exception {
        when(contaService.buscarConta(CPF)).thenReturn(conta("900.00"));

        mockMvc.perform(post("/transferencias")
                        .principal(CLIENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpfDestino":"11144477735","valor":100.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(900.00));

        verify(contaService).transferir(eq(CPF), eq("11144477735"), any(BigDecimal.class));
    }

    // ---- admin ----

    @Test
    void deveListarContasPaginadas() throws Exception {
        List<ResumoConta> itens = List.of(
                new ResumoConta(CPF, "Maria Souza", TipoConta.CORRENTE, new BigDecimal("1000.00")));
        when(contaService.listarPorSaldo(0, 20)).thenReturn(new PaginaDeContas(itens, 1, 0, 20));

        mockMvc.perform(get("/contas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].cpf").value(CPF))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.totalDePaginas").value(1));
    }

    @Test
    void deveConsultarContaPorCpfComoAdmin() throws Exception {
        when(contaService.buscarConta(CPF)).thenReturn(conta("500.00"));

        mockMvc.perform(get("/contas/admin/{cpf}", CPF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF));
    }

    @Test
    void deveRemoverContaERetornar204() throws Exception {
        mockMvc.perform(delete("/contas/admin/{cpf}", CPF))
                .andExpect(status().isNoContent());

        verify(contaService).removerConta(CPF);
    }

    @Test
    void deveCobrarTaxaDeManutencao() throws Exception {
        when(contaService.buscarConta(CPF)).thenReturn(conta("988.00"));

        mockMvc.perform(post("/contas/admin/{cpf}/taxa-manutencao", CPF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(988.00));

        verify(contaService).cobrarTaxaManutencao(CPF);
    }

    private ContaBancaria conta(String saldo) {
        Endereco endereco = new Endereco("01001000", "Praca da Se", "Se", "Sao Paulo", "SP");
        return new ContaCorrente("Maria Souza", new BigDecimal(saldo), CPF, "maria@email.com", endereco);
    }
}