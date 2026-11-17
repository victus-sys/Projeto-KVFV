package br.com.hotel.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"hotel.database.url=jdbc:sqlite:target/hotel-web-smoke.db","hotel.admin.username=admin@hotel.local","hotel.admin.password=admin123"})
@AutoConfigureMockMvc
class WebPagesSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired RoomRepository roomRepository;

    @Test void rendersPublicLoginAndRejectsRoleMismatch() throws Exception {
        mvc.perform(get("/login?perfil=hospede")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Hóspede")));
        mvc.perform(post("/login").param("username","admin@hotel.local").param("password","admin123").param("perfil","funcionario").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?perfil=funcionario&tipoInvalido"));
    }

    @Test void rendersProtectedPagesForCorrectProfiles() throws Exception {
        var admin=SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN");
        var staff=SecurityMockMvcRequestPostProcessors.user("atendente").roles("RECEPCIONISTA");
        var guest=SecurityMockMvcRequestPostProcessors.user("hospede@exemplo.test").roles("HOSPEDE");

        mvc.perform(get("/admin/quartos").with(admin)).andExpect(status().isOk()).andExpect(view().name("admin-quartos"));
        mvc.perform(get("/admin/funcionarios").with(admin)).andExpect(status().isOk()).andExpect(view().name("funcionarios"));
        mvc.perform(get("/admin/relatorios").with(admin)).andExpect(status().isOk()).andExpect(view().name("relatorios"));
        mvc.perform(get("/hospedes").with(staff)).andExpect(status().isOk()).andExpect(view().name("hospedes"));
        mvc.perform(get("/reservas").with(staff)).andExpect(status().isOk()).andExpect(view().name("reservas"));
        mvc.perform(get("/quartos").with(guest)).andExpect(status().isOk()).andExpect(view().name("quartos"));
        mvc.perform(get("/minhas-reservas").with(guest)).andExpect(status().isOk()).andExpect(view().name("reservas"));
        mvc.perform(get("/minha-conta").with(guest)).andExpect(status().isOk()).andExpect(view().name("minha-conta"));
    }

    @Test void adminCanCreateAndEditRoomsThroughTheWebForm() throws Exception {
        var admin=SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN");
        String number="TEST-"+java.util.UUID.randomUUID();
        mvc.perform(post("/admin/quartos/salvar").with(admin).with(csrf())
                .param("numero",number).param("tipo","CASAL").param("status","DISPONIVEL")
                .param("valorDiaria","180").param("capacidade","2").param("descricao","Original"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/quartos"));
        var room=roomRepository.listar().stream().filter(q -> q.numero().equals(number)).findFirst().orElse(null);
        assertNotNull(room);
        try {
            mvc.perform(post("/admin/quartos/salvar").with(admin).with(csrf())
                    .param("id",String.valueOf(room.id())).param("numero",number).param("tipo","SUITE")
                    .param("status","DISPONIVEL").param("valorDiaria","245").param("capacidade","3")
                    .param("descricao","Atualizado"))
                    .andExpect(status().is3xxRedirection());
            var saved=roomRepository.buscar(room.id());
            assertEquals("SUITE",saved.tipo());assertEquals(245.0,saved.valorDiaria());assertEquals("Atualizado",saved.descricao());
        } finally { roomRepository.excluir(room.id()); }
    }

    @Test void preventsStaffAndGuestsFromAdministrativeFunctions() throws Exception {
        var staff=SecurityMockMvcRequestPostProcessors.user("atendente").roles("RECEPCIONISTA");
        var guest=SecurityMockMvcRequestPostProcessors.user("hospede@exemplo.test").roles("HOSPEDE");
        mvc.perform(get("/admin/quartos").with(staff)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/funcionarios").with(staff)).andExpect(status().isForbidden());
        mvc.perform(get("/hospedes").with(guest)).andExpect(status().isForbidden());
        mvc.perform(post("/hospedes/1/excluir").with(staff).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(delete("/api/v1/hospedes/1").with(staff).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/reservas/canceladas/excluir").with(staff).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/reservas/canceladas/excluir").with(guest).with(csrf())).andExpect(status().isForbidden());
    }
}
