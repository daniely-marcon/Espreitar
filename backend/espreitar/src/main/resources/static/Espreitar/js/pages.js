const pageName = document.body.dataset.page;
const query = new URLSearchParams(location.search);
let selectedRating = 3;
let currentMediaId = null;
let reviewEditing = null;
let searchTimer = null;
const loadedReviews = new Map();

const pageContent = () => document.querySelector("#conteudo");
const prettyDate = (date) => date ? new Date(date).toLocaleDateString("pt-BR") : "";
const showError = (error) => toast(error?.message || "Não foi possível concluir a operação.");

async function pageHome() {
    try {
        const searchParams = new URLSearchParams();
        if (query.get("q")) searchParams.set("q", query.get("q"));
        if (query.get("type")) searchParams.set("tipo", query.get("type"));
        if (query.get("rating")) searchParams.set("notaMinima", query.get("rating"));
        const all = await api(`/midias${searchParams.size ? `?${searchParams}` : ""}`);
        const term = (query.get("q") || "").trim().toLocaleLowerCase("pt-BR");
        const type = query.get("type") || "";
        const minRating = Number(query.get("rating") || 0);
        const media = all.filter((item) =>
            (!term || item.titulo.toLocaleLowerCase("pt-BR").includes(term))
            && (!type || item.tipo === type)
            && (Number(item.notaMedia) || 0) >= minRating);
        pageContent().innerHTML = `<main>
          <h2 class="secao-titulo">${term ? `Resultados para “${escapeHtml(term)}”` : "Em Destaque"}</h2>
          <div class="grid-filmes">${media.map((item) => `<article class="card" role="link" tabindex="0"
              onclick="go('filme.html?id=${item.id}')" onkeydown="if(event.key==='Enter')go('filme.html?id=${item.id}')">
              ${poster(item.cartaz, item.titulo)}<h3>${escapeHtml(item.titulo)}</h3>
              <small>${escapeHtml(item.ano || "")} · ${escapeHtml(item.tipo === "SERIE" ? "Série" : "Filme")}</small>
              <div class="avaliacao">${stars(item.notaMedia)} <span>${Number(item.totalAvaliacoes) || 0}</span></div>
            </article>`).join("") || "<p>Nenhuma obra cadastrada foi encontrada.</p>"}</div>
        </main>`;
    } catch (error) {
        pageContent().innerHTML = "<main><p>Não foi possível carregar o catálogo.</p></main>";
        showError(error);
    }
}

async function pageFilme() {
    currentMediaId = query.get("id");
    if (!currentMediaId) {
        pageContent().innerHTML = "<main><p>Filme não encontrado.</p></main>";
        return;
    }
    try {
        const [media, reviews] = await Promise.all([
            api(`/midias/${encodeURIComponent(currentMediaId)}`),
            api(`/avaliacoes/midia/${encodeURIComponent(currentMediaId)}`),
        ]);
        loadedReviews.clear();
        reviews.forEach((review) => loadedReviews.set(review.id, review));
        pageContent().innerHTML = `<main>
          <div class="detalhe">${poster(media.cartaz, media.titulo)}<div>
            <h2 class="titulo-filme">${escapeHtml(media.titulo)}
              <small class="tipo-filme">(${media.tipo === "SERIE" ? "Série" : "Filme"})</small></h2>
            <div>${stars(media.notaMedia)} <span>${Number(media.totalAvaliacoes) || 0} avaliações</span></div>
            <small>${escapeHtml(media.ano || "")}</small>
            <p>${escapeHtml(media.sinopse || "Sinopse indisponível.")}</p>
          </div></div>
          <h2 class="secao-titulo">Revisões</h2>
          <label class="filtro-spoiler"><input id="filtro-spoiler" type="checkbox" checked> Ocultar spoilers</label>
          <div id="lista-revisoes">${reviews.map(reviewCard).join("") || "<p>Seja a primeira pessoa a avaliar.</p>"}</div>
          <button class="btn-adicionar" id="adicionar-revisao" title="Adicionar comentário" aria-label="Adicionar comentário">+</button>
        </main>`;
        $("#adicionar-revisao").addEventListener("click", () => abrirRevisao());
        $("#filtro-spoiler").addEventListener("change", (event) => {
            document.querySelectorAll(".borrado").forEach((item) => {
                item.classList.toggle("spoiler-oculto", event.target.checked);
            });
        });
        document.querySelectorAll(".acao-denunciar").forEach((button) => {
            button.addEventListener("click", () => denunciar(button.dataset.id));
        });
        document.querySelectorAll(".btn-editar-revisao").forEach((button) => {
            button.addEventListener("click", () => editarRevisao(loadedReviews.get(Number(button.dataset.id))));
        });
    } catch (error) {
        pageContent().innerHTML = "<main><p>Não foi possível carregar esta mídia.</p></main>";
        showError(error);
    }
}

