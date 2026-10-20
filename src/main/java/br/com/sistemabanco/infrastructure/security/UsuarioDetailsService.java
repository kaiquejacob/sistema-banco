package br.com.sistemabanco.infrastructure.security;

import br.com.sistemabanco.util.Validador;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioJpaRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioJpaRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        // O cliente pode digitar o CPF com pontos e traço; o login guardado é só dígitos.
        String loginNormalizado = Validador.normalizarCpf(login);

        UsuarioEntity usuario = usuarioRepository.findById(loginNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        return User.withUsername(usuario.getLogin())
                .password(usuario.getSenhaHash())
                .roles(usuario.getPapel().name())
                .build();
    }
}