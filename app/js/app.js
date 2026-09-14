/* =========================================================================
   Assembleia — cliente HTML para a voting-api
   Todas as telas (formulário e seleção) são carregadas DINAMICAMENTE a
   partir do JSON de tela devolvido pela API (/api/v1/screens/...).
   ========================================================================= */

/* ---------------------------- Config / estado --------------------------- */

const DEFAULT_API_BASE = ""; // mesma origem (recomendado: servir estes arquivos pelo próprio Spring Boot)
const apiBaseInput = document.getElementById("api-base");
apiBaseInput.value = localStorage.getItem("apiBase") ?? DEFAULT_API_BASE;
apiBaseInput.placeholder = window.location.origin;
apiBaseInput.addEventListener("change", () => {
  localStorage.setItem("apiBase", apiBaseInput.value.trim());
  toast("Endpoint da API atualizado.", "info");
});

function apiBase() {
  return (localStorage.getItem("apiBase") ?? DEFAULT_API_BASE).replace(/\/+$/, "");
}

/* URLs de entrada da árvore de telas. A partir daqui, toda navegação segue
   os links que a própria API devolve dentro do JSON de cada tela. */
const SCREEN_TOPICS = "/api/v1/screens/topics";
const SCREEN_NEW_TOPIC = "/api/v1/screens/topics/new";

const app = document.getElementById("app");
const contextTitle = document.getElementById("context-title");
const topbarTitle = document.getElementById("topbar-title");
const btnBackDesktop = document.getElementById("btn-back-desktop");
const btnBackMobile = document.getElementById("btn-back");

/* Pilha de navegação: guarda apenas as URLs das telas já visitadas.
   Cada "run" busca a tela na API na hora */
let stack = [];

function setHeader(title) {
  contextTitle.textContent = title;
  topbarTitle.textContent = title;
  const showBack = stack.length > 1;
  btnBackDesktop.hidden = !showBack;
  btnBackMobile.hidden = !showBack;
}

function run() {
  window.scrollTo({ top: 0 });
  const url = stack[stack.length - 1];
  loadAndRender(url);
  updateActiveNav();
}

function push(url) {
  stack.push(url);
  run();
}

function resetTo(url) {
  stack = [url];
  run();
}

function goBack() {
  if (stack.length > 1) {
    stack.pop();
    run();
  }
}

btnBackDesktop.addEventListener("click", goBack);
btnBackMobile.addEventListener("click", goBack);

function updateActiveNav() {
  document.querySelectorAll(".rail-link[data-nav]").forEach((el) => el.classList.remove("is-active"));
  const url = stack[stack.length - 1];
  const key = url === SCREEN_TOPICS ? "topics" : url === SCREEN_NEW_TOPIC ? "newTopic" : null;
  if (key) {
    const el = document.querySelector(`.rail-link[data-nav="${key}"]`);
    if (el) el.classList.add("is-active");
  }
}

/* ---------------------------- Menu (rail / mobile) ----------------------- */

document.querySelector('[data-nav="topics"]').addEventListener("click", () => {
  closeRail();
  resetTo(SCREEN_TOPICS);
});
document.querySelector('[data-nav="newTopic"]').addEventListener("click", () => {
  closeRail();
  push(SCREEN_NEW_TOPIC);
});

const rail = document.querySelector(".rail");
document.getElementById("btn-menu").addEventListener("click", () => rail.classList.add("is-open"));
function closeRail() { rail.classList.remove("is-open"); }
document.addEventListener("click", (e) => {
  if (rail.classList.contains("is-open") && !rail.contains(e.target) && !e.target.closest("#btn-menu")) {
    closeRail();
  }
});

/* ---------------------------- Painel de atividade ------------------------ */

const logPanel = document.getElementById("log-panel");
const logScrim = document.getElementById("log-scrim");
const logBody = document.getElementById("log-body");
const logDot = document.getElementById("log-dot");
let logEntries = [];