function reviewCard(review) {
    const self = me()?.id === review.usuarioId;
    const avatar = safeAvatar(review.fotoPerfil);
    return `<article class="revisao">
      <div class="avatar">${avatar ? `<img src="${avatar}" alt="">` : escapeHtml(review.nomeUsuario?.slice(0, 1) || "?")}</div>
      <div><h3>${stars(review.nota)} ${review.spoiler ? '<small class="selo-spoiler">[spoiler]</small>' : ""}</h3>
        <p class="${review.spoiler ? "spoiler-oculto" : ""}">${escapeHtml(review.comentario || "Sem comentário.")}</p>
        <small class="autor">por ${escapeHtml(review.nomeUsuario)} · ${prettyDate(review.dataCriacao)}</small>
        ${self
            ? `<button class="btn-link btn-editar-revisao" type="button" data-id="${review.id}">Editar</button>`
            : me() ? `<button class="btn-link acao-denunciar" type="button" data-id="${review.id}">Denunciar</button>` : ""}
      </div>
    </article>`;
}

function safeAvatar(value) {
    if (!value) return "";
    if (value.startsWith("data:image/jpeg;base64,")) return escapeHtml(value);
    try {
        const url = new URL(value, location.origin);
        return ["https:", location.origin].includes(url.protocol === "https:" ? url.protocol : url.origin)
            ? escapeHtml(url.href) : "";
    } catch {
        return "";
    }
}

function abrirRevisao(review = null) {
    if (!requireLogin()) return;
    reviewEditing = review;
    selectedRating = review?.nota ?? 3;
    $("#area-popup").innerHTML = `<div class="popup-fundo" id="popup-fundo"><section class="popup" role="dialog" aria-modal="true" aria-label="Avaliar mídia">
      <h2 class="secao-titulo">${review ? "Editar revisão" : "Sua revisão"}</h2>
      <div class="seletor-nota" id="popup-notas">${["Ruim", "Razoável", "Bom", "Muito bom", "Excelente"].map((label, index) => {
        const note = index + 1;
        return `<button type="button" data-n="${note}" class="${note === selectedRating ? "ativa" : ""}"><span>${note === selectedRating ? "★" : "☆"}</span>${label}</button>`;
      }).join("")}</div>
      <textarea id="popup-texto" maxlength="5000" placeholder="Escreva seu comentário">${escapeHtml(review?.comentario || "")}</textarea>
      <label class="opcao-spoiler"><input type="checkbox" id="popup-spoiler" ${review?.spoiler ? "checked" : ""}> Contém spoiler</label>
      <div class="acoes"><button class="cancelar" type="button" id="cancelar-revisao">Cancelar</button>
        <button class="enviar" type="button" id="publicar-revisao">Publicar</button></div>
    </section></div>`;
    $("#popup-notas").addEventListener("click", (event) => {
        const button = event.target.closest("button[data-n]");
        if (!button) return;
        selectedRating = Number(button.dataset.n);
        document.querySelectorAll("#popup-notas button").forEach((item) => {
            item.classList.toggle("ativa", item === button);
            item.querySelector("span").textContent = item === button ? "★" : "☆";
        });
    });
    $("#cancelar-revisao").addEventListener("click", cancelar);
    $("#publicar-revisao").addEventListener("click", publicarRevisao);
    $("#popup-fundo").addEventListener("click", (event) => {
        if (event.target.id === "popup-fundo") cancelar();
    });
}

