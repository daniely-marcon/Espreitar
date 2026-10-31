const API_BASE = "";
const $ = (selector) => document.querySelector(selector);
const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
})[char]);
const safeImage = (value) => {
    if (!value) return "";
    if (value.startsWith("data:image/jpeg;base64,")) return escapeHtml(value);
    try {
        const url = new URL(value, location.origin);
        return url.protocol === "https:" || url.origin === location.origin ? escapeHtml(url.href) : "";
    } catch {
        return "";
    }
};
const stars = (value) => {
    const note = Math.max(0, Math.min(5, Math.round(Number(value) || 0)));
    return `<span class="estrelas" aria-label="${note} de 5 estrelas">${"★".repeat(note)}${"☆".repeat(5 - note)}</span>`;
};
const go = (url) => { location.href = url; };
const toast = (message) => {
    const notice = document.createElement("div");
    notice.className = "aviso-flutuante";
    notice.textContent = message;
    document.body.appendChild(notice);
    setTimeout(() => notice.remove(), 3000);
};

let auth = null;
let notificationInterval = null;
try {
    auth = JSON.parse(localStorage.getItem("espreitar.auth") || "null");
} catch {
    localStorage.removeItem("espreitar.auth");
}
document.addEventListener("click", (event) => {
    if (!event.target.closest(".acesso")) {
        document.querySelectorAll(".menu-suspenso").forEach((menu) => { menu.hidden = true; });
    }
});
const me = () => auth?.usuario || null;
const setAuth = (response) => {
    auth = response;
    localStorage.setItem("espreitar.auth", JSON.stringify(response));
    header();
};
const api = async (path, options = {}) => {
    const headers = new Headers(options.headers || {});
    if (auth?.token) headers.set("Authorization", `Bearer ${auth.token}`);
    if (options.body && typeof options.body !== "string") {
        headers.set("Content-Type", "application/json");
        options = { ...options, body: JSON.stringify(options.body) };
    }
    const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
    if (response.status === 204) return null;
    const data = await response.json().catch(() => null);
    if (!response.ok) {
        if (response.status === 401 && auth?.token) {
            auth = null;
            localStorage.removeItem("espreitar.auth");
        }
        throw new Error(data?.detail || data?.message || `Falha na solicitação (${response.status}).`);
    }
    return data;
};
const requireLogin = () => {
    if (me()) return true;
    go("login.html");
    return false;
};
const requireAdmin = () => {
    if (me()?.perfil === "ADMIN") return true;
    go("index.html");
    toast("Acesso restrito à administração.");
    return false;
};
const poster = (url, title) => {
    const source = safeImage(url);
    return `<div class="capa">${source
        ? `<img src="${source}" alt="Pôster de ${escapeHtml(title)}" loading="lazy">`
        : `<span>${escapeHtml(title)}</span>`}</div>`;
};

