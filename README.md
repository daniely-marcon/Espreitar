# 🎬 Espreitar - Sistema de Avaliação de Mídias

**Espreitar** é uma aplicação web desenvolvida para que usuários possam descobrir, avaliar e comentar sobre filmes e séries. O sistema consome dados de uma API externa para montar o catálogo e permite que a comunidade construa um acervo de críticas e notas (de 0 a 5 estrelas).

Este projeto foi desenvolvido como requisito de avaliação para a disciplina de **Programação Para WEB** da **Universidade Federal de Mato Grosso do Sul (UFMS)**, sob a orientação do Prof. Matheus Albuquerque de Melo.

---

## 🚀 Funcionalidades

* **Exploração de Catálogo:** Busca de filmes e séries por título consumindo dados de uma API externa (capa, sinopse, ano).
* **Trailers:** Reprodução na página da mídia de trailers do YouTube importados do TMDb ou adicionados manualmente.
* **Sistema de Avaliações:** Usuários logados podem atribuir notas de 0 a 5 estrelas e adicionar comentários em texto às obras.
* **Gestão de Histórico:** Cada usuário possui um perfil próprio para gerenciar, editar ou excluir suas avaliações anteriores.
* **Autenticação Flexível:** Cadastro manual (e-mail e senha) e suporte a login integrado (Google OAuth).
* **Recuperação de senha:** Redefinição segura por link temporário enviado por SMTP; contas Google também podem definir uma senha para entrar por e-mail.
* **Moderação:** Painel administrativo para configurar o limite de denúncias antes da remoção automática de uma avaliação, com notificação à pessoa autora.
* **Cadastro de catálogo:** Inclusão manual de filmes e séries ou preenchimento assistido pelos dados do TMDb.
* **Acessibilidade e telas móveis:** Navegação por teclado, foco visível, atalho para o conteúdo, estados de carregamento acessíveis, respeito à preferência por movimento reduzido e layout adaptado para celular.

---

## 👥 Perfis de Acesso

O sistema gerencia diferentes níveis de permissão através do Spring Security:
1. **Visitante:** Acesso anônimo apenas para leitura (busca de mídias e visualização de avaliações alheias).
2. **Usuário Comum (ROLE_USER):** Acesso autenticado. Pode publicar novas avaliações, além de editar e excluir o próprio histórico.
3. **Administrador (ROLE_ADMIN):** Acesso autenticado com privilégios de moderação. Pode excluir avaliações denunciadas e contas de usuário; o sistema impede excluir a própria conta ou o último administrador.

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

### Identidade visual e interações

A marca é apresentada como **Espreitar** no cabeçalho e nos títulos das páginas. No catálogo, os cards de filmes e séries têm um efeito suave ao passar o mouse: elevam-se e inclinam levemente, enquanto o pôster ganha zoom. O efeito também funciona ao navegar por teclado e é limitado a dispositivos com mouse; em telas sensíveis ao toque, os cards permanecem sem esse movimento.

---

## ⚙️ Pré-requisitos e Execução local

