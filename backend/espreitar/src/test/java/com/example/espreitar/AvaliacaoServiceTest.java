package com.example.espreitar;

import com.example.espreitar.model.Avaliacao;
import com.example.espreitar.repository.AvaliacaoRepository;
import com.example.espreitar.repository.MidiaRepository;
import com.example.espreitar.repository.UsuarioRepository;
import com.example.espreitar.service.AvaliacaoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AvaliacaoServiceTest {

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private MidiaRepository midiaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private AvaliacaoService avaliacaoService;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);

        avaliacaoService = new AvaliacaoService(
            avaliacaoRepository,
            midiaRepository,
            usuarioRepository
        );
    }

    @Test
    void naoDevePermitirAvaliacaoDuplicada() {
        when(avaliacaoRepository.existsByUsuarioIdAndMidiaId(1L, 2L))
            .thenReturn(true);

        RuntimeException erro = assertThrows(
            RuntimeException.class,
            () -> avaliacaoService.cadastrarAvaliacao(
                1L, 2L, new Avaliacao()
            )
        );

        assertEquals("Usuário já avaliou esta mídia!", erro.getMessage());

        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarUsuarioInexistente() {
        when(avaliacaoRepository.existsByUsuarioIdAndMidiaId(1L, 2L))
            .thenReturn(false);

        when(usuarioRepository.findById(1L))
            .thenReturn(Optional.empty());

        RuntimeException erro = assertThrows(
            RuntimeException.class,
            () -> avaliacaoService.cadastrarAvaliacao(
                1L, 2L, new Avaliacao()
            )
        );

        assertEquals("Usuário não encontrado!", erro.getMessage());

        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarMidiaInexistente() {
        when(avaliacaoRepository.existsByUsuarioIdAndMidiaId(1L, 2L))
            .thenReturn(false);

        when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(
                new com.example.espreitar.model.Usuario()
            ));

        when(midiaRepository.findById(2L))
            .thenReturn(Optional.empty());

        RuntimeException erro = assertThrows(
            RuntimeException.class,
            () -> avaliacaoService.cadastrarAvaliacao(
                1L, 2L, new Avaliacao()
            )
        );

        assertEquals("Mídia não encontrada!", erro.getMessage());

        verify(avaliacaoRepository, never()).save(any());
    }
}
