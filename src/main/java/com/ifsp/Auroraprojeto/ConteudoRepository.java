package com.ifsp.Auroraprojeto;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConteudoRepository extends JpaRepository<Conteudo, Long> {
    
    // Busca por tipo (usado nos materiais extras e provas)
    List<Conteudo> findByTipo(TipoConteudo tipo);

    List<Conteudo> findByDisciplinaAndNivel(
            Disciplina disciplina,
            String nivel
    );

}