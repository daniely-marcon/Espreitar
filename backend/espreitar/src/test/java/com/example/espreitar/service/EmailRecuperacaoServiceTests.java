package com.example.espreitar.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmailRecuperacaoServiceTests {
    @Test
    void enviaLinkPelaApiHttpsDaBrevo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        EmailRecuperacaoService service = new EmailRecuperacaoService(
                builder.build(), "chave-de-teste", "remetente@example.com", "https://espreitar.example.com");
        servidor.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "chave-de-teste"))
                .andExpect(content().json("""
                        {
                          "sender": {"email": "remetente@example.com", "name": "Espreitar"},
                          "to": [{"email": "pessoa@example.com", "name": "Pessoa"}],
                          "subject": "Redefinição de senha do Espreitar",
                          "textContent": "Olá, Pessoa!\\n\\nRecebemos um pedido para redefinir a senha da sua conta Espreitar.\\nUse este link em até 30 minutos:\\nhttps://espreitar.example.com/Espreitar/html/redefinir-senha.html?token=token-teste\\n\\nSe você não solicitou a redefinição, ignore esta mensagem."
                        }
                        """))
                .andRespond(withSuccess());

        assertTrue(service.estaConfigurado());
        service.enviarRedefinicao("pessoa@example.com", "Pessoa", "token-teste");
        servidor.verify();
    }

    @Test
    void informaQuandoBrevoNaoEstaConfigurada() {
        EmailRecuperacaoService service = new EmailRecuperacaoService(
                RestClient.create(), "", "remetente@example.com", "http://localhost:8081");

        assertTrue(!service.estaConfigurado());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.enviarRedefinicao("pessoa@example.com", "Pessoa", "token-teste"));
    }
}
