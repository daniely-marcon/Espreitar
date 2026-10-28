/* ===== Código compartilhado por todas as páginas ===== */

document.addEventListener("click", (event) => {
    if (!event.target.closest(".acesso"))
        document.querySelectorAll(".menu-suspenso").forEach((p) => (p.hidden = true));
});

const especiais = (s) =>
    String(s).replace(
        /[&<>"]/g,
        (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c],
    ); /* funcao de seguraça -innerHTML*/

const $ = (s) => document.querySelector(s);

let S;

try { S = JSON.parse(localStorage.getItem("espreitar"));
} catch (e) {}  /* ==== completar back ==== */


/* Filtros (ficam na URL para funcionar entre páginas) */
const Q = new URLSearchParams(location.search);
const F = { type: Q.get("type") || "", rating: Q.get("rating") || "", q: Q.get("q") || "" };


/* Arrow functions temporarias para serem alteradas com a verificação no back*/

const me = () => S.users.find((u)=> u.id === S.me); /* tipo de usuario logado*/
const user = (id) => S.users.find((u) => u.id === id) || { name: "?", av: 0 }; /* usario geral*/
const idConteudo = (id) => S.content.find((m) => m.id == id);

const notaMedia = (id) => {
    const media = S.reviews.filter((x) => x.movie == id);
    return media.length ? Math.round(media.reduce((a, x) => a + x.stars, 0) / media.length) : 0;
};

const estrelas = (n) => `<span class="estrelas">${"★".repeat(n)}${"☆".repeat(5 - n)}</span>`;
const ftPerfil = (u) =>
    `<div class="avatar"> <img ${u.img} ></div>`;
const cartaz = (m) =>
    `<div class="capa" style=" /* completar com cartaz do filme*/ </div>`;
/* === alterar até aqui */

const go = (url) => (location.href = url);

let render = () => {};

const logo_ = `<a class="logo" href="index.html"><img src="../midia/logo (2).png"  width="92" height="92" alt="Logo do Espreitar"><h1>espreitar.</h1></a>`;


/* Cabeçalho -  */
function htmlAcesso(user, qtdAvisos) {
    /* ==== usuario nao esta logado ==== */
    if (!user) {
        return `<button class="registrar" id="resgitrar" onclick="tog('register.html')" >Registrar</button>
      <button class="enviar"  id="login" onclick="tog('login.html')" >Login</button>`;
    }

    const avisos =  qtdAvisos;/*==== completar com os avisos para usuario logado====*/

    return `<button class="btn-icone" id="notificacao" onclick="tog('lista-notificacoes')">
                    <img src="../midia/notifications.png"><span class="contador"></span> 
            </button>

            <div class="usuario">
                <div class="saudacao">Bem-Vindo<br> </div>
            </div>
            <button class="btn-icone" id="botao-menu" onclick="tog('menu-opcoes')">⋮</button>
                   
            <div class="menu-suspenso" id="menu-opcoes" hidden>
              <a href="dados.html">Alterar dados</a>
              <a href="user.html">Minhas revisões</a>   
              <a href="admin-denuncias.html">Gerenciamento</a> 
              /*==== completar com verificacao do tipo do usuario  para usuario logado====*/        
              <a onclick="logout()">Sair</a>      
            </div>
            
            <div class="menu-suspenso" id="lista-notificacoes" hidden>
                <div class="aviso"></div>
                <div class="aviso aviso-vazio">Sem notificações</div>
            </div>`
}


function header(){
    $("#cabecalho").innerHTML = `<header class="cabecalho">
    ${logo_}
    <div class="busca">
      <select class="categoria" id="categoria" aria-label="Categoria">
        <option value="">Filme / Série</option>
        <option>Filme</option>
        <option>Série</option>
      </select>
      <select class="classificacao" id="classificacao" aria-label="Classificação">
        <option value="">Classificação</option>
        ${[5, 4, 3, 2, 1].map((n) =>
        `<option value="${n}">${"★".repeat(n)} ou mais</option>`).join("")}
      </select>
      
    </div>
    <form class="pesquisa" id="pesquisa" role="search">
      <input type="search" id="campo-pesquisa" placeholder="Buscar..." aria-label="Buscar por nome">
      <button type="submit" aria-label="Buscar"><i class="fa fa-search"></i></button>
    </form>
    
    <aside class="acesso">
        <button class="registrar" id="resgitrar" onclick="go('register.html')" >Registrar</button>
        <button class="enviar"  id="login" onclick="go('login.html')" >Login</button>
      </aside>
  </header>`;
    $("#categoria").value = F.type;
    $("#classificacao").value = F.rating;
    $("#campo-pesquisa").value = F.q;
}


function headerSimples() {
    if (document.body.dataset.simples) {
        $("#cabecalho").innerHTML = `<header class="cabecalho simples">${logo_}</header>`;
    }

}

function tog(id) {
    const e = $("#" + id),
        h = e.hidden;
    document.querySelectorAll(".menu-suspenso").forEach((p) => (p.hidden = true));
    e.hidden = !h;
    if (id === "lista-notificacoes" && h) {
         salvar();
    }
}

const buscar = () => {
     F.type = $("#categoria").value;
     F.rating = $("#classificacao").value;
     F.q = $("#campo-pesquisa").value;
     if (document.body.dataset.page === "home") {
         page = 0;
         render();
     } else go("index.html?" + new URLSearchParams(F));
 };/*
 $("#categoria").onchange = buscar;
 $("#classificacao").onchange = buscar;
 $("#campo-pesquisa").oninput = () => {
     if (document.body.dataset.page === "home") buscar();


    $("#pesquisa").onsubmit = (e) => {
        e.preventDefault();
        buscar();
}
}*/


function toast(t) {
        const d = document.createElement("div");
        d.className = "aviso-flutuante";
        d.textContent = t;
        document.body.appendChild(d);
        setTimeout(() => d.remove(), 2600);
}

const requireLogin = () => {
       if (!me()) {
           go("login.html");
           return false;
       }
       return true;
};

const requireAdmin = () => {
      const user = me();
      if (!user || user.papel !== "admin") { /*=== completar / alterar===*/
           go("login.html");
           return false;
      }
      return true;
};

const salvar = () => {
    try {
        localStorage.setItem("espreitar", JSON.stringify(S));
    } catch (e) {}
}; /* ==== completar funcao ==== */

function logout() {
    /* ====  completar ==== */
    salvar();
    go("index.html");
}


/* Popup de comentário (usado na página do filme e em Minhas Revisões) */
let sel = 3;

function abrirRevisao() {

      $("#area-popup").innerHTML =
          `<div class="popup-fundo" onclick=")cancelar()">
                <div class="popup">
                    <input class="campo-titulo" id="popup-titulo" placeholder="Título" value=""><!-- completar c titulo*/-->
                <div class="seletor-nota" id="popup-notas">${["Ruim", "Razoável", "Bom", "Muito Bom", "Excelente"]
                            .map((l, i) => `<button data-n="${i + 1}" class="${i + 1 === sel ? "ativa" : ""}">
                            <span>${i + 1 === sel ? "★" : "☆"}</span>${l}</button>`).join("")}
                </div>
                    <textarea id="popup-texto" placeholder="Escreva seu comentário"></textarea>
                 <label class="opcao-spoiler">
                    <input type="checkbox" id="popup-spoiler" > Contém Spoiler</label>
                 <div class="acoes">
                     <button class="cancelar" onclick="cancelar()">Cancelar</button>
                     <button class="enviar" onclick="publicarRevisao()">Publicar</button>
                 </div>
             </div>
         </div>`;

}


const cancelar = () => ($("#area-popup").innerHTML = "");

function publicarRevisao() {

        /*=== completar publicacao da revisao / alterar===*/
       salvar();
       cancelar();
       render();
}
