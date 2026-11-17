package br.com.hotel.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportController {
    private final ReportRepository reports;
    public ReportController(ReportRepository reports){this.reports=reports;}
    @GetMapping("/admin/relatorios") public String relatorios(Model model)throws Exception{model.addAttribute("resumo",reports.resumo());return "relatorios";}
}