function editarRevisao(review) {
    abrirRevisao(review);
}

function cancelar() {
    $("#area-popup").innerHTML = "";
    reviewEditing = null;
}

async function publicarRevisao() {
    try {
        const body = {
            nota: selectedRating,
            comentario: $("#popup-texto").value.trim(),
            spoiler: $("#popup-spoiler").checked,
        };
        if (reviewEditing) {
            await api(`/avaliacoes/${reviewEditing.id}`, { method: "PUT", body });
        } else {
            await api(`/avaliacoes/midia/${currentMediaId}`, { method: "POST", body });
        }
        const fromReviewList = pageName === "revisoes";
        cancelar();
        toast("Revisão salva.");
        await (fromReviewList ? pageRevisoes() : pageFilme());
    } catch (error) {
        showError(error);
    }
}

async function denunciar(id) {
    if (!requireLogin()) return;
    try {
        await api(`/denuncias?avaliacaoId=${encodeURIComponent(id)}`, { method: "POST" });
        toast("Denúncia enviada à moderação.");
    } catch (error) {
        showError(error);
    }
}

async function pageRevisoes() {
    if (!requireLogin()) return;
    try {
        const reviews = await api("/avaliacoes/minhas");
        loadedReviews.clear();
        reviews.forEach((review) => loadedReviews.set(review.id, review));
        pageContent().innerHTML = `<main><h2 class="secao-titulo">Minhas Revisões</h2>
          <div class="grid-revisoes">${reviews.map((review) => `<article class="card-revisao">
            <h3>${escapeHtml(review.tituloMidia)} ${stars(review.nota)}</h3>
            <small>${prettyDate(review.dataCriacao)}</small><p>${escapeHtml(review.comentario || "Sem comentário.")}</p>
            <button type="button" class="btn-editar-revisao" data-id="${review.id}" title="Editar">✏</button>
            <button type="button" title="Excluir" onclick="deletarRevisao(${review.id})">Excluir</button>
          </article>`).join("") || "<p>Você ainda não fez revisões.</p>"}</div></main>`;
        document.querySelectorAll(".btn-editar-revisao").forEach((button) => {
            button.addEventListener("click", () => editarRevisao(loadedReviews.get(Number(button.dataset.id))));
        });
    } catch (error) {
        showError(error);
    }
}

async function deletarRevisao(id) {
    if (!confirm("Deseja excluir sua revisão?")) return;
    try {
        await api(`/avaliacoes/${id}`, { method: "DELETE" });
        toast("Revisão excluída.");
        await pageRevisoes();
    } catch (error) {
        showError(error);
    }
}

async function pageDados() {
    if (!requireLogin()) return;
    try {
        const user = await api("/usuarios/me");
        pageContent().innerHTML = `<main><h2 class="secao-titulo centralizado">Atualizar Dados Cadastrados</h2>
          <form class="formulario" id="form-dados">
            <label for="dados-nome">Nome</label><input id="dados-nome" value="${escapeHtml(user.nome)}" required maxlength="100">
            <label for="dados-email">E-mail</label><input id="dados-email" type="email" value="${escapeHtml(user.email)}" required maxlength="150">
            <label for="dados-senha">Senha</label><input id="dados-senha" type="password" placeholder="Nova senha (opcional)" minlength="8">
            <label for="dados-foto">Foto de perfil</label><input id="dados-foto" type="file" accept="image/*">
            <div class="acoes"><button class="cancelar" type="button" onclick="history.back()">Cancelar</button>
              <button class="enviar" type="submit">Atualizar</button></div>
          </form></main>`;
        $("#form-dados").addEventListener("submit", saveData);
    } catch (error) {
        showError(error);
    }
}

