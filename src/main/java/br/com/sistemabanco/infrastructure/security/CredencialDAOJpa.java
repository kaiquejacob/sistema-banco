package br.com.sistemabanco.infrastructure.security;

import br.com.sistemabanco.domain.repository.CredencialDAO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CredencialDAOJpa implements CredencialDAO {

    private final UsuarioJpaRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CredencialDAOJpa(UsuarioJpaRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void criarCliente(String cpf, String senha) {
        usuarioRepository.save(
                new UsuarioEntity(cpf, passwordEncoder.encode(senha), Papel.CLIENTE, cpf)
        );
    }
}