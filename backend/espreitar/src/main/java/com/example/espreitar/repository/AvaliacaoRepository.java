package com.example.espreitar.repository;

import com.example.espreitar.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    /** Resumo usado para recalcular nota_media e total_avaliacoes de uma midia. */
    interface ResumoNotas {
        Double getMedia();

        Long getTotal();
    }

    boolean existsByUsuarioIdAndMidiaId(Long usuarioId, Long midiaId);

    Optional<Avaliacao> findByUsuarioIdAndMidiaId(Long usuarioId, Long midiaId);

    List<Avaliacao> findByMidiaIdOrderByDataCriacaoDesc(Long midiaId);

    List<Avaliacao> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);

    @Query("select avg(a.nota) as media, count(a) as total from Avaliacao a where a.midia.id = :midiaId")
    ResumoNotas resumirNotas(@Param("midiaId") Long midiaId);
}
