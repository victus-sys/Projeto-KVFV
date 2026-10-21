# Documento de Casos de Uso

## 1. Visão geral

O Sistema de Gerenciamento de Hotel tem como objetivo auxiliar no controle de hóspedes, quartos, reservas, hospedagens, pagamentos, funcionários e relatórios, facilitando as atividades administrativas e o atendimento aos clientes.

## 2. Atores

### Administrador
Responsável pelo gerenciamento geral do sistema, pelo cadastro de quartos e funcionários e pela consulta de relatórios.

### Recepcionista
Responsável pelo atendimento aos hóspedes, cadastro e consulta de informações, realização de reservas, check-in, check-out e registro de pagamentos.

### Hóspede
Cliente que utiliza os serviços do hotel e fornece seus dados para realizar uma hospedagem.

## 3. Casos de uso

| Código | Caso de uso | Ator principal |
|---|---|---|
| UC01 | Cadastrar hóspede | Recepcionista |
| UC02 | Consultar hóspedes | Recepcionista |
| UC03 | Cadastrar quarto | Administrador |
| UC04 | Consultar quartos | Recepcionista |
| UC05 | Realizar reserva | Recepcionista |
| UC06 | Consultar reservas | Recepcionista |
| UC07 | Cancelar reserva | Recepcionista |
| UC08 | Realizar check-in | Recepcionista |
| UC09 | Realizar check-out | Recepcionista |
| UC10 | Registrar pagamento | Recepcionista |
| UC11 | Cadastrar funcionário | Administrador |
| UC12 | Gerar relatório de hospedagens | Administrador |

## 4. Especificações resumidas

### UC01 — Cadastrar hóspede
**Ator:** Recepcionista

**Descrição:** permite cadastrar os dados pessoais e de contato de um novo hóspede.

### UC02 — Consultar hóspedes
**Ator:** Recepcionista

**Descrição:** permite listar e localizar hóspedes cadastrados por nome ou CPF.

### UC03 — Cadastrar quarto
**Ator:** Administrador

**Descrição:** permite registrar quartos, seus números, tipos e valores de diária.

### UC04 — Consultar quartos
**Ator:** Recepcionista

**Descrição:** permite consultar os quartos cadastrados e verificar suas informações e disponibilidade.

### UC05 — Realizar reserva
**Ator:** Recepcionista

**Descrição:** permite registrar uma reserva vinculada a um hóspede, a um quarto e a um período de hospedagem.

### UC06 — Consultar reservas
**Ator:** Recepcionista

**Descrição:** permite consultar as reservas cadastradas, incluindo hóspede, quarto, datas e situação.

### UC07 — Cancelar reserva
**Ator:** Recepcionista

**Descrição:** permite cancelar uma reserva existente, desde que ela possa ser cancelada de acordo com sua situação.

### UC08 — Realizar check-in
**Ator:** Recepcionista

**Descrição:** permite registrar a chegada do hóspede e iniciar sua hospedagem mediante uma reserva válida.

### UC09 — Realizar check-out
**Ator:** Recepcionista

**Descrição:** permite registrar a saída do hóspede e finalizar sua hospedagem, liberando o quarto conforme as regras do sistema.

### UC10 — Registrar pagamento
**Ator:** Recepcionista

**Descrição:** permite registrar o pagamento referente a uma hospedagem, incluindo valor e forma de pagamento.

### UC11 — Cadastrar funcionário
**Ator:** Administrador

**Descrição:** permite cadastrar funcionários e registrar suas informações pessoais e profissionais.

### UC12 — Gerar relatório de hospedagens
**Ator:** Administrador

**Descrição:** permite consultar e apresentar informações das hospedagens registradas, incluindo períodos e situações.

## 5. Rastreabilidade

| Caso de uso | História de usuário |
|---|---|
| UC01 — Cadastrar hóspede | HU01 — Cadastrar hóspede |
| UC02 — Consultar hóspedes | HU02 — Consultar hóspedes |
| UC03 — Cadastrar quarto | HU03 — Cadastrar quarto |
| UC04 — Consultar quartos | HU04 — Consultar quartos |
| UC05 — Realizar reserva | HU05 — Realizar reserva |
| UC06 — Consultar reservas | HU06 — Consultar reservas |
| UC07 — Cancelar reserva | HU07 — Cancelar reserva |
| UC08 — Realizar check-in | HU08 — Realizar check-in |
| UC09 — Realizar check-out | HU09 — Realizar check-out |
| UC10 — Registrar pagamento | HU10 — Registrar pagamento |
| UC11 — C