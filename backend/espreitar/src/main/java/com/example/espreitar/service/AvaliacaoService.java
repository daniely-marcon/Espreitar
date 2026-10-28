package com.example.espreitar.service;

import com.example.espreitar.model.Avaliacao;
import com.example.espreitar.model.Midia;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.repository.AvaliacaoRepository;
import com.example.espreitar.repository.MidiaRepository;
import com.example.espreitar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final MidiaRepository midiaRepository;
    private final UsuarioRepository usuarioRepository;

    public Avaliacao cadastrarAvaliacao(Long usuarioId, Long midiaId, Avaliacao novaAvaliacao) {
        if (avaliacaoRepository.existsByUsuarioIdAndMidiaId(usuarioId, midiaId)) {
            throw new RuntimeException("Usuário já avaliou esta mídia!");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));
        Midia midia = midiaRepository.findById(midiaId)
                .orElseThrow(() -> new RuntimeException("Mídia não encontrada!"));

        novaAvaliacao.setUsuario(usuario);
        novaAvaliacao.setMidia(midia);

        Avaliacao salva = avaliacaoRepository.save(novaAvaliacao);


        recalcularMediaMidia(midiaId);

        return salva;
    }


    public List<Avaliacao> listarAvaliacoesDaMidia(Long midiaId, Long usuarioIdLogado, Boolean spoiler) {
        List<Avaliacao> avaliacoes;


        if (spoiler != null) {
            avaliacoes = new ArrayList<>(avaliacaoRepository.findByMidiaIdAndSpoilerOrderByDataCriacaoDesc(midiaId, spoiler));
        } else {
            avaliacoes = new ArrayList<>(avaliacaoRepository.findByMidiaIdOrderByDataCriacaoDesc(midiaId));
        }


        if (usuarioIdLogado != null) {
            Optional<Avaliacao> avaliacaoUsuario = avaliacoes.stream()
                    .filter(a -> a.getUsuario().getId().equals(usuarioIdLogado))
                    .findFirst();

            if (avaliacaoUsuario.isPresent()) {
                avaliacoes.remove(avaliacaoUsuario.get());
                avaliacoes.add(0, avaliacaoUsuario.get());
            }
        }

        return avaliacoes;
    }


    public Avaliacao atualizarAvaliacao(Long id, Avaliacao novosDados) {
        Avaliacao existente = avaliacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada!"));

        existente.setNota(novosDados.getNota());
        existente.setComentario(novosDados.getComentario());
        existente.setSpoiler(novosDados.isSpoiler());

        Avaliacao salva = avaliacaoRepository.save(existente);


        recalcularMediaMidia(existente.getMidia().getId());

        return salva;
    }


    public void excluirAvaliacao(Long id) {
        Avaliacao avaliacao = avaliacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada!"));

        Long midiaId = avaliacao.getMidia().getId();
        avaliacaoRepository.delete(avaliacao);

        recalcularMediaMidia(midiaId);
    }

    private void recalcularMediaMidia(Long midiaId) {
        Midia midia = midiaRepository.findById(midiaId).orElseThrow();
        AvaliacaoRepository.ResumoNotas resumo = avaliacaoRepository.resumirNotas(midiaId);

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