package com.example.espreitar.service;


import com.example.espreitar.model.Midia;
import com.example.espreitar.model.TipoMidia;
import com.example.espreitar.model.Usuario;
import com.example.espreitar.repository.MidiaRepository;
import jakarta.persistence.EnumType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.example.espreitar.model.TipoMidia.FILME;

@Service
@RequiredArgsConstructor
public class MidiaService {
    private final MidiaRepository midiaRepository;

    public Midia cadastrarMidia(Midia midia){
        if (midiaRepository. existsByTituloIgnoreCaseAndAnoAndTipo(midia.getTitulo(), midia.getAno(), midia.getTipo())){
            throw new RuntimeException("Filme já cadastrado!");
        }else{
            return midiaRepository.save(midia);
        }
    }

    public Midia buscarMidiaTituloAno(String titulo, Integer ano, TipoMidia tipo) {
        Optional<Midia> resultado = midiaRepository.findByTituloIgnoreCaseAndAnoAndTipo(titulo, ano, tipo);

        if (resultado.isPresent()) {
            return resultado.get();
        } else {
            if (tipo != null && tipo.name().equals("FILME")) {
                throw new RuntimeException("Filme não encontrado!");
            } else {
                throw new RuntimeException("Série não encontrada!");
            }
        }
    }

    public List<Midia> listarTodas() {
        return midiaRepository.findAll();
    }

    public void excluirMidia(Long id) {
        if (!midiaRepository.existsById(id)) {
            throw new RuntimeException("Mídia não encontrada para exclusão!");
        }
        midiaRepository.deleteById(id);
    }

    public List<Midia> filtrarMidias(TipoMidia tipo, Double notaMinima) {
        return midiaRepository.filtrarPorTipoENota(tipo, notaMinima);
    }

    public Midia atualizarMidia(Long id, Midia dadosAtualizados) {
        Midia midiaExistente = midiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mídia não encontrada para atualização!"));

        midiaExistente.setTitulo(dadosAtualizados.getTitulo());
        midiaExistente.setAno(dadosAtualizados.getAno());
        midiaExistente.setTipo(dadosAtualizados.getTipo());
        midiaExistente.setSinopse(dadosAtualizados.getSinopse());
        midiaExistente.setCartaz(dadosAtualizados.getCartaz());


        return midiaRepository.save(midiaExistente);
    }
}
