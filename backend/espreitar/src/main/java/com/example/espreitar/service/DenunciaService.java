package com.example.espreitar.service;

import com.example.espreitar.model.Avaliacao;
import com.example.espreitar.model.Denuncia;
import com.example.espreitar.model.MotivoDenuncia;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.model.dto.DenunciaResposta;
import com.example.espreitar.model.dto.LimiteDenuncias;
import com.example.espreitar.repository.AvaliacaoRepository;
import com.example.espreitar.repository.DenunciaRepository;
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
public class DenunciaService {
    private final DenunciaRepository denunciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final ConfiguracaoSistemaService configuracaoSistemaService;
    private final AvaliacaoService avaliacaoService;

    @Transactional
    public DenunciaResposta criarDenuncia(String email, Long avaliacaoId, MotivoDenuncia motivo) {
        String emailNormalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        Avaliacao avaliacao = avaliacaoRepository.findByIdForUpdate(avaliacaoId)
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
        denuncia.setMotivo(motivo == null ? MotivoDenuncia.OUTRO : motivo);
        DenunciaResposta resposta = resposta(denunciaRepository.saveAndFlush(denuncia));
        LimiteDenuncias limite = configuracaoSistemaService.buscarLimiteDenuncias();
        if (resposta.totalDenuncias() >= limite.limiteDenuncias()) {
            String titulo = avaliacao.getMidia().getTitulo();
            String tituloResumido = titulo.length() > 100 ? titulo.substring(0, 97) + "..." : titulo;
            String notificacao = "Sua avaliação foi removida automaticamente após receber "
                    + limite.limiteDenuncias() + " denúncias. Título: " + tituloResumido
                    + " [MIDIA_ID=" + avaliacao.getMidia().getId() + "]";
            avaliacao.getUsuario().adicionarNotificacao(notificacao);
            denunciaRepository.deleteByAvaliacaoId(avaliacao.getId());
            denunciaRepository.flush();
            avaliacaoService.excluirAutomaticamente(avaliacao.getId());
        }
        return resposta;
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
                d.getAvaliacao().getMidia().getTitulo(), d.getUsuario().getNome(),
                d.getMotivo(), d.getDataCriacao(),
                denunciaRepository.countByAvaliacaoId(d.getAvaliacao().getId()));
    }
}
