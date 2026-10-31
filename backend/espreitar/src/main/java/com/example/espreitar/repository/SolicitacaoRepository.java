package com.example.espreitar.repository;

import com.example.espreitar.model.Solicitacao;
import com.example.espreitar.model.StatusSolicitacao;
import com.example.espreitar.model.TipoMidia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    /** Fila do administrador: as mais antigas primeiro. */
    List<Solicitacao> findByStatusOrderByDataCriacaoAsc(StatusSolicitacao status);

    List<Solicitacao> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);

    boolean existsByUsuarioIdAndIdExternoAndTipoAndStatus(Long usuarioId, String idExterno,
                                                           TipoMidia tipo, StatusSolicitacao status);
}
