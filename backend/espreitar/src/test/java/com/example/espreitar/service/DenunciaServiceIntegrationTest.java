package com.example.espreitar.service;

import com.example.espreitar.model.Avaliacao;
import com.example.espreitar.model.Midia;
import com.example.espreitar.model.MotivoDenuncia;
import com.example.espreitar.model.TipoMidia;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.model.dto.DenunciaResposta;
import com.example.espreitar.repository.AvaliacaoRepository;
import com.example.espreitar.repository.DenunciaRepository;
import com.example.espreitar.repository.MidiaRepository;
import com.example.espreitar.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:denuncia-integration;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
class DenunciaServiceIntegrationTest {
    @Autowired
    private DenunciaService denunciaService;

    @Autowired
    private DenunciaRepository denunciaRepository;

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private MidiaRepository midiaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void persisteDenunciaComRelacionamentosNoBanco() {
        Usuario autor = salvarUsuario("autor");
        Usuario denunciante = salvarUsuario("denunciante");
        Avaliacao avaliacao = salvarAvaliacao(autor);

        DenunciaResposta denuncia = denunciaService.criarDenuncia(
            denunciante.getEmail(), avaliacao.getId(), MotivoDenuncia.SPOILER);

        assertNotNull(denuncia.dataCriacao());
        assertEquals(avaliacao.getId(), denuncia.avaliacaoId());
        assertEquals("Avaliação de teste", denuncia.comentario());
        assertEquals("Usuário autor", denuncia.autor());
        assertEquals("Mídia de teste", denuncia.tituloMidia());
        assertEquals("Usuário denunciante", denuncia.denunciante());
        assertEquals(MotivoDenuncia.SPOILER, denuncia.motivo());
        assertEquals(1, denunciaRepository.countByAvaliacaoId(avaliacao.getId()));
    }

    @Test
        void impedeDenunciaDuplicadaEAutoDenuncia() {
        Usuario autor = salvarUsuario("autor-regras");
        Usuario denunciante = salvarUsuario("denunciante-regras");
        Avaliacao avaliacao = salvarAvaliacao(autor);

        denunciaService.criarDenuncia(denunciante.getEmail(), avaliacao.getId(), MotivoDenuncia.OUTRO);
        ResponseStatusException duplicada = assertThrows(ResponseStatusException.class,
            () -> denunciaService.criarDenuncia(
                denunciante.getEmail(), avaliacao.getId(), MotivoDenuncia.SPOILER));
        ResponseStatusException propria = assertThrows(ResponseStatusException.class,
            () -> denunciaService.criarDenuncia(autor.getEmail(), avaliacao.getId(), MotivoDenuncia.OUTRO));

        assertEquals(HttpStatus.CONFLICT, duplicada.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, propria.getStatusCode());
        assertEquals(1, denunciaRepository.countByAvaliacaoId(avaliacao.getId()));
        assertTrue(avaliacaoRepository.findById(avaliacao.getId()).isPresent());
    }

    private Usuario salvarUsuario(String identificador) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + identificador);
        usuario.setEmail(identificador + "@example.com");
        usuario.setSenha("hash-de-teste");
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Avaliacao salvarAvaliacao(Usuario autor) {
        Midia midia = new Midia();
        midia.setTitulo("Mídia de teste");
        midia.setTipo(TipoMidia.FILME);
        midia = midiaRepository.saveAndFlush(midia);

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setUsuario(autor);
        avaliacao.setMidia(midia);
        avaliacao.setNota(4);
        avaliacao.setComentario("Avaliação de teste");
        return avaliacaoRepository.saveAndFlush(avaliacao);
    }
}