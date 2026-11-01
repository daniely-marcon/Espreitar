package com.example.espreitar.service;

import com.example.espreitar.model.ConfiguracaoSistema;
import com.example.espreitar.model.dto.LimiteDenuncias;
import com.example.espreitar.repository.ConfiguracaoSistemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfiguracaoSistemaService {
    private static final String CHAVE_LIMITE_DENUNCIAS = "limite_denuncias";
    private final ConfiguracaoSistemaRepository configuracoes;

    @Transactional(readOnly = true)
    public LimiteDenuncias buscarLimiteDenuncias() {
        int limite = configuracoes.findById(CHAVE_LIMITE_DENUNCIAS)
                .map(configuracao -> Integer.parseInt(configuracao.getValor()))
                .orElse(5);
        return new LimiteDenuncias(limite);
    }

    @Transactional
    public LimiteDenuncias atualizarLimiteDenuncias(LimiteDenuncias dados) {
        ConfiguracaoSistema configuracao = configuracoes.findById(CHAVE_LIMITE_DENUNCIAS)
                .orElseGet(ConfiguracaoSistema::new);
        configuracao.setChave(CHAVE_LIMITE_DENUNCIAS);
        configuracao.setValor(Integer.toString(dados.limiteDenuncias()));
        configuracoes.save(configuracao);
        return dados;
    }
}
