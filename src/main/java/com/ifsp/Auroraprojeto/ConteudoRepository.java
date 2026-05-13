package com.ifsp.Auroraprojeto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConteudoRepository extends JpaRepository<Conteudo, Long> {

    List<Conteudo> findByTipo(TipoConteudo tipo);

    List<Conteudo> findByDisciplinaAndNivel(
            Disciplina disciplina,
            String nivel
    );

}