package com.example.espreitar.controller;

import com.example.espreitar.model.Midia;
import com.example.espreitar.model.TipoMidia;
import com.example.espreitar.model.dto.BuscaExterna;
import com.example.espreitar.model.dto.DetalheExterno;
import com.example.espreitar.model.dto.TrailerResposta;
import com.example.espreitar.service.MidiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/midias")
@RequiredArgsConstructor
public class MidiaController {
    private final MidiaService midiaService;

    @PostMapping
    public Midia salvar(@RequestBody Midia midia){
       return midiaService.cadastrarMidia(midia);
    }

    @GetMapping("/buscar-exata")
    public Midia buscarPorTituloAnoETipo(
            @RequestParam String titulo,
            @RequestParam Integer ano,
            @RequestParam TipoMidia tipo) {
        return midiaService.buscarMidiaTituloAno(titulo, ano, tipo);
    }

    @GetMapping("/externa")
    public List<BuscaExterna> buscarExterna(
            @RequestParam String q,
            @RequestParam(defaultValue = "FILME") TipoMidia tipo) {
        return midiaService.buscarExterna(q, tipo);
    }

    @GetMapping("/externa/{idExterno}")
    public DetalheExterno detalheExterno(
            @PathVariable String idExterno,
            @RequestParam(defaultValue = "FILME") TipoMidia tipo) {
        return midiaService.buscarDetalheExterno(idExterno, tipo);
    }

    @GetMapping("/{id}")
    public Midia buscarPorId(@PathVariable Long id) {
        return midiaService.buscarPorId(id);
    }

    @GetMapping("/{id}/trailer")
    public TrailerResposta buscarTrailer(@PathVariable Long id) {
        return new TrailerResposta(midiaService.buscarTrailer(id));
    }

    @GetMapping
    public List<Midia> listarTodas(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TipoMidia tipo,
            @RequestParam(required = false) Double notaMinima) {
        return midiaService.buscarCatalogo(q, tipo, notaMinima);
    }
    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) {
        midiaService.excluirMidia(id);
    }

    @GetMapping("/filtrar")
    public List<Midia> filtrar(
            @RequestParam(required = false) TipoMidia tipo,
            @RequestParam(required = false) Double notaMinima) {
        return midiaService.filtrarMidias(tipo, notaMinima);
    }

    @PutMapping("/{id}")
    public Midia atualizar(@PathVariable Long id, @RequestBody Midia midia) {
        return midiaService.atualizarMidia(id, midia);
    }

}
