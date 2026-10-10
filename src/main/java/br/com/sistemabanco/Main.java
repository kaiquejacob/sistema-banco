package br.com.sistemabanco;

import br.com.sistemabanco.application.ExtratoService;
import br.com.sistemabanco.application.ContaService;
import br.com.sistemabanco.infrastructure.client.CepServiceImpl;
import br.com.sistemabanco.infrastructure.config.ConexaoBanco;
import br.com.sistemabanco.infrastructure.persistence.ContaDAOImpl;

public class Main {
    public static void main(String[] args) {
        // Dependency Injection
        ContaService contaService = new ContaService(new ContaDAOImpl(), new CepServiceImpl());
        ExtratoService extratoService = new ExtratoService();

        // UI Initialization
        BancoConsoleUI ui = new BancoConsoleUI(contaService, extratoService);
        ui.executar();
        
        // Resource Cleanup
        ConexaoBanco.fechar();
    }
}