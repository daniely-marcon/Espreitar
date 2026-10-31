package com.example.espreitar.service;

import com.example.espreitar.model.Avaliacao;
import com.example.espreitar.model.Denuncia;
import com.example.espreitar.model.Midia;
import com.example.espreitar.model.MotivoDenuncia;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.repository.AvaliacaoRepository;
import com.example.espreitar.repository.DenunciaRepository;
import com.example.espreitar.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DenunciaServiceTest {
    @Mock
    private DenunciaRepository denunciaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @InjectMocks
    private DenunciaService denunciaService;

    private Usuario denunciante;
    private Avaliacao avaliacao;

    @BeforeEach
    void configurarDados() {
        denunciante = usuario(1L, "Carolina", "carolina@example.com");
        Usuario autor = usuario(2L, "Autora", "autora@example.com");
        Midia midia = new Midia();
        midia.setTitulo("Filme de teste");
        avaliacao = new Avaliacao();
        avaliacao.setId(3L);
        avaliacao.setUsuario(autor);
        avaliacao.setMidia(midia);
        avaliacao.setComentario("Comentário denunciado");
    }

    @Test
    void criaDenunciaComEmailNormalizadoEMotivoInformado() {
        when(usuarioRepository.findByEmail("carolina@example.com")).thenReturn(Optional.of(denunciante));
        when(avaliacaoRepository.findById(3L)).thenReturn(Optional.of(avaliacao));
        when(denunciaRepository.existsByUsuarioIdAndAvaliacaoId(1L, 3L)).thenReturn(false);
        when(denunciaRepository.save(any(Denuncia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = denunciaService.criarDenuncia("  CAROLINA@EXAMPLE.COM ", 3L, MotivoDenuncia.SPOILER);

        assertEquals(3L, resposta.avaliacaoId());
        assertEquals("Comentário denunciado", resposta.comentario());
        assertEquals("Autora", resposta.autor());
        assertEquals("Filme de teste", resposta.tituloMidia());
        assertEquals("Carolina", resposta.denunciante());
        assertEquals(MotivoDenuncia.SPOILER, resposta.motivo());
        verify(usuarioRepository).findByEmail("carolina@example.com");

        ArgumentCaptor<Denuncia> denunciaSalva = ArgumentCaptor.forClass(Denuncia.class);
        verify(denunciaRepository).save(denunciaSalva.capture());
        assertEquals(denunciante, denunciaSalva.getValue().getUsuario());
        assertEquals(avaliacao, denunciaSalva.getValue().getAvaliacao());
    }

    @Test
    void usaOutroQuandoMotivoNaoFoiInformado() {
        prepararDenunciaDisponivel();
        when(denunciaRepository.save(any(Denuncia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = denunciaService.criarDenuncia(denunciante.getEmail(), 3L, null);

        assertEquals(MotivoDenuncia.OUTRO, resposta.motivo());
    }

    @Test
    void impedeDenunciarPropriaAvaliacao() {
        avaliacao.setUsuario(denunciante);
        when(usuarioRepository.findByEmail(denunciante.getEmail())).thenReturn(Optional.of(denunciante));
        when(avaliacaoRepository.findById(3L)).thenReturn(Optional.of(avaliacao));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> denunciaService.criarDenuncia(denunciante.getEmail(), 3L, MotivoDenuncia.OUTRO));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(denunciaRepository, never()).save(any(Denuncia.class));
    }

    @Test
    void impedeDenunciaDuplicada() {
        when(usuarioRepository.findByEmail(denunciante.getEmail())).thenReturn(Optional.of(denunciante));
        when(avaliacaoRepository.findById(3L)).thenReturn(Optional.of(avaliacao));
        when(denunciaRepository.existsByUsuarioIdAndAvaliacaoId(1L, 3L)).thenReturn(true);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> denunciaService.criarDenuncia(denunciante.getEmail(), 3L, MotivoDenuncia.OUTRO));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(denunciaRepository, never()).save(any(Denuncia.class));
    }

    private void prepararDenunciaDisponivel() {
        when(usuarioRepository.findByEmail(denunciante.getEmail())).thenReturn(Optional.of(denunciante));
        when(avaliacaoRepository.findById(3L)).thenReturn(Optional.of(avaliacao));
        when(denunciaRepository.existsByUsuarioIdAndAvaliacaoId(1L, 3L)).thenReturn(false);
    }

    private Usuario usuario(Long id, String nome, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setEmail(email);
        return usuario;
    }
}