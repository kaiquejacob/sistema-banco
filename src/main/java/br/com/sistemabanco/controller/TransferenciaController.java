package br.com.sistemabanco.controller;

import br.com.sistemabanco.application.ContaService;
import br.com.sistemabanco.requests.TransferenciaRequest;
import br.com.sistemabanco.responses.ContaResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.security.Principal;

@Tag(name = "Transferências", description = "Transferências entre contas bancárias")
@RestController
@RequestMapping("/transferencias")
public class TransferenciaController {

    private final ContaService contaService;

    public TransferenciaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @Operation(summary = "Realiza uma transferência da conta autenticada para outra conta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferência realizada"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Conta de destino não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de concorrência"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente")
    })
    @PostMapping
    public ContaResponse transferir(Principal principal, @Valid @RequestBody TransferenciaRequest request) {
        contaService.transferir(principal.getName(), request.cpfDestino(), request.valor());
        return ContaResponse.de(contaService.buscarConta(principal.getName()));
    }
}