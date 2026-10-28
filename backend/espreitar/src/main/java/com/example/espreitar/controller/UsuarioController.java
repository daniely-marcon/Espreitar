package com.example.espreitar.controller;


import com.example.espreitar.model.Usuario;
import com.example.espreitar.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;


    @PostMapping
    public Usuario salvar(@RequestBody Usuario user){
        return usuarioService.cadastrarUsuario(user);
    }
    @GetMapping("/{email}")
    public Usuario buscarEmail(@PathVariable String email){
        return usuarioService.buscarUsuarioEmail(email);
    }
    @DeleteMapping("/{email}")
    public void excluir(@PathVariable String email){
        usuarioService.excluirUsuarioEmail(email);
    }
    @PutMapping("/{email}")
    public Usuario atualizar(@PathVariable String email,@RequestBody Usuario user){
        return usuarioService.atualizarUsuario(email,user);
    }
}
