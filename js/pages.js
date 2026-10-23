/* ===== Lógica de cada página ===== */

let page = 0,
    spoiler = false;

/* index.html — Em Destaque */
function pageHome() {
    /* completar*/


    $("#conteudo").innerHTML =
        `<main>
             <h2 class="secao-titulo">Em Destaque</h2>
             <div class="carrossel">
                 <button class="seta" onclick="render()">◀</button>
                     <div class="grid-filmes">
                          <div class="card" onclick="go('filme.html?id='/* completar id*/)">
                                  <h3></h3>  <!--====== completar com titulo* -->
                                  <small class="avaliacao"></small>
                          </div>
                    <p>Nada encontrado.</p>
             </div>
             <button class="seta" >▶</button>
             </div>
    </main>`;
}

/* filme.html?id=1 — detalhe + revisões */
function pageFilme() {
    const id = Q.get("id"),
        conteudo = idConteudo(id),
        user = me();
    if (!conteudo) {
        $("#conteudo").innerHTML = "<main>Filme não encontrado.</main>";
        return;
    }
    const rs = 0; /* construir id filme e revisao */

    $("#conteudo").innerHTML =
        `<main>
        <div class="detalhe">${cartaz(conteudo)}<div>
            <h2 class="titulo-filme">${especiais(/* completar*/)} <small class="tipo-filme">(/* completar*/)</small></h2>        
            <div>${estrelas(notaMedia(id))}</div>
             <small>${conteudo.year}</small><p>${especiais(conteudo.desc)}</p>
            </div
        ></div>
        <h2 class="secao-titulo">Revisões</h2>
            <label class="filtro-spoiler">
         <input type="checkbox"> Spoiler</label>
    ${
            rs
                .map((r) => {
                    const o  = null ; /* completar*/
                    const borrar = null; /* completar*/
                    return `<div class="revisao">${ftPerfil()}<div>
                              <h3><small class="selo-spoiler">[spoiler]</small></h3>
                              <div class="borrado"</div>
                                  <small class="autor">por ${especiais(/* completar*/)}</small>
                                  <button class="btn-link" onclick=report()>Denunciar</button>
                              </div>
                          </div>`;
                            })
                            .join("") || "<p>Seja o primeiro a avaliar.</p>"
                    }
                        <button class="btn-adicionar" onclick="abrirRevisao(/* completar*/)" title="Adicionar comentário">+</button>
          </main>`;
}



function report(id) { toast("Denúncia enviada à moderação.");}  /*====== completar funcao  */

/* revisoes.html — Minhas Revisões */
function pageRevisoes() {
    if (!requireLogin())
        return;
    const u = me();
    $("#conteudo").innerHTML =
        `<main>
        <h2 class="secao-titulo">Minhas Revisões</h2>
        <div class="grid-revisoes">
         ${
            S.reviews
                .filter()/* completar funcao de verificao do perfil */
                .map(
                    () => `<div class="card-revisao">
                            <h3>${especiais()}</h3>
                            <small>${especiais(idConteudo())}</small> ${estrelas()}
                            <p>${especiais()}</p>
         <button onclick="abrirRevisao()" title="Editar">✏</button>
         <button onclick="deletarRevisao()" title="Excluir"></button>
         </div>`,
                )
                .join("") || "<p>Você ainda não fez revisões.</p>"}
         </div>
 </main>`;
}

function deletarRevisao(id) {} /* completar*/

/* dados.html — Atualizar Dados Cadastrados */
function pageDados() {
    if (!requireLogin()) return;
    const u = null ; /* completar c verificacao */

    $("#conteudo").innerHTML =
        `<main>
              <h2 class="secao-titulo centralizado">Atualizar Dados Cadastrados</h2>
              <div class="formulario">
                 <label>Nome</label>
                 <input id="dados-nome" value="${especiais(/* completar*/ )}">
                 <label>E-mail</label>
                 <input id="dados-email" type="email" value="${especiais(/* completar*/ )}">
                 <label>Senha</label>
                 <input id="dados-senha" type="password" placeholder="Nova senha (opcional)">
             <div class="acoes">
                 <button class="cancelar" onclick="history.back()">Cancelar</button>
                 <button class="enviar" onclick="saveData()">Atualizar ️</button>
             </div></div>
        </main>`;
}

function saveData() {
    const u = null ;/* completar*/
    const n = $("#dados-nome").value.trim();
    const e = $("#dados-email").value.trim();
    const p = $("#dados-senha").value;
    if (!n || !e)
        return toast("Preencha nome e e-mail.");
    /* completar*/
    salvar();
    go("index.html");
}