async function saveData(event) {
    event.preventDefault();
    try {
        const response = await api("/usuarios/me", {
            method: "PUT",
            body: {
                nome: $("#dados-nome").value.trim(),
                email: $("#dados-email").value.trim(),
                senha: $("#dados-senha").value,
                fotoPerfil: await carregarFoto($("#dados-foto")),
            },
        });
        setAuth(response);
        toast("Dados atualizados.");
        go("index.html");
    } catch (error) {
        showError(error);
    }
}

function pageLogin() {
    pageContent().innerHTML = `<main><form class="formulario" id="form-login">
      <div class="campos"><h2 class="secao-titulo">Login</h2>
        <label for="email">E-mail</label><input type="email" id="email" autocomplete="email" required>
        <label for="senha">Senha</label><input type="password" id="senha" autocomplete="current-password" required>
      </div>
      <div class="google-signin" id="google-button"></div>
      <div class="acoes"><button type="button" class="cancelar" onclick="go('index.html')">Cancelar</button>
        <button type="submit" class="enviar">Login</button></div>
      <p>Não tem conta? <a href="register.html">Registrar-se</a></p>
    </form></main>`;
    $("#form-login").addEventListener("submit", login);
    setupGoogleSignIn();
}

async function login(event) {
    event.preventDefault();
    try {
        setAuth(await api("/auth/login", {
            method: "POST",
            body: { email: $("#email").value.trim(), senha: $("#senha").value },
        }));
        go("index.html");
    } catch (error) {
        showError(error);
    }
}

function pageRegistro() {
    pageContent().innerHTML = `<main><form class="formulario" id="form-registro">
      <div class="campos"><h2 class="secao-titulo">Registrar-se</h2>
        <label for="nome">Nome</label><input type="text" id="nome" required maxlength="100" autocomplete="name">
        <label for="email">E-mail</label><input type="email" id="email" required maxlength="150" autocomplete="email">
        <label for="senha">Senha</label><input type="password" id="senha" required minlength="8" maxlength="72" autocomplete="new-password">
        <label for="confirmar-senha">Confirmar senha</label><input type="password" id="confirmar-senha" required minlength="8" autocomplete="new-password">
        <label for="registro-foto">Foto de perfil</label><input type="file" id="registro-foto" accept="image/*">
      </div>
      <div class="google-signin" id="google-button"></div>
      <div class="acoes"><button type="button" class="cancelar" onclick="go('index.html')">Cancelar</button>
        <button type="submit" class="enviar">Confirmar</button></div>
      <p>Já tem conta? <a href="login.html">Login</a></p>
    </form></main>`;
    $("#form-registro").addEventListener("submit", registrar);
    setupGoogleSignIn();
}

async function registrar(event) {
    event.preventDefault();
    if ($("#senha").value !== $("#confirmar-senha").value) {
        toast("As senhas não conferem.");
        return;
    }
    try {
        setAuth(await api("/auth/registro", {
            method: "POST",
            body: {
                nome: $("#nome").value.trim(),
                email: $("#email").value.trim(),
                senha: $("#senha").value,
                fotoPerfil: await carregarFoto($("#registro-foto")),
            },
        }));
        go("index.html");
    } catch (error) {
        showError(error);
    }
}

