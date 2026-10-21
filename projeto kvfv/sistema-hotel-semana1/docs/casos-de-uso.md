# Documento de Casos de Uso

## 1. Visão geral

O Sistema de Gerenciamento de Hotel tem como objetivo auxiliar no controle de hóspedes,
quartos, reservas, funcionários, check-in, check-out e relatórios.

## 2. Atores

### Administrador
Responsável pelo gerenciamento geral do sistema, funcionários, quartos e relatórios.

### Recepcionista
Responsável pelo atendimento aos hóspedes, cadastro, reservas, check-in e check-out.

### Hóspede
Cliente que utiliza os serviços do hotel e fornece seus dados para hospedagem.

## 3. Casos de uso

| Código | Caso de uso | Ator principal |
|---|---|---|
| UC01 | Realizar login | Administrador / Recepcionista |
| UC02 | Cadastrar hóspede | Recepcionista |
| UC03 | Consultar hóspede | Recepcionista |
| UC04 | Editar hóspede | Recepcionista |
| UC05 | Cadastrar quarto | Administrador |
| UC06 | Consultar quartos | Recepcionista |
| UC07 | Realizar reserva | Recepcionista |
| UC08 | Cancelar reserva | Recepcionista |
| UC09 | Registrar check-in | Recepcionista |
| UC10 | Registrar check-out | Recepcionista |
| UC11 | Gerenciar funcionários | Administrador |
| UC12 | Gerar relatório | Administrador |

## 4. Especificações resumidas

### UC01 — Realizar login
**Ator:** Administrador / Recepcionista  
**Descrição:** permite acesso conforme usuário, senha e perfil.

### UC02 — Cadastrar hóspede
**Ator:** Recepcionista  
**Descrição:** cadastra os dados de um novo hóspede.

### UC03 — Consultar hóspede
**Ator:** Recepcionista  
**Descrição:** localiza os dados de um hóspede.

### UC04 — Editar hóspede
**Ator:** Recepcionista  
**Descrição:** altera dados de um hóspede já cadastrado.

### UC05 — Cadastrar quarto
**Ator:** Administrador  
**Descrição:** cadastra um quarto e seu tipo/status.

### UC06 — Consultar quartos
**Ator:** Recepcionista  
**Descrição:** consulta quartos e disponibilidade.

### UC07 — Realizar reserva
**Ator:** Recepcionista  
**Descrição:** registra uma reserva para um hóspede.

### UC08 — Cancelar reserva
**Ator:** Recepcionista  
**Descrição:** cancela uma reserva existente e libera o quarto.

### UC09 — Registrar check-in
**Ator:** Recepcionista  
**Descrição:** registra a entrada do hóspede.

### UC10 — Registrar check-out
**Ator:** Recepcionista  
**Descrição:** registra a saída do hóspede e libera o quarto.

### UC11 — Gerenciar funcionários
**Ator:** Administrador  
**Descrição:** cadastra, consulta e edita funcionários/usuários.

### UC12 — Gerar relatório
**Ator:** Administrador  
**Descrição:** gera relatórios a partir dos dados do sistema.

## 5. Rastreabilidade

| Caso de uso | História de usuário |
|---|---|
| UC01 | HU01 |
| UC02 | HU02 |
| UC03 | HU03 |
| UC04 | HU04 |
| UC05 | HU05 |
| UC06 | HU06 |
| UC07 | HU07 |
| UC08 | HU08 |
| UC09 | HU09 |
| UC10 | HU10 |
| UC11 | HU11 |
| UC12 | HU12 |
