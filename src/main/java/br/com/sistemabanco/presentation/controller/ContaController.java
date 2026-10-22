package br.com.sistemabanco.presentation.controller;

import br.com.sistemabanco.application.ContaService;
import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.presentation.mapper.ExtratoMapper;
import br.com.sistemabanco.presentation.requests.CriarContaRequest;
import br.com.sistemabanco.presentation.requests.ValorRequest;
import br.com.sistemabanco.presentation.responses.ContaResponse;
import br.com.sistemabanco.presentation.responses.ExtratoResponse;
import br.com.sistemabanco.presentation.responses.PaginaResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.security.Principal;

@Tag(name = "Contas", description = "Cadastro, consulta e operações em contas bancárias")
@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

    // ---- Público: cadastro ----

    @Operation(summary = "Cadastra uma nova conta bancária")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "CPF já cadastrado"),
            @ApiResponse(responseCode = "502", description = "Falha ao consultar o ViaCEP")
    })
    @PostMapping
    public ResponseEntity<ContaResponse> criar(@Valid @RequestBody CriarContaRequest request) {
        ContaBancaria conta = contaService.criarConta(
                request.tipo(),
                request.titular(),
                request.saldoInicial(),
                request.cpf(),
                request.email(),
                request.cep(),
                request.senha()
        );

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/me")
                .build()
                .toUri();

        return ResponseEntity.created(location).body(ContaResponse.de(conta));
    }

    // ---- Cliente: o CPF vem do login, nunca da URL nem do corpo ----

    @Operation(summary = "Retorna a conta do cliente autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta encontrada"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada")
    })
    @GetMapping("/me")
    public ContaResponse minhaConta(Principal principal) {
        return ContaResponse.de(contaService.buscarConta(principal.getName()));
    }

    @GetMapping("/me/extrato")
    public ExtratoResponse meuExtrato(Principal principal) {
        return ExtratoMapper.de(contaService.buscarConta(principal.getName()));
    }

    @Operation(summary = "Realiza um depósito na conta do cliente autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Depósito realizado"),
            @ApiResponse(responseCode = "400", description = "Valor inválido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "422", description = "Operação não permitida")
    })
    @PostMapping("/me/depositos")
    public ContaResponse depositar(Principal principal, @Valid @RequestBody ValorRequest request) {
        contaService.depositar(principal.getName(), request.valor());
        return ContaResponse.de(contaService.buscarConta(principal.getName()));
    }

    @Operation(summary = "Realiza um saque na conta do cliente autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saque realizado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente ou limite atingido")
    })
    @PostMapping("/me/saques")
    public ContaResponse sacar(Principal principal, @Valid @RequestBody ValorRequest request) {
        contaService.sacar(principal.getName(), request.valor());
        return ContaResponse.de(contaService.buscarConta(principal.getName()));
    }

    // ---- Admin ----

    @Operation(summary = "Lista contas ordenadas por saldo", description = "Operação exclusiva de administrador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de contas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administrador")
    })
    @GetMapping
    public PaginaResponse<ContaResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho
    ) {
        return PaginaResponse.de(contaService.listarPorSaldo(pagina, tamanho));
    }

    @GetMapping("/admin/{cpf}")
    public ContaResponse buscar(@PathVariable String cpf) {
        return ContaResponse.de(contaService.buscarConta(cpf));
    }

    @GetMapping("/admin/{cpf}/extrato")
    public ExtratoResponse extrato(@PathVariable String cpf) {
        return ExtratoMapper.de(contaService.buscarConta(cpf));
    }

    @Operation(summary = "Remove uma conta", description = "Operação exclusiva de administrador.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conta removida"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administrador"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conta possui histórico ou saldo")
    })
    @DeleteMapping("/admin/{cpf}")
    public ResponseEntity<Void> remover(@PathVariable String cpf) {
        contaService.removerConta(cpf);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/{cpf}/taxa-manutencao")
    public ContaResponse cobrarTaxaManutencao(@PathVariable String cpf) {
        contaService.cobrarTaxaManutencao(cpf);
        return ContaResponse.de(contaService.buscarConta(cpf));
    }
}