function openLog() { logPanel.classList.add("is-open"); logScrim.hidden = false; logDot.hidden = true; }
function closeLogPanel() { logPanel.classList.remove("is-open"); logScrim.hidden = true; }
document.getElementById("btn-toggle-log").addEventListener("click", openLog);
document.getElementById("btn-close-log").addEventListener("click", closeLogPanel);
logScrim.addEventListener("click", closeLogPanel);

function recordLog(entry) {
  logEntries.unshift(entry);
  if (logEntries.length > 40) logEntries.pop();
  renderLog();
  if (!entry.ok && !logPanel.classList.contains("is-open")) logDot.hidden = false;
}

function renderLog() {
  if (logEntries.length === 0) {
    logBody.innerHTML = '<p class="log-empty">Nenhuma chamada ainda.</p>';
    return;
  }
  logBody.innerHTML = logEntries.map((e) => `
    <div class="log-entry">
      <div class="log-entry-top">
        <span><span class="log-method">${e.method}</span> <span class="log-status ${e.ok ? "ok" : "err"}">${e.status}</span></span>
        <span class="log-time">${e.time}</span>
      </div>
      <div class="log-path">${escapeHtml(e.path)}</div>
    </div>
  `).join("");
}

/* ---------------------------- Cliente HTTP -------------------------------- */

/* Resolve a URL de uma chamada. As telas da API já vêm com links absolutos
   (ex.: "http://localhost:8080/api/v1/screens/topics/5") */
function resolveUrl(pathOrUrl) {
  if (/^https?:\/\//i.test(pathOrUrl)) return pathOrUrl;
  return apiBase() + pathOrUrl;
}

async function apiFetch(method, pathOrUrl, body) {
  const url = resolveUrl(pathOrUrl);
  const time = new Date().toLocaleTimeString("pt-BR");
  let status = 0, ok = false, data = null, networkError = false;
  try {
    const res = await fetch(url, {
      method,
      headers: body !== undefined ? { "Content-Type": "application/json" } : undefined,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    status = res.status;
    ok = res.ok;
    const text = await res.text();
    if (text) {
      try { data = JSON.parse(text); } catch { data = text; }
    }
  } catch (err) {
    networkError = true;
  }
  recordLog({ method, path: pathOrUrl, status: networkError ? "—" : status, ok: networkError ? false : ok, time });
  return { ok, status, data, networkError };
}

function apiErrorMessage(result, fallback) {
  if (result.networkError) {
    return `Não foi possível conectar à API em ${apiBase() || window.location.origin}. Verifique o endereço no rodapé do menu.`;
  }
  const data = result.data;
  if (data && typeof data === "object") {
    if (data.detail) return data.detail;
    if (data.message) return data.message;
    if (data.title) return data.title;
  }
  if (typeof data === "string" && data.trim()) return data;
  return fallback ?? `A API retornou um erro (${result.status}).`;
}

/* ---------------------------- Utilidades ---------------------------------- */

function escapeHtml(str) {
  return String(str ?? "").replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  })[c]);
}

