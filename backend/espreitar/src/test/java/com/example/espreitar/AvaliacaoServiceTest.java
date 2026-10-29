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
import static org.mockito.ArgumentMatchers.any;
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
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setNota(4);
        when(avaliacaoRepository.existsByUsuarioIdAndMidiaId(1L, 2L))
            .thenReturn(true);

        RuntimeException erro = assertThrows(
            RuntimeException.class,
            () -> avaliacaoService.cadastrarAvaliacao(
                1L, 2L,  avaliacao
            )
        );

        assertEquals("Usuário já avaliou esta mídia!", erro.getMessage());

        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarUsuarioInexistente() {
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setNota(4);
        when(avaliacaoRepository.existsByUsuarioIdAndMidiaId(1L, 2L))
            .thenReturn(false);

        when(usuarioRepository.findById(1L))
            .thenReturn(Optional.empty());

        RuntimeException erro = assertThrows(
            RuntimeException.class,
            () -> avaliacaoService.cadastrarAvaliacao(
                1L, 2L,  avaliacao
            )
        );

        assertEquals("Usuário não encontrado!", erro.getMessage());

        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarMidiaInexistente() {
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setNota(4);
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
                1L, 2L, avaliacao
            )
        );

        assertEquals("Mídia não encontrada!", erro.getMessage());

        verify(avaliacaoRepository, never()).save(any());
    }
    @Test
    void deveRejeitarNotaMaiorQueCinco() {
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setNota(6);

       IllegalArgumentException erro = assertThrows(
IllegalArgumentException.class,
          () -> avaliacaoService.cadastrarAvaliacao(1L, 2L, avaliacao)
        );

        assertEquals("A nota deve estar entre 0 e 5!", erro.getMessage());
        verify(avaliacaoRepository, never()).save(any());
    }
    @Test
    void deveRejeitarNotaNegativa() {
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setNota(-1);

    IllegalArgumentException erro = assertThrows(
IllegalArgumentException.class,
             () -> avaliacaoService.cadastrarAvaliacao(1L, 2L, avaliacao)
    );

    assertEquals("A nota deve estar entre 0 e 5!", erro.getMessage());
    verify(avaliacaoRepository, never()).save(any());
}

@Test
void deveRejeitarNotaInvalidaAoAtualizar() {
    Avaliacao novosDados = new Avaliacao();
    novosDados.setNota(7);

    IllegalArgumentException erro = assertThrows(
IllegalArgumentException.class,
            () -> avaliacaoService.atualizarAvaliacao(1L, novosDados)
    );

    assertEquals("A nota deve estar entre 0 e 5!", erro.getMessage());
    verify(avaliacaoRepository, never()).save(any());
}
}
