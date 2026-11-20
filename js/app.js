(function () {
  "use strict";

  var L = window.LeiaLogic;
  var KEY = "leia-a-biblia";
  var BIBLE_APPS = ["YouVersion", "Bíblia Sagrada", "Bíblia Ave Maria", "Outro app"];
  var DISTRACTIONS = ["Instagram", "TikTok", "YouTube", "WhatsApp", "Facebook", "Chrome"];
  var VERSES = [
    { ref: "Salmos 119.105", text: "Lâmpada para os meus pés é a tua palavra, e luz para o meu caminho." },
    { ref: "Salmos 1.2", text: "O seu prazer está na lei do Senhor, e na sua lei medita de dia e de noite." },
    { ref: "Mateus 4.4", text: "Nem só de pão viverá o homem, mas de toda palavra que sai da boca de Deus." },
    { ref: "Josué 1.8", text: "Medita nele dia e noite, para teres cuidado de fazer conforme tudo que nele está escrito." },
    { ref: "2 Timóteo 3.16", text: "Toda a Escritura é inspirada por Deus, e útil para ensinar, corrigir e instruir." }
  ];
  var SLIDES = [
    {
      img: "tree-00-semente.jpg",
      title: "Medite na Palavra",
      text: "Um cristão lê e medita na Escritura. Não é um extra do dia. É o dia.",
      ref: "Josué 1.8"
    },
    {
      img: "tree-03-planta.jpg",
      title: "Antes do que distrai",
      text: "Quando um app escolhido abrir, a pergunta é uma só: você já meditou na Bíblia hoje?",
      ref: ""
    },
    {
      img: "tree-05-arvore.jpg",
      title: "O dia recomeça à meia-noite",
      text: "Não é uma folga de 24 horas. No dia seguinte a pergunta volta, qualquer que tenha sido a hora da leitura de ontem.",
      ref: ""
    }
  ];

  var state;
  var screen = "home";
  var overlay = null;
  var slide = 0;
  var pendingMilestone = null;
  var clockTimer = null;
  var root = document.getElementById("app");

  function esc(value) {
    return String(value == null ? "" : value).replace(/[&<>"']/g, function (ch) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[ch];
    });
  }

  function load() {
    try {
      var raw = localStorage.getItem(KEY);
      state = raw ? Object.assign(L.freshState(), JSON.parse(raw)) : L.freshState();
    } catch (err) {
      state = L.freshState();
    }
    if (!Array.isArray(state.intercept)) state.intercept = [];
    state = L.reconcile(state, todayKey());
    save();
  }

  function save() {
    localStorage.setItem(KEY, JSON.stringify(state));
  }

  function nowDate() {
    var n = new Date();
    if (state.dayOffset) n.setDate(n.getDate() + state.dayOffset);
    return n;
  }

  function todayKey() {
    return L.dateKey(nowDate());
  }

  function nowMs() {
    return nowDate().getTime();
  }

  function maxSilence() {
    var now = nowDate();
    return L.minutesUntil(now.getTime(), L.endOfLocalDay(now).getTime());
  }

  function status() {
    state = L.reconcile(state, todayKey());
    if (state.silenceUntil && !L.silenceActive(state, todayKey(), nowMs())) {
      state.silenceUntil = null;
      state.silenceDate = null;
    }
    return L.dayStatus(state, todayKey(), nowMs());
  }

  function verseToday() {
    var dt = L.parseKey(todayKey());
    var start = new Date(dt.getFullYear(), 0, 0);
    var day = Math.floor((dt - start) / 86400000);
    return VERSES[((day % VERSES.length) + VERSES.length) % VERSES.length];
  }

  function clock(ms) {
    var d = new Date(ms);
    return String(d.getHours()).padStart(2, "0") + ":" + String(d.getMinutes()).padStart(2, "0");
  }

  function dayWord(n) {
    return (n === 1 ? "1 dia" : n + " dias");
  }

  function prettyDate(key) {
    var d = L.parseKey(key);
    return d.toLocaleDateString("pt-BR", { day: "numeric", month: "long" });
  }

  function asset(file) {
    return "assets/" + file;
  }

  function gear() {
    return '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="3"/><path d="M12 3v2.2M12 18.8V21M3 12h2.2M18.8 12H21M5.6 5.6l1.6 1.6M16.8 16.8l1.6 1.6M18.4 5.6l-1.6 1.6M7.2 16.8l-1.6 1.6"/></svg>';
  }

  function cal() {
    return '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><rect x="4" y="5" width="16" height="15" rx="2"/><path d="M8 3v4M16 3v4M4 10h16"/></svg>';
  }

  function navIcon(name) {
    if (name === "home") return '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 11.5 12 5l8 6.5V20a1 1 0 0 1-1 1h-5v-6H10v6H5a1 1 0 0 1-1-1z"/></svg>';
    if (name === "progress") return '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M5 19V10M12 19V5M19 19v-7"/></svg>';
    return '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 20s-7-4.4-7-9a4 4 0 0 1 7-2 4 4 0 0 1 7 2c0 4.6-7 9-7 9z"/></svg>';
  }

  function nav(active) {
    function item(id, label, icon) {
      var on = active === id ? " active" : "";
      return '<button class="' + on.trim() + '" data-act="go" data-screen="' + id + '">' +
        navIcon(icon) + esc(label) + (active === id ? '<i class="mark"></i>' : "") + "</button>";
    }
    return '<nav class="nav">' + item("home", "Início", "home") + item("progress", "Progresso", "progress") + item("about", "Sobre", "about") + "</nav>";
  }

  function mascot() {
    return '<img class="mascot" src="' + asset("boneco.jpg") + '" alt="">';
  }

  function top() {
    return '<header class="top"><button class="icon-btn" data-act="settings" aria-label="Ajustes">' + gear() + "</button></header>";
  }

  function treeImg(streak, large) {
    var tree = L.treeFor(streak);
    return '<img class="tree' + (large ? " large" : "") + '" src="' + asset(tree.file) + '" alt="Árvore no estágio ' + esc(tree.name) + '">' +
      '<p class="tree-name">' + esc(tree.name) + "</p>";
  }

  function home() {
    var st = status();
    var tree = L.treeFor(state.streak || 0);
    var cardTitle = "A pergunta ainda está de pé.";
    var button = '<button class="gold" data-act="ask">Já meditei hoje?</button>';
    if (st === "done") {
      cardTitle = "Você já respondeu hoje.";
      button = '<button class="gold" disabled>Até a meia-noite, sem pergunta</button>';
    } else if (st === "silent") {
      cardTitle = "Silenciado até " + clock(state.silenceUntil) + ".";
      button = '<button class="gold" disabled>Os apps abrem sem pergunta</button>';
    }
    var broke = state.broke && st !== "done"
      ? '<p class="note">A sequência zerou. A maior foi ' + esc(dayWord(state.best)) + ". A semente recomeça.</p>"
      : "";
    var verse = verseToday();
    return top() +
      '<main class="stage">' +
      treeImg(state.streak || 0) +
      "<h1>Leia a Bíblia</h1>" +
      '<p class="sub">Medite na Palavra antes do que distrai.</p>' +
      '<section class="card"><div class="card-icon">' + cal() + "</div><div>" +
      '<div class="kicker">Hoje, ' + esc(prettyDate(todayKey())) + "</div>" +
      "<strong>" + esc(cardTitle) + "</strong></div></section>" +
      '<p class="meta">Sequência: <strong>' + esc(dayWord(state.streak || 0)) + "</strong> · Maior: <strong>" + esc(dayWord(state.best || 0)) + "</strong></p>" +
      broke +
      button +
      '<section class="verse"><div class="verse-label">Palavra do dia</div>' +
      "<blockquote>“" + esc(verse.text) + "”</blockquote>" +
      '<p class="ref">' + esc(verse.ref) + "</p></section>" +
      "</main>" + nav("home") + mascot();
  }

  function nextMark(streak) {
    var steps = [
      { step: 7, label: "a próxima semana" },
      { step: 30, label: "o próximo mês" },
      { step: 180, label: "o próximo semestre" },
      { step: 365, label: "o próximo ano" },
      { step: 1825, label: "os próximos 5 anos" },
      { step: 3650, label: "os próximos 10 anos" }
    ];
    var best = null;
    steps.forEach(function (item) {
      var days = (Math.floor(streak / item.step) + 1) * item.step;
      if (!best || days < best.days) best = { days: days, label: item.label };
    });
    return best;
  }

  function progress() {
    var streak = state.streak || 0;
    var next = nextMark(streak);
    var left = next.days - streak;
    var gallery = L.TREES.map(function (tree) {
      var here = L.treeFor(streak).file === tree.file;
      return '<figure class="' + (here ? "here" : "") + '"><img src="' + asset(tree.file) + '" alt="' + esc(tree.name) + '"><figcaption>' +
        esc(tree.name) + (here ? " · agora" : "") + "</figcaption></figure>";
    }).join("");
    var marks = [
      ["Semana", "7 dias seguidos"],
      ["Mês", "30 dias seguidos"],
      ["Semestre", "180 dias seguidos"],
      ["Ano", "365 dias seguidos"],
      ["5 anos", "1.825 dias seguidos"],
      ["10 anos", "3.650 dias seguidos"]
    ].map(function (row) {
      return "<li><b>" + esc(row[0]) + "</b><span>" + esc(row[1]) + ". A mensagem fica mais solene quanto maior o tempo.</span></li>";
    }).join("");
    return top() +
      '<main class="stage">' +
      treeImg(streak, true) +
      "<h1>" + esc(streak) + (streak === 1 ? " dia" : " dias") + "</h1>" +
      '<p class="sub">Maior sequência: ' + esc(dayWord(state.best || 0)) + ". A árvore segue a sequência de agora. Se um dia ficar sem leitura, ela volta a ser semente. A maior sequência continua guardada.</p>" +
      '<p class="note">Faltam ' + esc(left) + " dias para " + esc(next.label) + ".</p>" +
      '<p class="group-label">Da semente à árvore antiga</p>' +
      '<div class="gallery">' + gallery + "</div>" +
      '<p class="group-label">Quando a mensagem chega</p>' +
      '<ul class="milestones">' + marks + "</ul>" +
      '<p class="prose">A pergunta volta todo dia, à meia-noite, mesmo que a sequência continue. A sequência só zera se um dia passar sem o sim verdadeiro.</p>' +
      "</main>" + nav("progress") + mascot();
  }

  function about() {
    return top() +
      '<main class="stage"><div class="prose">' +
      "<h1 style=\"text-align:left;margin-left:0\">Sobre</h1>" +
      "<p>Leia a Bíblia é um lembrete leve. Antes dos apps que você escolheu, uma pergunta: você já meditou na Bíblia hoje?</p>" +
      "<p>O dia é o dia do calendário deste celular, da meia-noite até a próxima. Não é uma janela de 24 horas. Leu às 8h ou às 23h50, no dia seguinte a pergunta volta.</p>" +
      "<p>Sim e sim de novo contam o dia e somam a sequência. Se um dia ficar de fora, a sequência zera. A maior sequência fica guardada. Semana, mês, semestre, ano, 5 anos e 10 anos têm uma mensagem, cada uma mais celebrada que a anterior.</p>" +
      "<p>Tudo fica neste aparelho. O protótipo não pede internet para funcionar depois de aberto, e não envia a sua leitura para lugar nenhum.</p>" +
      "<p>Este protótipo ainda não cobre o Instagram de verdade. No Android, isso pede permissões do sistema. Aqui você percorre a pergunta, o silêncio e a árvore. Para ver a sequência crescer, use Ajustes e avance um dia, depois responda de novo.</p>" +
      "<p>App da Bíblia escolhido: " + esc(state.bibleApp || "nenhum") + ".<br>Apps da pergunta: " + esc((state.intercept || []).join(", ") || "nenhum") + ".</p>" +
      "</div></main>" + nav("about") + mascot();
  }

  function onboarding() {
    var item = SLIDES[slide];
    var dots = SLIDES.map(function (_, i) {
      return '<i class="' + (i === slide ? "on" : "") + '"></i>';
    }).join("");
    var last = slide === SLIDES.length - 1;
    return '<main class="stage slides">' +
      '<img class="tree large" src="' + asset(item.img) + '" alt="">' +
      "<h1>" + esc(item.title) + "</h1>" +
      '<p class="sub">' + esc(item.text) + "</p>" +
      (item.ref ? '<p class="ref">' + esc(item.ref) + "</p>" : "") +
      '<div class="dots">' + dots + "</div>" +
      '<button class="gold" data-act="slide">' + (last ? "Começar" : "Continuar") + "</button>" +
      "</main>";
  }

  function question(title, body, buttons) {
    return '<div class="overlay"><h2>' + esc(title) + "</h2>" +
      (body ? '<p class="lead">' + body + "</p>" : "") +
      '<div class="actions">' + buttons + "</div></div>";
  }

  function bless() {
    return question(
      "Deus abençoe seu dia e que a vontade dEle seja feita!",
      "Até a meia-noite não vamos perguntar de novo.",
      '<button class="gold" data-act="after-bless">Continuar</button>'
    );
  }

  function celebrate() {
    var m = pendingMilestone;
    var titles = {
      semana: m.count === 1 ? "Uma semana" : m.count + " semanas",
      mes: m.count === 1 ? "Um mês" : m.count + " meses",
      semestre: m.count === 1 ? "Um semestre" : m.count + " semestres",
      ano: m.count === 1 ? "Um ano" : m.count + " anos",
      "5a": m.count === 1 ? "Cinco anos" : (m.count * 5) + " anos",
      "10a": m.count === 1 ? "Dez anos" : (m.count * 10) + " anos"
    };
    return '<div class="overlay cele cele-' + esc(m.id) + '">' +
      '<p class="eyebrow">Parabéns</p>' +
      treeImg(m.streak, true) +
      "<h2>" + esc(titles[m.id]) + "</h2>" +
      '<p class="lead">' + esc(m.message) + "</p>" +
      '<div class="actions"><button class="gold" data-act="close">Continuar</button></div></div>';
  }

  function silenceScreen() {
    var max = maxSilence();
    if (max < 1) {
      return question(
        "O dia acaba em menos de um minuto.",
        "O silêncio não passa da meia-noite.",
        '<button class="gold" data-act="read">Vou ler agora!</button>'
      );
    }
    var choices = L.silenceChoices(state.lastCustomMinutes, max).map(function (choice) {
      if (choice.kind === "custom") {
        return '<button class="choice" data-act="custom"><b>Valor customizado</b><span>Você coloca as horas e os minutos</span></button>';
      }
      var until = clock(nowMs() + choice.minutes * 60000);
      var label = choice.kind === "last" ? "Último que você escolheu" : L.formatMinutes(choice.minutes);
      var detail = choice.kind === "last" ? L.formatMinutes(choice.minutes) + " · até " + until : "até " + until;
      return '<button class="choice" data-act="silence" data-min="' + choice.minutes + '"><b>' + esc(label) + "</b><span>" + esc(detail) + "</span></button>";
    }).join("");
    return '<div class="overlay"><h2>Silenciar</h2>' +
      '<p class="help">O silêncio vale só até a meia-noite de hoje. Ainda cabem ' + esc(L.formatMinutes(max)) + ".</p>" +
      choices +
      '<button class="line" data-act="back-priority">Voltar</button></div>';
  }

  function customScreen() {
    var max = maxSilence();
    return '<div class="overlay"><h2>Valor customizado</h2>' +
      '<p class="help">Horas e minutos. Não passa da meia-noite. Ainda cabem ' + esc(L.formatMinutes(max)) + ".</p>" +
      '<div class="fields"><label><span>Horas</span><input id="hours" inputmode="numeric" value="0"></label>' +
      '<label><span>Minutos</span><input id="mins" inputmode="numeric" value="15"></label></div>' +
      '<button class="gold" data-act="custom-ok">Silenciar</button>' +
      '<button class="line" data-act="silence-open">Voltar</button></div>';
  }

  function readScreen() {
    var verse = verseToday();
    var appName = state.bibleApp ? " No celular, isto abriria o " + esc(state.bibleApp) + "." : "";
    return '<div class="overlay"><p class="eyebrow">Leitura</p><h2>' + esc(verse.ref) + "</h2>" +
      '<p class="lead">“' + esc(verse.text) + "”</p>" +
      '<p class="help">Isto não conclui o dia.' + appName + " Quando um app escolhido abrir de novo, a pergunta volta.</p>" +
      '<button class="gold" data-act="close">Fechei a leitura</button></div>';
  }

  function settings() {
    function chips(list, selected, act) {
      return list.map(function (name) {
        var on = selected.indexOf(name) !== -1;
        return '<button class="chip' + (on ? " on" : "") + '" data-act="' + act + '" data-name="' + esc(name) + '">' + esc(name) + "</button>";
      }).join("");
    }
    return '<div class="overlay sheet"><header class="top"><button class="icon-btn" data-act="close" aria-label="Fechar">' + gear() + "</button></header>" +
      "<h2>Ajustes</h2>" +
      '<p class="group-label">App da Bíblia</p><div class="chips">' + chips(BIBLE_APPS, [state.bibleApp], "bible") + "</div>" +
      '<p class="group-label">Apps da pergunta</p><div class="chips">' + chips(DISTRACTIONS, state.intercept || [], "intercept") + "</div>" +
      '<p class="help">Neste protótipo a lista é de exemplo. No Android, serão os apps instalados no celular.</p>' +
      '<p class="group-label">Para testar neste protótipo</p>' +
      '<p class="help">Dia em uso: ' + esc(prettyDate(todayKey())) + (state.dayOffset ? " (adiantado)" : " (o dia real)") + ". Avançar o dia não conclui a leitura. A pergunta volta, e a sequência só continua se o dia anterior foi concluído.</p>" +
      '<button class="ghost" data-act="advance" data-n="1">Avançar um dia</button>' +
      '<button class="ghost" data-act="advance" data-n="7">Avançar 7 dias</button>' +
      '<button class="line" data-act="real-day">Voltar ao dia real</button>' +
      '<button class="line" data-act="reset">Apagar dados deste celular</button>' +
      '<button class="gold" data-act="close">Concluir</button></div>';
  }

  function overlayHtml() {
    if (overlay === "q1") {
      return question("Você já meditou na Bíblia hoje?", "Como se " + esc((state.intercept && state.intercept[0]) || "um app escolhido") + " tivesse aberto.",
        '<button class="gold" data-act="yes">Sim</button><button class="ghost" data-act="no">Não</button><button class="line" data-act="close">Fechar</button>');
    }
    if (overlay === "q2") {
      return question("Você fala a verdade?", "",
        '<button class="gold" data-act="truth-yes">Sim</button><button class="ghost" data-act="truth-no">Não</button>');
    }
    if (overlay === "bless") return bless();
    if (overlay === "celebrate") return celebrate();
    if (overlay === "grace") {
      return question("Saiba que Deus perdoa esse seu pecado e que ainda há tempo de mudar isso, então leia e mude!", "O dia não foi concluído. A pergunta continua.",
        '<button class="gold" data-act="read">Vou ler agora!</button>');
    }
    if (overlay === "priority") {
      var silenceBtn = maxSilence() >= 1 ? '<button class="ghost" data-act="silence-open">Silenciar</button>' : '<p class="help">O dia acaba em menos de um minuto. O silêncio não passa da meia-noite.</p>';
      return question("Priorize a verdade da vida no seu dia, não deixe para depois!", "",
        '<button class="gold" data-act="read">Vou ler agora!</button>' + silenceBtn);
    }
    if (overlay === "silence") return silenceScreen();
    if (overlay === "custom") return customScreen();
    if (overlay === "read") return readScreen();
    if (overlay === "settings") return settings();
    return "";
  }

  function render() {
    var html;
    if (!state.onboarded) html = onboarding();
    else if (screen === "progress") html = progress();
    else if (screen === "about") html = about();
    else html = home();
    root.innerHTML = html + overlayHtml();
    punchImages();
    armClock();
  }

  var punched = {};

  function punchImages() {
    root.querySelectorAll(".tree, .mascot, .gallery img").forEach(function (img) {
      var src = img.getAttribute("src");
      if (!src || src.indexOf("data:") === 0) return;
      if (punched[src]) {
        img.src = punched[src];
        return;
      }
      var source = new Image();
      source.onload = function () {
        var canvas = document.createElement("canvas");
        canvas.width = source.naturalWidth;
        canvas.height = source.naturalHeight;
        var ctx = canvas.getContext("2d");
        ctx.drawImage(source, 0, 0);
        var image = ctx.getImageData(0, 0, canvas.width, canvas.height);
        var px = image.data;
        var i;
        for (i = 0; i < px.length; i += 4) {
          var r = px[i];
          var g = px[i + 1];
          var b = px[i + 2];
          if (r > 226 && g > 214 && b > 190 && (r - b) < 55 && (r - g) < 30) px[i + 3] = 0;
        }
        ctx.putImageData(image, 0, 0);
        punched[src] = canvas.toDataURL("image/png");
        if (img.getAttribute("src") === src || img.src.indexOf(src) !== -1) img.src = punched[src];
      };
      source.src = src;
    });
  }

  function armClock() {
    clearTimeout(clockTimer);
    if (!L.silenceActive(state, todayKey(), nowMs())) return;
    var left = state.silenceUntil - nowMs();
    clockTimer = setTimeout(function () {
      status();
      save();
      render();
    }, Math.max(400, Math.min(left + 40, 30000)));
  }

  function startSilence(minutes, custom) {
    var max = maxSilence();
    if (max < 1) return;
    var amount = Math.min(minutes, max);
    if (amount < 1) return;
    var now = nowMs();
    state.silenceUntil = now + amount * 60000;
    state.silenceDate = todayKey();
    if (custom) state.lastCustomMinutes = amount;
    save();
    overlay = null;
    screen = "home";
    render();
  }

  root.addEventListener("click", function (event) {
    var el = event.target.closest("[data-act]");
    if (!el) return;
    var act = el.getAttribute("data-act");
    if (act === "slide") {
      if (slide < SLIDES.length - 1) slide += 1;
      else state.onboarded = true;
      save();
      render();
      return;
    }
    if (act === "go") {
      screen = el.getAttribute("data-screen");
      overlay = null;
      render();
      return;
    }
    if (act === "settings") { overlay = "settings"; render(); return; }
    if (act === "close") { overlay = null; pendingMilestone = null; render(); return; }
    if (act === "ask") {
      if (status() === "open") overlay = "q1";
      render();
      return;
    }
    if (act === "yes") { overlay = "q2"; render(); return; }
    if (act === "no") { overlay = "priority"; render(); return; }
    if (act === "truth-yes") {
      var result = L.complete(state, todayKey());
      state = result.state;
      pendingMilestone = result.milestone;
      save();
      overlay = "bless";
      render();
      return;
    }
    if (act === "truth-no") { overlay = "grace"; render(); return; }
    if (act === "after-bless") {
      overlay = pendingMilestone ? "celebrate" : null;
      render();
      return;
    }
    if (act === "read") { overlay = "read"; render(); return; }
    if (act === "silence-open") { overlay = "silence"; render(); return; }
    if (act === "back-priority") { overlay = "priority"; render(); return; }
    if (act === "custom") { overlay = "custom"; render(); return; }
    if (act === "silence") {
      startSilence(Number(el.getAttribute("data-min")), false);
      return;
    }
    if (act === "custom-ok") {
      var hours = Math.max(0, Number(document.getElementById("hours").value) || 0);
      var mins = Math.max(0, Number(document.getElementById("mins").value) || 0);
      var total = hours * 60 + mins;
      if (total < 1) return;
      startSilence(total, true);
      return;
    }
    if (act === "bible") {
      state.bibleApp = el.getAttribute("data-name");
      save();
      render();
      return;
    }
    if (act === "intercept") {
      var name = el.getAttribute("data-name");
      var list = (state.intercept || []).slice();
      var at = list.indexOf(name);
      if (at === -1) list.push(name);
      else list.splice(at, 1);
      state.intercept = list;
      save();
      render();
      return;
    }
    if (act === "advance") {
      state.dayOffset = (state.dayOffset || 0) + Number(el.getAttribute("data-n") || 1);
      state = L.reconcile(state, todayKey());
      save();
      overlay = null;
      screen = "home";
      render();
      return;
    }
    if (act === "real-day") {
      state.dayOffset = 0;
      state = L.reconcile(state, todayKey());
      save();
      render();
      return;
    }
    if (act === "reset") {
      state = L.freshState();
      state.onboarded = true;
      save();
      screen = "home";
      overlay = null;
      slide = 0;
      render();
    }
  });

  document.addEventListener("visibilitychange", function () {
    if (document.hidden) return;
    status();
    save();
    render();
  });

  load();
  render();

  if ("serviceWorker" in navigator && location.protocol.indexOf("http") === 0) {
    navigator.serviceWorker.register("sw.js").catch(function () {});
  }
})();
