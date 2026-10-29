package com.example.espreitar.service;

import com.example.espreitar.model.Avaliacao;
import com.example.espreitar.model.Denuncia;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.repository.AvaliacaoRepository;
import com.example.espreitar.repository.DenunciaRepository;
import com.example.espreitar.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DenunciaService {

    private final DenunciaRepository denunciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoRepository avaliacaoRepository;

    public Denuncia criarDenuncia(Long usuarioId, Long avaliacaoId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada!"));

        Denuncia denuncia = new Denuncia();
        denuncia.setUsuario(usuario);
        denuncia.setAvaliacao(avaliacao);

        Denuncia denunciaSalva = denunciaRepository.save(denuncia);


        long totalDenuncias = denunciaRepository.countByAvaliacaoId(avaliacaoId);
        if (totalDenuncias >= 5) {
            avaliacaoRepository.delete(avaliacao);
        }

        return denunciaSalva;
    }

    public List<Denuncia> listarTodas() {
        return denunciaRepository.findAllByOrderByDataCriacaoDesc();
    }

    public void excluirDenuncia(Long id) {
        Denuncia denuncia = denunciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Denúncia não encontrada!"));
        denunciaRepository.delete(denuncia);
    }
}