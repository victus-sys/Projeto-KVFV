package br.com.hotel.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class HospedeApiController {
    private final HospedeRepository hospedes;
    private final AccountRepository contas;

    public HospedeApiController(HospedeRepository hospedes, AccountRepository contas) {
        this.hospedes = hospedes;
        this.contas = contas;
    }

    @GetMapping("/hospedes")
    public List<Hospede> listar(@RequestParam(defaultValue = "") String busca) throws Exception {
        return hospedes.buscar(busca);
    }

    @PostMapping("/hospedes")
    @ResponseStatus(HttpStatus.CREATED)
    public Hospede criar(@Valid @RequestBody HospedeForm form) throws Exception {
        int id = hospedes.salvar(null, form);
        return hospedes.buscarPorId(id);
    }

    @PutMapping("/hospedes/{id}")
    public Hospede atualizar(@PathVariable int id, @Valid @RequestBody HospedeForm form) throws Exception {
        if (hospedes.buscarPorId(id) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        hospedes.salvar(id, form);
        return hospedes.buscarPorId(id);
    }

    @DeleteMapping("/hospedes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable int id) throws Exception {
        if (hospedes.buscarPorId(id) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        hospedes.excluir(id);
    }

    @GetMapping("/minha-conta")
    public Hospede minhaConta(Authentication auth) throws Exception {
        Hospede hospede = contas.hospedeDaConta(auth.getName());
        if (hospede == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return hospede;
    }
}
