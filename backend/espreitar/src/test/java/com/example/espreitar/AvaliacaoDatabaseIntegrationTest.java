
package com.example.espreitar;

import com.example.espreitar.model.Avaliacao;
import com.example.espreitar.model.Midia;
import com.example.espreitar.model.TipoMidia;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.repository.AvaliacaoRepository;
import com.example.espreitar.repository.MidiaRepository;
import com.example.espreitar.repository.UsuarioRepository;
import com.example.espreitar.service.AvaliacaoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AvaliacaoDatabaseIntegrationTest {

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MidiaRepository midiaRepository;

    @Autowired
    private AvaliacaoService avaliacaoService;

    @Test
    void deveConectarAoBancoDeTestes() {
        assertNotNull(avaliacaoRepository);
        assertTrue(avaliacaoRepository.count() >= 0);
    }

    @Test
    void deveSalvarAvaliacaoNoBanco() {

        // Criar usuario de teste
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario Teste");
        usuario.setEmail("teste.integracao@espreitar.com");
        usuario = usuarioRepository.save(usuario);

        // Criar midia de teste
        Midia midia = new Midia();
        midia.setTitulo("Filme de Teste");
        midia.setTipo(TipoMidia.FILME);
        midia.setAno(2026);
        midia = midiaRepository.save(midia);

        // Criar avaliacao
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setNota(5);
        avaliacao.setComentario("Excelente filme!");
        avaliacao.setSpoiler(false);

        // Chamar o servico real
        Avaliacao salva = avaliacaoService.cadastrarAvaliacao(
                usuario.getId(),
                midia.getId(),
                avaliacao
        );

        // Consultar a avaliacao no banco
        Avaliacao encontrada = avaliacaoRepository
                .findById(salva.getId())
                .orElseThrow();

        assertNotNull(encontrada.getId());
        assertEquals(5, encontrada.getNota());
        assertEquals("Excelente filme!",
                encontrada.getComentario());

        assertEquals(usuario.getId(),
                encontrada.getUsuario().getId());

        assertEquals(midia.getId(),
                encontrada.getMidia().getId());
    }

@Test
void deveImpedirAvaliacaoDuplicada() {

    // Criar usuario
    Usuario usuario = new Usuario();
    usuario.setNome("Usuario Duplicado");
    usuario.setEmail("duplicado@espreitar.com");
    usuario = usuarioRepository.save(usuario);

    // Criar filme
    Midia midia = new Midia();
    midia.setTitulo("Filme Duplicado");
    midia.setTipo(TipoMidia.FILME);
    midia = midiaRepository.save(midia);

    // Primeira avaliacao
    Avaliacao primeira = new Avaliacao();
    primeira.setNota(4);
    primeira.setComentario("Muito bom!");

    avaliacaoService.cadastrarAvaliacao(
            usuario.getId(),
            midia.getId(),
            primeira
    );

    // Segunda avaliacao da mesma midia pelo mesmo usuario
    Avaliacao segunda = new Avaliacao();
    segunda.setNota(5);
    segunda.setComentario("Outra avaliacao");

    Long usuarioId = usuario.getId();
    Long midiaId = midia.getId();

    // Deve impedir a segunda avaliacao
    assertThrows(RuntimeException.class, () ->
            avaliacaoService.cadastrarAvaliacao(
                    usuarioId,
                    midiaId,
                    segunda
            )
    );

    // Deve continuar existindo apenas uma avaliacao
    assertEquals(
            1,
            avaliacaoRepository
                    .findByMidiaIdOrderByDataCriacaoDesc(midiaId)
                    .size()
    );
}

@Test
void deveAtualizarAvaliacaoNoBanco() {

    // Criar usuario
    Usuario usuario = new Usuario();
    usuario.setNome("Usuario Atualizacao");
    usuario.setEmail("atualizacao@espreitar.com");
    usuario = usuarioRepository.save(usuario);

    // Criar filme
    Midia midia = new Midia();
    midia.setTitulo("Filme Atualizacao");
    midia.setTipo(TipoMidia.FILME);
    midia = midiaRepository.save(midia);

    // Cadastrar avaliacao original
    Avaliacao original = new Avaliacao();
    original.setNota(3);
    original.setComentario("Filme razoavel");

    Avaliacao salva = avaliacaoService.cadastrarAvaliacao(
            usuario.getId(),
            midia.getId(),
            original
    );

    // Preparar novos dados
    Avaliacao novosDados = new Avaliacao();
    novosDados.setNota(5);
    novosDados.setComentario("Excelente filme!");
    novosDados.setSpoiler(false);

    // Atualizar usando o servico real
    avaliacaoService.atualizarAvaliacao(
            salva.getId(),
            novosDados
    );

    // Consultar novamente no banco
    avaliacaoRepository.flush();

    Avaliacao atualizada = avaliacaoRepository
            .findById(salva.getId())
            .orElseThrow();

    assertEquals(5, atualizada.getNota());
    assertEquals(
            "Excelente filme!",
            atualizada.getComentario()
    );
}

@Test
void deveExcluirAvaliacaoDoBanco() {

    // Criar usuario de teste
    Usuario usuario = new Usuario();
    usuario.setNome("Usuario Exclusao");
    usuario.setEmail("exclusao@espreitar.com");
    usuario = usuarioRepository.save(usuario);

    // Criar filme de teste
    Midia midia = new Midia();
    midia.setTitulo("Filme Exclusao");
    midia.setTipo(TipoMidia.FILME);
    midia = midiaRepository.save(midia);

    // Cadastrar avaliacao
    Avaliacao avaliacao = new Avaliacao();
    avaliacao.setNota(4);
    avaliacao.setComentario("Bom filme!");

    Avaliacao salva = avaliacaoService.cadastrarAvaliacao(
            usuario.getId(),
            midia.getId(),
            avaliacao
    );

    Long avaliacaoId = salva.getId();

    // Confirmar que a avaliacao existe
    assertTrue(avaliacaoRepository.existsById(avaliacaoId));

    // Excluir usando o servico real
    avaliacaoService.excluirAvaliacao(avaliacaoId);

    // Confirmar exclusao no banco
    avaliacaoRepository.flush();

    assertFalse(avaliacaoRepository.existsById(avaliacaoId));
}

@Test
void deveRecalcularMediaDaMidia() {

    Usuario usuario = new Usuario();
    usuario.setNome("Usuario Media");
    usuario.setEmail("media@espreitar.com");
    usuario = usuarioRepository.save(usuario);

    Midia midia = new Midia();
    midia.setTitulo("Filme Media");
    midia.setTipo(TipoMidia.FILME);
    midia = midiaRepository.save(midia);

    Avaliacao avaliacao = new Avaliacao();
    avaliacao.setNota(4);
    avaliacao.setComentario("Muito bom!");

    avaliacaoService.cadastrarAvaliacao(
            usuario.getId(),
            midia.getId(),
            avaliacao
    );

    // Forcar sincronizacao com o banco
    avaliacaoRepository.flush();

    // Consultar os dados atualizados
    Midia atualizada = midiaRepository
            .findById(midia.getId())
            .orElseThrow();

    assertEquals(4.0, atualizada.getNotaMedia(), 0.001);
    assertEquals(1, atualizada.getTotalAvaliacoes());
}

}
