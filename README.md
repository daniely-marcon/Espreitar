# 🎬 Espreitar - Sistema de Avaliação de Mídias

**Espreitar** é uma aplicação web desenvolvida para que usuários possam descobrir, avaliar e comentar sobre filmes e séries. O sistema consome dados de uma API externa para montar o catálogo e permite que a comunidade construa um acervo de críticas e notas (de 0 a 5 estrelas).

Este projeto foi desenvolvido como requisito de avaliação para a disciplina de **Programação Para WEB** da **Universidade Federal de Mato Grosso do Sul (UFMS)**, sob a orientação do Prof. Matheus Albuquerque de Melo.

---

## 🚀 Funcionalidades

* **Exploração de Catálogo:** Busca de filmes e séries por título consumindo dados de uma API externa (capa, sinopse, ano).
* **Sistema de Avaliações:** Usuários logados podem atribuir notas de 0 a 5 estrelas e adicionar comentários em texto às obras.
* **Gestão de Histórico:** Cada usuário possui um perfil próprio para gerenciar, editar ou excluir suas avaliações anteriores.
* **Autenticação Flexível:** Cadastro manual (e-mail e senha) e suporte a login integrado (Google OAuth).
* **Moderação:** Painel administrativo para moderação e exclusão de comentários que infrinjam as regras da plataforma.

---

## 👥 Perfis de Acesso

O sistema gerencia diferentes níveis de permissão através do Spring Security:
1. **Visitante:** Acesso anônimo apenas para leitura (busca de mídias e visualização de avaliações alheias).
2. **Usuário Comum (ROLE_USER):** Acesso autenticado. Pode publicar novas avaliações, além de editar e excluir o próprio histórico.
3. **Administrador (ROLE_ADMIN):** Acesso autenticado com privilégios de moderação. Pode excluir avaliações de terceiros e gerenciar o banimento de contas infratoras.

---

## 🛠️ Tecnologias Utilizadas

A aplicação adota uma arquitetura em duas camadas, separando o Back-end (API RESTful) do Front-end.

**Back-end:**
* Java (Spring Boot)
* Spring MVC (arquitetura de API REST)
* Spring Data JPA (Persistência)
* Spring Security + JWT (Autenticação e Autorização)
* MySQL (Banco de Dados Relacional)

**Front-end:**
* HTML5 / CSS3 / JavaScript (puro)
* Acessibilidade em conformidade com as diretrizes **WCAG AA**.

---

## ⚙️ Pré-requisitos e Execução local

Para rodar o projeto localmente, você precisará ter instalado em sua máquina:
* [Java 17+](https://www.oracle.com/java/technologies/javase-downloads.html) ou superior
* [Maven](https://maven.apache.org/)
* [MySQL](https://dev.mysql.com/downloads/mysql/)

### Banco de dados e migrações

O esquema do banco é criado pelo Flyway na inicialização do backend. As migrações ficam em `backend/espreitar/src/main/resources/db/migration`; a migração inicial `V1__criar_schema_inicial.sql` cria as tabelas das entidades. O Hibernate usa `ddl-auto=validate` para conferir o esquema, sem criar ou alterar tabelas por conta própria.

O backend está configurado para conectar em `localhost:3310/espreitar`. O usuário padrão é `user_lbd`; defina `DB_PASSWORD` com a senha configurada no seu MySQL antes de iniciar. Por exemplo, no Linux/macOS:

```bash
export DB_USER=user_lbd
read -rsp "Senha do MySQL: " DB_PASSWORD
echo
export DB_PASSWORD
cd backend/espreitar
./mvnw spring-boot:run
```

O MySQL precisa estar ativo, ter o banco `espreitar` criado e conceder ao usuário da aplicação permissão para criar tabelas, índices e chaves estrangeiras. Na primeira inicialização, o Flyway aplica a `V1`; nas seguintes, consulta `flyway_schema_history` e executa apenas migrações ainda não aplicadas.

O `docker-compose.yml` na raiz do repositório inicia um MySQL local alternativo. O stack `lesson_mysql` usado no ambiente de desenvolvimento é externo e não faz parte deste repositório. Se usar o Compose do projeto, configure `DB_PASSWORD` com a senha definida nele; se usar o stack externo, configure a senha desse stack. Não versione senhas reais.

### Estado atual

Até este marco, o backend tem entidades JPA, repositórios e a migração inicial do Flyway. A migração `V1` foi aplicada e validada no banco `espreitar`. Ainda faltam serviços, controladores/endpoints e a interface própria; a tela de login atual é a página padrão do Spring Security.
