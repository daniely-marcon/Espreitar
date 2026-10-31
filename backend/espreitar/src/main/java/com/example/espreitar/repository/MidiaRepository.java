package com.example.espreitar.repository;

import com.example.espreitar.model.Midia;
import com.example.espreitar.model.TipoMidia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MidiaRepository extends JpaRepository<Midia, Long> {

    /** Reencontra uma obra que veio da API. */
    Optional<Midia> findByIdExternoAndTipo(String idExterno, TipoMidia tipo);

    /** Busca local por titulo (obras manuais e obras ja avaliadas). */
    List<Midia> findByTituloContainingIgnoreCase(String titulo);

    /** Checagem de duplicata antes de cadastrar uma obra manualmente. */
    boolean existsByTituloIgnoreCaseAndAnoAndTipo(String titulo, Integer ano, TipoMidia tipo);

    /** Destaques da pagina principal (RF09). */
    Page<Midia> findAllByOrderByNotaMediaDescTotalAvaliacoesDesc(Pageable pageable);

    Optional<Midia> findByTituloIgnoreCaseAndAnoAndTipo(String titulo, Integer ano, TipoMidia tipo);

    @Query("SELECT m FROM Midia m WHERE (:tipo IS NULL OR m.tipo = :tipo) AND (:notaMinima IS NULL OR m.notaMedia >= :notaMinima)")
    List<Midia> filtrarPorTipoENota(
            @Param("tipo") TipoMidia tipo,
            @Param("notaMinima") Double notaMinima
    );

    @Query("""
            SELECT m FROM Midia m
            WHERE (:tipo IS NULL OR m.tipo = :tipo)
              AND (:notaMinima IS NULL OR
                   CASE WHEN m.totalAvaliacoes > 0 THEN m.notaMedia ELSE m.notaMediaExterna END >= :notaMinima)
              AND (:titulo IS NULL OR LOWER(m.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')))
            ORDER BY CASE WHEN m.totalAvaliacoes > 0 THEN m.notaMedia ELSE m.notaMediaExterna END DESC,
                     CASE WHEN m.totalAvaliacoes > 0 THEN m.totalAvaliacoes ELSE m.totalAvaliacoesExternas END DESC,
                     m.titulo ASC
            """)
    List<Midia> buscarCatalogo(
            @Param("titulo") String titulo,
            @Param("tipo") TipoMidia tipo,
            @Param("notaMinima") Double notaMinima
    );
}
