/* =========================================================================
   ScreenRenderer — motor genérico de telas do Anexo 1
   -------------------------------------------------------------------------
   Este módulo é o ÚNICO lugar do cliente que sabe interpretar as respostas
   de tela da API (GET /api/v1/screens/*). Ele não conhece nenhuma tela de
   negócio específica (não sabe o que é "pauta", "voto" etc.), apenas o
   contrato genérico:

     FORMULARIO -> { tipo, titulo, itens[], botaoOk, botaoCancelar }
       itens[].tipo   -> "TEXTO" | "INPUT_TEXTO" | "INPUT_NUMERO" | "INPUT_DATA"
       itens[].id     -> chave usada no payload enviado a botaoOk.url
       itens[].titulo -> rótulo do campo
       botaoOk / botaoCancelar -> { texto, url, body? }

     SELECAO -> { tipo, titulo, itens[] }
       itens[].texto -> rótulo do botão/opção
       itens[].url   -> destino (GET para navegar, ou alvo do POST se houver body)
       itens[].body  -> payload da ação (quando presente, o clique é um POST)

   O app.js apenas registra, via init(), como buscar dados (apiFetch), onde
   desenhar (mountEl) e como se integrar à pilha de navegação do app
   (push/goBack/setTop) e a duas situações fora do contrato genérico:
     - onFormSuccess(data): o que fazer depois de um POST bem-sucedido de
       um botaoOk (a resposta é dado de negócio, não uma tela);
     - onSelectionNavigateData(item, data): o que fazer quando um item de
       SELECAO sem "body" leva a uma URL que NÃO devolve outra tela (tipo
       ausente), ou seja, dado de negócio puro (ex.: resultado de apuração).
   ========================================================================= */