async function carregarFoto(input) {
    const file = input?.files?.[0];
    if (!file) return null;
    if (!file.type.startsWith("image/") || file.size > 8 * 1024 * 1024) {
        throw new Error("Escolha uma imagem válida com até 8 MB.");
    }
    let bitmap;
    try {
        bitmap = await createImageBitmap(file);
    } catch {
        throw new Error("Não foi possível ler a imagem selecionada.");
    }
    let width = bitmap.width;
    let height = bitmap.height;
    const scale = Math.min(1, 320 / Math.max(width, height));
    width = Math.max(1, Math.round(width * scale));
    height = Math.max(1, Math.round(height * scale));
    const canvas = document.createElement("canvas");
    const context = canvas.getContext("2d");
    if (!context) throw new Error("Não foi possível processar a imagem.");
    for (let attempt = 0; attempt < 4; attempt += 1) {
        canvas.width = width;
        canvas.height = height;
        context.drawImage(bitmap, 0, 0, width, height);
        const data = canvas.toDataURL("image/jpeg", 0.72 - attempt * 0.12);
        if (data.length <= 49000) return data;
        width = Math.max(1, Math.round(width * 0.75));
        height = Math.max(1, Math.round(height * 0.75));
    }
    throw new Error("A imagem ficou muito grande mesmo após a compactação.");
}

async function setupGoogleSignIn() {
    const container = $("#google-button");
    if (!container) return;
    try {
        const config = await api("/auth/config");
        if (!config.googleClientId) {
            container.innerHTML = '<small>Login Google indisponível: configure GOOGLE_CLIENT_ID.</small>';
            return;
        }
        if (!window.google?.accounts?.id) {
            await new Promise((resolve, reject) => {
                const script = document.createElement("script");
                script.src = "https://accounts.google.com/gsi/client";
                script.async = true;
                script.onload = resolve;
                script.onerror = () => reject(new Error("Não foi possível carregar o login Google."));
                document.head.appendChild(script);
            });
        }
        window.google.accounts.id.initialize({
            client_id: config.googleClientId,
            callback: async ({ credential }) => {
                try {
                    setAuth(await api("/auth/google", { method: "POST", body: { credential } }));
                    go("index.html");
                } catch (error) {
                    showError(error);
                }
            },
        });
        window.google.accounts.id.renderButton(container, {
            type: "standard", theme: "outline", size: "large", text: "continue_with",
            shape: "rectangular", width: Math.min(container.parentElement.clientWidth, 320),
        });
    } catch (error) {
        showError(error);
    }
}

const adminMarkup = (title, body) => `<main><div class="painel">
  <nav><a href="admin_denuncias.html">Denúncias</a><a href="admin_filmes.html">Novo filme / série</a></nav>
  <section><h2 class="secao-titulo">${title}</h2>${body}</section></div></main>`;

async function pageDenuncias() {
    if (!requireAdmin()) return;
    try {
        const reports = await api("/denuncias");
        pageContent().innerHTML = adminMarkup("Denúncias Pendentes",
            reports.map((item) => `<article class="item-denuncia">
              <div><strong>${escapeHtml(item.tituloMidia)}</strong><p>${escapeHtml(item.comentario || "Sem comentário.")}</p>
                <small>Autor: ${escapeHtml(item.autor)} · Denunciado por: ${escapeHtml(item.denunciante)}</small></div>
              <button class="botao-manter" type="button" onclick="manterComentario(${item.id})">Manter</button>
              <button class="botao-remover" type="button" onclick="removerComentario(${item.id}, ${item.avaliacaoId})">Remover</button>
            </article>`).join("") || "<p>Nenhuma denúncia pendente.</p>");
    } catch (error) {
        showError(error);
    }
}

async function manterComentario(id) {
    try {
        await api(`/denuncias/${id}`, { method: "DELETE" });
        toast("Denúncia encerrada; avaliação mantida.");
        await pageDenuncias();
    } catch (error) {
        showError(error);
    }
}

async function removerComentario(reportId, reviewId) {
    if (!confirm("Remover a avaliação denunciada?")) return;
    try {
        await api(`/denuncias/avaliacao/${reviewId}`, { method: "DELETE" });
        await api(`/avaliacoes/${reviewId}`, { method: "DELETE" });
        toast("Avaliação removida.");
        await pageDenuncias();
    } catch (error) {
        showError(error);
    }
}

