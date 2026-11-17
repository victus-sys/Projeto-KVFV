package br.com.hotel.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RoomController {
    private final RoomRepository rooms;
    public RoomController(RoomRepository rooms){this.rooms=rooms;}

    @GetMapping("/admin/quartos") public String listar(Model model)throws Exception{
        model.addAttribute("quartos",rooms.listar());if(!model.containsAttribute("form"))model.addAttribute("form",new RoomRepository.RoomForm());return "admin-quartos";
    }
    @GetMapping("/admin/quartos/{id}/editar") public String editar(@PathVariable int id,Model model,RedirectAttributes redirect)throws Exception{
        var room=rooms.buscar(id);if(room==null){redirect.addFlashAttribute("erro","Quarto não encontrado.");return "redirect:/admin/quartos";}
        var form=new RoomRepository.RoomForm();form.setNumero(room.numero());form.setTipo(room.tipo());form.setStatus(room.status());form.setValorDiaria(room.valorDiaria());form.setCapacidade(room.capacidade());form.setDescricao(room.descricao());
        model.addAttribute("id",id);model.addAttribute("form",form);model.addAttribute("quartos",rooms.listar());return "admin-quartos";
    }
    @PostMapping("/admin/quartos/salvar") public String salvar(@RequestParam(required=false) Integer id,@ModelAttribute("form") RoomRepository.RoomForm form,Model model,RedirectAttributes redirect)throws Exception{
        try{rooms.salvar(id,form);redirect.addFlashAttribute("sucesso",id==null?"Quarto cadastrado.":"Quarto atualizado.");return "redirect:/admin/quartos";}
        catch(IllegalArgumentException|IllegalStateException e){model.addAttribute("erro",e.getMessage());}
        catch(java.sql.SQLException e){model.addAttribute("erro","Não foi possível salvar. Verifique se o número do quarto já está cadastrado.");}
        model.addAttribute("id",id);model.addAttribute("form",form);model.addAttribute("quartos",rooms.listar());return "admin-quartos";
    }
    @PostMapping("/admin/quartos/{id}/excluir") public String excluir(@PathVariable int id,RedirectAttributes redirect)throws Exception{
        try{rooms.excluir(id);redirect.addFlashAttribute("sucesso","Quarto excluído.");}catch(IllegalStateException|IllegalArgumentException e){redirect.addFlashAttribute("erro",e.getMessage());}return "redirect:/admin/quartos";
    }
}
