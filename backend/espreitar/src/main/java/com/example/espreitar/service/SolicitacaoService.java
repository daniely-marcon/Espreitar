package com.example.espreitar.service;

import com.example.espreitar.model.Avaliacao;
import com.example.espreitar.model.Midia;
import com.example.espreitar.model.Solicitacao;
import com.example.espreitar.model.StatusSolicitacao;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.repository.AvaliacaoRepository;
import com.example.espreitar.repository.MidiaRepository;
import com.example.espreitar.repository.SolicitacaoRepository;
import com.example.espreitar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MidiaRepository midiaRepository;
    private final AvaliacaoRepository avaliacaoRepository;

    public Solicitacao criarSolicitacao(Long usuarioId, Solicitacao solicitacao) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        solicitacao.setUsuario(usuario);
        solicitacao.setStatus(StatusSolicitacao.PENDENTE);

        return solicitacaoRepository.save(solicitacao);
    }

    public List<Solicitacao> listarPorStatus(StatusSolicitacao status) {
        return solicitacaoRepository.findByStatusOrderByDataCriacaoAsc(status);
    }

    public List<Solicitacao> listarPorUsuario(Long usuarioId) {
        return solicitacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioId);
    }

    public Midia aprovarSolicitacao(Long solicitacaoId) {
        Solicitacao solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada!"));

        Midia novaMidia = new Midia();
        novaMidia.setTitulo(solicitacao.getTitulo());
        novaMidia.setAno(solicitacao.getAno());
        novaMidia.setTipo(solicitacao.getTipo());
        novaMidia.setSinopse(solicitacao.getSinopse());
        Midia midiaSalva = midiaRepository.save(novaMidia);

        if (solicitacao.getNota() != null) {
            Avaliacao avaliacao = new Avaliacao();
            avaliacao.setUsuario(solicitacao.getUsuario());
            avaliacao.setMidia(midiaSalva);
            avaliacao.setNota(solicitacao.getNota());
            avaliacao.setComentario(solicitacao.getComentario());
            avaliacao.setSpoiler(solicitacao.isSpoiler());
            avaliacaoRepository.save(avaliacao);

            midiaSalva.setNotaMedia(Double.valueOf(solicitacao.getNota()));
            midiaSalva.setTotalAvaliacoes(1);
            midiaRepository.save(midiaSalva);
        }

        Usuario usuario = solicitacao.getUsuario();
        usuario.getNotificacoes().add("Sua solicitação para a obra '" + solicitacao.getTitulo() + "' foi APROVADA e já está disponível!");
        usuarioRepository.save(usuario);

        solicitacaoRepository.delete(solicitacao);

        return midiaSalva;
    }

    public void rejeitarSolicitacao(Long solicitacaoId) {
        Solicitacao solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada!"));

        Usuario usuario = solicitacao.getUsuario();
        usuario.getNotificacoes().add("Infelizmente sua solicitação para a obra '" + solicitacao.getTitulo() + "' foi rejeitada.");
        usuarioRepository.save(usuario);

        solicitacaoRepository.delete(solicitacao);
    }
}