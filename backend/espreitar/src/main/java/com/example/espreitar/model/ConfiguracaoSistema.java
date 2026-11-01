package com.example.espreitar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracao_sistema")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracaoSistema {
    @Id
    @Column(length = 80)
    private String chave;

    @Column(nullable = false, length = 255)
    private String valor;
}
