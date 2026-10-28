package com.example.espreitar.service;

import com.example.espreitar.model.Usuario;
import com.example.espreitar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService{
    private final UsuarioRepository usuarioRepository;

    public Usuario cadastrarUsuario(Usuario user){

        if (usuarioRepository.existsByEmail(user.getEmail())){
            throw new RuntimeException("E-mail já cadastrado");
        }else{
            return usuarioRepository.save(user);
        }
    }
}