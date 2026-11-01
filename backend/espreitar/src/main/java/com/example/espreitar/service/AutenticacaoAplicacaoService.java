package com.example.espreitar.service;

import com.example.espreitar.model.Perfil;
import com.example.espreitar.model.Provedor;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.model.dto.AuthResposta;
import com.example.espreitar.model.dto.CadastroUsuario;
import com.example.espreitar.model.dto.UsuarioResumo;
import com.example.espreitar.repository.UsuarioRepository;
import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AutenticacaoAplicacaoService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RestClient restClient = RestClient.create();
    private final String googleClientId;

    public AutenticacaoAplicacaoService(
            UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            @Value("${google.client-id:}") String googleClientId) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.googleClientId = googleClientId;
    }

    public AuthResposta cadastrar(CadastroUsuario dados) {
        SenhaForte.validar(dados.senha());
        String email = normalizarEmail(dados.email());
        if (usuarios.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(dados.nome().trim());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(dados.senha()));
        validarFoto(dados.fotoPerfil());
        usuario.setFotoPerfil(dados.fotoPerfil());
        usuario.setProvedor(Provedor.LOCAL);
        usuario.setPerfil(Perfil.USER);
        return resposta(usuarios.save(usuario));
    }

    public AuthResposta login(String email, String senha) {
        Usuario usuario = usuarios.findByEmail(normalizarEmail(email))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos."));
        if (usuario.getSenha() == null || !passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
        }
        return resposta(usuario);
    }

    public AuthResposta autenticarGoogle(String idToken) {
        if (googleClientId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Login Google ainda não foi configurado.");
        }
        JsonNode claims;
        try {
            claims = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Google inválido.");
        }
        if (claims == null
                || !googleClientId.equals(claims.path("aud").asText())
                || !("accounts.google.com".equals(claims.path("iss").asText())
                    || "https://accounts.google.com".equals(claims.path("iss").asText()))
                || !"true".equals(claims.path("email_verified").asText())
                || claims.path("sub").asText().isBlank()
                || claims.path("email").asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Google inválido.");
        }

        String googleId = claims.path("sub").asText();
        String email = normalizarEmail(claims.path("email").asText());
        Usuario usuario = usuarios.findByGoogleId(googleId).orElseGet(() -> {
            if (usuarios.existsByEmail(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Este e-mail já pertence a outra conta. Entre com ela primeiro.");
            }
            Usuario novo = new Usuario();
            novo.setNome(claims.path("name").asText(email));
            novo.setEmail(email);
            novo.setGoogleId(googleId);
            novo.setFotoPerfil(claims.path("picture").asText(null));
            novo.setProvedor(Provedor.GOOGLE);
            novo.setPerfil(Perfil.USER);
            return usuarios.save(novo);
        });
        return resposta(usuario);
    }

    private AuthResposta resposta(Usuario usuario) {
        return new AuthResposta(tokenService.gerar(usuario), new UsuarioResumo(
                usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPerfil(), usuario.getFotoPerfil()));
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validarFoto(String foto) {
        if (foto != null && !foto.isBlank()
                && !foto.startsWith("data:image/jpeg;base64,")
                && !foto.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de foto inválido.");
        }
    }
}
