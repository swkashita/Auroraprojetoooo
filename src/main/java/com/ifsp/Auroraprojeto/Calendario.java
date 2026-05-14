package com.ifsp.Auroraprojeto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id; // RESOLVE O ERRO: "Id cannot be resolved"
import jakarta.persistence.Table;

@Entity
@Table(name = "calendarios")
public class Calendario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String data;
    private String descricao;

    // Construtor vazio (obrigatório para o Spring/JPA)
    public Calendario() {
    }

    // Construtor com parâmetros (RESOLVE O ERRO: "Syntax error on token public")
    // O erro ocorria porque este construtor estava dentro das chaves do anterior.
    public Calendario(String data, String descricao) {
        this.data = data;
        this.descricao = descricao;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}