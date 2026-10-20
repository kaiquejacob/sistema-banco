package br.com.sistemabanco.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInicialTest {

    @Mock
    private UsuarioJpaRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void deveCriarOAdminQuandoNaoExisteEHaSenhaValida() {
        when(usuarioRepository.existsById("admin")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte1")).thenReturn("{bcrypt}hash");

        new AdminInicial(usuarioRepository, passwordEncoder, "SenhaForte1").run(null);

        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());
        UsuarioEntity salvo = captor.getValue();
        assertEquals("admin", salvo.getLogin());
        assertEquals("{bcrypt}hash", salvo.getSenhaHash());
        assertEquals(Papel.ADMIN, salvo.getPapel());
        assertNull(salvo.getCpfTitular());
    }

    @Test
    void naoDeveMexerNoAdminQueJaExiste() {
        when(usuarioRepository.existsById("admin")).thenReturn(true);

        new AdminInicial(usuarioRepository, passwordEncoder, "OutraSenha1").run(null);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void naoDeveCriarOAdminSemSenhaOuComSenhaCurta() {
        when(usuarioRepository.existsById("admin")).thenReturn(false);

        new AdminInicial(usuarioRepository, passwordEncoder, "").run(null);
        new AdminInicial(usuarioRepository, passwordEncoder, "curta").run(null);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}