Para rodar o projeto localmente, você precisará ter instalado em sua máquina:
* [Java 25 LTS](https://www.oracle.com/java/technologies/javase-downloads.html)
* [Maven](https://maven.apache.org/)
* [MySQL](https://dev.mysql.com/downloads/mysql/)

### Banco de dados e migrações

O esquema do banco é criado pelo Flyway na inicialização do backend. As migrações ficam em `backend/espreitar/src/main/resources/db/migration`; `V1` cria o esquema inicial, `V2` adiciona a unicidade das denúncias, `V3` amplia o armazenamento de fotos de perfil, `V4` armazena as notas e os votos do TMDb separados das avaliações da comunidade, `V5` guarda os metadados TMDb dos pedidos de inclusão, `V6` adiciona trailers, `V7` registra o motivo de cada denúncia e `V8` cria a configuração do limite de denúncias e os tokens de recuperação de senha.

O backend conecta ao MySQL em `localhost:3310/espreitar`. Para iniciar o MySQL e o phpMyAdmin do projeto, na raiz do repositório execute:

```bash
docker compose up -d
```

O arquivo `.env` versionado está vazio. Para rodar o projeto localmente, preencha-o com as variáveis de `.env.example`, usando valores apropriados para seu ambiente. Para iniciar o backend, abra outro terminal no diretório `backend/espreitar` e execute:

```bash
./mvnw spring-boot:run
```

A configuração local usa `DB_PASSWORD` quando ela está definida; caso contrário, usa `123`. O backend usa a porta `8081` e fica acessível apenas no próprio computador (`127.0.0.1`). Em uma hospedagem, configure `SERVER_ADDRESS=0.0.0.0`; a porta pode ser informada pela variável `PORT` e a conexão JDBC do banco por `DB_URL`.

O phpMyAdmin fica disponível em `http://localhost:8184`; entre com `DB_USER` e `DB_PASSWORD` do `.env` e selecione o banco `espreitar`. Na primeira inicialização do backend, o Flyway aplica as migrações pendentes; nas seguintes, consulta `flyway_schema_history` e executa apenas as novas.

### Início rápido e teste do efeito dos cards

As credenciais usadas neste ambiente de testes são temporárias; as credenciais do Google serão revogadas após os testes. O `.env` incluído no Git está vazio apenas para indicar o arquivo necessário. Preencha uma cópia local com as variáveis de `.env.example` para executar o projeto. Nunca faça commit ou push do `.env` preenchido, pois ele pode conter credenciais.

Depois, execute na raiz do repositório:

```bash
docker compose up -d
cd backend/espreitar
./mvnw spring-boot:run
```

Quando o backend terminar de iniciar, abra `http://localhost:8081/` no computador. Para testar a aparência de celular sem publicar o site, use o modo de dispositivo do Chrome DevTools descrito abaixo. O botão de trailer aparece no catálogo para mídias do TMDb; se ainda não estiver salvo, o app tenta buscá-lo ao abrir os detalhes. Catálogo e trailers não exigem login. Para parar o backend, use `Ctrl+C`; para parar os containers sem apagar os dados, execute `docker compose down` na raiz.

#### Simular celular no navegador

1. Inicie o banco e o backend, depois abra `http://localhost:8081/` no Chrome.
2. Pressione `F12` ou `Ctrl+Shift+I` para abrir o DevTools.
3. Pressione `Ctrl+Shift+M` (no macOS, `Cmd+Shift+M`) para ativar a barra de dispositivos.
4. Escolha um aparelho ou **Responsive** e teste larguras como 375 px e 320 px; recarregue a página se necessário.
5. Teste catálogo, busca, botão de trailer, login, menus e formulários. A simulação ajuda a revisar layout e responsividade, mas não substitui um teste em um aparelho real.

Para usar o login Google localmente, configure `GOOGLE_CLIENT_ID` no `.env` com o ID do mesmo cliente OAuth usado pelo app e autorize `http://localhost:8081` em **Origens JavaScript autorizadas** no Google Cloud Console. Em uma publicação, autorize a origem HTTPS do domínio público; IPs privados da rede local não são aceitos pelo Google. Também é possível entrar e registrar com e-mail e senha. As senhas novas precisam ter ao menos 8 caracteres, uma letra maiúscula, uma minúscula, um número e um símbolo. Quem entrou com Google pode definir uma senha na tela **Alterar dados** e também passar a entrar por e-mail e senha.

Na administração do catálogo, escolha **Com ajuda do TMDb** para buscar e importar os metadados ou **Cadastro manual** para preencher título, tipo, ano, sinopse, pôster e, opcionalmente, o link de um trailer do YouTube. Os links de vídeo do YouTube são convertidos para um formato seguro de incorporação. O modo assistido importa a nota externa (convertida da escala 0–10 para 0–5), a quantidade de votos e o trailer quando disponíveis. Quando não há trailer cadastrado/encontrado, a tela de detalhes informa apenas que o trailer está indisponível. No painel de denúncias, a administração configura o limite de 1 a 100 denúncias únicas; ao atingir o limite, a avaliação é removida e a pessoa autora recebe uma notificação no app. O valor inicial é 5. Para conceder acesso administrativo a uma conta criada, use o seletor de perfil em **Usuários** ou execute no banco local, substituindo o e-mail:

```sql
UPDATE usuario SET perfil = 'ADMIN' WHERE email = 'admin@example.com';
```

### Recuperação de senha por e-mail

A recuperação usa SMTP e links de uso único com validade de 30 minutos. Para testar localmente com Gmail, preencha no `.env` `MAIL_USERNAME` com seu endereço Gmail, `MAIL_PASSWORD` com uma senha de app do Google e `MAIL_FROM` com o mesmo endereço. O host, a porta, TLS e a URL local já estão configurados no exemplo abaixo. Não use a senha normal da conta Google nem publique credenciais SMTP:

```text
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seu-endereco@gmail.com
MAIL_PASSWORD=sua-senha-de-app-do-google
MAIL_FROM=seu-endereco@gmail.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
APP_PUBLIC_URL=http://localhost:8081
```

Ative a verificação em duas etapas na conta Google e crie uma senha de app na área de segurança da Conta Google. `MAIL_PASSWORD` recebe essa senha de app, não a senha normal. O `.env` é carregado automaticamente pelo Spring Boot ao executar o projeto a partir da pasta `backend/espreitar`. `APP_PUBLIC_URL` é a origem usada para montar o link de redefinição; localmente mantenha `http://localhost:8081`. Depois de preencher as credenciais, reinicie o backend e teste **Esqueci minha senha**. Sem SMTP configurado, o serviço avisa que a recuperação por e-mail ainda não está disponível. O formulário não revela se o endereço informado possui uma conta. No Render, configure as mesmas variáveis no painel do serviço e use a URL HTTPS pública em `APP_PUBLIC_URL`.

### Publicar na Oracle Cloud Always Free

Para este projeto, a opção gratuita mais compatível é uma VM **Oracle Cloud Always Free**: mantém o MySQL atual e roda Java, MySQL e HTTPS juntos. A camada gratuita pode ter falta de capacidade na região escolhida e instâncias consideradas ociosas podem ser recuperadas; consulte as [condições oficiais](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm). Você precisará de uma conta Oracle e de um nome DNS. Para um subdomínio gratuito, consulte o [DuckDNS](https://www.duckdns.org/about.jsp); é um serviço DNS comunitário, sem garantia de disponibilidade.

#### 1. Criar a VM

No console Oracle Cloud, crie uma instância Compute Always Free com Ubuntu 24.04 ARM/Ampere A1. Uma configuração de 1 OCPU e 6 GB RAM fica dentro dos limites Always Free publicados e deixa margem para o MySQL e o Java; a disponibilidade de Ampere varia por região. Guarde o IP público e a chave SSH privada com segurança.

Na rede da instância, permita entrada TCP para 22 (restrinja ao seu IP, se possível), 80 e 443. Depois da conexão, se o firewall UFW estiver ativo, libere as mesmas portas:

```bash
ssh -i caminho/para/chave.pem ubuntu@IP_PUBLICO
```

```bash
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
```

#### 2. Apontar um domínio gratuito para a VM

Crie um subdomínio no DuckDNS e atualize-o com o IP público da VM seguindo as instruções do próprio DuckDNS. Anote o nome completo, por exemplo `espreitar.duckdns.org`; o Caddy precisa que o DNS já resolva para a VM e que as portas 80/443 estejam acessíveis para emitir HTTPS.

#### 3. Instalar Docker e copiar o projeto

Na VM Ubuntu:

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2 git
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```

Saia da sessão SSH e conecte novamente para aplicar o grupo Docker. Depois, clone o repositório no qual você publicou o projeto (se for privado, use um método de autenticação Git seguro; nunca coloque senha ou token na URL):

```bash
git clone https://github.com/daniely-marcon/Espreitar.git
cd Espreitar
```

#### 4. Configurar segredos e subir

Crie `.env` **na VM**, sem colocar o arquivo no Git:

```bash
cp .env.example .env
nano .env
chmod 600 .env
```

Preencha `DB_USER`, `DB_PASSWORD` e `MYSQL_ROOT_PASSWORD` com senhas fortes; `JWT_SECRET` com uma chave aleatória de pelo menos 32 caracteres; `TMDB_ACCESS_TOKEN` com seu token TMDb; `GOOGLE_CLIENT_ID` com seu cliente OAuth; e `APP_DOMAIN` com o subdomínio completo do DuckDNS. Não copie para o `.env` o texto de exemplo `APP_DOMAIN=app.seudominio.com` como se fosse um domínio seu.

Então inicie os serviços:

```bash
sudo docker compose -f compose.oracle.yml up -d --build
sudo docker compose -f compose.oracle.yml logs -f app
```

Quando os logs mostrarem que o Spring iniciou, abra `https://SEU_SUBDOMINIO.duckdns.org`. No Google Cloud Console, cadastre exatamente essa origem HTTPS em **Origens JavaScript autorizadas** do cliente OAuth. Para publicar atualizações futuras:

```bash
cd ~/Espreitar
git pull
sudo docker compose -f compose.oracle.yml up -d --build
```

Os dados ficam no volume `mysql_oracle`; não execute `docker compose -f compose.oracle.yml down -v` se quiser preservá-los. Veja os logs com `sudo docker compose -f compose.oracle.yml logs -f app`. O Compose não publica o MySQL nem a porta interna do app para a internet.

Saia e entre novamente para receber um token com a nova permissão. Avaliações são únicas por pessoa e mídia; cada pessoa também pode enviar apenas uma denúncia por avaliação. Denúncias não removem conteúdo automaticamente: a decisão fica com a moderação.

Para encerrar os containers sem apagar os dados, use `docker compose down`. Não use `docker compose down -v`, pois essa opção remove também o volume do MySQL. Os valores de exemplo, como a senha `123`, servem apenas para desenvolvimento local e devem ser substituídos por valores apropriados antes de compartilhar o ambiente. Antes de qualquer commit ou push, confirme que o `.env` está vazio.

### Testes

Execute os testes automatizados no diretório `backend/espreitar`:

```bash
./mvnw test
```

Os testes de integração usam o MySQL configurado em `application.properties` e aplicam as migrações do Flyway. Inicie o banco com Docker Compose e configure `DB_USER` e `DB_PASSWORD` antes de executar `./mvnw test`. Eles cobrem cadastro e login local, carregamento do frontend e catálogo, prioridade da revisão da pessoa logada, aprovação e recusa de pedidos com notificações, cálculo da média e limite de denúncias.

Para popular o banco de desenvolvimento com registros fictícios e testar a busca no frontend, execute o fixture [catalogo-demo.sql](./backend/espreitar/src/test/resources/fixtures/catalogo-demo.sql) no banco `espreitar`. Use esses dados apenas em desenvolvimento/teste; eles não são inseridos automaticamente em produção.

### Estado atual

O frontend está servido pelo próprio Spring Boot em `http://localhost:8081/` e consome a API no mesmo host. A API usa JWT para autenticação, autorização por perfil, validação de dados e migrações Flyway.
