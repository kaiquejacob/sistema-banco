package br.com.sistemabanco;

import br.com.sistemabanco.domain.exception.*;
import br.com.sistemabanco.application.ExtratoService;
import br.com.sistemabanco.domain.model.*;
import br.com.sistemabanco.application.ContaService;
import br.com.sistemabanco.util.Validador;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ContaService contaService = new ContaService();
        ExtratoService extratoService = new ExtratoService();

        int opcao = 0;
        while (opcao != 8) {
            System.out.println("\n╔══════════════════════════╗");
            System.out.println("║   🏦 BANCO - MENU        ║");
            System.out.println("╠══════════════════════════╣");
            System.out.println("║ 1 - Criar conta          ║");
            System.out.println("║ 2 - Depositar            ║");
            System.out.println("║ 3 - Sacar                ║");
            System.out.println("║ 4 - Ver extrato          ║");
            System.out.println("║ 5 - Listar todas contas  ║");
            System.out.println("║ 6 - Remover conta        ║");
            System.out.println("║ 7 - Transferir           ║");
            System.out.println("║ 8 - Sair                 ║");
            System.out.println("╚══════════════════════════╝");
            System.out.print("Escolha: ");

            try {
                opcao = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Opção inválida! Digite um número.");
                continue;
            }

            switch (opcao) {
                case 1:
                    System.out.println("\n--- 📝 Criar Conta ---");
                    System.out.print("Nome do titular: ");
                    String nomeTitular = scanner.nextLine();
                    if (!Validador.validarNome(nomeTitular)) {
                        System.out.println("Nome inválido!");
                        break;
                    }

                    System.out.print("CPF: ");
                    String cpf = scanner.nextLine();
                    System.out.print("Email: ");
                    String email = scanner.nextLine();
                    System.out.print("CEP: ");
                    String cep = scanner.nextLine();

                    System.out.print("Saldo inicial: R$");
                    BigDecimal saldo;
                    try {
                        saldo = new BigDecimal(scanner.nextLine().replace(",", "."));
                    } catch (NumberFormatException e) {
                        System.out.println("Valor de saldo inválido!");
                        break;
                    }

                    System.out.println("\nTipos de conta disponíveis:");
                    TipoConta[] tipoConta = TipoConta.values();
                    for (int i = 0; i < tipoConta.length; i++) {
                        System.out.println(i + " - " + tipoConta[i].nome);
                    }

                    System.out.print("Escolha um tipo: ");
                    int escolhaConta;
                    try {
                        escolhaConta = Integer.parseInt(scanner.nextLine());
                        if (escolhaConta < 0 || escolhaConta >= tipoConta.length) {
                            System.out.println("Opção de conta inválida!");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Opção de conta inválida!");
                        break;
                    }

                    try {
                        contaService.criarConta(tipoConta[escolhaConta], nomeTitular, saldo, cpf, email, cep);
                        System.out.println("Conta criada com sucesso!");
                    } catch (RuntimeException e) {
                        System.out.println("Erro ao criar conta: " + e.getMessage());
                    }
                    break;

                case 2:
                    System.out.println("\n--- 💰 Depositar ---");
                    System.out.print("CPF do titular: ");
                    cpf = Validador.normalizarCpf(scanner.nextLine());
                    System.out.print("Valor a depositar: R$");
                    try {
                        BigDecimal valorDepositar = new BigDecimal(scanner.nextLine().replace(",", "."));
                        contaService.depositar(cpf, valorDepositar);
                        System.out.println("Depósito realizado com sucesso!");
                    } catch (RuntimeException e) {
                        System.out.println("Erro ao depositar: " + e.getMessage());
                    }
                    break;

                case 3:
                    System.out.println("\n--- 💸 Sacar ---");
                    System.out.print("CPF do titular: ");
                    cpf = Validador.normalizarCpf(scanner.nextLine());
                    System.out.print("Valor a sacar: R$");
                    try {
                        BigDecimal valorSacar = new BigDecimal(scanner.nextLine().replace(",", "."));
                        contaService.sacar(cpf, valorSacar);
                        System.out.println("Saque realizado com sucesso!");
                    } catch (RuntimeException e) {
                        System.out.println("Erro ao sacar: " + e.getMessage());
                    }
                    break;

                case 4:
                    System.out.println("\n--- 📊 Extrato ---");
                    System.out.print("CPF do titular: ");
                    cpf = Validador.normalizarCpf(scanner.nextLine());
                    try {
                        ContaBancaria conta = contaService.buscarConta(cpf);
                        System.out.print(extratoService.gerar(conta));
                    } catch (ContaNaoEncontradaException e) {
                        System.out.println(e.getMessage());
                    }
                    break;

                case 5:
                    System.out.println("\n--- 📋 Todas as Contas ---");
                    List<ContaBancaria> listaContas = contaService.listarTodasOrdenadasPorSaldo();
                    if (listaContas.isEmpty()) {
                        System.out.println("Nenhuma conta cadastrada.");
                        break;
                    }
                    for (ContaBancaria c : listaContas) {
                        System.out.println(c);
                    }
                    break;

                case 6:
                    System.out.println("\n--- ❌ Remover Conta ---");
                    System.out.print("CPF do titular: ");
                    cpf = Validador.normalizarCpf(scanner.nextLine());
                    try {
                        contaService.removerConta(cpf);
                        System.out.println("Conta removida com sucesso!");
                    } catch (RuntimeException e) {
                        System.out.println("Erro ao remover conta: " + e.getMessage());
                    }
                    break;

                case 7:
                    System.out.println("\n--- 🔄 Transferência ---");
                    System.out.print("CPF da conta de origem: ");
                    String cpfOrigem = Validador.normalizarCpf(scanner.nextLine());
                    System.out.print("CPF da conta de destino: ");
                    String cpfDestino = Validador.normalizarCpf(scanner.nextLine());
                    System.out.print("Valor a transferir: R$");
                    try {
                        BigDecimal valorTransferir = new BigDecimal(scanner.nextLine().replace(",", "."));
                        contaService.transferir(cpfOrigem, cpfDestino, valorTransferir);
                        System.out.println("Transferência realizada com sucesso!");
                    } catch (RuntimeException e) {
                        System.out.println("Erro na transferência: " + e.getMessage());
                    }
                    break;

                case 8:
                    break;

                default:
                    System.out.println("Opção inválida!");
            }
        }

        System.out.println("Encerrando...");
        scanner.close();
    }
}