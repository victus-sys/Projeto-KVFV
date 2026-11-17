package br.com.hotel.web;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {
    private final HospedeRepository hospedes;
    private final AccountRepository contas;
    private final PasswordEncoder encoder;

    public WebController(HospedeRepository hospedes, AccountRepository contas, PasswordEncoder encoder) {
        this.hospedes = hospedes; this.contas = contas; this.encoder = encoder;
    }

    @GetMapping("/")
    public String inicio(Authentication auth) {
        if (auth == null) return "redirect:/login";
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOSPEDE"))) return "redirect:/minha-conta";
        return "redirect:/hospedes";
    }

    @GetMapping("/login") public String login(@RequestParam(defaultValue="admin") String perfil, @RequestParam(required=false) String tipoInvalido, @RequestParam(required=false) String dadosAtualizados, Model model) {
        String selecionado = java.util.Set.of("admin", "funcionario", "hospede").contains(perfil) ? perfil : "admin";
        model.addAttribute("perfilSelecionado", selecionado);
        model.addAttribute("tipoInvalido", tipoInvalido != null);
        model.addAttribute("dadosAtualizados", dadosAtualizados != null);
        return "login";
    }

    @GetMapping("/cadastro") public String cadastro(Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", new CadastroForm());
        return "cadastro";
    }

    @PostMapping("/cadastro") public String criarConta(@Valid @ModelAttribute("form") CadastroForm form, BindingResult erros, RedirectAttributes redirect) {
        if (form.getSenha() != null && form.getConfirmacao() != null && !form.getSenha().equals(form.getConfirmacao())) erros.rejectValue("confirmacao", "senha.diferente", "As senhas não conferem.");
        if (erros.hasErrors()) return "cadastro";
        try {
            contas.cadastrarHospede(form.getNome(), form.getLogin(), form.getSenha(), encoder);
            redirect.addFlashAttribute("sucesso", "Conta criada. Entre usando seu e-mail e a senha cadastrada.");
            return "redirect:/login";
        } catch (Exception e) {
            erros.rejectValue("login", "login.indisponivel", "Não foi possível criar a conta. Confira se o e-mail já está cadastrado.");
            return "cadastro";
        }
    }

    @GetMapping("/hospedes") public String listar(@RequestParam(defaultValue = "") String busca, Model model) throws Exception {
        model.addAttribute("hospedes", hospedes.buscar(busca)); model.addAttribute("hospedesInativos",hospedes.buscarInativos()); model.addAttribute("busca", busca);
        if (!model.containsAttribute("form")) model.addAttribute("form", new HospedeForm());
        return "hospedes";
    }

    @PostMapping({"/hospedes", "/hospedes/{id}"}) public String salvar(@Valid @ModelAttribute("form") HospedeForm form, BindingResult erros, @PathVariable(required = false) Integer id, Model model, RedirectAttributes redirect) throws Exception {
        if (erros.hasErrors()) { model.addAttribute("id", id); model.addAttribute("hospedes", hospedes.buscar("")); model.addAttribute("busca", ""); return "hospedes"; }
        hospedes.salvar(id, form); redirect.addFlashAttribute("sucesso", id == null ? "Hóspede cadastrado com sucesso." : "Dados do hóspede atualizados."); return "redirect:/hospedes";
    }

    @GetMapping("/hospedes/{id}/editar") public String editar(@PathVariable int id, Model model) throws Exception {
        Hospede h = hospedes.buscarPorId(id); if (h == null) return "redirect:/hospedes";
        HospedeForm f = new HospedeForm(); f.setNome(h.nome()); f.setDocumento(h.documento()); f.setTelefone(h.telefone()); f.setEmail(h.email());
        model.addAttribute("form", f); model.addAttribute("id", id); model.addAttribute("hospedes", hospedes.buscar("")); model.addAttribute("busca", ""); return "hospedes";
    }

    @PostMapping("/hospedes/{id}/excluir") public String excluir(@PathVariable int id, RedirectAttributes redirect) throws Exception {
        try {
            int reservasExcluidas=hospedes.excluir(id);
            redirect.addFlashAttribute("sucesso",reservasExcluidas>0
                    ? "Hóspede excluído definitivamente junto com " + reservasExcluidas + " reserva(s)."
                    : "Hóspede excluído definitivamente.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("erro",e.getMessage());
        }
        return "redirect:/hospedes";
    }

    @GetMapping("/minha-conta") public String minhaConta(Authentication auth, Model model) throws Exception {
        Hospede h=contas.hospedeDaConta(auth.getName());model.addAttribute("hospede",h);
        if(!model.containsAttribute("dadosForm")&&h!=null){DadosHospedeForm f=new DadosHospedeForm();f.setNome(h.nome());f.setLogin(h.email());f.setDocumento(h.documento());f.setTelefone(h.telefone());model.addAttribute("dadosForm",f);}
        if(!model.containsAttribute("senhaForm"))model.addAttribute("senhaForm",new SenhaHospedeForm());return "minha-conta";
    }

    @PostMapping("/minha-conta/dados") public String salvarDadosHospede(@Valid @ModelAttribute("dadosForm") DadosHospedeForm form,BindingResult erros,Authentication auth,jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response,RedirectAttributes redirect)throws Exception{
        if(erros.hasErrors()){redirect.addFlashAttribute("erro","Confira o nome e o formato do e-mail.");return "redirect:/minha-conta";}
        try{contas.atualizarDadosHospede(auth.getName(),form.getNome(),form.getLogin(),form.getDocumento(),form.getTelefone());new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(request,response,auth);return "redirect:/login?perfil=hospede&dadosAtualizados";}
        catch(Exception e){redirect.addFlashAttribute("erro","Não foi possível atualizar. Esse e-mail pode já estar em uso.");}
        return "redirect:/minha-conta";
    }

    @PostMapping("/minha-conta/senha") public String salvarSenhaHospede(@ModelAttribute SenhaHospedeForm form,Authentication auth,RedirectAttributes redirect)throws Exception{
        try{if(form.getNova()==null||!form.getNova().equals(form.getConfirmacao()))throw new IllegalArgumentException("A confirmação da senha não corresponde.");contas.alterarSenhaHospede(auth.getName(),form.getAtual(),form.getNova(),encoder);redirect.addFlashAttribute("sucesso","Senha alterada.");}
        catch(IllegalArgumentException e){redirect.addFlashAttribute("erro",e.getMessage());}
        return "redirect:/minha-conta";
    }

    @GetMapping("/admin/funcionarios") public String funcionarios(Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", new FuncionarioForm());
        try { model.addAttribute("funcionarios", contas.funcionarios()); }
        catch (Exception e) { model.addAttribute("erro", "Não foi possível carregar funcionários."); }
        return "funcionarios";
    }

    @PostMapping("/admin/funcionarios") public String criarFuncionario(@Valid @ModelAttribute("form") FuncionarioForm form, BindingResult erros,Model model) throws Exception {
        if (form.getSenha() == null || form.getSenha().length() < 6) erros.rejectValue("senha", "senha.curta", "Use pelo menos 6 caracteres.");
        if (erros.hasErrors()) {model.addAttribute("funcionarios",contas.funcionarios());return "funcionarios";}
        try {
            contas.cadastrarRecepcionista(form.getNome(), form.getLogin(), form.getSenha(), encoder);
            return "redirect:/admin/funcionarios?criado";
        } catch (Exception e) {
            erros.rejectValue("login", "login.indisponivel", "Esse e-mail já está cadastrado.");
            model.addAttribute("funcionarios",contas.funcionarios());
            return "funcionarios";
        }
    }

    @GetMapping("/admin/funcionarios/{id}/editar") public String editarFuncionario(@PathVariable int id,Model model,RedirectAttributes redirect)throws Exception{
        var staff=contas.funcionario(id);if(staff==null){redirect.addFlashAttribute("erro","Funcionário não encontrado.");return "redirect:/admin/funcionarios";}
        FuncionarioForm form=new FuncionarioForm();form.setId(staff.id());form.setNome(staff.nome());form.setLogin(staff.login());
        model.addAttribute("id",id);model.addAttribute("form",form);model.addAttribute("funcionarios",contas.funcionarios());return "funcionarios";
    }

    @PostMapping("/admin/funcionarios/alterar") public String alterarFuncionario(@Valid @ModelAttribute("form") FuncionarioForm form,BindingResult erros,Model model,RedirectAttributes redirect)throws Exception{
        if(form.getSenha()!=null&&!form.getSenha().isBlank()&&form.getSenha().length()<6)erros.rejectValue("senha","senha.curta","Use pelo menos 6 caracteres.");
        if(erros.hasErrors()){model.addAttribute("id",form.getId());model.addAttribute("funcionarios",contas.funcionarios());return "funcionarios";}
        try{contas.atualizarFuncionario(form.getId(),form.getNome(),form.getLogin(),form.getSenha(),encoder);redirect.addFlashAttribute("sucesso","Funcionário atualizado.");return "redirect:/admin/funcionarios";}
        catch(Exception e){erros.rejectValue("login","login.indisponivel","Não foi possível atualizar. O e-mail pode já estar em uso.");model.addAttribute("id",form.getId());model.addAttribute("funcionarios",contas.funcionarios());return "funcionarios";}
    }

    @PostMapping("/admin/funcionarios/{id}/excluir") public String excluirFuncionario(@PathVariable int id,RedirectAttributes redirect)throws Exception{
        try{contas.excluirFuncionario(id);redirect.addFlashAttribute("sucesso","Funcionário excluído.");}catch(Exception e){redirect.addFlashAttribute("erro",e.getMessage());}return "redirect:/admin/funcionarios";
    }

    public static class CadastroForm {
        @jakarta.validation.constraints.NotBlank(message = "Informe seu nome.") private String nome;
        @jakarta.validation.constraints.Email(message = "Informe um e-mail válido.") @jakarta.validation.constraints.NotBlank(message = "Informe seu e-mail.") private String login;
        @jakarta.validation.constraints.Size(min = 6, message = "Use pelo menos 6 caracteres.") private String senha;
        @jakarta.validation.constraints.NotBlank(message = "Confirme a senha.") private String confirmacao;
        public String getNome(){return nome;} public void setNome(String v){nome=v;}
        public String getLogin(){return login;} public void setLogin(String v){login=v;}
        public String getSenha(){return senha;} public void setSenha(String v){senha=v;}
        public String getConfirmacao(){return confirmacao;} public void setConfirmacao(String v){confirmacao=v;}
    }

    public static class FuncionarioForm {
        private Integer id;
        @jakarta.validation.constraints.NotBlank(message = "Informe o nome.") private String nome;
        @jakarta.validation.constraints.Email(message = "Informe um e-mail válido.") @jakarta.validation.constraints.NotBlank(message = "Informe o e-mail.") private String login;
        private String senha;
        public Integer getId(){return id;} public void setId(Integer v){id=v;}
        public String getNome(){return nome;} public void setNome(String v){nome=v;}
        public String getLogin(){return login;} public void setLogin(String v){login=v;}
        public String getSenha(){return senha;} public void setSenha(String v){senha=v;}
    }

    public static class DadosHospedeForm {
        @jakarta.validation.constraints.NotBlank(message="Informe seu nome.") private String nome;
        @jakarta.validation.constraints.Email(message="Informe um e-mail válido.") @jakarta.validation.constraints.NotBlank(message="Informe seu e-mail.") private String login;
        private String documento,telefone;
        public String getNome(){return nome;}public void setNome(String v){nome=v;}public String getLogin(){return login;}public void setLogin(String v){login=v;}
        public String getDocumento(){return documento;}public void setDocumento(String v){documento=v;}public String getTelefone(){return telefone;}public void setTelefone(String v){telefone=v;}
    }
    public static class SenhaHospedeForm {
        private String atual,nova,confirmacao;
        public String getAtual(){return atual;}public void setAtual(String v){atual=v;}public String getNova(){return nova;}public void setNova(String v){nova=v;}public String getConfirmacao(){return confirmacao;}public void setConfirmacao(String v){confirmacao=v;}
    }
}
