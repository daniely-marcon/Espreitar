package com.example.espreitar.service;


import com.example.espreitar.model.Midia;
import com.example.espreitar.model.TipoMidia;
import com.example.espreitar.model.dto.BuscaExterna;
import com.example.espreitar.model.dto.DetalheExterno;
import com.example.espreitar.repository.MidiaRepository;
import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MidiaService {
    private final MidiaRepository midiaRepository;
    private final RestClient restClient = RestClient.create();
    @Value("${omdb.api-key:}")
    private String chaveOmdb;

    public Midia cadastrarMidia(Midia midia){
        if (midia.getIdExterno() == null || midia.getIdExterno().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma obra encontrada na busca externa.");
        }
        if (midiaRepository.findByIdExternoAndTipo(midia.getIdExterno(), midia.getTipo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta obra já está cadastrada.");
        }
        return midiaRepository.save(midia);
    }

    public Midia buscarMidiaTituloAno(String titulo, Integer ano, TipoMidia tipo) {
        return midiaRepository.findByTituloIgnoreCaseAndAnoAndTipo(titulo, ano, tipo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mídia não encontrada."));
    }

    public List<Midia> listarTodas() {
        return midiaRepository.buscarCatalogo(null, null, null);
    }

    public List<Midia> buscarCatalogo(String titulo, TipoMidia tipo, Double notaMinima) {
        String termo = titulo == null || titulo.isBlank() ? null : titulo.trim();
        return midiaRepository.buscarCatalogo(termo, tipo, notaMinima);
    }

    public void excluirMidia(Long id) {
        if (!midiaRepository.existsById(id)) {
            throw new RuntimeException("Mídia não encontrada para exclusão!");
        }
        midiaRepository.deleteById(id);
    }

    public List<Midia> filtrarMidias(TipoMidia tipo, Double notaMinima) {
        return midiaRepository.filtrarPorTipoENota(tipo, notaMinima);
    }

    public Midia atualizarMidia(Long id, Midia dadosAtualizados) {
        Midia midiaExistente = midiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mídia não encontrada."));

        midiaExistente.setTitulo(dadosAtualizados.getTitulo());
        midiaExistente.setAno(dadosAtualizados.getAno());
        midiaExistente.setTipo(dadosAtualizados.getTipo());
        midiaExistente.setSinopse(dadosAtualizados.getSinopse());
        midiaExistente.setCartaz(dadosAtualizados.getCartaz());


        return midiaRepository.save(midiaExistente);
    }

    public Midia buscarPorId(Long id) {
        return midiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mídia não encontrada."));
    }

    public List<BuscaExterna> buscarExterna(String termo, TipoMidia tipo) {
        if (chaveOmdb.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Busca externa indisponível: configure OMDB_API_KEY.");
        }
        if (termo == null || termo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um título para buscar.");
        }
        String tipoOmdb = tipo == TipoMidia.SERIE ? "series" : "movie";
        JsonNode resultado = requisitarOmdb(uri -> uri
                        .scheme("https").host("www.omdbapi.com").path("/")
                        .queryParam("apikey", chaveOmdb)
                        .queryParam("s", termo.trim())
                        .queryParam("type", tipoOmdb).build());
        if (resultado == null || !"True".equals(resultado.path("Response").asText())) {
            if (resultado != null && resultado.path("Error").asText().contains("not found")) {
                return List.of();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    resultado == null ? "A API externa não retornou dados." : resultado.path("Error").asText());
        }
        var lista = resultado.path("Search");
        var encontrados = new java.util.ArrayList<BuscaExterna>();
        for (JsonNode item : lista) {
            encontrados.add(new BuscaExterna(
                    item.path("imdbID").asText(),
                    item.path("Title").asText(),
                    item.path("Year").asText(),
                    tipo.name(),
                    urlCartaz(item.path("Poster").asText(null))));
        }
        return encontrados;
    }

    public DetalheExterno buscarDetalheExterno(String idExterno, TipoMidia tipo) {
        if (chaveOmdb.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Busca externa indisponível: configure OMDB_API_KEY.");
        }
        JsonNode item = requisitarOmdb(uri -> uri
                        .scheme("https").host("www.omdbapi.com").path("/")
                        .queryParam("apikey", chaveOmdb).queryParam("i", idExterno)
                        .queryParam("plot", "full").build());
        if (item == null || !"True".equals(item.path("Response").asText())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Obra não encontrada na API externa.");
        }
        Integer ano = null;
        try {
            String anoTexto = item.path("Year").asText().split("–", 2)[0].split("-", 2)[0];
            ano = Integer.valueOf(anoTexto);
        } catch (NumberFormatException ignored) {
            // A API também retorna intervalos ou datas sem ano numérico utilizável.
        }
        return new DetalheExterno(idExterno, item.path("Title").asText(), ano,
                tipo.name(), item.path("Plot").asText(null), urlCartaz(item.path("Poster").asText(null)));
    }

    private String urlCartaz(String url) {
        return url == null || url.equals("N/A") ? null : url;
    }

    private JsonNode requisitarOmdb(
            java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uriBuilder) {
        try {
            return restClient.get().uri(uriBuilder).retrieve().body(JsonNode.class);
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "A API de catálogo está indisponível.", ex);
        }
    }
}
