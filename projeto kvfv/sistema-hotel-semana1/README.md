# Hotel Alba — Sistema de Gerenciamento

Aplicação desktop acadêmica para apoiar o gerenciamento de um hotel. O trabalho documenta o problema, os atores, as histórias de usuário e os casos de uso. A versão executável atual concentra-se no cadastro de hóspedes.

## Funcionalidades disponíveis

- Cadastrar hóspedes com nome, documento, telefone e e-mail.
- Consultar e pesquisar hóspedes por nome ou documento.
- Editar e excluir hóspedes.
- Validar o preenchimento do nome e o formato do e-mail quando informado.
- Criar e utilizar um banco SQLite local.

As funções de login, quartos, reservas, check-in, check-out, pagamentos, funcionários e relatórios estão no planejamento e ainda não foram implementadas na interface.

## Situação em relação aos requisitos do semestre

O projeto já tem três atores e doze casos de uso documentados, além de duas validações no cadastro de hóspedes (nome obrigatório e formato do e-mail). Para cumprir todos os requisitos mínimos de complexidade ao longo do semestre, ainda será necessário implementar autenticação com perfis, ampliar as classes de domínio para pelo menos nove e modelar uma entidade com ciclo de vida de ao menos quatro estados. A versão atual é uma etapa inicial e ainda não atende a esses itens pendentes.

## Tecnologias

- Java 21
- JavaFX
- SQLite via `sqlite-jdbc` e JDBC
- Maven

## Como executar

É necessário ter JDK 21 e Maven instalados. No terminal, nesta pasta (a mesma deste `pom.xml`), execute:

```bash
mvn javafx:run
```

Na primeira execução, o sistema cria a pasta `data/` e o banco `data/hotel.db` no diretório de execução. O banco contém tabelas para usuários, hóspedes, quartos e reservas; nesta versão, a interface trabalha com hóspedes.

## Integrantes

- Victor
- Francisco Vinicius
- Kauã David
- Vitoria Ellen

## Materiais da semana 1

- [Documento de casos de uso](docs/casos-de-uso.md)
- [Diagrama de casos de uso](docs/diagrama-de-casos-de-uso.md) (imagem PNG e fonte editável Draw.io)
- [Product Backlog inicial](docs/product-backlog.md)

## Estrutura resumida

```text
src/main/java/br/com/hotel/   código da aplicação
src/main/resources/images/    imagem usada na interface
docs/                         casos de uso, diagrama e backlog
pom.xml                       dependências e configuração Maven
```