/* login.html */
function pageLogin() {
    $("#conteudo").innerHTML = `<main>
    <form class="formulario" id="form-login">
      <div class="campos">
        <h2 class="secao-titulo">Login</h2>
        <label for="email">E-mail</label>
        <input type="email" id="email" placeholder="email" required>
        <label for="senha">Senha</label>
        <input type="password" id="senha" placeholder="senha" required>
      </div>
      <button type="button" class="gsi-material-button" onclick="toast('Login com Google requer OAuth no back-end.')">
      <div class="gsi-material-button-state"></div>
      <div class="gsi-material-button-content-wrapper">
        <div class="gsi-material-button-icon">
          <svg version="1.1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" xmlns:xlink="http://www.w3.org/1999/xlink" style="display: block;">
            <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"></path>
            <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"></path>
            <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"></path>
            <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"></path>
          </svg>
        </div>
        <span class="gsi-material-button-contents">Sign in with Google</span>
      </div>
    </button>
      <div class="acoes">
        <button type="button" class="cancelar" onclick="go('index.html')">Cancelar</button>
        <button type="submit" class="enviar">Login</button>
      </div>
      
    </form>
  </main>`;
    $("#form-login").onsubmit = (e) => {
        e.preventDefault();
        login();
    };
}


function login() {
    const u = null;/*receber user e completar funcao*/
    if (!u) return toast("E-mail ou senha incorretos.");

    salvar();
    go("index.html");
}

/* registro.html — cadastro livre, sem aprovação */
function pageRegistro() {
    $("#conteudo").innerHTML = `<main>
    <form class="formulario" id="form-registro">
      <div class="campos">
        <h2 class="secao-titulo">Registrar-se</h2>
        <label for="nome">Nome</label>
            <input type="text" id="nome" required>
        <label for="email">E-mail</label>
            <input type="email" id="email" required>
        <label for="senha">Senha</label>
            <input type="password" id="senha" required>
        <label for="confirmar-senha">Confirmar senha</label>
            <input type="password" id="confirmar-senha" required>
        <label for="imagem">Foto de perfil</label>
            <input type="file" accept="image/*" alt="Selecionar imagem" optional>
      </div>
      <div class="acoes">
        <button type="button" class="cancelar" onclick="go('index.html')">Cancelar</button>
        <button type="submit" class="enviar">Confirmar</button>
      </div>
    </form>
  </main>`;
    $("#form-registro").onsubmit = (e) => {
        e.preventDefault();
        registrar();
    };
}
function registrar() {
    const nome = $("#nome").value.trim(),
        email = $("#email").value.trim(),
        pass = $("#senha").value;
    if (!nome || !email || !pass)
        return toast("Preencha todos os campos.");

    /* incluir verificacao da senha*/

    salvar();
    go("index.html");
}

/* Gerenciamento (somente admin) */
const admin = (t, b) =>
    `<main>
        <div class="painel">
            <nav>
                <a href="admin-denuncias.html">Denúncias</a>
                <a href="admin-filmes.html">Novo filme / série</a>
            </nav>
            <section>
                <h2 class="secao-titulo">${t}</h2>${b}
            </section>
        </div>
    </main>`;

function pageDenuncias() {
    if (!requireAdmin()) return;
    $("#conteudo").innerHTML = admin(
        "Denúncias Pendentes",
        S.reports
            .map()/* ajustar para revisão == user*/
            .filter(Boolean)
            .map(( ) => `<div class="item-denuncia">
                     <div> dono do comentário <br>
                     </div>
                     <button class="botao-manter" onclick="manterComentario()">Manter</button>
                     <button class="botao-remover" onclick="removeComentario()">Remover</button>
                     </div>`,
            )
            .join("") || "<p>Nenhuma denúncia pendente.</p>",
    );
}
function manterComentario() {

    salvar();
    render();
} /* completar*/


function removeComentario() {

    salvar();
    render();
} /* completar*/

function pageFilmes() {
    if (!requireAdmin()) return;
    $("#conteudo").innerHTML = admin(
        "Adicionar ao Catálogo",
        `<div class="formulario" style="margin:0">
            <label>Título</label>
            <input id="filme-titulo">
            <label>Tipo</label>
            <select id="filme-tipo">
                <option>Filme</option>
                <option>Série</option>
            </select>
            <label>Ano</label>
            <input id="filme-ano" type="number">
            <label>Sinopse</label>
            <textarea id="filme-sinopse" rows="4"></textarea>
            <label>Pôster (imagem)</label >
            <input id="filme-poster" type="file" accept="image/*">
          <div class="acoes">
              <button class="enviar" onclick="addConteudo()">Adicionar</button>
          </div>
      </div>`,
    );
}


function addConteudo(){} /* fazer funcao*/

/* Inicialização */
render = {
    home: pageHome,
    filme: pageFilme,
    revisoes: pageRevisoes,
    dados: pageDados,
    login: pageLogin,
    registro: pageRegistro,
    denuncias: pageDenuncias,
    filmes: pageFilmes,
}[document.body.dataset.page];


header();
render();
