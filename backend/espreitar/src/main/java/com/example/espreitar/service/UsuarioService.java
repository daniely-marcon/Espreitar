package com.example.espreitar.service;

import com.example.espreitar.model.Usuario;
import com.example.espreitar.model.Perfil;
import com.example.espreitar.model.dto.AtualizarPerfil;
import com.example.espreitar.model.dto.AtualizarPerfilAdmin;
import com.example.espreitar.model.dto.AuthResposta;
import com.example.espreitar.model.dto.UsuarioResumo;
import com.example.espreitar.model.dto.UsuarioAdminResumo;
import com.example.espreitar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public UsuarioResumo buscarPerfil(String email) {
        return resumo(buscarUsuarioEmail(email));
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminResumo> listarUsuariosParaAdmin() {
        return usuarioRepository.listarResumoAdmin();
    }

    @Transactional
    public void atualizarPerfilAdmin(String emailAdmin, Long usuarioId, AtualizarPerfilAdmin dados) {
        Usuario admin = buscarUsuarioEmail(emailAdmin);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (admin.getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível alterar o próprio perfil por este painel.");
        }
        if (usuario.getPerfil() == Perfil.ADMIN && dados.perfil() != Perfil.ADMIN
                && usuarioRepository.countByPerfil(Perfil.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O sistema precisa manter pelo menos um administrador.");
        }
        usuario.setPerfil(dados.perfil());
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void excluirUsuarioAdmin(String emailAdmin, Long usuarioId) {
        Usuario admin = buscarUsuarioEmail(emailAdmin);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (admin.getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível excluir a própria conta.");
        }
        if (usuario.getPerfil() == Perfil.ADMIN && usuarioRepository.countByPerfil(Perfil.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível excluir o último administrador.");
        }
        usuarioRepository.delete(usuario);
    }

    @Transactional
    public AuthResposta atualizarPerfil(String emailAtual, AtualizarPerfil dados) {
        Usuario usuario = buscarUsuarioEmail(emailAtual);
        String novoEmail = dados.email().trim().toLowerCase(Locale.ROOT);
        if (!usuario.getEmail().equals(novoEmail) && usuarioRepository.existsByEmail(novoEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }
        usuario.setNome(dados.nome().trim());
        usuario.setEmail(novoEmail);
        if (dados.senha() != null && !dados.senha().isBlank()) {
            SenhaForte.validar(dados.senha());
            usuario.setSenha(passwordEncoder.encode(dados.senha()));
        }
        if (dados.fotoPerfil() != null) {
            if (!dados.fotoPerfil().isBlank()
                    && !dados.fotoPerfil().startsWith("data:image/jpeg;base64,")
                    && !dados.fotoPerfil().startsWith("https://")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de foto inválido.");
            }
            usuario.setFotoPerfil(dados.fotoPerfil());
        }
        Usuario atualizado = usuarioRepository.save(usuario);
        return new AuthResposta(tokenService.gerar(atualizado), resumo(atualizado));
    }

    @Transactional(readOnly = true)
    public List<String> listarNotificacoes(String email) {
        return buscarUsuarioEmail(email).getNotificacoes().stream().distinct().toList();
    }

    @Transactional
    public void limparNotificacoes(String email) {
        buscarUsuarioEmail(email).getNotificacoes().clear();
    }

    private Usuario buscarUsuarioEmail(String email) {
        String emailNormalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private UsuarioResumo resumo(Usuario usuario) {
        return new UsuarioResumo(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPerfil(), usuario.getFotoPerfil());
    }
}