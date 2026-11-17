# Acompanhamento dos requisitos da disciplina

Checklist baseado no documento da disciplina recebido pela equipe.

| Requisito | Situação no repositório | Evidência / próximo passo |
|---|---|---|
| Projeto desktop em Java 21 | Atendido na versão desktop | `mvn javafx:run` continua sendo o comando principal. A web é complementar. |
| JavaFX, SQLite via JDBC, Maven | Atendido na versão desktop | `pom.xml`, `Main.java` e `Database.java`. |
| 3 atores/perfis | Atendido | Administrador, recepcionista e hóspede; autenticação desktop e web. |
| Pelo menos 12 casos de uso | Atendido: 17 casos documentados | Documento e diagrama de casos de uso. |
| No máximo metade dos casos como CRUD puro | Atendido no escopo documentado: no máximo 8 de 17 | Reserva, disponibilidade, cálculo das diárias, estados e pagamento têm regras próprias no modelo. |
| Pelo menos 9 classes de domínio | Atendido no modelo | Ver `diagrama-de-classes.md` e pacote `src/main/java/br/com/hotel/domain`. |
| Entidade com pelo menos 4 estados | Atendido no modelo | `Reserva` tem cinco estados e valida transições; ver `diagrama-de-estados-reserva.md`. Persistência e telas de reserva ainda precisam ser implementadas. |
| 2 regras de negócio com cálculo ou validação | Atendido no modelo atual | Datas de reserva, transições permitidas, valor positivo de diária/pagamento e validação de hóspede. |
| Autenticação por perfis | Atendido | Desktop e web; o admin inicial é configurável por variáveis de ambiente. |
| Product Backlog em issues ou cards do GitHub | Pendente de publicação | O backlog está versionado como Markdown; ainda é necessário criar/sincronizar as issues ou cards no GitHub. |
| Repositório com todos os integrantes e professor como colaboradores | Não verificado | Conferir as permissões de colaboradores diretamente no GitHub. |
| Diagrama e código rastreáveis em cada entrega semanal | Parcial | Os diagramas foram atualizados junto com o modelo e as regras implementadas; vários casos ainda aguardam telas e persistência. Vincular commits futuros ao item correspondente. |

O modelo de domínio já implementa as regras de estados, disponibilidade e cálculo. O diagrama de casos de uso também representa funções planejadas que ainda não têm telas e persistência. O sistema só estará plenamente alinhado ao requisito de rastreabilidade quando esses casos forem implementados ou o escopo do diagrama for reduzido para refletir a aplicação executável.
