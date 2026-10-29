
package com.example.espreitar;

import com.example.espreitar.controller.AvaliacaoController;
import com.example.espreitar.model.Avaliacao;
import com.example.espreitar.service.AvaliacaoService;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AvaliacaoIntegrationTest {

    private final AvaliacaoService avaliacaoService =
            mock(AvaliacaoService.class);

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(
                new AvaliacaoController(avaliacaoService)
            ).build();

    @Test
    void deveCadastrarAvaliacaoComSucesso() throws Exception {

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setNota(4);

        when(avaliacaoService.cadastrarAvaliacao(
                eq(1L), eq(2L), any(Avaliacao.class)))
                .thenReturn(avaliacao);

        mockMvc.perform(post("/avaliacoes")
                .param("usuarioId", "1")
                .param("midiaId", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nota\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value(4));

        verify(avaliacaoService).cadastrarAvaliacao(
                eq(1L), eq(2L), any(Avaliacao.class));
    }

@Test
void deveListarAvaliacoesDaMidia() throws Exception {

    when(avaliacaoService.listarAvaliacoesDaMidia(
            eq(2L), isNull(), isNull()))
            .thenReturn(java.util.Collections.emptyList());

    mockMvc.perform(get("/avaliacoes/midia/2"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));

    verify(avaliacaoService)
            .listarAvaliacoesDaMidia(2L, null, null);
}

@Test
void deveAtualizarAvaliacao() throws Exception {
    Avaliacao avaliacao = new Avaliacao();
    avaliacao.setNota(5);
    avaliacao.setComentario("Excelente filme!");

    when(avaliacaoService.atualizarAvaliacao(
            eq(1L), any(Avaliacao.class)))
            .thenReturn(avaliacao);

    mockMvc.perform(put("/avaliacoes/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "nota": 5,
                    "comentario": "Excelente filme!",
                    "spoiler": false
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nota").value(5))
            .andExpect(jsonPath("$.comentario")
                    .value("Excelente filme!"));

    verify(avaliacaoService)
            .atualizarAvaliacao(eq(1L), any(Avaliacao.class));
}

@Test
void deveExcluirAvaliacao() throws Exception {

    mockMvc.perform(delete("/avaliacoes/1"))
            .andExpect(status().isOk());

    verify(avaliacaoService, times(1))
            .excluirAvaliacao(1L);
}



}
