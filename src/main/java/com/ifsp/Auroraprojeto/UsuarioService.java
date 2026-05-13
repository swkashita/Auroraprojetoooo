package com.ifsp.Auroraprojeto;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public boolean cadastrar(Usuario usuario){

        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(usuario.getEmail());

        if(usuarioExistente.isPresent()){
            return false;
        }

        usuarioRepository.save(usuario);

        return true;
    }

    public Usuario login(String email, String senha){

        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);

        if(usuario.isPresent() && usuario.get().getSenha().equals(senha)){
            return usuario.get();
        }

        return null;
    }

    public void salvar(Usuario usuario){

        usuarioRepository.save(usuario);

    }
}