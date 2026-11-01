const pageName = document.body.dataset.page;
let query = new URLSearchParams(location.search);
let selectedRating = 3;
let currentMediaId = null;
let reviewEditing = null;
let adminMediaEditingId = null;
let searchTimer = null;
let homeSearchVersion = 0;
const loadedReviews = new Map();

const pageContent = () => document.querySelector("#conteudo");
const prettyDate = (date) => date ? new Date(date).toLocaleDateString("pt-BR") : "";
const formatRating = (rating) => (Number(rating) || 0).toFixed(2);
const motivoDenunciaLabel = (motivo) => ({
    SPOILER: "Contém spoiler",
    PALAVRAS_OFENSIVAS: "Palavras ofensivas",
    OUTRO: "Outro motivo",
}[motivo] || "Outro motivo");
const showError = (error) => toast(friendlyErrorMessage(error));
const loadingMarkup = (label = "Carregando...") => `<div class="loading-state" role="status" aria-live="polite">
  <span class="loading-spinner" aria-hidden="true"></span><span>${escapeHtml(label)}</span>
</div>`;
const emptyState = (title, message) => `<div class="estado-vazio">
  <img src="../midia/sem-resultados.svg" alt="" width="180" height="140">
  <h3>${escapeHtml(title)}</h3><p>${escapeHtml(message)}</p>
</div>`;

