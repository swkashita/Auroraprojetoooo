package com.ifsp.Auroraprojeto;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

    
    public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // buscar usuário pelo email
    Optional<Usuario> findByEmail(String email);

}
