package com.example.espreitar.repository;

import com.example.espreitar.model.Denuncia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DenunciaRepository extends JpaRepository<Denuncia, Long> {
    List<Denuncia> findAllByOrderByDataCriacaoDesc();

    long countByAvaliacaoId(Long avaliacaoId);
   }