function formatCpfDisplay(digits) {
  const d = digits.padEnd(11, "•").slice(0, 11);
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9, 11)}`.replace(/•/g, "");
}

let toastTimer = null;
function toast(message, kind = "info") {
  const el = document.getElementById("toast");
  el.textContent = message;
  el.className = "toast" + (kind === "error" ? " is-error" : kind === "success" ? " is-success" : "");
  el.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.hidden = true; }, 3600);
}

function loadingHtml(label) {
  return `<div class="loading-row"><span class="spinner"></span> ${escapeHtml(label)}</div>`;
}

function errorAlertHtml(message) {
  return `<div class="alert alert-error">
    <svg viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="8" stroke="currentColor" stroke-width="1.6"/><path d="M10 6.5v4.2M10 13.4h.01" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
    <span>${escapeHtml(message)}</span>
  </div>`;
}

/* ============================ Motor de telas =============================
   Renderiza qualquer FormScreen ("FORMULARIO") ou SelectionScreen
   ("SELECAO") devolvida pela API */

async function loadAndRender(url) {
  app.innerHTML = loadingHtml("Carregando tela…");
  const res = await apiFetch("GET", url);
  if (!res.ok) {
    setHeader("Erro");
    app.innerHTML = errorAlertHtml(apiErrorMessage(res, "Não foi possível carregar esta tela."));
    return;
  }
  renderScreen(res.data);
}

function renderScreen(screen) {
  if (!screen || !screen.tipo) {
    setHeader("Erro");
    app.innerHTML = errorAlertHtml("A API devolveu uma resposta que não é uma tela reconhecida.");
    return;
  }
  if (screen.tipo === "FORMULARIO") return renderFormScreen(screen);
  if (screen.tipo === "SELECAO") return renderSelectionScreen(screen);
  setHeader("Erro");
  app.innerHTML = errorAlertHtml(`Tipo de tela não suportado: ${screen.tipo}`);
}

/* ---------------------------- Tela: FORMULARIO ---------------------------- */

function formItemHtml(item) {
  switch (item.tipo) {
    case "TEXTO":
      return `<p class="detail-desc" style="margin-bottom:16px;">${escapeHtml(item.texto)}</p>`;
    case "INPUT_TEXTO":
      return `
        <div class="field">
          <label for="f-${item.id}">${escapeHtml(item.titulo)}</label>
          <input type="text" id="f-${item.id}" data-field-id="${item.id}" data-field-type="texto"
                 value="${escapeHtml(item.valor ?? "")}">
        </div>`;
    case "INPUT_NUMERO":
      return `
        <div class="field">
          <label for="f-${item.id}">${escapeHtml(item.titulo)}</label>
          <input type="number" id="f-${item.id}" data-field-id="${item.id}" data-field-type="numero"
                 value="${item.valor ?? ""}">
        </div>`;
    case "INPUT_DATA":
      return `
        <div class="field">
          <label for="f-${item.id}">${escapeHtml(item.titulo)}</label>
          <input type="date" id="f-${item.id}" data-field-id="${item.id}" data-field-type="data"
                 value="${escapeHtml(item.valor ?? "")}">
        </div>`;
    default:
      return "";
  }
}

function renderFormScreen(screen) {
  setHeader(screen.titulo || "Formulário");
  const itens = screen.itens || [];

  app.innerHTML = `
    <form id="dyn-form" novalidate>
      ${itens.map(formItemHtml).join("")}
      <div id="form-alert"></div>
      <div class="btn-row">
        ${screen.botaoOk ? `<button type="submit" class="btn btn-primary" id="btn-ok">${escapeHtml(screen.botaoOk.texto)}</button>` : ""}
        ${screen.botaoCancelar ? `<button type="button" class="btn btn-ghost" id="btn-cancel">${escapeHtml(screen.botaoCancelar.texto)}</button>` : ""}
      </div>
    </form>
  `;

  if (screen.botaoCancelar) {
    document.getElementById("btn-cancel").addEventListener("click", goBack);
  }

  if (screen.botaoOk) {
    document.getElementById("dyn-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const alertHolder = document.getElementById("form-alert");
      alertHolder.innerHTML = "";

      const body = { ...(screen.botaoOk.body || {}) };
      app.querySelectorAll("[data-field-id]").forEach((input) => {
        const key = input.dataset.fieldId;
        const raw = input.value.trim();
        if (input.dataset.fieldType === "numero") {
          body[key] = raw === "" ? null : Number(raw);
        } else {
          body[key] = raw === "" ? null : raw;
        }
      });

      const btn = document.getElementById("btn-ok");
      btn.disabled = true;
      const originalLabel = btn.textContent;
      btn.textContent = "Enviando…";

      const res = await apiFetch("POST", screen.botaoOk.url, body);

      btn.disabled = false;
      btn.textContent = originalLabel;

      if (!res.ok) {
        alertHolder.innerHTML = errorAlertHtml(apiErrorMessage(res, "Não foi possível concluir a ação."));
        return;
      }

      toast("Feito.", "success");
      goBack(); // volta para a tela anterior, que será recarregada da API já atualizada
    });
  }
}

/* ---------------------------- Tela: SELECAO -------------------------------- */

function renderSelectionScreen(screen) {
  setHeader(screen.titulo || "Selecione");
  const itens = screen.itens || [];

  if (itens.length === 0) {
    app.innerHTML = `<div class="empty-state"><h3>Nada por aqui</h3><p>Esta tela não tem opções no momento.</p></div>`;
    return;
  }

  // Itens de navegação (sem body) levam a outra tela via GET.
  // Itens de ação (com body) representam um comando a ser enviado via POST.
  const navItems = itens.filter((it) => !it.body);
  const actionItems = itens.filter((it) => !!it.body);

  // Ações de voto não carregam o identificador do associado no body (ele não
  // faz parte da definição de tela, pois a autenticação é externa ao app)
  const needsMemberId = actionItems.some((it) => /\/votes(\/|$|\?)/.test(it.url));

  app.innerHTML = `
    ${navItems.length ? `
      <ul class="topic-list" id="dyn-nav-list">
        ${navItems.map((it, i) => `
          <li>
            <button class="topic-row" data-idx="${i}">
              <span class="topic-main"><span class="topic-title">${escapeHtml(it.texto)}</span></span>
              <svg class="topic-chevron" viewBox="0 0 20 20" fill="none" width="16" height="16"><path d="M7 4l6 6-6 6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
          </li>
        `).join("")}
      </ul>
    ` : ""}

    ${actionItems.length ? `
      ${needsMemberId ? `
        <div class="field" id="field-cpf">
          <label for="cpf">CPF do associado</label>
          <input id="cpf" inputmode="numeric" autocomplete="off" placeholder="000.000.000-00" maxlength="14">
          <span class="hint">Usado para identificar o associado e impedir voto duplicado.</span>
          <span class="field-error" hidden>Informe um CPF com 11 dígitos.</span>
        </div>
      ` : ""}
      <div id="form-alert"></div>
      <div class="vote-options">
        ${actionItems.map((it, i) => `
          <button type="button" class="vote-option ${/n[aã]o/i.test(it.texto) ? "option-nao" : "option-sim"}" data-action-idx="${i}" ${needsMemberId ? "disabled" : ""}>
            <svg viewBox="0 0 20 20" fill="none"><path d="M4 10.5l4 4 8-9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            ${escapeHtml(it.texto)}
          </button>
        `).join("")}
      </div>
    ` : ""}
  `;

  app.querySelectorAll("#dyn-nav-list .topic-row").forEach((btn) => {
    btn.addEventListener("click", () => {
      const item = navItems[Number(btn.dataset.idx)];
      push(item.url);
    });
  });

  const cpfInput = document.getElementById("cpf");
  if (needsMemberId && cpfInput) {
    cpfInput.addEventListener("input", () => {
      const digits = cpfInput.value.replace(/\D/g, "").slice(0, 11);
      cpfInput.value = digits.length > 3 ? formatCpfDisplay(digits) : digits;
      cpfInput.dataset.digits = digits;
      const ready = digits.length === 11;
      app.querySelectorAll("[data-action-idx]").forEach((b) => (b.disabled = !ready));
    });
  }

  app.querySelectorAll("[data-action-idx]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const item = actionItems[Number(btn.dataset.actionIdx)];
      const alertHolder = document.getElementById("form-alert");
      alertHolder.innerHTML = "";

      let body = { ...item.body };
      if (needsMemberId) {
        const digits = cpfInput.dataset.digits || "";
        if (digits.length !== 11) {
          document.getElementById("field-cpf").classList.add("has-error");
          document.getElementById("field-cpf").querySelector(".field-error").hidden = false;
          return;
        }
        body.memberId = digits;
      }

      app.querySelectorAll("[data-action-idx]").forEach((b) => (b.disabled = true));
      if (cpfInput) cpfInput.disabled = true;

      const res = await apiFetch("POST", item.url, body);

      if (!res.ok) {
        app.querySelectorAll("[data-action-idx]").forEach((b) => (b.disabled = false));
        if (cpfInput) cpfInput.disabled = false;
        alertHolder.innerHTML = errorAlertHtml(apiErrorMessage(res, "Não foi possível concluir a ação."));
        return;
      }

      toast("Feito.", "success");
      goBack(); 
    });
  });
}

/* ---------------------------- Boot ---------------------------------------- */

resetTo(SCREEN_TOPICS);
