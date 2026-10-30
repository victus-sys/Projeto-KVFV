# Hotel Alba

Sistema desktop de gerenciamento de hotel, desenvolvido em Java como projeto da disciplina de Análise e Projeto de Sistemas. O projeto começou pelo levantamento dos requisitos, histórias de usuário, casos de uso e diagrama.

## Objetivo

Organizar informações usadas no atendimento de um hotel, como hóspedes, quartos e reservas. A proposta prevê perfis de administrador e recepcionista, com funções adequadas a cada papel.

## O que está implementado

A versão atual permite cadastrar, consultar, pesquisar, editar e excluir hóspedes. Os dados são persistidos em um banco SQLite local. A interface foi construída em JavaFX.

O cadastro exige o nome e verifica o formato do e-mail quando informado. O sistema também cria as tabelas de usuários, quartos e reservas na inicialização. As funções de login, gerenciamento de quartos e reservas, check-in, check-out, pagamentos, funcionários e relatórios ainda fazem parte do escopo documentado e não estão disponíveis na interface atual.

## Tecnologias

- Java 21
- JavaFX
- SQLite com JDBC (`sqlite-jdbc`)
- Maven

## Requisitos para executar

Instale o JDK 21 e o Maven. No terminal, entre na pasta `projeto kvfv/sistema-hotel-semana1` e execute:

```bash
mvn javafx:run
```

Na primeira inicialização, o programa cria a pasta `data/` e o arquivo `data/hotel.db` no diretório de execução. Não é necessário criar o banco manualmente.

## Integrantes

- Victor
- Francisco Vinicius
- Kauã David
- Vitoria Ellen

## Documentação do projeto

- [Casos de uso e rastreabilidade](projeto%20kvfv/sistema-hotel-semana1/docs/casos-de-uso.md)
- [Diagrama de casos de uso](projeto%20kvfv/sistema-hotel-semana1/docs/diagrama-de-casos-de-uso.md)
- [Product Backlog](projeto%20kvfv/sistema-hotel-semana1/docs/product-backlog.md)
- [README técnico do sistema](projeto%20kvfv/sistema-hotel-semana1/README.md)

## Roteiro breve para apresentação

1. Apresentar o problema: manter informações do hotel organizadas e facilitar o atendimento.
2. Mostrar os atores definidos: administrador, recepcionista e hóspede.
3. Explicar os casos de uso e como eles se relacionam com as histórias do backlog.
4. Demonstrar o cadastro, a pesquisa, a edição e a exclusão de hóspedes.
5. Mostrar o uso do JavaFX, JDBC e SQLite e explicar que o banco local é criado automaticamente.
6. Encerrar indicando as próximas funções previstas: autenticação por perfil, quartos, reservas, hospedagem e relatórios.
