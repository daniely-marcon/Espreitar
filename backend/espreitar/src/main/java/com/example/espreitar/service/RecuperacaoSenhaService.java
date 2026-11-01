package com.example.espreitar.service;

import com.example.espreitar.model.TokenRedefinicaoSenha;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.model.dto.RedefinirSenha;
import com.example.espreitar.model.dto.SolicitarRedefinicaoSenha;
import com.example.espreitar.repository.TokenRedefinicaoSenhaRepository;
import com.example.espreitar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RecuperacaoSenhaService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final TokenRedefinicaoSenhaRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final EmailRecuperacaoService emailRecuperacaoService;

    @Transactional
    public void solicitar(SolicitarRedefinicaoSenha dados) {
        if (!emailRecuperacaoService.estaConfigurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A recuperação por e-mail ainda não está configurada.");
        }
        String email = dados.email().trim().toLowerCase(Locale.ROOT);
        usuarios.findByEmail(email).ifPresent(this::enviarToken);
    }

    @Transactional
    public void redefinir(RedefinirSenha dados) {
        SenhaForte.validar(dados.senha());
        TokenRedefinicaoSenha token = tokens.findByTokenHash(hashToken(dados.token()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "O link de redefinição é inválido ou já foi usado."));
        if (!token.getDataExpiracao().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "O link de redefinição expirou. Solicite outro.");
        }
        Usuario usuario = token.getUsuario();
        usuario.setSenha(passwordEncoder.encode(dados.senha()));
        usuarios.save(usuario);
        tokens.deleteByUsuarioId(usuario.getId());
    }

    private void enviarToken(Usuario usuario) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String tokenBruto = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.deleteByUsuarioId(usuario.getId());

        TokenRedefinicaoSenha token = new TokenRedefinicaoSenha();
        token.setUsuario(usuario);
        token.setTokenHash(hashToken(tokenBruto));
        token.setDataExpiracao(LocalDateTime.now().plusMinutes(30));
        tokens.save(token);
        emailRecuperacaoService.enviarRedefinicao(usuario.getEmail(), usuario.getNome(), tokenBruto);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não está disponível.", ex);
        }
    }
}