const ScreenRenderer = (function () {
  const FORM_ITEM_INPUT_TYPES = {
    INPUT_TEXTO: "text",
    INPUT_NUMERO: "number",
    INPUT_DATA: "date",
  };

  let deps = null;

  /**
   * Conecta o motor ao restante do app.
   * @param {object} d
   *   apiFetch(method, urlOrPath, body)   -> { ok, status, data }
   *   apiErrorMessage(res, fallback)      -> string
   *   loadingHtml(label)                  -> string
   *   errorAlertHtml(message)             -> string
   *   escapeHtml(str)                     -> string
   *   toast(message, kind)                -> void
   *   mountEl                             -> elemento DOM onde a tela é desenhada
   *   setHeader(title)                    -> void
   *   push(screenFn)                      -> empilha uma nova tela
   *   goBack()                            -> volta uma tela
   *   setTop(screenFn)                    -> substitui a tela atual (sem empilhar)
   *   onFormSuccess(data, screen)         -> chamado após POST de botaoOk com sucesso
   *   onSelectionNavigateData(item, data) -> chamado quando um item de SELECAO
   *                                          leva a uma resposta que não é tela
   *   extractTopicIdFromUrl(url)          -> (opcional) usado só para o link
   *                                          extra de gerenciamento de pauta
   *   onManageTopic(topicId, texto)       -> (opcional) idem
   */
  function init(d) {
    deps = d;
  }

  /** Busca uma tela pela URL (relativa ou absoluta) e a renderiza. */
  async function loadScreen(url) {
    deps.setHeader("Carregando…");
    deps.mountEl.innerHTML = deps.loadingHtml("Carregando tela…");
    const res = await deps.apiFetch("GET", url);
    if (!res.ok) {
      deps.mountEl.innerHTML = deps.errorAlertHtml(deps.apiErrorMessage(res, "Não foi possível carregar a tela."));
      return;
    }
    renderScreen(res.data);
  }

  /** Despacha para o renderizador correto conforme screen.tipo. */
  function renderScreen(screen) {
    if (!screen || typeof screen !== "object") {
      deps.mountEl.innerHTML = deps.errorAlertHtml("Resposta de tela inválida.");
      return;
    }
    switch (screen.tipo) {
      case "FORMULARIO":
        renderFormulario(screen);
        return;
      case "SELECAO":
        renderSelecao(screen);
        return;
      default:
        deps.mountEl.innerHTML = deps.errorAlertHtml(`Tipo de tela não suportado: ${deps.escapeHtml(String(screen.tipo))}`);
    }
  }

  /* ---- FORMULARIO: campos e botões 100% a partir do JSON ---- */
  function renderFormulario(screen) {
    const { mountEl, escapeHtml, push, setHeader } = deps;
    setHeader(screen.titulo || "Formulário");

    const fieldsHtml = (screen.itens || []).map((item) => {
      if (item.tipo === "TEXTO") {
        return `<p class="list-intro">${escapeHtml(item.texto || "")}</p>`;
      }
      const inputType = FORM_ITEM_INPUT_TYPES[item.tipo] || "text";
      return `
        <div class="field" data-field-id="${escapeHtml(item.id)}">
          <label for="screen-field-${escapeHtml(item.id)}">${escapeHtml(item.titulo || item.id)}</label>
          <input id="screen-field-${escapeHtml(item.id)}" type="${inputType}" value="${escapeHtml(item.valor ?? "")}">
        </div>`;
    }).join("");

    mountEl.innerHTML = `
      <form id="screen-form" novalidate>
        ${fieldsHtml}
        <div id="screen-form-alert"></div>
        <div class="btn-row">
          ${screen.botaoOk ? `<button type="submit" class="btn btn-primary" id="screen-btn-ok">${escapeHtml(screen.botaoOk.texto || "Confirmar")}</button>` : ""}
          ${screen.botaoCancelar ? `<button type="button" class="btn btn-ghost" id="screen-btn-cancel">${escapeHtml(screen.botaoCancelar.texto || "Cancelar")}</button>` : ""}
        </div>
      </form>
    `;

    if (screen.botaoCancelar) {
      mountEl.querySelector("#screen-btn-cancel").addEventListener("click", () => {
        // botaoCancelar.url aponta para outra tela definida pela API.
        push(() => loadScreen(screen.botaoCancelar.url));
      });
    }

    if (screen.botaoOk) {
      mountEl.querySelector("#screen-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const alertHolder = mountEl.querySelector("#screen-form-alert");
        alertHolder.innerHTML = "";

        // Payload do POST = valores digitados, indexados pelo id de cada
        // item, mesclados com o body eventualmente já definido no botão.
        const values = { ...(screen.botaoOk.body || {}) };
        (screen.itens || []).forEach((item) => {
          if (item.tipo === "TEXTO") return;
          const input = mountEl.querySelector(`#screen-field-${item.id}`);
          if (!input) return;
          const raw = input.value.trim();
          values[item.id] = item.tipo === "INPUT_NUMERO" ? (raw === "" ? null : Number(raw)) : (raw || null);
        });

        const btn = mountEl.querySelector("#screen-btn-ok");
        btn.disabled = true;
        const originalLabel = btn.textContent;
        btn.textContent = "Enviando…";

        const res = await deps.apiFetch("POST", screen.botaoOk.url, values);

        btn.disabled = false;
        btn.textContent = originalLabel;

        if (!res.ok) {
          alertHolder.innerHTML = deps.errorAlertHtml(deps.apiErrorMessage(res, "Não foi possível concluir a ação."));
          return;
        }

        deps.toast("Concluído.", "success");
        deps.onFormSuccess(res.data, screen);
      });
    }
  }

  /* ---- SELECAO: cada item vira um botão/opção lido do JSON ---- */
  function renderSelecao(screen) {
    const { mountEl, escapeHtml, setHeader, push } = deps;
    setHeader(screen.titulo || "Selecione uma opção");

    const itens = screen.itens || [];

    if (itens.length === 0) {
      mountEl.innerHTML = `
        <div class="empty-state">
          <h3>Nenhuma opção disponível</h3>
          <p>Não há opções para exibir no momento.</p>
        </div>`;
      return;
    }

    const extractTopicId = deps.extractTopicIdFromUrl || (() => null);

    mountEl.innerHTML = `
      <p class="list-intro">${itens.length} ${itens.length === 1 ? "opção disponível" : "opções disponíveis"}.</p>
      <ul class="topic-list">
        ${itens.map((item, i) => {
          const topicId = extractTopicId(item.url);
          return `
          <li>
            <button class="topic-row" data-index="${i}">
              <span class="topic-num">${String(i + 1).padStart(2, "0")}</span>
              <span class="topic-main">
                <span class="topic-title">${escapeHtml(item.texto)}</span>
              </span>
              <svg class="topic-chevron" viewBox="0 0 20 20" fill="none" width="16" height="16"><path d="M7 4l6 6-6 6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
            ${topicId && deps.onManageTopic ? `<button class="topic-manage-link" data-manage-id="${topicId}">Gerenciar pauta</button>` : ""}
          </li>`;
        }).join("")}
      </ul>
    `;

    mountEl.querySelectorAll(".topic-row").forEach((row) => {
      row.addEventListener("click", () => handleSelecaoItem(itens[Number(row.dataset.index)]));
    });
    mountEl.querySelectorAll(".topic-manage-link").forEach((link) => {
      link.addEventListener("click", (e) => {
        e.stopPropagation();
        deps.onManageTopic(Number(link.dataset.manageId));
      });
    });
  }

  /** Executa a ação de um item de SELECAO: POST (se tiver body) ou navegação (GET). */
  async function handleSelecaoItem(item) {
    if (item.body) {
      // Item de ação (ex.: opção de voto) — POST com a URL e o payload
      // exatamente como a API definiu, sem o cliente conhecer as opções.
      const res = await deps.apiFetch("POST", item.url, item.body);
      if (!res.ok) {
        deps.toast(deps.apiErrorMessage(res, "Não foi possível concluir a ação."), "error");
        return;
      }
      deps.toast("Concluído.", "success");
      deps.goBack();
      return;
    }

    // Item de navegação: segue a URL e interpreta a resposta.
    const res = await deps.apiFetch("GET", item.url);
    if (!res.ok) {
      deps.toast(deps.apiErrorMessage(res, "Não foi possível abrir esta opção."), "error");
      return;
    }
    if (res.data && (res.data.tipo === "FORMULARIO" || res.data.tipo === "SELECAO")) {
      deps.push(() => renderScreen(res.data));
      return;
    }

    if (deps.onSelectionNavigateData) {
      deps.onSelectionNavigateData(item, res.data);
    }
  }

  return { init, loadScreen, renderScreen };
})();
