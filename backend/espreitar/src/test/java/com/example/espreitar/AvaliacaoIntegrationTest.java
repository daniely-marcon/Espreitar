package com.example.espreitar;

import com.example.espreitar.controller.AvaliacaoController;
import com.example.espreitar.service.AvaliacaoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AvaliacaoIntegrationTest {

    private AvaliacaoService avaliacaoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        avaliacaoService = mock(AvaliacaoService.class);
        AvaliacaoController avaliacaoController = new AvaliacaoController(avaliacaoService);
        mockMvc = MockMvcBuilders.standaloneSetup(avaliacaoController).build();
    }

    @Test
    void deveListarAvaliacoesDaMidia() throws Exception {
        when(avaliacaoService.listarDaMidia(eq(2L), isNull(), isNull()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/avaliacoes/midia/2"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(avaliacaoService).listarDaMidia(eq(2L), isNull(), isNull());
    }
}