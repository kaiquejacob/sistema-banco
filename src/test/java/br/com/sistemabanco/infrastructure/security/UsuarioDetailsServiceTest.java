package br.com.sistemabanco.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioJpaRepository usuarioRepository;

    @InjectMocks
    private UsuarioDetailsService service;

    @Test
    void deveCarregarClienteDigitandoOCpfComPontuacao() {
        when(usuarioRepository.findById("12345678909")).thenReturn(Optional.of(
                new UsuarioEntity("12345678909", "{bcrypt}hash", Papel.CLIENTE, "12345678909")));

        UserDetails usuario = service.loadUserByUsername("123.456.789-09");

        assertEquals("12345678909", usuario.getUsername());
        assertEquals("{bcrypt}hash", usuario.getPassword());
        assertTrue(usuario.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE")));
    }

    @Test
    void deveCarregarOAdministrador() {
        when(usuarioRepository.findById("admin")).thenReturn(Optional.of(
                new UsuarioEntity("admin", "{bcrypt}hash", Papel.ADMIN, null)));

        UserDetails usuario = service.loadUserByUsername("admin");

        assertTrue(usuario.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void deveLancarUsernameNotFoundQuandoOUsuarioNaoExiste() {
        when(usuarioRepository.findById("39053344705")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("39053344705"));
    }
}