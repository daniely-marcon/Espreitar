package com.example.espreitar.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Hash BCrypt. Nula em conta Google. */
    @Column(length = 255)
    private String senha;

    // @JdbcTypeCode forca VARCHAR e evita divergencia com o ddl-auto=validate
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Provedor provedor = Provedor.LOCAL;

    /** Campo "sub" do token do Google. Nulo em conta manual. */
    @Column(name = "google_id", unique = true, length = 50)
    private String googleId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Perfil perfil = Perfil.USER;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "foto_perfil", columnDefinition = "TEXT")
    private String fotoPerfil;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    /** Atributo multivalorado do DER (tabela usuario_notificacao). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "usuario_notificacao", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "mensagem", nullable = false, length = 255)
    private List<String> notificacoes = new ArrayList<>();

    public void adicionarNotificacao(String mensagem) {
        if (!notificacoes.contains(mensagem)) {
            notificacoes.add(mensagem);
        }
    }

    @PrePersist
    void aoCriar() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }
}
