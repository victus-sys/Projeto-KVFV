# Diagrama de Casos de Uso

```mermaid
flowchart LR
    A[Administrador]
    R[Recepcionista]
    H[Hóspede]

    subgraph S[Sistema de Gerenciamento de Hotel]
      UC1((UC01 Realizar login))
      UC2((UC02 Cadastrar hóspede))
      UC3((UC03 Consultar hóspede))
      UC4((UC04 Editar hóspede))
      UC5((UC05 Cadastrar quarto))
      UC6((UC06 Consultar quartos))
      UC7((UC07 Realizar reserva))
      UC8((UC08 Cancelar reserva))
      UC9((UC09 Registrar check-in))
      UC10((UC10 Registrar check-out))
      UC11((UC11 Gerenciar funcionários))
      UC12((UC12 Gerar relatório))
    end

    A --- UC1
    A --- UC5
    A --- UC11
    A --- UC12

    R --- UC1
    R --- UC2
    R --- UC3
    R --- UC4
    R --- UC6
    R --- UC7
    R --- UC8
    R --- UC9
    R --- UC10

    H --- UC7
    H --- UC8
