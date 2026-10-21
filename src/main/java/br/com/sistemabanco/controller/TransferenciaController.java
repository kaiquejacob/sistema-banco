package br.com.sistemabanco.controller;

import br.com.sistemabanco.application.ContaService;
import br.com.sistemabanco.requests.TransferenciaRequest;
import br.com.sistemabanco.responses.ContaResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/transferencias")
public class TransferenciaController {

    private final ContaService contaService;

    public TransferenciaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @PostMapping
    public ContaResponse transferir(Principal principal, @Valid @RequestBody TransferenciaRequest request) {
        contaService.transferir(principal.getName(), request.cpfDestino(), request.valor());
        return ContaResponse.de(contaService.buscarConta(principal.getName()));
    }
}