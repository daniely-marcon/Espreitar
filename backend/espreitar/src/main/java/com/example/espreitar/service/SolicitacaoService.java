package com.example.espreitar.service;

import com.example.espreitar.model.Avaliacao;
import com.example.espreitar.model.Midia;
import com.example.espreitar.model.Perfil;
import com.example.espreitar.model.Solicitacao;
import com.example.espreitar.model.StatusSolicitacao;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.model.dto.SolicitacaoEntrada;
import com.example.espreitar.model.dto.SolicitacaoResposta;
import com.example.espreitar.model.dto.DetalheExterno;
import com.example.espreitar.repository.AvaliacaoRepository;
import com.example.espreitar.repository.MidiaRepository;
import com.example.espreitar.repository.SolicitacaoRepository;
import com.example.espreitar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SolicitacaoService {
    private final SolicitacaoRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MidiaRepository midiaRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final MidiaService midiaService;

    @Transactional
    public SolicitacaoResposta criarSolicitacao(String email, SolicitacaoEntrada dados) {
        String emailNormalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        DetalheExterno externo = midiaService.buscarDetalheExterno(dados.idExterno(), dados.tipo());
        if (midiaRepository.findByIdExternoAndTipo(externo.idExterno(), dados.tipo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa mídia já está disponível no catálogo.");
        }
        if (solicitacaoRepository.existsByUsuarioIdAndIdExternoAndTipoAndStatus(
                usuario.getId(), externo.idExterno(), dados.tipo(), StatusSolicitacao.PENDENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já pediu a inclusão dessa mídia.");
        }
        Solicitacao solicitacao = new Solicitacao();
        solicitacao.setUsuario(usuario);
        solicitacao.setTitulo(externo.titulo());
        solicitacao.setIdExterno(externo.idExterno());
        solicitacao.setAno(externo.ano());
        solicitacao.setTipo(dados.tipo());
        solicitacao.setSinopse(externo.sinopse());
        solicitacao.setCartaz(externo.cartaz());
        solicitacao.setTrailerUrl(externo.trailerUrl());
        solicitacao.setNotaMediaExterna(externo.notaMedia());
        solicitacao.setTotalAvaliacoesExternas(externo.totalAvaliacoes());
        solicitacao.setNota(dados.nota());
        solicitacao.setComentario(dados.comentario());
        solicitacao.setSpoiler(dados.spoiler());
        solicitacao.setStatus(StatusSolicitacao.PENDENTE);
        Solicitacao salva = solicitacaoRepository.save(solicitacao);
        String tituloResumido = externo.titulo().length() > 160
                ? externo.titulo().substring(0, 157) + "..." : externo.titulo();
        for (Usuario admin : usuarioRepository.findByPerfil(Perfil.ADMIN)) {
            admin.getNotificacoes().add("Novo pedido de inclusão: '" + tituloResumido + "'.");
        }
        return resposta(salva);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoResposta> listarPorStatus(StatusSolicitacao status) {
        return solicitacaoRepository.findByStatusOrderByDataCriacaoAsc(status)
                .stream().map(this::resposta).toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoResposta> listarDoUsuario(String email) {
        String emailNormalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        return solicitacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuario.getId())
                .stream().map(this::resposta).toList();
    }

    @Transactional
    public Midia aprovarSolicitacao(Long solicitacaoId) {
        Solicitacao solicitacao = buscar(solicitacaoId);
        if (midiaRepository.existsByTituloIgnoreCaseAndAnoAndTipo(
                solicitacao.getTitulo(), solicitacao.getAno(), solicitacao.getTipo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa mídia já existe no catálogo.");
        }
        if (solicitacao.getIdExterno() != null
                && midiaRepository.findByIdExternoAndTipo(solicitacao.getIdExterno(), solicitacao.getTipo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa mídia já existe no catálogo.");
        }
        Midia novaMidia = new Midia();
        novaMidia.setIdExterno(solicitacao.getIdExterno());
        novaMidia.setTitulo(solicitacao.getTitulo());
        novaMidia.setAno(solicitacao.getAno());
        novaMidia.setTipo(solicitacao.getTipo());
        novaMidia.setSinopse(solicitacao.getSinopse());
        novaMidia.setCartaz(solicitacao.getCartaz());
        novaMidia.setTrailerUrl(solicitacao.getTrailerUrl());
        novaMidia.setNotaMediaExterna(solicitacao.getNotaMediaExterna());
        novaMidia.setTotalAvaliacoesExternas(solicitacao.getTotalAvaliacoesExternas());
        Midia midiaSalva = midiaRepository.save(novaMidia);

        if (solicitacao.getNota() != null) {
            Avaliacao avaliacao = new Avaliacao();
            avaliacao.setUsuario(solicitacao.getUsuario());
            avaliacao.setMidia(midiaSalva);
            avaliacao.setNota(solicitacao.getNota());
            avaliacao.setComentario(solicitacao.getComentario());
            avaliacao.setSpoiler(solicitacao.isSpoiler());
            avaliacaoRepository.save(avaliacao);
            midiaSalva.setNotaMedia(solicitacao.getNota().doubleValue());
            midiaSalva.setTotalAvaliacoes(1);
        }
        Usuario usuario = solicitacao.getUsuario();
        usuario.getNotificacoes().add(mensagemNotificacao(solicitacao.getTitulo(), "foi aprovada e já está disponível."));
        solicitacaoRepository.delete(solicitacao);
        return midiaRepository.save(midiaSalva);
    }

    @Transactional
    public void rejeitarSolicitacao(Long solicitacaoId) {
        Solicitacao solicitacao = buscar(solicitacaoId);
        solicitacao.getUsuario().getNotificacoes().add(
                mensagemNotificacao(solicitacao.getTitulo(), "foi rejeitada."));
        solicitacaoRepository.delete(solicitacao);
    }

    private Solicitacao buscar(Long id) {
        return solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));
    }

    private String mensagemNotificacao(String titulo, String resultado) {
        String tituloResumido = titulo.length() > 160 ? titulo.substring(0, 157) + "..." : titulo;
        return "Seu pedido para '" + tituloResumido + "' " + resultado;
    }

    private SolicitacaoResposta resposta(Solicitacao s) {
        return new SolicitacaoResposta(s.getId(), s.getUsuario().getId(), s.getUsuario().getNome(),
                s.getTitulo(), s.getIdExterno(), s.getAno(), s.getTipo(), s.getSinopse(),
                s.getCartaz(), s.getTrailerUrl(), s.getNotaMediaExterna(),
                s.getTotalAvaliacoesExternas(), s.getStatus(),
                s.getNota(), s.getComentario(), s.isSpoiler(), s.getDataCriacao());
    }
}
