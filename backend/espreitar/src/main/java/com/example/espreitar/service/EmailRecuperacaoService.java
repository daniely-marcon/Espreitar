package com.example.espreitar.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EmailRecuperacaoService {
    private static final Logger logger = LoggerFactory.getLogger(EmailRecuperacaoService.class);
    private static final String ENDPOINT_BREVO = "https://api.brevo.com/v3/smtp/email";
    private final RestClient restClient;
    private final String chaveApi;
    private final String remetente;
    private final String urlPublica;

    @Autowired
    public EmailRecuperacaoService(
            @Value("${app.mail.brevo-api-key:}") String chaveApi,
            @Value("${app.mail.from:}") String remetente,
            @Value("${app.public-url:http://localhost:8081}") String urlPublica) {
        this(RestClient.create(), chaveApi, remetente, urlPublica);
    }

    EmailRecuperacaoService(RestClient restClient, String chaveApi, String remetente, String urlPublica) {
        this.restClient = restClient;
        this.chaveApi = chaveApi;
        this.remetente = remetente;
        this.urlPublica = urlPublica;
    }

    public boolean estaConfigurado() {
        return chaveApi != null && !chaveApi.isBlank()
                && remetente != null && !remetente.isBlank();
    }

    public void enviarRedefinicao(String email, String nome, String token) {
        if (!estaConfigurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A recuperação por e-mail ainda não está configurada.");
        }
        String link = UriComponentsBuilder.fromUriString(urlPublica)
                .path("/Espreitar/html/redefinir-senha.html")
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
        MensagemEmail mensagem = new MensagemEmail(
                new Remetente(remetente, "Espreitar"),
                List.of(new Destinatario(email, nome)),
                "Redefinição de senha do Espreitar",
                "Olá, " + nome + "!\n\n"
                + "Recebemos um pedido para redefinir a senha da sua conta Espreitar.\n"
                + "Use este link em até 30 minutos:\n" + link + "\n\n"
                + "Se você não solicitou a redefinição, ignore esta mensagem.");
        try {
            restClient.post()
                    .uri(ENDPOINT_BREVO)
                    .header("api-key", chaveApi)
                    .body(mensagem)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            logger.error("Falha ao enviar e-mail de redefinição pela API Brevo.", ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o e-mail de recuperação. Tente novamente mais tarde.", ex);
        }
    }

    private record Remetente(String email, String name) {
    }

    private record Destinatario(String email, String name) {
    }

    private record MensagemEmail(
            Remetente sender,
            List<Destinatario> to,
            String subject,
            String textContent) {
    }
}
