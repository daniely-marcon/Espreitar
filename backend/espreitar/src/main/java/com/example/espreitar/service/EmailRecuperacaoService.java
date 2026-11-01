package com.example.espreitar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EmailRecuperacaoService {
    private static final Logger logger = LoggerFactory.getLogger(EmailRecuperacaoService.class);
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${app.mail.from:}")
    private String remetente;

    @Value("${app.public-url:http://localhost:8081}")
    private String urlPublica;

    public EmailRecuperacaoService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public boolean estaConfigurado() {
        return host != null && !host.isBlank()
                && remetente != null && !remetente.isBlank()
                && mailSenderProvider.getIfAvailable() != null;
    }

    public void enviarRedefinicao(String email, String nome, String token) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (host == null || host.isBlank() || remetente == null || remetente.isBlank() || mailSender == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A recuperação por e-mail ainda não está configurada.");
        }
        String link = UriComponentsBuilder.fromUriString(urlPublica)
                .path("/Espreitar/html/redefinir-senha.html")
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(email);
        mensagem.setSubject("Redefinição de senha do Espreitar");
        mensagem.setText("Olá, " + nome + "!\n\n"
                + "Recebemos um pedido para redefinir a senha da sua conta Espreitar.\n"
                + "Use este link em até 30 minutos:\n" + link + "\n\n"
                + "Se você não solicitou a redefinição, ignore esta mensagem.");
        try {
            mailSender.send(mensagem);
        } catch (MailException ex) {
            logger.error("Falha ao enviar e-mail de redefinição de senha.", ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o e-mail de recuperação. Tente novamente mais tarde.", ex);
        }
    }
}
