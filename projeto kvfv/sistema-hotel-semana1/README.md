# Hotel Alba — Sistema de Gerenciamento

Aplicação acadêmica de gerenciamento de hotel, com interface JavaFX desktop e uma interface web complementar. Ambas persistem no mesmo banco SQLite local.

## Funcionalidades

- **Administrador:** gerencia hóspedes, quartos, funcionários, reservas e acompanha relatórios de ocupação.
- **Funcionário/atendente:** cadastra e atualiza hóspedes, consulta quartos e reservas, confirma reservas e realiza check-in/check-out.
- **Hóspede:** cria a própria conta, atualiza os dados e a senha, consulta quartos por período, realiza reservas e vê/cancela apenas as próprias reservas.
- O sistema impede conflito de datas, bloqueia quartos em manutenção e calcula o valor da hospedagem no check-out a partir da diária e das noites.
- O mapa de quartos é uma visualização simples dos quartos cadastrados no banco. Não há hóspedes, funcionários ou quartos de exemplo criados automaticamente.

## Requisitos

- JDK 21
- Maven

## Executar o desktop

No terminal, nesta pasta (a mesma deste `pom.xml`), execute:

```bash
mvn javafx:run
```

## Executar a web

Entre na pasta `web` e execute:

```bash
mvn spring-boot:run
```

Abra `http://localhost:8080`. O servidor web usa `../data/hotel.db` quando iniciado da pasta `web`; o desktop usa `data/hotel.db` quando iniciado desta pasta.

## Primeiro acesso

Na primeira inicialização, o sistema cria uma conta administrativa de configuração: `admin@hotel.local` / `admin123`. Configure outras credenciais com `HOTEL_ADMIN_USERNAME` e `HOTEL_ADMIN_PASSWORD` antes da primeira execução, se preferir.

O administrador cria os acessos de funcionários em **Funcionários**. Hóspedes criam as próprias contas pela opção de cadastro no login. O administrador precisa cadastrar quartos e diárias antes de eles aparecerem como disponíveis para reserva.

Faça cópia de `data/hotel.db` antes de testar mudanças se o banco já tiver informações que queira preservar.

## Testes

Execute os testes do desktop e da web separadamente:

```bash
mvn test
```

O primeiro comando deve ser executado nesta pasta; para testar a web, execute-o dentro de `web`.
