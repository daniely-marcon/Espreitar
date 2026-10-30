package com.example.espreitar;

import com.example.espreitar.model.Perfil;
import com.example.espreitar.model.Midia;
import com.example.espreitar.model.TipoMidia;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.model.dto.AvaliacaoEntrada;
import com.example.espreitar.model.dto.CadastroUsuario;
import com.example.espreitar.service.AutenticacaoAplicacaoService;
import com.example.espreitar.service.AvaliacaoService;
import com.example.espreitar.service.DenunciaService;
import com.example.espreitar.service.MidiaService;
import com.example.espreitar.repository.AvaliacaoRepository;
import com.example.espreitar.repository.MidiaRepository;
import com.example.espreitar.repository.UsuarioRepository;
import com.example.espreitar.service.AutenticacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EspreitarApplicationTests {

	@Autowired
	private AutenticacaoService autenticacaoService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private MidiaRepository midiaRepository;

	@Autowired
	private AvaliacaoRepository avaliacaoRepository;

	@Autowired
	private AutenticacaoAplicacaoService autenticacaoAplicacaoService;

	@Autowired
	private AvaliacaoService avaliacaoService;

	@Autowired
	private DenunciaService denunciaService;

	@Autowired
	private MidiaService midiaService;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void carregaUsuarioComPerfilCorreto() {
		Usuario usuario = new Usuario();
		usuario.setNome("Teste de autenticação");
		usuario.setEmail("autenticacao@example.com");
		usuario.setSenha("$2a$10$hash-de-teste");
		usuario.setPerfil(Perfil.ADMIN);
		usuarioRepository.saveAndFlush(usuario);

		UserDetails detalhes = autenticacaoService.loadUserByUsername(usuario.getEmail());

		assertEquals(usuario.getEmail(), detalhes.getUsername());
		assertEquals(usuario.getSenha(), detalhes.getPassword());
		assertEquals("ROLE_ADMIN", detalhes.getAuthorities().iterator().next().getAuthority());
	}

	@Test
	void usuarioInexistenteLancaExcecaoDeAutenticacao() {
		assertThrows(
				UsernameNotFoundException.class,
				() -> autenticacaoService.loadUserByUsername("inexistente@example.com")
		);
	}

	@Test
	void cadastroELoginLocalEmitemToken() {
		var cadastro = autenticacaoAplicacaoService.cadastrar(
				new CadastroUsuario("Pessoa de teste", "pessoa.teste@example.com", "SenhaForte123", null));

		assertEquals("pessoa.teste@example.com", cadastro.usuario().email());
		assertEquals(3, cadastro.token().split("\\.").length);
		assertEquals(cadastro.usuario().email(),
				autenticacaoAplicacaoService.login("PESSOA.TESTE@example.com", "SenhaForte123").usuario().email());
	}

	@Test
	void homeEConsultaDoCatalogoSaoPublicas() throws Exception {
		mockMvc.perform(get("/Espreitar/html/index.html"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/midias"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void administradorPodeConsultarResumoDeUsuariosSemDadosSecretos() throws Exception {
		Usuario usuario = usuario("resumo.admin@example.com");
		Midia midia = midiaRepository.save(midia("Obra para resumo"));
		avaliacaoService.cadastrar(usuario.getEmail(), midia.getId(),
				new AvaliacaoEntrada(5, "Gostei", false));

		mockMvc.perform(get("/usuarios/admin"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.email == 'resumo.admin@example.com')].totalAvaliacoes").value(1))
				.andExpect(jsonPath("$[?(@.email == 'resumo.admin@example.com')].senha").doesNotExist())
				.andExpect(jsonPath("$[?(@.email == 'resumo.admin@example.com')].googleId").doesNotExist());
	}

	@Test
	void painelAdminDeUsuariosExigeAutenticacao() throws Exception {
		mockMvc.perform(get("/usuarios/admin"))
				.andExpect(status().isForbidden());
	}

	@Test
	void avaliacaoDoUsuarioAparecePrimeiroEAtualizaMedia() {
		Usuario primeiraPessoa = usuario("primeira.avaliacao@example.com");
		Usuario segundaPessoa = usuario("segunda.avaliacao@example.com");
		Midia midia = midiaRepository.save(midia("Obra de teste"));
		midia.setCartaz("https://image.tmdb.org/t/p/w500/teste.jpg");
		midiaRepository.save(midia);

		avaliacaoService.cadastrar(primeiraPessoa.getEmail(), midia.getId(),
				new AvaliacaoEntrada(2, "Primeiro comentário", false));
		var avaliacaoDaPessoaLogada = avaliacaoService.cadastrar(segundaPessoa.getEmail(), midia.getId(),
				new AvaliacaoEntrada(4, "Minha avaliação", true));

		var lista = avaliacaoService.listarDaMidia(midia.getId(), segundaPessoa.getEmail(), null);
		assertEquals(avaliacaoDaPessoaLogada.id(), lista.get(0).id());
		assertEquals(midia.getCartaz(), lista.get(0).cartaz());
		assertEquals(3.0, midiaRepository.findById(midia.getId()).orElseThrow().getNotaMedia(), 0.0001);
	}

	@Test
	void umaPessoaNaoPodeDenunciarDuasVezesEReviewContinuaParaModeracao() {
		Usuario autor = usuario("autor.denuncia@example.com");
		Usuario denunciante = usuario("denunciante@example.com");
		Midia midia = midiaRepository.save(midia("Obra denunciada"));
		var review = avaliacaoService.cadastrar(autor.getEmail(), midia.getId(),
				new AvaliacaoEntrada(1, "Comentário de teste", false));

		denunciaService.criarDenuncia(denunciante.getEmail(), review.id());
		assertThrows(org.springframework.web.server.ResponseStatusException.class,
				() -> denunciaService.criarDenuncia(denunciante.getEmail(), review.id()));
		assertTrue(avaliacaoRepository.existsById(review.id()));
	}

	@Test
	void cadastroHttpEmiteRespostaSemExporSenha() throws Exception {
		mockMvc.perform(post("/auth/registro")
						.contentType("application/json")
						.content("""
								{"nome":"Conta HTTP","email":"http.teste@example.com","senha":"SenhaForte123"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.usuario.senha").doesNotExist());
	}

	@Test
	void cadastroDaMidiaPersisteNotaEQuantidadeDeVotosDoTmdb() {
		Midia midia = midia("Obra com nota externa");
		midia.setNotaMediaExterna(4.35);
		midia.setTotalAvaliacoesExternas(12500);

		Midia salva = midiaService.cadastrarMidia(midia);
		Midia recarregada = midiaRepository.findById(salva.getId()).orElseThrow();

		assertEquals(4.35, recarregada.getNotaMediaExterna(), 0.0001);
		assertEquals(12500, recarregada.getTotalAvaliacoesExternas());
	}

	private Usuario usuario(String email) {
		Usuario usuario = new Usuario();
		usuario.setNome("Usuário de teste");
		usuario.setEmail(email);
		usuario.setSenha("hash de teste");
		usuario.setPerfil(Perfil.USER);
		return usuarioRepository.saveAndFlush(usuario);
	}

	private Midia midia(String titulo) {
		Midia midia = new Midia();
		midia.setTitulo(titulo);
		midia.setTipo(TipoMidia.FILME);
		midia.setIdExterno("test-" + titulo.replace(" ", "-"));
		return midia;
	}
}
