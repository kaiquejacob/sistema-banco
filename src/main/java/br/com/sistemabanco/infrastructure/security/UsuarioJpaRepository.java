package br.com.sistemabanco.infrastructure.security;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, String> {
}