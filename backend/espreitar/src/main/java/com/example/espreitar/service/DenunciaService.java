package com.example.espreitar.service;

import com.example.espreitar.model.Avaliacao;
import com.example.espreitar.model.Denuncia;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.model.dto.DenunciaResposta;
import com.example.espreitar.repository.AvaliacaoRepository;
import com.example.espreitar.repository.DenunciaRepository;
import com.example.espreitar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DenunciaService {
    private final DenunciaRepository denunciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoRepository avaliacaoRepository;

    @Transactional
    public DenunciaResposta criarDenuncia(String email, Long avaliacaoId) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada."));
        if (avaliacao.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível denunciar sua própria avaliação.");
        }
        if (denunciaRepository.existsByUsuarioIdAndAvaliacaoId(usuario.getId(), avaliacaoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já denunciou esta avaliação.");
        }
        Denuncia denuncia = new Denuncia();
        denuncia.setUsuario(usuario);
        denuncia.setAvaliacao(avaliacao);
        return resposta(denunciaRepository.save(denuncia));
    }

    @Transactional(readOnly = true)
    public List<DenunciaResposta> listarTodas() {
        return denunciaRepository.findAllByOrderByDataCriacaoDesc().stream()
                .map(this::resposta).toList();
    }

    @Transactional
    public void excluirDenuncia(Long id) {
        Denuncia denuncia = denunciaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Denúncia não encontrada."));
        denunciaRepository.delete(denuncia);
    }

    @Transactional
    public void limparDenunciasDaAvaliacao(Long avaliacaoId) {
        denunciaRepository.deleteByAvaliacaoId(avaliacaoId);
    }

    private DenunciaResposta resposta(Denuncia d) {
        return new DenunciaResposta(d.getId(), d.getAvaliacao().getId(),
                d.getAvaliacao().getComentario(), d.getAvaliacao().getUsuario().getNome(),
                d.getAvaliacao().getMidia().getTitulo(), d.getUsuario().getNome(), d.getDataCriacao());
    }
}
