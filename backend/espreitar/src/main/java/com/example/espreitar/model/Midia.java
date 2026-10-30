package com.example.espreitar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "midia", uniqueConstraints = @UniqueConstraint(name = "uk_midia_externo", columnNames = {"id_externo", "tipo"}))
@Getter
@Setter
@NoArgsConstructor
public class Midia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Codigo da obra na API externa. Nulo em obra cadastrada manualmente. */
    @Column(name = "id_externo", length = 50)
    private String idExterno;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private TipoMidia tipo;

    @Column(nullable = false, length = 255)
    private String titulo;

    private Integer ano;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "TEXT")
    private String sinopse;

    /** URL do cartaz. Vem da API; nulo em obra manual. */
    @Column(length = 500)
    private String cartaz;

    /** Derivado das avaliacoes, mas gravado por desempenho. */
    @Column(name = "nota_media", nullable = false)
    private Double notaMedia = 0.0;

    @Column(name = "total_avaliacoes", nullable = false)
    private Integer totalAvaliacoes = 0;

    @Column(name = "nota_media_externa", nullable = false)
    private Double notaMediaExterna = 0.0;

    @Column(name = "total_avaliacoes_externas", nullable = false)
    private Integer totalAvaliacoesExternas = 0;
}
