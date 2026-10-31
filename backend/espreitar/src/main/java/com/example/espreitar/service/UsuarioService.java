package com.example.espreitar.service;

import com.example.espreitar.model.Usuario;
import com.example.espreitar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder; // 1. Importe o PasswordEncoder
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService{
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder; // 2. Injete o PasswordEncoder aqui

    public Usuario cadastrarUsuario(Usuario user){
        if (usuarioRepository.existsByEmail(user.getEmail())){
            throw new RuntimeException("E-mail já cadastrado");
        }


        if (user.getSenha() != null && !user.getSenha().isBlank()) {
            user.setSenha(passwordEncoder.encode(user.getSenha()));
        }

        return usuarioRepository.save(user);
    }

    public Usuario buscarUsuarioEmail(String email){
        Optional<Usuario> resultado = usuarioRepository.findByEmail(email);

        if (resultado.isPresent()) {
            return resultado.get();
        } else {
            throw new RuntimeException("Usuário não encontrado");
        }
    }

    public void excluirUsuarioEmail(String email){
        Usuario usuario = buscarUsuarioEmail(email);
        usuarioRepository.delete(usuario);
    }

    public Usuario atualizarUsuario(String email, Usuario dadosAtualizados){
        Usuario usuarioExistente = buscarUsuarioEmail(email);
        usuarioExistente.setNome(dadosAtualizados.getNome());

        if (dadosAtualizados.getSenha() != null && !dadosAtualizados.getSenha().isBlank()) {
            usuarioExistente.setSenha(passwordEncoder.encode(dadosAtualizados.getSenha()));
        }

        usuarioExistente.setFotoPerfil(dadosAtualizados.getFotoPerfil());
        return usuarioRepository.save(usuarioExistente);
    }

    public List<String> listarNotificacoes(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));
        return usuario.getNotificacoes();
    }
}