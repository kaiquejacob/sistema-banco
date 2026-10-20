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

@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

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
                .path("/{cpf}")
                .buildAndExpand(conta.getCpf())
                .toUri();

        return ResponseEntity.created(location).body(ContaResponse.de(conta));
    }

    @GetMapping
    public PaginaResponse<ContaResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho
    ) {
        return PaginaResponse.de(contaService.listarPorSaldo(pagina, tamanho));
    }

    @GetMapping("/{cpf}")
    public ContaResponse buscar(@PathVariable String cpf) {
        return ContaResponse.de(contaService.buscarConta(cpf));
    }

    @GetMapping("/{cpf}/extrato")
    public ExtratoResponse extrato(@PathVariable String cpf) {
        return ExtratoMapper.de(contaService.buscarConta(cpf));
    }

    @PostMapping("/{cpf}/depositos")
    public ContaResponse depositar(@PathVariable String cpf, @Valid @RequestBody ValorRequest request) {
        contaService.depositar(cpf, request.valor());
        return ContaResponse.de(contaService.buscarConta(cpf));
    }

    @PostMapping("/{cpf}/saques")
    public ContaResponse sacar(@PathVariable String cpf, @Valid @RequestBody ValorRequest request) {
        contaService.sacar(cpf, request.valor());
        return ContaResponse.de(contaService.buscarConta(cpf));
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