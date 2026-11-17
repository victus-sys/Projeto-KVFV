package br.com.hotel.web;

import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.time.LocalDate;

@Controller
public class ReservationController {
    private final ReservationRepository repository;
    public ReservationController(ReservationRepository repository) { this.repository = repository; }

    @GetMapping("/quartos")
    public String quartos(@RequestParam(required=false) LocalDate entrada, @RequestParam(required=false) LocalDate saida, Model model) throws SQLException {
        LocalDate hoje=LocalDate.now(); if (entrada==null || entrada.isBefore(hoje)) entrada=hoje; if (saida==null || !saida.isAfter(entrada)) saida=entrada.plusDays(1);
        model.addAttribute("entrada",entrada); model.addAttribute("saida",saida); model.addAttribute("quartos",repository.quartos(entrada,saida));
        return "quartos";
    }

    @GetMapping("/reservas")
    public String reservas(Authentication auth, Model model) throws Exception {
        if (hospede(auth)) return "redirect:/minhas-reservas";
        model.addAttribute("reservas",repository.reservas(null)); model.addAttribute("isHospede",false); return "reservas";
    }

    @GetMapping("/minhas-reservas")
    public String minhasReservas(Authentication auth, Model model) throws Exception {
        int hospedeId=repository.hospedeId(auth.getName());
        model.addAttribute("reservas",repository.reservas(hospedeId)); model.addAttribute("isHospede",true); return "reservas";
    }

    @GetMapping("/reservas/nova")
    public String nova(@RequestParam(required=false) Integer quarto, @RequestParam(required=false) LocalDate entrada, @RequestParam(required=false) LocalDate saida, Authentication auth, Model model) throws Exception {
        ReservaForm form=new ReservaForm(); form.setQuartoId(quarto); form.setEntrada(entrada); form.setSaida(saida);
        model.addAttribute("form",form); model.addAttribute("hospedes",repository.hospedes()); model.addAttribute("quartos",repository.quartos(entrada==null?LocalDate.now():entrada,saida==null?(entrada==null?LocalDate.now().plusDays(1):entrada.plusDays(1)):saida));
        model.addAttribute("modoEdicao",false); model.addAttribute("souHospede",hospede(auth)); return "reserva-form";
    }

    @GetMapping("/reservas/{id}/editar")
    public String editar(@PathVariable int id, Authentication auth, Model model, RedirectAttributes redirect) throws Exception {
        Integer owner=hospede(auth)?repository.hospedeId(auth.getName()):null;
        var reserva=repository.reserva(id,owner); if (reserva==null) return "redirect:/reservas";
        ReservaForm form=new ReservaForm(); form.setReservaId(id); form.setHospedeId(reserva.hospedeId()); form.setQuartoId(reserva.quartoId()); form.setEntrada(reserva.entrada()); form.setSaida(reserva.saida());
        model.addAttribute("form",form); model.addAttribute("hospedes",repository.hospedes()); model.addAttribute("quartos",repository.quartos(reserva.entrada(),reserva.saida())); model.addAttribute("modoEdicao",true); model.addAttribute("souHospede",hospede(auth));
        return "reserva-form";
    }

    @PostMapping("/reservas/salvar")
    public String salvar(@ModelAttribute ReservaForm form, Authentication auth, RedirectAttributes redirect) throws Exception {
        try {
            boolean guest=hospede(auth); int guestId=guest?repository.hospedeId(auth.getName()):form.getHospedeId();
            if (guestId<1) throw new IllegalArgumentException("Não encontramos seu cadastro de hóspede.");
            if (form.getQuartoId()==null) throw new IllegalArgumentException("Selecione um quarto disponível.");
            if (form.getReservaId()==null) repository.criar(guestId,form.getQuartoId(),form.getEntrada(),form.getSaida());
            else repository.atualizar(form.getReservaId(),guest?guestId:null,guestId,form.getQuartoId(),form.getEntrada(),form.getSaida());
            redirect.addFlashAttribute("sucesso",form.getReservaId()==null?"Reserva criada com sucesso.":"Reserva atualizada com sucesso.");
            return guest?"redirect:/minhas-reservas":"redirect:/reservas";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("erro",e.getMessage());
            return "redirect:/reservas/nova";
        }
    }

    @PostMapping("/reservas/{id}/cancelar")
    public String cancelar(@PathVariable int id, Authentication auth, RedirectAttributes redirect) throws Exception {
        Integer owner=hospede(auth)?repository.hospedeId(auth.getName()):null;
        try { repository.cancelar(id,owner); redirect.addFlashAttribute("sucesso","Reserva cancelada."); }
        catch (IllegalStateException e) { redirect.addFlashAttribute("erro",e.getMessage()); }
        return hospede(auth)?"redirect:/minhas-reservas":"redirect:/reservas";
    }

    @PostMapping("/reservas/canceladas/excluir")
    public String excluirCanceladas(RedirectAttributes redirect)throws Exception{
        int quantidade=repository.excluirCanceladas();
        redirect.addFlashAttribute("sucesso",quantidade==0?"Não há reservas canceladas para apagar.":quantidade+" reserva(s) cancelada(s) apagada(s).");
        return "redirect:/reservas";
    }

    @PostMapping("/reservas/{id}/{acao:confirmar|checkin|checkout}")
    public String operacao(@PathVariable int id,@PathVariable String acao,RedirectAttributes redirect)throws Exception{
        try{
            switch(acao){case "confirmar"->repository.confirmar(id);case "checkin"->repository.checkin(id);case "checkout"->repository.checkout(id);default->throw new IllegalArgumentException("Ação inválida.");}
            redirect.addFlashAttribute("sucesso",switch(acao){case "confirmar"->"Reserva confirmada.";case "checkin"->"Check-in realizado.";default->"Check-out realizado e quarto liberado.";});
        }catch(IllegalStateException e){redirect.addFlashAttribute("erro",e.getMessage());}
        return "redirect:/reservas";
    }

    private boolean hospede(Authentication auth) { return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_HOSPEDE")); }

    public static class ReservaForm {
        private Integer reservaId; private Integer hospedeId; private Integer quartoId;
        @NotNull private LocalDate entrada; @NotNull private LocalDate saida;
        public Integer getReservaId(){return reservaId;} public void setReservaId(Integer v){reservaId=v;}
        public Integer getHospedeId(){return hospedeId;} public void setHospedeId(Integer v){hospedeId=v;}
        public Integer getQuartoId(){return quartoId;} public void setQuartoId(Integer v){quartoId=v;}
        public LocalDate getEntrada(){return entrada;} public void setEntrada(LocalDate v){entrada=v;}
        public LocalDate getSaida(){return saida;} public void setSaida(LocalDate v){saida=v;}
    }
}
