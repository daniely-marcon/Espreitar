package com.example.espreitar.service;


import com.example.espreitar.model.Midia;
import com.example.espreitar.model.TipoMidia;
import com.example.espreitar.model.dto.BuscaExterna;
import com.example.espreitar.model.dto.DetalheExterno;
import com.example.espreitar.repository.AvaliacaoRepository;
import com.example.espreitar.repository.MidiaRepository;
import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MidiaService {
    private final MidiaRepository midiaRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final RestClient restClient = RestClient.create();
    @Value("${tmdb.access-token:}")
    private String tokenTmdb;

    public Midia cadastrarMidia(Midia midia){
        if (midia.getTipo() == null || midia.getTitulo() == null || midia.getTitulo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o tipo e o título da obra.");
        }
        boolean cadastroManual = midia.getIdExterno() == null || midia.getIdExterno().isBlank();
        if (cadastroManual) {
            midia.setIdExterno(null);
            midia.setNotaMediaExterna(0.0);
            midia.setTotalAvaliacoesExternas(0);
        } else {
            if (midia.getNotaMediaExterna() == null
                    || midia.getNotaMediaExterna() < 0
                    || midia.getNotaMediaExterna() > 5
                    || midia.getTotalAvaliacoesExternas() == null
                    || midia.getTotalAvaliacoesExternas() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nota ou total de votos do TMDb inválido.");
            }
            midia.setIdExterno(midia.getIdExterno().trim());
        }
        midia.setTrailerUrl(normalizarTrailerYoutube(midia.getTrailerUrl()));
        midia.setTitulo(midia.getTitulo().trim());
        if ((!cadastroManual && midiaRepository.findByIdExternoAndTipo(
                midia.getIdExterno(), midia.getTipo()).isPresent())
                || midiaRepository.existsDuplicate(
                        midia.getTitulo(), midia.getAno(), midia.getTipo(), null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse filme ou série já existe no catálogo.");
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mídia não encontrada.");
        }
        if (avaliacaoRepository.existsByMidiaId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível excluir uma mídia que já possui avaliações.");
        }
        midiaRepository.deleteById(id);
    }

    public List<Midia> filtrarMidias(TipoMidia tipo, Double notaMinima) {
        return midiaRepository.filtrarPorTipoENota(tipo, notaMinima);
    }

    public Midia atualizarMidia(Long id, Midia dadosAtualizados) {
        Midia midiaExistente = midiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mídia não encontrada."));

        String titulo = dadosAtualizados.getTitulo().trim();
        if (midiaRepository.existsDuplicate(
                titulo, dadosAtualizados.getAno(), dadosAtualizados.getTipo(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta obra já está cadastrada.");
        }
        midiaExistente.setTitulo(titulo);
        midiaExistente.setAno(dadosAtualizados.getAno());
        midiaExistente.setTipo(dadosAtualizados.getTipo());
        midiaExistente.setSinopse(dadosAtualizados.getSinopse());
        midiaExistente.setCartaz(dadosAtualizados.getCartaz());
        midiaExistente.setTrailerUrl(normalizarTrailerYoutube(dadosAtualizados.getTrailerUrl()));

        return midiaRepository.save(midiaExistente);
    }

    public Midia buscarPorId(Long id) {
        return midiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mídia não encontrada."));
    }

    public String buscarTrailer(Long id) {
        Midia midia = buscarPorId(id);
        if (midia.getTrailerUrl() != null && !midia.getTrailerUrl().isBlank()) {
            return midia.getTrailerUrl();
        }
        if (midia.getIdExterno() == null || !midia.getIdExterno().matches("\\d+")) {
            return null;
        }
        String trailerUrl = buscarDetalheExterno(midia.getIdExterno(), midia.getTipo()).trailerUrl();
        if (trailerUrl != null && !trailerUrl.isBlank()) {
            midia.setTrailerUrl(trailerUrl);
            midiaRepository.save(midia);
        }
        return trailerUrl;
    }

    public List<BuscaExterna> buscarExterna(String termo, TipoMidia tipo) {
        if (tokenTmdb.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Busca externa indisponível: configure TMDB_ACCESS_TOKEN.");
        }
        if (termo == null || termo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um título para buscar.");
        }
        String tipoTmdb = tipo == TipoMidia.SERIE ? "tv" : "movie";
        JsonNode resultado = requisitarTmdb(uri -> uri
                .scheme("https")
                .host("api.themoviedb.org")
                .path("/3/search/" + tipoTmdb)
                .queryParam("query", termo.trim())
                .queryParam("language", "pt-BR")
                .queryParam("include_adult", false)
                .build());
        var lista = resultado == null ? null : resultado.path("results");
        var encontrados = new java.util.ArrayList<BuscaExterna>();
        if (lista == null || !lista.isArray()) {
            return encontrados;
        }
        for (JsonNode item : lista) {
            String titulo = item.path(tipo == TipoMidia.SERIE ? "name" : "title").asText("");
            if (titulo.isBlank()) {
                continue;
            }
            encontrados.add(new BuscaExterna(
                    item.path("id").asText(),
                    titulo,
                    anoExibicao(item.path(tipo == TipoMidia.SERIE ? "first_air_date" : "release_date").asText(null)),
                    tipo.name(),
                    urlCartaz(item.path("poster_path").asText(null)),
                    notaNaEscalaLocal(item.path("vote_average").asDouble(0)),
                    item.path("vote_count").asInt(0)));
        }
        return encontrados;
    }

    public DetalheExterno buscarDetalheExterno(String idExterno, TipoMidia tipo) {
        if (tokenTmdb.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Busca externa indisponível: configure TMDB_ACCESS_TOKEN.");
        }
        if (idExterno == null || !idExterno.matches("\\d+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador TMDb inválido.");
        }
        String tipoTmdb = tipo == TipoMidia.SERIE ? "tv" : "movie";
        JsonNode item = requisitarTmdb(uri -> uri
                .scheme("https")
                .host("api.themoviedb.org")
                .path("/3/" + tipoTmdb + "/" + idExterno)
                .queryParam("language", "pt-BR")
                .queryParam("append_to_response", "videos")
                .build());
        if (item == null || item.path("id").isMissingNode()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Obra não encontrada na API externa.");
        }
        String titulo = item.path(tipo == TipoMidia.SERIE ? "name" : "title").asText();
        String lancamento = item.path(tipo == TipoMidia.SERIE ? "first_air_date" : "release_date").asText(null);
        return new DetalheExterno(idExterno, titulo, ano(lancamento),
                tipo.name(), item.path("overview").asText(null),
                urlCartaz(item.path("poster_path").asText(null)),
                trailerYoutube(item),
                notaNaEscalaLocal(item.path("vote_average").asDouble(0)),
                item.path("vote_count").asInt(0));
    }

    private String trailerYoutube(JsonNode item) {
        JsonNode videos = item.path("videos").path("results");
        if (!videos.isArray()) {
            return null;
        }
        JsonNode trailer = null;
        for (JsonNode video : videos) {
            if (!"YouTube".equalsIgnoreCase(video.path("site").asText())
                    || !"Trailer".equalsIgnoreCase(video.path("type").asText())
                    || video.path("key").asText().isBlank()) {
                continue;
            }
            if (video.path("official").asBoolean(false)) {
                trailer = video;
                break;
            }
            if (trailer == null) {
                trailer = video;
            }
        }
        return trailer == null ? null
                : "https://www.youtube-nocookie.com/embed/" + trailer.path("key").asText();
    }

    private String normalizarTrailerYoutube(String trailerUrl) {
        if (trailerUrl == null || trailerUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(trailerUrl.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)
                    || host == null) {
                throw new IllegalArgumentException();
            }
            host = host.toLowerCase(java.util.Locale.ROOT);
            String videoId = null;
            if (host.equals("youtu.be") || host.equals("www.youtu.be")) {
                java.util.regex.Matcher shortLink = java.util.regex.Pattern
                        .compile("^/([A-Za-z0-9_-]{6,})/?$")
                        .matcher(uri.getPath());
                if (shortLink.matches()) {
                    videoId = shortLink.group(1);
                }
            } else if (host.equals("youtube.com") || host.equals("www.youtube.com")
                    || host.equals("m.youtube.com") || host.equals("youtube-nocookie.com")
                    || host.equals("www.youtube-nocookie.com")) {
                if ("/watch".equals(uri.getPath()) && uri.getRawQuery() != null) {
                    for (String parameter : uri.getRawQuery().split("&")) {
                        if (parameter.startsWith("v=")) {
                            videoId = parameter.substring(2);
                            break;
                        }
                    }
                } else {
                    java.util.regex.Matcher embedLink = java.util.regex.Pattern
                            .compile("^/(?:embed|shorts|live)/([A-Za-z0-9_-]{6,})/?$")
                            .matcher(uri.getPath());
                    if (embedLink.matches()) {
                        videoId = embedLink.group(1);
                    }
                }
            }
            if (videoId == null || !videoId.matches("[A-Za-z0-9_-]{6,}")) {
                throw new IllegalArgumentException();
            }
            return "https://www.youtube-nocookie.com/embed/" + videoId;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe um link válido de trailer do YouTube.", ex);
        }
    }

    private double notaNaEscalaLocal(double notaTmdb) {
        return Math.round(Math.max(0, Math.min(10, notaTmdb)) * 50.0) / 100.0;
    }

    private String urlCartaz(String url) {
        return url == null || url.isBlank() || url.equals("N/A")
                ? null
                : url.startsWith("http") ? url : "https://image.tmdb.org/t/p/w500" + url;
    }

    private String anoExibicao(String data) {
        Integer ano = ano(data);
        return ano == null ? "" : ano.toString();
    }

    private Integer ano(String data) {
        if (data == null || data.length() < 4) {
            return null;
        }
        try {
            return Integer.valueOf(data.substring(0, 4));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private JsonNode requisitarTmdb(java.util.function.Function<org.springframework.web.util.UriBuilder, URI> uriBuilder) {
        try {
            return restClient.get()
                    .uri(uriBuilder)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTmdb)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "O TMDb recusou o token. Confira o TMDB_ACCESS_TOKEN configurado.", ex);
            }
            if (ex.getStatusCode().value() == 429) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Limite de consultas do TMDb atingido. Tente novamente mais tarde.", ex);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "O TMDb retornou um erro ao buscar a mídia.", ex);
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível conectar ao TMDb. Verifique a conexão e tente novamente.", ex);
        }
    }
}
