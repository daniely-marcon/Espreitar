package com.example.espreitar.repository;

import com.example.espreitar.model.Midia;
import com.example.espreitar.model.TipoMidia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
