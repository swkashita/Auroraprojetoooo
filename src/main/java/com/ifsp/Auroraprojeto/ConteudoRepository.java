package com.ifsp.Auroraprojeto;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConteudoRepository extends JpaRepository<Conteudo, Long> {
    
    // ADICIONE ESTA LINHA AQUI:
    List<Conteudo> findByNivel(String nivel);
    
    // Estas abaixo você provavelmente já tem, mantenha-as:
    List<Conteudo> findByTipo(TipoConteudo tipo);
    List<Conteudo> findByDisciplinaAndNivel(Disciplina disciplina, String nivel);
}