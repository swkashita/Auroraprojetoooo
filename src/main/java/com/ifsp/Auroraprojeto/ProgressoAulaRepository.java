package com.ifsp.Auroraprojeto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgressoAulaRepository extends JpaRepository<ProgressoAula, Long> {
    
    // Conta quantas aulas um usuário específico concluiu
    long countByUsuario(Usuario usuario);

    // Verifica se o usuário já concluiu uma aula específica (para não duplicar)
    boolean existsByUsuarioAndConteudo(Usuario usuario, Conteudo conteudo);
}