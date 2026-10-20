package br.com.sistemabanco.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminInicial implements ApplicationRunner {

    static final String LOGIN_ADMIN = "admin";
    static final int TAMANHO_MINIMO_SENHA = 8;

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioJpaRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String senhaAdmin;

    public AdminInicial(
            UsuarioJpaRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${seguranca.admin-password:}") String senhaAdmin
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.senhaAdmin = senhaAdmin;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsById(LOGIN_ADMIN)) {
            return;
        }

        if (senhaAdmin == null || senhaAdmin.length() < TAMANHO_MINIMO_SENHA) {
            log.warn("Nenhum administrador foi criado. Defina ADMIN_PASSWORD com pelo menos {} caracteres.",
                    TAMANHO_MINIMO_SENHA);
            return;
        }

        usuarioRepository.save(
                new UsuarioEntity(LOGIN_ADMIN, passwordEncoder.encode(senhaAdmin), Papel.ADMIN, null)
        );
        log.info("Administrador '{}' criado.", LOGIN_ADMIN);
    }
}