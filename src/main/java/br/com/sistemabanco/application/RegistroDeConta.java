package br.com.sistemabanco.application;

import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.repository.ContaDAO;
import br.com.sistemabanco.domain.repository.CredencialDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroDeConta {

    private final ContaDAO contaDAO;
    private final CredencialDAO credencialDAO;

    public RegistroDeConta(ContaDAO contaDAO, CredencialDAO credencialDAO) {
        this.contaDAO = contaDAO;
        this.credencialDAO = credencialDAO;
    }

    @Transactional
    public void registrar(ContaBancaria conta, String senha) {
        contaDAO.salvar(conta);
        credencialDAO.criarCliente(conta.getCpf(), senha);
    }
}