function header() {
    const container = $("#cabecalho");
    if (!container) return;
    if (document.body.dataset.simples) {
        container.innerHTML = `<header class="cabecalho simples"><a class="logo" href="index.html"><img src="../midia/logo (2).png" width="92" height="92" alt="Logo do Espreitar"><h1>Espreitar</h1></a></header>`;
        return;
    }
    const user = me();
    container.innerHTML = `<header class="cabecalho">
      <a class="logo" href="index.html"><img src="../midia/logo (2).png" width="64" height="64" alt="Logo do Espreitar"><h1>Espreitar</h1></a>
      <div class="busca">
        <select class="categoria" id="categoria" aria-label="Categoria">
          <option value="">Filme / Série</option><option value="FILME">Filme</option><option value="SERIE">Série</option>
        </select>
        <select class="classificacao" id="classificacao" aria-label="Classificação">
          <option value="">Classificação</option>${[5, 4, 3, 2, 1].map((n) => `<option value="${n}">${"★".repeat(n)} ou mais</option>`).join("")}
        </select>
      </div>
      <form class="pesquisa" id="pesquisa" role="search">
        <input type="search" id="campo-pesquisa" placeholder="Buscar..." aria-label="Buscar por nome">
        <button type="submit" aria-label="Buscar"><i class="fa fa-search"></i></button>
      </form>
      <aside class="acesso">${user
        ? `<button class="btn-icone botao-notificacao" id="notificacao" type="button" aria-label="Notificações">
             <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/></svg>
             <span class="notificacao-contador" id="notificacao-contador" hidden></span>
           </button>
           ${user.fotoPerfil && safeImage(user.fotoPerfil)
                ? `<img class="foto-usuario" src="${safeImage(user.fotoPerfil)}" alt="Foto de ${escapeHtml(user.nome)}">`
                : ""}
           <div class="usuario"><div class="saudacao">Olá, ${escapeHtml(user.nome)}</div></div>
           <button class="btn-icone" id="botao-menu" type="button" aria-label="Menu">⋮</button>
           <div class="menu-suspenso" id="menu-opcoes" hidden>
             <a href="dados.html">Alterar dados</a><a href="user.html">Minhas revisões</a>
             ${user.perfil === "ADMIN" ? '<a href="admin_denuncias.html">Denúncias</a><a href="admin_solicitacoes.html">Pedidos</a><a href="admin_filmes.html">Catálogo</a><a href="admin_usuarios.html">Usuários</a>' : ""}
             <button type="button" id="sair">Sair</button>
           </div><div class="menu-suspenso" id="lista-notificacoes" hidden></div>`
        : `<button class="registrar" type="button" onclick="go('register.html')">Registrar</button>
           <button class="enviar" type="button" onclick="go('login.html')">Login</button>`}
      </aside>
    </header>`;
    const buscarCatalogo = () => {
        const query = new URLSearchParams();
        if ($("#categoria").value) query.set("type", $("#categoria").value);
        if ($("#classificacao").value) query.set("rating", $("#classificacao").value);
        if ($("#campo-pesquisa").value.trim()) query.set("q", $("#campo-pesquisa").value.trim());
        go(`index.html${query.size ? `?${query}` : ""}`);
    };
    $("#pesquisa")?.addEventListener("submit", (event) => {
        event.preventDefault();
        buscarCatalogo();
    });
    $("#categoria")?.addEventListener("change", buscarCatalogo);
    $("#classificacao")?.addEventListener("change", buscarCatalogo);
    $("#botao-menu")?.addEventListener("click", () => toggleMenu("menu-opcoes"));
    $("#sair")?.addEventListener("click", logout);
    $("#notificacao")?.addEventListener("click", mostrarNotificacoes);
    if (notificationInterval !== null) {
        window.clearInterval(notificationInterval);
        notificationInterval = null;
    }
    if (user) {
        atualizarIndicadorNotificacoes();
        notificationInterval = window.setInterval(() => atualizarIndicadorNotificacoes(), 30_000);
    }
    const query = new URLSearchParams(location.search);
    if ($("#categoria")) $("#categoria").value = query.get("type") || "";
    if ($("#classificacao")) $("#classificacao").value = query.get("rating") || "";
    if ($("#campo-pesquisa")) $("#campo-pesquisa").value = query.get("q") || "";
}

function toggleMenu(id) {
    const menu = document.getElementById(id);
    if (!menu) return;
    document.querySelectorAll(".menu-suspenso").forEach((item) => { item.hidden = true; });
    menu.hidden = !menu.hidden;
}

async function mostrarNotificacoes() {
    const menu = $("#lista-notificacoes");
    if (!menu) return;
    try {
        const notifications = await api("/usuarios/me/notificacoes");
        menu.innerHTML = notifications.length
            ? notifications.map((item) => `<p class="aviso">${escapeHtml(item)}</p>`).join("")
            : '<p class="aviso aviso-vazio">Sem notificações</p>';
        toggleMenu("lista-notificacoes");
        if (notifications.length) {
            await api("/usuarios/me/notificacoes", { method: "DELETE" });
            atualizarIndicadorNotificacoes(0);
        }
    } catch (error) {
        toast(error.message);
    }
}

async function atualizarIndicadorNotificacoes(total = null) {
    const indicator = $("#notificacao-contador");
    const button = $("#notificacao");
    if (!indicator || !button) return;
    try {
        const notifications = total === null ? await api("/usuarios/me/notificacoes") : null;
        const count = total === null ? notifications.length : total;
        indicator.textContent = count > 9 ? "9+" : String(count);
        indicator.hidden = count === 0;
        button.classList.toggle("tem-notificacoes", count > 0);
        button.setAttribute("aria-label", count ? `Notificações: ${count} novas` : "Notificações");
    } catch (error) {
        console.error("Não foi possível consultar notificações.", error);
    }
}

function logout() {
    auth = null;
    localStorage.removeItem("espreitar.auth");
    go("index.html");
}

let render = () => {};
window.api = api;
window.me = me;
window.setAuth = setAuth;
window.go = go;
window.toast = toast;
window.header = header;
window.escapeHtml = escapeHtml;
window.poster = poster;
window.stars = stars;
window.requireLogin = requireLogin;
window.requireAdmin = requireAdmin;
window.getRender = () => render;
window.setRender = (callback) => { render = callback; };
window.logout = logout;
