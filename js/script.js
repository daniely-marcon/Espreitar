/* ==========================================================================
   Espreitar — Home: lista de filmes com scroll infinito
   ========================================================================== */

/* --------------------------------------------------------------------------
   1. CAMADA DE DADOS
   parte que muda quando o back-end ficar pronto.

   Contrato combinado com o back-end:
     GET /api/filmes?page=1&limit=12
     Resposta:
     {
       "items": [
         { "id": 1, "titulo": "Nome", "imagem": "https://...", "nota": 4 }
       ],
       "hasMore": true        // false na última página
     }
   -------------------------------------------------------------------------- */
const USAR_MOCK = true;        // troque para false quando a API estiver no ar
const API_URL = "/api/filmes";
const POR_PAGINA = 12;

async function buscarFilmes(pagina) {
    if (USAR_MOCK) return buscarFilmesMock(pagina);

    const resposta = await fetch(`${API_URL}?page=${pagina}&limit=${POR_PAGINA}`);
    if (!resposta.ok) throw new Error(`Erro ${resposta.status}`);
    return resposta.json();
}

// Dados de teste só para ver o scroll funcionando. Apague junto com USAR_MOCK.
function buscarFilmesMock(pagina) {
    const TOTAL = 50;
    return new Promise((resolve) => {
        setTimeout(() => {
            const inicio = (pagina - 1) * POR_PAGINA;
            const fim = Math.min(inicio + POR_PAGINA, TOTAL);
            const items = [];
            for (let i = inicio; i < fim; i++) {
                items.push({
                    id: i + 1,
                    titulo: `Filme ${i + 1}`,
                    imagem: `https://picsum.photos/seed/espreitar${i + 1}/300/450`,
                    nota: (i % 5) + 1,
                });
            }
            resolve({ items, hasMore: fim < TOTAL });
        }, 600);
    });
}

/* --------------------------------------------------------------------------
   2. RENDERIZAÇÃO
   Usa createElement/textContent (e não innerHTML) para que um título vindo da
   API nunca seja interpretado como HTML.
   -------------------------------------------------------------------------- */
const grid = document.getElementById("grid-filmes");
const status = document.getElementById("status");
const sentinela = document.getElementById("sentinela");

function criarCard(filme) {
    const card = document.createElement("a");
    card.className = "card";
    card.href = `filme.html?id=${encodeURIComponent(filme.id)}`;

    const img = document.createElement("img");
    img.src = filme.imagem;
    img.alt = `Pôster de ${filme.titulo}`;
    img.loading = "lazy";

    const titulo = document.createElement("h3");
    titulo.textContent = filme.titulo;

    const nota = Math.max(0, Math.min(5, Math.round(filme.nota) || 0));
    const avaliacao = document.createElement("p");
    avaliacao.className = "avaliacao";
    avaliacao.textContent = "★".repeat(nota) + "☆".repeat(5 - nota);
    avaliacao.setAttribute("aria-label", `Nota ${nota} de 5`);

    card.append(img, titulo, avaliacao);
    return card;
}

function mostrarErro() {
    status.textContent = "Não foi possível carregar os filmes.";
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = "btn-tentar";
    botao.textContent = "Tentar de novo";
    botao.addEventListener("click", carregarMais);
    status.append(botao);
}

/* --------------------------------------------------------------------------
   3. SCROLL INFINITO
   O observer vigia o #sentinela (no fim do grid). Quando ele chega perto da
   área visível, carregamos a próxima página.
   -------------------------------------------------------------------------- */
let paginaAtual = 1;
let carregando = false;
let temMais = true;

const observer = new IntersectionObserver((entradas) => {
    if (entradas[0].isIntersecting) carregarMais();
}, { rootMargin: "300px" });   // começa a carregar 300px antes de chegar ao fim

async function carregarMais() {
    if (carregando || !temMais) return;   // evita pedidos duplicados
    carregando = true;
    status.textContent = "Carregando...";
    let deuCerto = false;

    try {
        const { items, hasMore } = await buscarFilmes(paginaAtual);
        grid.append(...items.map(criarCard));
        paginaAtual++;
        temMais = hasMore && items.length > 0;
        deuCerto = true;

        if (temMais) {
            status.textContent = "";
        } else {
            status.textContent = grid.children.length
                ? "Você chegou ao fim."
                : "Nenhum filme encontrado.";
            observer.disconnect();
        }
    } catch (erro) {
        console.error(erro);
        mostrarErro();
    }

    carregando = false;

    // Se o sentinela ainda estiver visível (tela alta), o observer não dispara
    // de novo sozinho. Reobservar força uma nova checagem.
    if (deuCerto && temMais) {
        observer.unobserve(sentinela);
        observer.observe(sentinela);
    }
}

observer.observe(sentinela);