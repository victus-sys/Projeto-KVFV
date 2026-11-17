# Diagrama de estados — Reserva

O ciclo abaixo é implementado em `br.com.hotel.domain.Reserva` e `StatusReserva`. Cancelamento só é permitido enquanto a reserva está pendente ou confirmada; check-in e check-out só podem ocorrer na ordem indicada.

```mermaid
stateDiagram-v2
    [*] --> PENDENTE
    PENDENTE --> CONFIRMADA: confirmar
    PENDENTE --> CANCELADA: cancelar
    CONFIRMADA --> EM_HOSPEDAGEM: realizar check-in
    CONFIRMADA --> CANCELADA: cancelar
    EM_HOSPEDAGEM --> CONCLUIDA: realizar check-out
    CONCLUIDA --> [*]
    CANCELADA --> [*]
```