async function pageHome() {
    const searchVersion = ++homeSearchVersion;
    pageContent().innerHTML = loadingMarkup("Carregando catálogo...");
    try {
        const all = await api("/midias");
        if (searchVersion !== homeSearchVersion) return;
        const term = (query.get("q") || "").trim().toLocaleLowerCase("pt-BR");
        const type = query.get("type") || "";
        const minRating = Number(query.get("rating") || 0);
        const media = all.filter((item) =>
            (!term || item.titulo.toLocaleLowerCase("pt-BR").includes(term))
            && (!type || item.tipo === type)
            && (Number(item.totalAvaliacoes) > 0
                ? (Number(item.notaMedia) || 0)
                : (Number(item.notaMediaExterna) || 0)) >= minRating);
        let sugestoesExternas = [];
        let erroBuscaExterna = "";
        if (term.length >= 2 && me()) {
            try {
                const tipos = type ? [type] : ["FILME", "SERIE"];
                const resultados = await Promise.all(tipos.map((tipo) =>
                    api(`/midias/externa?q=${encodeURIComponent(query.get("q"))}&tipo=${tipo}`)));
                if (searchVersion !== homeSearchVersion) return;
                const idsNoCatalogo = new Set(all.map((item) => `${item.idExterno}:${item.tipo}`));
                sugestoesExternas = resultados.flat().filter((item) => !idsNoCatalogo.has(`${item.idExterno}:${item.tipo}`));
            } catch (error) {
                console.error("Falha ao buscar títulos no TMDb.", error);
                erroBuscaExterna = friendlyErrorMessage(error, "A busca de filmes está temporariamente indisponível.");
            }
        }
        const destaque = term ? null : media[0];
        const restante = destaque ? media.slice(1) : media;
        const catalogo = (items) => items.map((item) => `<article class="card">
              <a class="card-link" href="filme.html?id=${encodeURIComponent(item.id)}">
                ${poster(item.cartaz, item.titulo)}<h3>${escapeHtml(item.titulo)}</h3>
                <small>${escapeHtml(item.ano || "")} · ${escapeHtml(item.tipo === "SERIE" ? "Série" : "Filme")}</small>
                <div class="avaliacao">${stars(Number(item.totalAvaliacoes) > 0 ? item.notaMedia : item.notaMediaExterna)}
                  <span>${Number(item.totalAvaliacoes) > 0
                    ? `${Number(item.totalAvaliacoes)} avaliações`
                    : `${formatRating(item.notaMediaExterna)}/5 · ${Number(item.totalAvaliacoesExternas) || 0} votos TMDb`}</span></div>
              </a>
              ${safeTrailer(item.trailerUrl) || /^\d+$/.test(item.idExterno || "") ? `<a class="trailer-cta"
                href="${trailerWatchUrl(item.trailerUrl) || `filme.html?id=${encodeURIComponent(item.id)}#trailer-player`}"
                ${trailerWatchUrl(item.trailerUrl) ? 'target="_blank" rel="noopener noreferrer"' : ""}
                aria-label="Assistir trailer de ${escapeHtml(item.titulo)}">▶ Assistir trailer</a>` : ""}
            </article>`).join("");
        pageContent().innerHTML = `<main>
          <h2 class="secao-titulo">${term ? `Resultados para “${escapeHtml(term)}”` : "Em Destaque"}</h2>
          ${destaque ? `<article class="destaque-hero">
              ${poster(destaque.cartaz, destaque.titulo)}
              <div class="destaque-info"><small>DESTAQUE</small>
                <h1>${escapeHtml(destaque.titulo)}</h1>
                <p>${escapeHtml(destaque.sinopse || "Descubra este título no Espreitar.")}</p>
                <span>${escapeHtml(destaque.ano || "")} · ${destaque.tipo === "SERIE" ? "Série" : "Filme"}</span>
                <div>${stars(Number(destaque.totalAvaliacoes) > 0 ? destaque.notaMedia : destaque.notaMediaExterna)}
                  <small>${Number(destaque.totalAvaliacoes) > 0
                    ? `${Number(destaque.totalAvaliacoes)} avaliações da comunidade`
                    : `${formatRating(destaque.notaMediaExterna)}/5 · ${Number(destaque.totalAvaliacoesExternas) || 0} votos TMDb`}</small>
                </div><a class="destaque-cta" href="filme.html?id=${encodeURIComponent(destaque.id)}">Ver detalhes</a>
                ${safeTrailer(destaque.trailerUrl) || /^\d+$/.test(destaque.idExterno || "") ? `<a class="trailer-cta"
                  href="${trailerWatchUrl(destaque.trailerUrl) || `filme.html?id=${encodeURIComponent(destaque.id)}#trailer-player`}"
                  ${trailerWatchUrl(destaque.trailerUrl) ? 'target="_blank" rel="noopener noreferrer"' : ""}
                  aria-label="Assistir trailer de ${escapeHtml(destaque.titulo)}">▶ Assistir trailer</a>` : ""}
              </div>
            </article>` : ""}
          ${restante.length ? `<h2 class="secao-titulo">${destaque ? "Mais para assistir" : "No catálogo"}</h2>
          <div class="grid-filmes">${catalogo(restante)}</div>` : !destaque ? `<div class="grid-filmes">${emptyState(
                term ? (sugestoesExternas.length ? "Este título ainda não está no catálogo" : "Não encontramos esse título")
                    : "O catálogo ainda está vazio",
                term ? (sugestoesExternas.length
                    ? "Encontramos opções no TMDb. Escolha uma para enviar um pedido e deixar sua avaliação."
                    : "Tente outro nome ou ajuste os filtros para ver mais resultados.")
                    : "Volte em breve para descobrir novos filmes e séries."
            )}</div>` : ""}
          ${sugestoesExternas.length ? `<section class="sugestoes-externas">
            <h2 class="secao-titulo">Opções para solicitar inclusão</h2>
            <p>Esses títulos ainda não fazem parte do catálogo. Selecione uma opção para enviar seu pedido e sua avaliação. A avaliação será publicada se a equipe aprovar a inclusão.</p>
            <div class="grid-filmes">${sugestoesExternas.map((item) => `<article class="card">
              ${poster(item.cartaz, item.titulo)}
              <h3>${escapeHtml(item.titulo)}</h3>
              <small>${escapeHtml(item.ano || "")} · ${item.tipo === "SERIE" ? "Série" : "Filme"}</small>
              <button type="button" class="enviar solicitar-midia" data-id="${escapeHtml(item.idExterno)}" data-type="${escapeHtml(item.tipo)}">
                Solicitar inclusão
              </button>
            </article>`).join("")}</div>
          </section>` : erroBuscaExterna ? `<p class="erro-busca-externa">Busca no TMDb indisponível: ${escapeHtml(erroBuscaExterna)}</p>`
              : term && !media.length && !me() ? `<p class="login-pedido">Entre ou <a href="register.html">crie uma conta</a> para buscar títulos no TMDb e pedir a inclusão.</p>` : ""}
        </main>`;
        document.querySelectorAll(".solicitar-midia").forEach((button) => {
            button.addEventListener("click", () => iniciarPedidoMidia(button.dataset.id, button.dataset.type));
        });
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
    pageContent().innerHTML = loadingMarkup("Carregando detalhes e avaliações...");
    try {
        const [media, reviews] = await Promise.all([
            api(`/midias/${encodeURIComponent(currentMediaId)}`),
            api(`/avaliacoes/midia/${encodeURIComponent(currentMediaId)}`),
        ]);
        let trailerUrl = safeTrailer(media.trailerUrl);
        let trailerError = "";
        const hasTmdbId = /^\d+$/.test(media.idExterno || "");
        if (!trailerUrl && hasTmdbId) {
            try {
                trailerUrl = safeTrailer((await api(`/midias/${encodeURIComponent(currentMediaId)}/trailer`)).trailerUrl);
            } catch (error) {
                console.error("Falha ao buscar trailer da mídia.", error);
                trailerError = "Trailer indisponível.";
            }
        }
        const trailerMarkup = trailerUrl
            ? `<section class="trailer-midia" id="trailer-midia">
                <h3>Trailer</h3>
                <a class="trailer-cta" href="${trailerWatchUrl(trailerUrl)}" target="_blank" rel="noopener noreferrer"
                  aria-label="Assistir trailer de ${escapeHtml(media.titulo)}">▶ Abrir trailer no YouTube</a>
                <iframe src="${trailerUrl}" title="Trailer de ${escapeHtml(media.titulo)}"
                  id="trailer-player"
                  loading="lazy" referrerpolicy="strict-origin-when-cross-origin"
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                  allowfullscreen></iframe></section>`
            : trailerError
                ? `<p id="trailer-player" class="trailer-indisponivel" role="status">${escapeHtml(trailerError)}</p>`
                : hasTmdbId ? '<p id="trailer-player" class="trailer-indisponivel" role="status">Trailer indisponível.</p>' : "";
        loadedReviews.clear();
        reviews.forEach((review) => loadedReviews.set(review.id, review));
        pageContent().innerHTML = `<main>
          <div class="detalhe">${poster(media.cartaz, media.titulo)}<div>
            <h2 class="titulo-filme">${escapeHtml(media.titulo)}
              <small class="tipo-filme">(${media.tipo === "SERIE" ? "Série" : "Filme"})</small></h2>
            <div>${stars(media.notaMedia)} <span>${Number(media.totalAvaliacoes) || 0} avaliações</span></div>
            <div>${stars(media.notaMediaExterna)}
              <span>${formatRating(media.notaMediaExterna)}/5 · ${Number(media.totalAvaliacoesExternas) || 0} votos no TMDb</span></div>
            <small>${escapeHtml(media.ano || "")}</small>
            <p>${escapeHtml(media.sinopse || "Sinopse indisponível.")}</p>
          </div></div>
          ${trailerMarkup}
          <h2 class="secao-titulo">Revisões</h2>
          <label class="filtro-spoiler"><input id="filtro-spoiler" type="checkbox" checked> Ocultar spoilers</label>
          <div id="lista-revisoes">${reviews.map(reviewCard).join("") || "<p>Seja a primeira pessoa a avaliar.</p>"}</div>
          <button class="btn-adicionar" id="adicionar-revisao" type="button" aria-label="Adicionar comentário">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 11.5a7.5 7.5 0 0 1-7.5 7.5H6l-3 2v-6.5A7.5 7.5 0 1 1 20 11.5Z"/><path d="M12.5 8v7m-3.5-3.5h7"/></svg>
            <span>Adicionar comentário</span>
          </button>
        </main>`;
        $("#adicionar-revisao").addEventListener("click", () => abrirRevisao());
        $("#filtro-spoiler").addEventListener("change", (event) => {
            document.querySelectorAll(".revisao [data-spoiler='true']").forEach((item) => {
                item.classList.toggle("spoiler-oculto", event.target.checked);
            });
        });
        const ocultarSpoilers = $("#filtro-spoiler").checked;
        document.querySelectorAll(".revisao [data-spoiler='true']").forEach((item) => {
            item.classList.toggle("spoiler-oculto", ocultarSpoilers);
        });
        document.querySelectorAll(".acao-denunciar").forEach((button) => {
            button.addEventListener("click", () => denunciar(button.dataset.id));
        });
        document.querySelectorAll(".btn-editar-revisao").forEach((button) => {
            button.addEventListener("click", () => editarRevisao(loadedReviews.get(Number(button.dataset.id))));
        });
        if (location.hash === "#trailer-player") {
            $("#trailer-player")?.scrollIntoView({ behavior: "smooth", block: "center" });
        }
    } catch (error) {
        pageContent().innerHTML = "<main><p>Não foi possível carregar esta mídia.</p></main>";
        showError(error);
    }
}

function reviewCard(review) {
    const self = me()?.id === review.usuarioId;
    const avatar = safeAvatar(review.fotoPerfil);
    return `<article class="revisao">
      <div class="avatar review-avatar">${avatar ? `<img src="${avatar}" alt="Foto de ${escapeHtml(review.nomeUsuario)}">` : escapeHtml(review.nomeUsuario?.slice(0, 1) || "?")}</div>
      <div class="review-content">
        <div class="review-heading"><div><h3>${stars(review.nota)} ${review.spoiler ? '<small class="selo-spoiler">[spoiler]</small>' : ""}</h3>
          <small class="autor">${escapeHtml(review.nomeUsuario)} · ${prettyDate(review.dataCriacao)}</small></div>
          ${self
            ? `<button class="btn-link btn-editar-revisao" type="button" data-id="${review.id}">Editar</button>`
            : me() ? `<button class="btn-link acao-denunciar" type="button" data-id="${review.id}">Denunciar</button>` : ""}
        </div>
        <p data-spoiler="${review.spoiler}" class="review-comentario ${review.spoiler ? "spoiler-oculto" : ""}">${escapeHtml(review.comentario || "Sem comentário.")}</p>
      </div>
    </article>`;
}

function safeTrailer(value) {
    if (typeof value !== "string") return "";
    const match = value.match(/^https:\/\/www\.youtube-nocookie\.com\/embed\/([A-Za-z0-9_-]{6,})$/);
    return match ? `https://www.youtube-nocookie.com/embed/${match[1]}` : "";
}

function trailerWatchUrl(value) {
    const embed = safeTrailer(value);
    if (!embed) return "";
    const videoId = embed.slice(embed.lastIndexOf("/") + 1);
    return `https://www.youtube.com/watch?v=${videoId}`;
}

async function iniciarPedidoMidia(idExterno, tipo) {
    if (!requireLogin()) return;
    try {
        const media = await api(`/midias/externa/${encodeURIComponent(idExterno)}?tipo=${encodeURIComponent(tipo)}`);
        const catalogo = await api("/midias");
        const existente = catalogo.find((item) => item.idExterno === media.idExterno && item.tipo === media.tipo);
        if (existente) {
            go(`filme.html?id=${existente.id}`);
            return;
        }
        window.selectedRequestMedia = media;
        selectedRating = 3;
        $("#area-popup").innerHTML = `<div class="popup-fundo" id="pedido-fundo"><section class="popup popup-pedido" role="dialog" aria-modal="true" aria-label="Pedir inclusão e avaliar">
          <h2 class="secao-titulo">Solicitar inclusão</h2>
          <p>Este título ainda não está no catálogo. Confira os dados e envie seu pedido com uma avaliação. Se a equipe aprovar, o título será adicionado e sua avaliação publicada.</p>
          <div class="pedido-midia">${poster(media.cartaz, media.titulo)}<div>
            <strong>${escapeHtml(media.titulo)}</strong>
            <span>${escapeHtml(media.ano || "")} · ${media.tipo === "SERIE" ? "Série" : "Filme"}</span>
            <small>Informações fornecidas pelo TMDb</small>
          </div></div>
          <label for="pedido-nota">Sua avaliação</label><select id="pedido-nota">${[5, 4, 3, 2, 1].map((nota) =>
              `<option value="${nota}" ${nota === selectedRating ? "selected" : ""}>${nota} ${"★".repeat(nota)}</option>`).join("")}</select>
          <label for="pedido-comentario">Comentário (opcional)</label><textarea id="pedido-comentario" maxlength="5000" placeholder="Conte o que achou deste título"></textarea>
          <label class="opcao-spoiler"><input type="checkbox" id="pedido-spoiler"> O comentário contém spoiler</label>
          <div class="acoes"><button class="cancelar" type="button" id="cancelar-pedido">Cancelar</button>
            <button class="enviar" type="button" id="enviar-pedido">Enviar pedido</button></div>
        </section></div>`;
        $("#cancelar-pedido").addEventListener("click", () => { $("#area-popup").innerHTML = ""; });
        $("#enviar-pedido").addEventListener("click", enviarPedidoMidia);
        $("#pedido-fundo").addEventListener("click", (event) => {
            if (event.target.id === "pedido-fundo") $("#area-popup").innerHTML = "";
        });
    } catch (error) {
        container.textContent = friendlyErrorMessage(error, "Não foi possível carregar o login Google.");
        showError(error);
    }
}

async function enviarPedidoMidia() {
    const media = window.selectedRequestMedia;
    if (!media) return;
    try {
        await api("/solicitacoes", {
            method: "POST",
            body: {
                idExterno: media.idExterno,
                tipo: media.tipo,
                nota: Number($("#pedido-nota").value),
                comentario: $("#pedido-comentario").value.trim(),
                spoiler: $("#pedido-spoiler").checked,
            },
        });
        window.selectedRequestMedia = null;
        $("#area-popup").innerHTML = "";
        toast("Pedido enviado. Se for aprovado, sua avaliação será publicada.");
    } catch (error) {
        showError(error);
    }
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
    $("#area-popup").innerHTML = `<div class="popup-fundo" id="denuncia-fundo">
      <section class="popup popup-denuncia" role="dialog" aria-modal="true" aria-labelledby="denuncia-titulo">
        <h2 class="secao-titulo" id="denuncia-titulo">Denunciar revisão</h2>
        <p>Selecione o motivo. O comentário original não será alterado.</p>
        <label for="motivo-denuncia">Motivo</label>
        <select id="motivo-denuncia">
          <option value="SPOILER">Contém spoiler</option>
          <option value="PALAVRAS_OFENSIVAS">Palavras ofensivas</option>
          <option value="OUTRO">Outro motivo</option>
        </select>
        <div class="acoes"><button class="cancelar" type="button" id="cancelar-denuncia">Cancelar</button>
          <button class="enviar" type="button" id="enviar-denuncia">Enviar denúncia</button></div>
      </section></div>`;
    $("#cancelar-denuncia").addEventListener("click", cancelar);
    $("#enviar-denuncia").addEventListener("click", () => enviarDenuncia(id));
    $("#denuncia-fundo").addEventListener("click", (event) => {
        if (event.target.id === "denuncia-fundo") cancelar();
    });
}

async function enviarDenuncia(id) {
    try {
        const motivo = $("#motivo-denuncia").value;
        await api(`/denuncias?avaliacaoId=${encodeURIComponent(id)}&motivo=${encodeURIComponent(motivo)}`, {
            method: "POST",
        });
        cancelar();
        toast("Denúncia enviada à moderação.");
    } catch (error) {
        showError(error);
    }
}

async function pageRevisoes() {
    if (!requireLogin()) return;
    pageContent().innerHTML = loadingMarkup("Carregando suas avaliações...");
    try {
        const reviews = await api("/avaliacoes/minhas");
        loadedReviews.clear();
        reviews.forEach((review) => loadedReviews.set(review.id, review));
        pageContent().innerHTML = `<main><h2 class="secao-titulo">Minhas Revisões</h2>
          <div class="grid-revisoes">${reviews.map((review) => `<article class="card-revisao">
            <a class="review-poster-link" href="filme.html?id=${review.midiaId}" aria-label="Abrir ${escapeHtml(review.tituloMidia)}">
              ${poster(review.cartaz, review.tituloMidia)}
            </a>
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
    pageContent().innerHTML = loadingMarkup("Carregando seus dados...");
    try {
        const user = await api("/usuarios/me");
        pageContent().innerHTML = `<main><h2 class="secao-titulo centralizado">Atualizar Dados Cadastrados</h2>
          <form class="formulario" id="form-dados">
            <div class="foto-preview" id="dados-foto-preview">${user.fotoPerfil && safeAvatar(user.fotoPerfil)
                ? `<img src="${safeAvatar(user.fotoPerfil)}" alt="Foto atual do perfil">`
                : "<span>Sem foto de perfil</span>"}</div>
            <label for="dados-nome">Nome</label><input id="dados-nome" value="${escapeHtml(user.nome)}" required maxlength="100">
            <label for="dados-email">E-mail</label><input id="dados-email" type="email" value="${escapeHtml(user.email)}" required maxlength="150">
            <label for="dados-senha">Nova senha (opcional)</label>
            <input id="dados-senha" type="password" placeholder="Mínimo de 8 caracteres, com maiúscula, minúscula, número e símbolo"
              minlength="8" maxlength="72" autocomplete="new-password"
              pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,72}">
            <small>Deixe em branco para manter a senha atual. Nome, e-mail e foto podem ser atualizados sem senha. Se você entrou com Google, definir uma senha também permite entrar com e-mail e senha.</small>
            <label for="dados-foto">Foto de perfil</label><input id="dados-foto" type="file" accept="image/*">
            <div class="acoes"><button class="cancelar" type="button" onclick="history.back()">Cancelar</button>
              <button class="enviar" type="submit">Atualizar</button></div>
          </form></main>`;
        $("#form-dados").addEventListener("submit", saveData);
        $("#dados-foto").addEventListener("change", () => previewProfilePhoto("#dados-foto", "#dados-foto-preview"));
    } catch (error) {
        showError(error);
    }
}

async function saveData(event) {
    event.preventDefault();
    try {
        const senha = $("#dados-senha").value;
        const dados = {
            nome: $("#dados-nome").value.trim(),
            email: $("#dados-email").value.trim(),
            fotoPerfil: await carregarFoto($("#dados-foto")),
        };
        if (senha) dados.senha = senha;
        const response = await api("/usuarios/me", {
            method: "PUT",
            body: dados,
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
      <div class="auth-separador">ou continue com Google</div>
      <div class="google-signin" id="google-button"></div>
      <p><a href="recuperar-senha.html">Esqueci minha senha</a></p>
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

function pageRecuperarSenha() {
    pageContent().innerHTML = `<main><form class="formulario" id="form-recuperar-senha">
      <h2 class="secao-titulo">Recuperar senha</h2>
      <p>Informe o e-mail da sua conta. Se ela existir, enviaremos um link válido por 30 minutos.</p>
      <label for="recuperar-email">E-mail</label><input id="recuperar-email" type="email" required maxlength="150" autocomplete="email">
      <div class="acoes"><button type="button" class="cancelar" onclick="go('login.html')">Voltar</button>
        <button type="submit" class="enviar">Enviar link</button></div>
    </form></main>`;
    $("#form-recuperar-senha").addEventListener("submit", solicitarRedefinicaoSenha);
}

async function solicitarRedefinicaoSenha(event) {
    event.preventDefault();
    const form = $("#form-recuperar-senha");
    const button = form.querySelector('[type="submit"]');
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
    try {
        const result = await api("/auth/esqueci-senha", {
            method: "POST",
            body: { email: $("#recuperar-email").value.trim() },
        });
        form.innerHTML = `<h2 class="secao-titulo">Confira seu e-mail</h2><p>${escapeHtml(result.message)}</p>
          <a href="login.html">Voltar para o login</a>`;
    } catch (error) {
        button.disabled = false;
        button.removeAttribute("aria-busy");
        showError(error);
    }
}

function pageRedefinirSenha() {
    const token = query.get("token");
    if (!token) {
        pageContent().innerHTML = `<main><p>Este link de redefinição é inválido. Solicite outro pela tela de login.</p>
          <a href="recuperar-senha.html">Solicitar novo link</a></main>`;
        return;
    }
    pageContent().innerHTML = `<main><form class="formulario" id="form-redefinir-senha">
      <h2 class="secao-titulo">Criar nova senha</h2>
      <label for="nova-senha">Nova senha</label>
      <input id="nova-senha" type="password" required minlength="8" maxlength="72" autocomplete="new-password"
        pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,72}">
      <small>Use ao menos 8 caracteres, com maiúscula, minúscula, número e símbolo.</small>
      <label for="confirmar-nova-senha">Confirmar nova senha</label>
      <input id="confirmar-nova-senha" type="password" required minlength="8" maxlength="72" autocomplete="new-password">
      <div class="acoes"><button type="button" class="cancelar" onclick="go('login.html')">Cancelar</button>
        <button type="submit" class="enviar">Salvar senha</button></div>
    </form></main>`;
    $("#form-redefinir-senha").addEventListener("submit", redefinirSenha);
}

async function redefinirSenha(event) {
    event.preventDefault();
    if ($("#nova-senha").value !== $("#confirmar-nova-senha").value) {
        toast("As senhas não conferem.");
        return;
    }
    const button = $("#form-redefinir-senha").querySelector('[type="submit"]');
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
    try {
        const result = await api("/auth/redefinir-senha", {
            method: "POST",
            body: { token: query.get("token"), senha: $("#nova-senha").value },
        });
        toast(result.message);
        go("login.html");
    } catch (error) {
        button.disabled = false;
        button.removeAttribute("aria-busy");
        showError(error);
    }
}

function pageRegistro() {
    pageContent().innerHTML = `<main><form class="formulario" id="form-registro">
      <div class="campos"><h2 class="secao-titulo">Registrar-se</h2>
        <label for="nome">Nome</label><input type="text" id="nome" required maxlength="100" autocomplete="name">
        <label for="email">E-mail</label><input type="email" id="email" required maxlength="150" autocomplete="email">
        <label for="senha">Senha</label><input type="password" id="senha" required minlength="8" maxlength="72" autocomplete="new-password"
          pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,72}">
        <small>Use ao menos 8 caracteres, com maiúscula, minúscula, número e símbolo.</small>
        <label for="confirmar-senha">Confirmar senha</label><input type="password" id="confirmar-senha" required minlength="8" maxlength="72" autocomplete="new-password">
        <label for="registro-foto">Foto de perfil</label><input type="file" id="registro-foto" accept="image/*">
        <div class="foto-preview" id="registro-foto-preview"><span>Prévia da foto de perfil</span></div>
      </div>
      <div class="auth-separador">ou crie sua conta com Google</div>
      <div class="google-signin" id="google-button"></div>
      <div class="acoes"><button type="button" class="cancelar" onclick="go('index.html')">Cancelar</button>
        <button type="submit" class="enviar">Confirmar</button></div>
      <p>Já tem conta? <a href="login.html">Login</a></p>
    </form></main>`;
    $("#form-registro").addEventListener("submit", registrar);
    $("#registro-foto").addEventListener("change", () => previewProfilePhoto("#registro-foto", "#registro-foto-preview"));
    setupGoogleSignIn();
}

async function previewProfilePhoto(inputSelector, previewSelector) {
    const preview = $(previewSelector);
    const input = $(inputSelector);
    if (!input.files?.length) {
        preview.innerHTML = "<span>Sem foto selecionada</span>";
        return;
    }
    try {
        const image = await carregarFoto(input);
        preview.innerHTML = `<img src="${safeAvatar(image)}" alt="Prévia da foto de perfil">`;
    } catch (error) {
        preview.innerHTML = "<span>Não foi possível gerar a prévia.</span>";
        showError(error);
    }
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
                    const help = container.parentElement?.querySelector(".google-ajuda");
                    if (help) help.textContent = friendlyErrorMessage(error);
                    showError(error);
                }
            },
        });
        window.google.accounts.id.renderButton(container, {
            type: "standard", theme: "outline", size: "large", text: "continue_with",
            shape: "rectangular", width: Math.min(container.parentElement.clientWidth, 320),
        });
        container.insertAdjacentHTML("afterend", '<small class="google-ajuda">O login Google depende da autorização do endereço deste site nas configurações do OAuth.</small>');
    } catch (error) {
        container.textContent = friendlyErrorMessage(error, "Não foi possível carregar o login Google.");
        showError(error);
    }
}

const adminMarkup = (title, body) => `<main><div class="painel">
  <nav><a href="admin_denuncias.html">Denúncias</a><a href="admin_solicitacoes.html">Pedidos</a><a href="admin_filmes.html">Catálogo</a><a href="admin_usuarios.html">Usuários</a></nav>
  <section><h2 class="secao-titulo">${title}</h2>${body}</section></div></main>`;

async function pageDenuncias() {
    if (!requireAdmin()) return;
    pageContent().innerHTML = loadingMarkup("Carregando denúncias...");
    try {
        const [reports, config] = await Promise.all([
            api("/denuncias"),
            api("/configuracoes/admin/denuncias"),
        ]);
        pageContent().innerHTML = adminMarkup("Denúncias Pendentes",
            `<form class="config-denuncias" id="form-limite-denuncias">
              <label for="limite-denuncias">Excluir avaliação automaticamente ao receber</label>
              <input id="limite-denuncias" type="number" min="1" max="100" required value="${config.limiteDenuncias}">
              <span>denúncias únicas</span><button class="enviar" type="submit">Salvar limite</button>
              <small>Quem publicou recebe uma notificação dentro do app. O intervalo permitido é de 1 a 100.</small>
            </form>
            <div id="lista-denuncias">${reports.map((item) => `<article class="item-denuncia">
              <div><strong>${escapeHtml(item.tituloMidia)}</strong><p>${escapeHtml(item.comentario || "Sem comentário.")}</p>
                <small class="motivo-denuncia">Motivo: ${escapeHtml(motivoDenunciaLabel(item.motivo))}</small>
                <small>Autor: ${escapeHtml(item.autor)} · Denunciado por: ${escapeHtml(item.denunciante)}</small>
                <small>Denúncia ${item.totalDenuncias} de ${config.limiteDenuncias} para remoção automática.</small></div>
              <button class="botao-manter" type="button" onclick="manterComentario(${item.id})">Manter</button>
              <button class="botao-remover" type="button" onclick="removerComentario(${item.id}, ${item.avaliacaoId})">Remover</button>
            </article>`).join("") || "<p>Nenhuma denúncia pendente.</p>"}</div>`);
        $("#form-limite-denuncias").addEventListener("submit", salvarLimiteDenuncias);
    } catch (error) {
        showError(error);
    }
}

async function salvarLimiteDenuncias(event) {
    event.preventDefault();
    const form = $("#form-limite-denuncias");
    const button = form.querySelector('[type="submit"]');
    button.disabled = true;
    try {
        await api("/configuracoes/admin/denuncias", {
            method: "PUT",
            body: { limiteDenuncias: Number($("#limite-denuncias").value) },
        });
        toast("Limite de denúncias atualizado.");
        await pageDenuncias();
    } catch (error) {
        button.disabled = false;
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

async function pageSolicitacoesAdmin() {
    if (!requireAdmin()) return;
    pageContent().innerHTML = adminMarkup("Pedidos de inclusão", `<div id="fila-solicitacoes">${loadingMarkup("Carregando pedidos...")}</div>`);
    try {
        const requests = await api("/solicitacoes/fila");
        $("#fila-solicitacoes").innerHTML = requests.length
            ? requests.map((item) => `<article class="pedido-admin">
                ${poster(item.cartaz, item.titulo)}
                <div class="pedido-admin-info"><h3>${escapeHtml(item.titulo)}</h3>
                  <small>${item.tipo === "SERIE" ? "Série" : "Filme"} · ${escapeHtml(item.ano || "")} · pedido por ${escapeHtml(item.nomeUsuario)}</small>
                  <p>${escapeHtml(item.sinopse || "Sinopse indisponível.")}</p>
                  <div>${stars(item.nota)} <small>avaliação inicial</small> · ${escapeHtml(item.comentario || "Sem comentário")}</div>
                  ${item.spoiler ? '<small class="selo-spoiler">O comentário contém spoiler</small>' : ""}
                  <small>TMDb: ${formatRating(item.notaMediaExterna)}/5 · ${Number(item.totalAvaliacoesExternas) || 0} votos</small>
                </div>
                <div class="acoes"><button class="enviar" type="button" data-aprovar-pedido="${item.id}">Aceitar</button>
                  <button class="botao-remover" type="button" data-rejeitar-pedido="${item.id}">Recusar</button></div>
              </article>`).join("")
            : "<p>Não há pedidos pendentes.</p>";
        $("#fila-solicitacoes").querySelectorAll("[data-aprovar-pedido]").forEach((button) => {
            button.addEventListener("click", () => decidirPedido(button.dataset.aprovarPedido, true));
        });
        $("#fila-solicitacoes").querySelectorAll("[data-rejeitar-pedido]").forEach((button) => {
            button.addEventListener("click", () => decidirPedido(button.dataset.rejeitarPedido, false));
        });
    } catch (error) {
        $("#fila-solicitacoes").textContent = "Não foi possível carregar os pedidos.";
        showError(error);
    }
}

async function decidirPedido(id, aprovar) {
    try {
        await api(`/solicitacoes/${encodeURIComponent(id)}/${aprovar ? "aprovar" : "rejeitar"}`, {
            method: aprovar ? "POST" : "DELETE",
        });
        toast(aprovar ? "Pedido aprovado e avaliação publicada." : "Pedido recusado; a pessoa será notificada.");
        await pageSolicitacoesAdmin();
    } catch (error) {
        showError(error);
    }
}

async function pageFilmes() {
    if (!requireAdmin()) return;
    adminMediaEditingId = null;
    window.selectedExternalMedia = null;
    pageContent().innerHTML = adminMarkup("Gerenciar Catálogo", `<form class="formulario formulario-admin" id="form-filme">
      <label for="filme-origem">Forma de cadastro</label>
      <select id="filme-origem"><option value="tmdb">Com ajuda do TMDb</option><option value="manual">Cadastro manual</option></select>
      <label for="filme-tipo">Tipo</label><select id="filme-tipo"><option value="FILME">Filme</option><option value="SERIE">Série</option></select>
      <label for="filme-titulo">Título</label><input id="filme-titulo" autocomplete="off" required maxlength="255">
      <div id="resultados-api" class="resultados-api" role="listbox"></div>
      <div id="filme-preview" class="preview-midia" hidden></div>
      <div id="campos-midia">
        <label for="filme-ano">Ano</label><input id="filme-ano" type="number" min="1888" max="2100">
        <label for="filme-sinopse">Sinopse</label><textarea id="filme-sinopse" rows="4"></textarea>
        <label for="filme-poster">Pôster (URL)</label><input id="filme-poster" type="url" maxlength="500">
        <label for="filme-trailer">Trailer do YouTube (link opcional)</label>
        <input id="filme-trailer" type="url" placeholder="https://www.youtube.com/watch?v=..." maxlength="255">
      </div>
      <div id="avaliacoes-tmdb">
        <label for="filme-nota-media">Nota média do TMDb (escala de 0 a 5)</label>
        <input id="filme-nota-media" type="number" min="0" max="5" step="0.01" readonly>
        <label for="filme-total-avaliacoes">Total de votos no TMDb</label>
        <input id="filme-total-avaliacoes" type="number" min="0" readonly>
      </div>
      <div class="acoes"><button class="enviar" id="salvar-filme" type="submit" disabled>Adicionar ao catálogo</button>
        <button class="cancelar" id="cancelar-edicao-filme" type="button" hidden>Cancelar edição</button></div>
    </form><section class="admin-lista"><h3>Obras cadastradas</h3><div id="catalogo-admin">${loadingMarkup("Carregando catálogo...")}</div></section>`);
    $("#filme-origem").addEventListener("change", () => alterarModoCadastro($("#filme-origem").value));
    $("#filme-titulo").addEventListener("input", buscarExternaComAtraso);
    $("#filme-tipo").addEventListener("change", buscarExternaComAtraso);
    $("#form-filme").addEventListener("submit", addConteudo);
    $("#filme-poster").addEventListener("input", () => {
        atualizarPreviewMedia($("#filme-poster").value, $("#filme-titulo").value);
        atualizarBotaoCadastroManual();
    });
    $("#cancelar-edicao-filme").addEventListener("click", pageFilmes);
    alterarModoCadastro("tmdb");
    await carregarCatalogoAdmin();
}

function alterarModoCadastro(modo) {
    const manual = modo === "manual";
    $("#resultados-api").hidden = manual;
    $("#avaliacoes-tmdb").hidden = manual;
    $("#campos-midia").hidden = !manual && !window.selectedExternalMedia && !adminMediaEditingId;
    if (manual) {
        clearTimeout(searchTimer);
        window.selectedExternalMedia = null;
        $("#resultados-api").innerHTML = "";
        $("#filme-preview").hidden = true;
        $("#salvar-filme").textContent = "Adicionar ao catálogo";
        atualizarBotaoCadastroManual();
    } else {
        $("#salvar-filme").disabled = !window.selectedExternalMedia && !adminMediaEditingId;
        if (!adminMediaEditingId) buscarExternaComAtraso();
    }
}

function atualizarBotaoCadastroManual() {
    if ($("#filme-origem")?.value === "manual" && !adminMediaEditingId) {
        $("#salvar-filme").disabled = !$("#filme-titulo").value.trim();
    }
}

function buscarExternaComAtraso() {
    if (adminMediaEditingId || $("#filme-origem").value !== "tmdb") {
        atualizarBotaoCadastroManual();
        return;
    }
    clearTimeout(searchTimer);
    window.selectedExternalMedia = null;
    $("#filme-preview").hidden = true;
    $("#salvar-filme").disabled = true;
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
    results.innerHTML = loadingMarkup("Buscando títulos no TMDb...");
    try {
        const items = await api(`/midias/externa?q=${encodeURIComponent(term)}&tipo=${type}`);
        results.innerHTML = items.map((item) => `<button type="button" class="resultado-filme" role="option"
          data-id="${escapeHtml(item.idExterno)}" data-type="${escapeHtml(item.tipo)}">
          ${item.cartaz ? poster(item.cartaz, item.titulo) : ""}
          <span>${escapeHtml(item.titulo)} <small>${escapeHtml(item.ano)}</small></span></button>`).join("")
            || emptyState("Não encontramos esse título", "Confira a escrita ou tente buscar pelo título original.");
        results.querySelectorAll(".resultado-filme").forEach((button) => {
            button.addEventListener("click", () => selecionarExterno(button.dataset.id, button.dataset.type));
        });
    } catch (error) {
        console.error("Falha na busca do catálogo administrativo.", error);
        results.innerHTML = emptyState(
            "Não foi possível fazer a busca",
            friendlyErrorMessage(error, "A busca está temporariamente indisponível."),
        );
    }
}

async function selecionarExterno(id, type) {
    $("#resultados-api").innerHTML = loadingMarkup("Carregando dados do TMDb...");
    try {
        const media = await api(`/midias/externa/${encodeURIComponent(id)}?tipo=${type}`);
        $("#filme-titulo").value = media.titulo;
        $("#filme-ano").value = media.ano || "";
        $("#filme-sinopse").value = media.sinopse || "";
        $("#filme-poster").value = media.cartaz || "";
        $("#filme-trailer").value = media.trailerUrl || "";
        $("#filme-nota-media").value = media.notaMedia;
        $("#filme-total-avaliacoes").value = media.totalAvaliacoes;
        $("#resultados-api").innerHTML = "";
        $("#campos-midia").hidden = false;
        $("#filme-preview").hidden = false;
        $("#filme-preview").innerHTML = `<strong>Prévia — confirme antes de adicionar</strong>${poster(media.cartaz, media.titulo)}
          <span>${escapeHtml(media.titulo)}${media.ano ? ` (${escapeHtml(media.ano)})` : ""}</span>`;
        $("#salvar-filme").disabled = false;
        window.selectedExternalMedia = media;
    } catch (error) {
        showError(error);
    }
}

async function addConteudo(event) {
    event.preventDefault();
    const media = window.selectedExternalMedia;
    const cadastroManual = $("#filme-origem").value === "manual";
    if (!adminMediaEditingId && !cadastroManual && !media) {
        toast("Escolha uma obra na lista da busca.");
        return;
    }
    if (cadastroManual && !$("#filme-titulo").value.trim()) {
        toast("Informe o título da obra.");
        return;
    }
    const body = adminMediaEditingId ? {
        tipo: $("#filme-tipo").value,
        titulo: $("#filme-titulo").value.trim(),
        ano: $("#filme-ano").value ? Number($("#filme-ano").value) : null,
        sinopse: $("#filme-sinopse").value.trim(),
        cartaz: $("#filme-poster").value.trim() || null,
        trailerUrl: $("#filme-trailer").value.trim() || null,
    } : cadastroManual ? {
        idExterno: null,
        tipo: $("#filme-tipo").value,
        titulo: $("#filme-titulo").value.trim(),
        ano: $("#filme-ano").value ? Number($("#filme-ano").value) : null,
        sinopse: $("#filme-sinopse").value.trim(),
        cartaz: $("#filme-poster").value.trim() || null,
        trailerUrl: $("#filme-trailer").value.trim() || null,
        notaMediaExterna: 0,
        totalAvaliacoesExternas: 0,
    } : {
        idExterno: media.idExterno, tipo: media.tipo, titulo: media.titulo,
        ano: media.ano, sinopse: media.sinopse, cartaz: media.cartaz,
        trailerUrl: $("#filme-trailer").value.trim() || null,
        notaMediaExterna: media.notaMedia,
        totalAvaliacoesExternas: media.totalAvaliacoes,
    };
    try {
        await api(adminMediaEditingId ? `/midias/${adminMediaEditingId}` : "/midias", {
            method: adminMediaEditingId ? "PUT" : "POST",
            body,
        });
        window.selectedExternalMedia = null;
        toast(adminMediaEditingId ? "Mídia atualizada." : "Mídia adicionada ao catálogo.");
        await pageFilmes();
    } catch (error) {
        showError(error);
    }
}

function atualizarPreviewMedia(url, title) {
    const preview = $("#filme-preview");
    if (!preview) return;
    preview.hidden = false;
    preview.innerHTML = `<strong>${adminMediaEditingId ? "Prévia da alteração" : "Prévia — confirme antes de adicionar"}</strong>
      ${poster(url, title)}<span>${escapeHtml(title || "Título da mídia")}</span>`;
}

async function carregarCatalogoAdmin() {
    $("#catalogo-admin").innerHTML = loadingMarkup("Carregando catálogo...");
    try {
        const catalog = await api("/midias");
        $("#catalogo-admin").innerHTML = catalog.length
            ? `<div class="tabela-admin"><div class="linha-admin cabecalho-admin"><span>Pôster</span><span>Obra</span><span>Avaliações</span><span>Ações</span></div>
              ${catalog.map((item) => `<div class="linha-admin">
                ${poster(item.cartaz, item.titulo)}
                <span><strong>${escapeHtml(item.titulo)}</strong><small>${escapeHtml(item.ano || "")} · ${item.tipo === "SERIE" ? "Série" : "Filme"}</small></span>
                <span>${Number(item.totalAvaliacoes) || 0}</span>
                <span class="acoes-admin"><button type="button" data-edit-media="${item.id}">Editar</button>
                  <button type="button" class="botao-remover" data-delete-media="${item.id}">Excluir</button></span>
              </div>`).join("")}</div>`
            : "<p>O catálogo ainda não tem obras.</p>";
        $("#catalogo-admin").querySelectorAll("[data-edit-media]").forEach((button) => {
            button.addEventListener("click", () => editarMidiaAdmin(catalog.find((item) => item.id === Number(button.dataset.editMedia))));
        });
        $("#catalogo-admin").querySelectorAll("[data-delete-media]").forEach((button) => {
            button.addEventListener("click", () => excluirMidiaAdmin(button.dataset.deleteMedia));
        });
    } catch (error) {
        $("#catalogo-admin").textContent = "Não foi possível carregar o catálogo.";
        showError(error);
    }
}

function editarMidiaAdmin(media) {
    if (!media) return;
    adminMediaEditingId = media.id;
    window.selectedExternalMedia = null;
    $("#campos-midia").hidden = false;
    $("#filme-tipo").value = media.tipo;
    $("#filme-titulo").value = media.titulo;
    $("#filme-ano").value = media.ano || "";
    $("#filme-sinopse").value = media.sinopse || "";
    $("#filme-poster").value = media.cartaz || "";
    $("#filme-trailer").value = media.trailerUrl || "";
    $("#filme-nota-media").value = media.notaMediaExterna || 0;
    $("#filme-total-avaliacoes").value = media.totalAvaliacoesExternas || 0;
    $("#resultados-api").innerHTML = "";
    $("#filme-preview").hidden = false;
    atualizarPreviewMedia(media.cartaz, media.titulo);
    $("#salvar-filme").textContent = "Salvar alterações";
    $("#salvar-filme").disabled = false;
    $("#cancelar-edicao-filme").hidden = false;
    $("#form-filme").scrollIntoView({ behavior: "smooth", block: "start" });
}

async function excluirMidiaAdmin(id) {
    if (!confirm("Excluir esta mídia do catálogo? Obras com avaliações não podem ser excluídas.")) return;
    try {
        await api(`/midias/${encodeURIComponent(id)}`, { method: "DELETE" });
        toast("Mídia excluída.");
        await carregarCatalogoAdmin();
    } catch (error) {
        showError(error);
    }
}

async function pageUsuariosAdmin() {
    if (!requireAdmin()) return;
    pageContent().innerHTML = adminMarkup("Usuários", `<div id="usuarios-admin">${loadingMarkup("Carregando usuários...")}</div>`);
    try {
        const users = await api("/usuarios/admin");
        const totalAdmins = users.filter((user) => user.perfil === "ADMIN").length;
        const usuarioAtual = me();
        $("#usuarios-admin").innerHTML = users.length
            ? `<div class="tabela-admin usuarios-admin"><div class="linha-admin cabecalho-admin"><span aria-hidden="true"></span><span>Usuário</span><span>Conta</span><span>Atividade</span><span>Ações</span></div>
              ${users.map((user) => {
                  const photo = safeAvatar(user.fotoPerfil);
                  const proprioUsuario = usuarioAtual?.id === user.id;
                  const ultimoAdmin = user.perfil === "ADMIN" && totalAdmins <= 1;
                  return `<article class="linha-admin">
                    <div class="avatar-admin">${photo ? `<img src="${photo}" alt="">` : escapeHtml(user.nome.slice(0, 1))}</div>
                    <span class="usuario-identidade"><strong>${escapeHtml(user.nome)}</strong><small>${escapeHtml(user.email)}</small></span>
                    <span class="usuario-conta"><strong>${user.perfil === "ADMIN" ? "Administrador" : "Usuário"}</strong><small>${user.provedor === "GOOGLE" ? "Google" : "Local"} · desde ${prettyDate(user.dataCriacao)}</small></span>
                    <span>${Number(user.totalAvaliacoes) || 0} revisões</span>
                    <span class="acoes-admin">${proprioUsuario
                      ? '<small>Sua conta</small>'
                      : `<select aria-label="Perfil de ${escapeHtml(user.nome)}" data-perfil-usuario="${user.id}">
                          <option value="USER" ${user.perfil === "USER" ? "selected" : ""}>Usuário</option>
                          <option value="ADMIN" ${user.perfil === "ADMIN" ? "selected" : ""}>Administrador</option>
                        </select>
                        <button type="button" data-salvar-perfil="${user.id}">Salvar perfil</button>
                        <button class="botao-remover" type="button" data-excluir-usuario="${user.id}"
                          ${ultimoAdmin ? "disabled title=\"O sistema precisa manter um administrador\"" : ""}>Excluir</button>`}</span>
                  </article>`;
              }).join("")}</div>`
            : "<p>Nenhuma conta cadastrada.</p>";
        $("#usuarios-admin").querySelectorAll("[data-excluir-usuario]").forEach((button) => {
            button.addEventListener("click", () => excluirUsuarioAdmin(button.dataset.excluirUsuario));
        });
        $("#usuarios-admin").querySelectorAll("[data-salvar-perfil]").forEach((button) => {
            button.addEventListener("click", () => salvarPerfilUsuario(button.dataset.salvarPerfil, button));
        });
    } catch (error) {
        $("#usuarios-admin").textContent = "Não foi possível carregar a lista de usuários.";
        showError(error);
    }
}

async function salvarPerfilUsuario(id, button) {
    const perfil = $(`[data-perfil-usuario="${CSS.escape(id)}"]`).value;
    button.disabled = true;
    try {
        await api(`/usuarios/admin/${encodeURIComponent(id)}/perfil`, {
            method: "PUT",
            body: { perfil },
        });
        toast("Perfil atualizado.");
        await pageUsuariosAdmin();
    } catch (error) {
        button.disabled = false;
        showError(error);
    }
}

async function excluirUsuarioAdmin(id) {
    if (!confirm("Excluir esta conta e suas atividades? Esta ação não pode ser desfeita.")) return;
    try {
        await api(`/usuarios/admin/${encodeURIComponent(id)}`, { method: "DELETE" });
        toast("Conta excluída.");
        await pageUsuariosAdmin();
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
    recuperarSenha: pageRecuperarSenha,
    redefinirSenha: pageRedefinirSenha,
    denuncias: pageDenuncias,
    solicitacoes: pageSolicitacoesAdmin,
    filmes: pageFilmes,
    usuarios: pageUsuariosAdmin,
};
const pageRenderer = pages[pageName] || pageHome;
setRender(pageRenderer);
header();
pageRenderer();
if (pageName === "home") {
    $("#campo-pesquisa")?.addEventListener("input", (event) => {
        clearTimeout(searchTimer);
        const termo = event.target.value.trim();
        if (termo && termo.length < 2) return;
        searchTimer = setTimeout(() => {
            const params = new URLSearchParams();
            if ($("#categoria").value) params.set("type", $("#categoria").value);
            if ($("#classificacao").value) params.set("rating", $("#classificacao").value);
            if (termo) params.set("q", termo);
            const url = `${location.pathname}${params.size ? `?${params}` : ""}`;
            history.replaceState(null, "", url);
            query = new URLSearchParams(location.search);
            pageHome();
        }, 400);
    });
}

window.editarRevisao = editarRevisao;
window.deletarRevisao = deletarRevisao;
window.manterComentario = manterComentario;
window.removerComentario = removerComentario;
