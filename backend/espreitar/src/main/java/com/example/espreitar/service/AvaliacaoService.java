package com.example.espreitar.service;

import com.example.espreitar.model.Avaliacao;
import com.example.espreitar.model.Midia;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.model.dto.AvaliacaoEntrada;
import com.example.espreitar.model.dto.AvaliacaoResposta;
import com.example.espreitar.repository.AvaliacaoRepository;
import com.example.espreitar.repository.MidiaRepository;
import com.example.espreitar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {
    private final AvaliacaoRepository avaliacaoRepository;
    private final MidiaRepository midiaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public AvaliacaoResposta cadastrar(String email, Long midiaId, AvaliacaoEntrada dados) {
        Usuario usuario = usuarioPorEmail(email);
        Midia midia = midiaRepository.findById(midiaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mídia não encontrada."));
        if (avaliacaoRepository.existsByUsuarioIdAndMidiaId(usuario.getId(), midiaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já avaliou esta mídia.");
        }
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setUsuario(usuario);
        avaliacao.setMidia(midia);
        aplicarDados(avaliacao, dados);
        Avaliacao salva = avaliacaoRepository.save(avaliacao);
        recalcularMediaMidia(midia);
        return resposta(salva);
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResposta> listarDaMidia(Long midiaId, String email, Boolean spoiler) {
        if (!midiaRepository.existsById(midiaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mídia não encontrada.");
        }
        List<Avaliacao> avaliacoes = spoiler == null
                ? new ArrayList<>(avaliacaoRepository.findByMidiaIdOrderByDataCriacaoDesc(midiaId))
                : new ArrayList<>(avaliacaoRepository.findByMidiaIdAndSpoilerOrderByDataCriacaoDesc(midiaId, spoiler));
        if (email != null) {
            String emailNormalizado = email.toLowerCase(Locale.ROOT);
            avaliacoes.sort((a, b) -> Boolean.compare(
                    !a.getUsuario().getEmail().equals(emailNormalizado),
                    !b.getUsuario().getEmail().equals(emailNormalizado)));
        }
        return avaliacoes.stream().map(this::resposta).toList();
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResposta> listarDoUsuario(String email) {
        Usuario usuario = usuarioPorEmail(email);
        return avaliacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuario.getId())
                .stream().map(this::resposta).toList();
    }

    @Transactional
    public AvaliacaoResposta atualizar(String email, boolean administrador, Long id, AvaliacaoEntrada dados) {
        Avaliacao existente = buscarAvaliacao(id);
        exigirDonoOuAdmin(existente, email, administrador);
        aplicarDados(existente, dados);
        recalcularMediaMidia(existente.getMidia());
        return resposta(existente);
    }

    @Transactional
    public void excluir(String email, boolean administrador, Long id) {
        Avaliacao avaliacao = buscarAvaliacao(id);
        exigirDonoOuAdmin(avaliacao, email, administrador);
        excluir(avaliacao);
    }

    @Transactional
    public void excluirAutomaticamente(Long id) {
        excluir(buscarAvaliacao(id));
    }

    private void excluir(Avaliacao avaliacao) {
        Midia midia = avaliacao.getMidia();
        avaliacaoRepository.delete(avaliacao);
        recalcularMediaMidia(midia);
    }

    private void exigirDonoOuAdmin(Avaliacao avaliacao, String email, boolean administrador) {
        if (!administrador && !avaliacao.getUsuario().getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você só pode alterar sua própria avaliação.");
        }
    }

    private Avaliacao buscarAvaliacao(Long id) {
        return avaliacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada."));
    }

    private Usuario usuarioPorEmail(String email) {
        String emailNormalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private void aplicarDados(Avaliacao avaliacao, AvaliacaoEntrada dados) {
        if (dados.nota() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nota é obrigatória.");
        }
        avaliacao.setNota(dados.nota());
        avaliacao.setComentario(dados.comentario());
        avaliacao.setSpoiler(dados.spoiler());
    }

    private AvaliacaoResposta resposta(Avaliacao a) {
        Usuario usuario = a.getUsuario();
        Midia midia = a.getMidia();
        return new AvaliacaoResposta(a.getId(), usuario.getId(), usuario.getNome(),
                usuario.getFotoPerfil(), midia.getId(), midia.getTitulo(), midia.getCartaz(), a.getNota(),
                a.getComentario(), a.isSpoiler(), a.getDataCriacao());
    }

    private void recalcularMediaMidia(Midia midia) {
        AvaliacaoRepository.ResumoNotas resumo = avaliacaoRepository.resumirNotas(midia.getId());
        if (resumo != null && resumo.getTotal() > 0) {
            midia.setNotaMedia(resumo.getMedia());
            midia.setTotalAvaliacoes(resumo.getTotal().intValue());
        } else {
            midia.setNotaMedia(0.0);
            midia.setTotalAvaliacoes(0);
        }
        midiaRepository.save(midia);
    }
}
