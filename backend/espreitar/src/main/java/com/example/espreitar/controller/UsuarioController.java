package com.example.espreitar.controller;


import com.example.espreitar.model.Usuario;
import com.example.espreitar.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;


    @PostMapping
    public Usuario salvar(@RequestBody Usuario user){
        return usuarioService.cadastrarUsuario(user);
    }
}