function pageFilmes() {
    if (!requireAdmin()) return;
    pageContent().innerHTML = adminMarkup("Adicionar ao Catálogo", `<form class="formulario" id="form-filme">
      <label for="filme-tipo">Tipo</label><select id="filme-tipo"><option value="FILME">Filme</option><option value="SERIE">Série</option></select>
      <label for="filme-titulo">Título</label><input id="filme-titulo" autocomplete="off" required>
      <div id="resultados-api" class="resultados-api" role="listbox"></div>
      <label for="filme-ano">Ano</label><input id="filme-ano" type="number" readonly>
      <label for="filme-sinopse">Sinopse</label><textarea id="filme-sinopse" rows="4" readonly></textarea>
      <label for="filme-poster">Pôster (URL)</label><input id="filme-poster" type="url" readonly>
      <div class="acoes"><button class="enviar" id="salvar-filme" type="submit" disabled>Adicionar</button></div>
    </form>`);
    $("#filme-titulo").addEventListener("input", buscarExternaComAtraso);
    $("#filme-tipo").addEventListener("change", buscarExternaComAtraso);
    $("#form-filme").addEventListener("submit", addConteudo);
}

function buscarExternaComAtraso() {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(buscarExterna, 300);
}

async function buscarExterna() {
    const term = $("#filme-titulo").value.trim();
    const type = $("#filme-tipo").value;
    const results = $("#resultados-api");
    $("#salvar-filme").disabled = true;
    if (term.length < 2) {
        results.innerHTML = "";
        return;
    }
    try {
        const items = await api(`/midias/externa?q=${encodeURIComponent(term)}&tipo=${type}`);
        results.innerHTML = items.map((item) => `<button type="button" class="resultado-filme" role="option"
          data-id="${escapeHtml(item.idExterno)}" data-type="${escapeHtml(item.tipo)}">
          ${escapeHtml(item.titulo)} <small>${escapeHtml(item.ano)}</small></button>`).join("")
            || "<p>Nenhum resultado da API externa.</p>";
        results.querySelectorAll(".resultado-filme").forEach((button) => {
            button.addEventListener("click", () => selecionarExterno(button.dataset.id, button.dataset.type));
        });
    } catch (error) {
        results.innerHTML = `<p>${escapeHtml(error.message)}</p>`;
    }
}

async function selecionarExterno(id, type) {
    try {
        const media = await api(`/midias/externa/${encodeURIComponent(id)}?tipo=${type}`);
        $("#filme-titulo").value = media.titulo;
        $("#filme-ano").value = media.ano || "";
        $("#filme-sinopse").value = media.sinopse || "";
        $("#filme-poster").value = media.cartaz || "";
        $("#resultados-api").innerHTML = "";
        $("#salvar-filme").disabled = false;
        window.selectedExternalMedia = media;
    } catch (error) {
        showError(error);
    }
}

async function addConteudo(event) {
    event.preventDefault();
    const media = window.selectedExternalMedia;
    if (!media) {
        toast("Escolha uma obra na lista da busca.");
        return;
    }
    try {
        await api("/midias", {
            method: "POST",
            body: {
                idExterno: media.idExterno, tipo: media.tipo, titulo: media.titulo,
                ano: media.ano, sinopse: media.sinopse, cartaz: media.cartaz,
            },
        });
        window.selectedExternalMedia = null;
        toast("Mídia adicionada ao catálogo.");
        pageFilmes();
    } catch (error) {
        showError(error);
    }
}

const pages = {
    home: pageHome,
    filme: pageFilme,
    revisoes: pageRevisoes,
    dados: pageDados,
    login: pageLogin,
    registro: pageRegistro,
    denuncias: pageDenuncias,
    filmes: pageFilmes,
};
const pageRenderer = pages[pageName] || pageHome;
setRender(pageRenderer);
header();
pageRenderer();

window.editarRevisao = editarRevisao;
window.deletarRevisao = deletarRevisao;
window.manterComentario = manterComentario;
window.removerComentario = removerComentario;
