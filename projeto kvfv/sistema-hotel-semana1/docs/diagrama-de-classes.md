# Modelo de domínio — Hotel Alba

O diagrama abaixo corresponde às classes de domínio da aplicação Java. A entidade `Reserva` controla um ciclo de vida com cinco estados possíveis e valida as transições de confirmação, check-in, check-out e cancelamento.

```mermaid
classDiagram
    class Usuario {
        +int id
        +String nome
        +String login
        +String senhaHash
        +PerfilAcesso perfil
    }
    class Hospede {
        +int id
        +String nome
        +String documento
        +String telefone
        +String email
        +Usuario usuario
        +Endereco endereco
    }
    class Funcionario {
        +int id
        +String nome
        +String documento
        +String cargo
        +Usuario usuario
    }
    class Endereco {
        +String logradouro
        +String numero
        +String cidade
        +String estado
        +String cep
    }
    class Quarto {
        +int id
        +String numero
        +TipoQuarto tipo
        +StatusQuarto status
        +BigDecimal valorDiaria
    }
    class Reserva {
        +int id
        +LocalDate dataEntrada
        +LocalDate dataSaida
        +StatusReserva status
        +confirmar()
        +realizarCheckIn()
        +realizarCheckOut()
        +cancelar()
    }
    class Hospedagem {
        +int id
        +LocalDateTime checkIn
        +LocalDateTime checkOut
        +List~Diaria~ diarias
        +calcularTotalDiarias()
    }
    class Pagamento {
        +int id
        +BigDecimal valor
        +FormaPagamento forma
        +LocalDateTime data
    }
    class Diaria {
        +LocalDate data
        +BigDecimal valor
    }
    class StatusReserva {
        <<enumeration>>
        PENDENTE
        CONFIRMADA
        EM_HOSPEDAGEM
        CONCLUIDA
        CANCELADA
    }
    Usuario "1" -- "0..1" Hospede : conta
    Hospede "0..1" -- "1" Endereco : endereço
    Usuario "1" -- "0..1" Funcionario : acesso
    Hospede "1" -- "0..*" Reserva : realiza
    Quarto "1" -- "0..*" Reserva : recebe
    Reserva "1" -- "0..1" Hospedagem : inicia
    Hospedagem "1" -- "0..*" Pagamento : recebe
    Hospedagem "1" -- "0..*" Diaria : compõe valor
    Reserva --> StatusReserva
```

As classes `TipoQuarto`, `StatusQuarto`, `PerfilAcesso` e `FormaPagamento` também estão implementadas como enumerações no pacote `br.com.hotel.domain`.
