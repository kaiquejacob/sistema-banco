package br.com.sistemabanco.controller;

import br.com.sistemabanco.application.ContaService;
import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.mapper.ExtratoMapper;
import br.com.sistemabanco.requests.CriarContaRequest;
import br.com.sistemabanco.requests.ValorRequest;
import br.com.sistemabanco.responses.ContaResponse;
import br.com.sistemabanco.responses.ExtratoResponse;
import br.com.sistemabanco.responses.PaginaResponse;
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

import java.net.URI;
import java.security.Principal;

@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

    // ---- Público: cadastro ----

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

    @GetMapping("/me")
    public ContaResponse minhaConta(Principal principal) {
        return ContaResponse.de(contaService.buscarConta(principal.getName()));
    }

    @GetMapping("/me/extrato")
    public ExtratoResponse meuExtrato(Principal principal) {
        return ExtratoMapper.de(contaService.buscarConta(principal.getName()));
    }

    @PostMapping("/me/depositos")
    public ContaResponse depositar(Principal principal, @Valid @RequestBody ValorRequest request) {
        contaService.depositar(principal.getName(), request.valor());
        return ContaResponse.de(contaService.buscarConta(principal.getName()));
    }

    @PostMapping("/me/saques")
    public ContaResponse sacar(Principal principal, @Valid @RequestBody ValorRequest request) {
        contaService.sacar(principal.getName(), request.valor());
        return ContaResponse.de(contaService.buscarConta(principal.getName()));
    }

    // ---- Admin ----

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