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

  var SAYINGS = [
    { kind: "Leia de novo", ref: "Josué 1.8", text: "Não se aparte da tua boca o livro desta lei; antes, medita nele dia e noite." },
    { kind: "A igreja", ref: "Hebreus 10.25", text: "Não deixemos a nossa congregação, como é costume de alguns." },
    { kind: "Os irmãos", ref: "Atos 2.42", text: "Perseveravam na doutrina dos apóstolos, na comunhão, no partir do pão e nas orações." },
    { kind: "Leia de novo", ref: "Salmos 1.2", text: "O seu prazer está na lei do Senhor, e na sua lei medita de dia e de noite." },
    { kind: "A igreja", ref: "Salmos 122.1", text: "Alegrei-me quando me disseram: Vamos à casa do Senhor." },
    { kind: "Os irmãos", ref: "1 João 1.7", text: "Se andarmos na luz, como ele está na luz, temos comunhão uns com os outros." },
    { kind: "Leia de novo", ref: "Mateus 4.4", text: "Nem só de pão viverá o homem, mas de toda palavra que sai da boca de Deus." },
    { kind: "A igreja", ref: "Lucas 4.16", text: "Num sábado, segundo o seu costume, entrou na sinagoga." },
    { kind: "Os irmãos", ref: "Hebreus 10.24", text: "Consideremo-nos uns aos outros, para nos estimularmos ao amor e às boas obras." },
    { kind: "Leia de novo", ref: "Tiago 1.22", text: "Sede cumpridores da palavra, e não somente ouvintes." },
    { kind: "A igreja", ref: "Mateus 18.20", text: "Onde estiverem dois ou três reunidos em meu nome, aí estou eu no meio deles." },
    { kind: "Os irmãos", ref: "Eclesiastes 4.9-10", text: "Melhor é serem dois do que um. Se um cair, o outro levanta o seu companheiro." },
    { kind: "Leia de novo", ref: "João 5.39", text: "Examinais as Escrituras, porque julgais ter nelas a vida eterna." },
    { kind: "A igreja", ref: "Salmos 84.10", text: "Vale mais um dia nos teus átrios do que mil em outra parte." },
    { kind: "Os irmãos", ref: "Gálatas 6.2", text: "Levai as cargas uns dos outros e assim cumprireis a lei de Cristo." },
    { kind: "Leia de novo", ref: "Atos 17.11", text: "Examinavam cada dia nas Escrituras se estas coisas eram assim." },
    { kind: "A igreja", ref: "Atos 2.46", text: "Todos os dias perseveravam unânimes no templo." },
    { kind: "Os irmãos", ref: "João 13.35", text: "Nisto conhecerão todos que sois meus discípulos: se vos amardes uns aos outros." },
    { kind: "Leia de novo", ref: "Colossenses 3.16", text: "A palavra de Cristo habite em vós abundantemente." },
    { kind: "Os irmãos", ref: "1 Tessalonicenses 5.11", text: "Consolai-vos uns aos outros e edificai-vos uns aos outros." },
    { kind: "Leia de novo", ref: "Romanos 15.4", text: "Tudo que dantes foi escrito, para nosso ensino foi escrito." },
    { kind: "Os irmãos", ref: "Romanos 12.10", text: "Amai-vos cordialmente uns aos outros com amor fraternal." },
    { kind: "Leia de novo", ref: "Salmos 119.11", text: "Escondi a tua palavra no meu coração, para eu não pecar contra ti." },
    { kind: "Os irmãos", ref: "Malaquias 3.16", text: "Os que temiam ao Senhor falavam cada um ao seu companheiro." }
  ];

  var state;
  var screen = "home";
  var overlay = null;
  var silenceOrigin = "priority";
  var bubble = false;
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

  function mascotFile() {
    var st = status();
    if (st === "done") return "boneco-feliz.jpg";
    if (st === "silent") return "boneco-desconfiado.jpg";
    return "boneco.jpg";
  }

  function mascot() {
    var label = status() === "silent" ? "Boneco desconfiado. Ouvir um versículo" : "Boneco. Ouvir um versículo";
    return '<button class="mascot-btn" data-act="mascot" aria-label="' + esc(label) + '"><img src="' + asset(mascotFile()) + '" alt=""></button>';
  }

  function top() {
    return '<header class="top"><button class="icon-btn" data-act="settings" aria-label="Ajustes">' + gear() + "</button></header>";
  }

  function treeBlock(streak, large) {
    var week = L.weekOf(streak);
    var tree = L.treeForWeek(week);
    var seen = state.lastSeenWeek;
    var vanish = seen !== null && seen !== undefined && seen !== week;
    var next = week < L.MAX_WEEK ? week + 1 : null;
    var oldCanvas = vanish
      ? '<canvas class="tree-canvas old" data-week="' + seen + '"></canvas>'
      : "";
    var preview = next === null
      ? '<p class="note">Árvore Iluminada. Dois anos de leitura.</p>'
      : '<p class="group-label">Próxima etapa</p><canvas class="tree-next" data-week="' + next + '" width="140" height="110" aria-hidden="true"></canvas><p class="help">Coberta de preto. A forma seguinte só aparece na semana dela.</p>';
    return '<div class="tree-wrap' + (large ? " large" : "") + (vanish ? " vanish" : "") + '">' +
      oldCanvas +
      '<div class="tree-sway"><canvas class="tree-canvas live" data-week="' + week + '"></canvas></div></div>' +
      '<p class="tree-name">' + esc(tree.name) + "</p>" +
      preview;
  }

  function home() {
    var st = status();
    var cardTitle = "A pergunta ainda está de pé.";
    var button = '<button class="gold" data-act="ask">Já meditei hoje?</button>';
    if (st === "done") {
      cardTitle = "Você já respondeu hoje.";
      button = '<button class="gold" disabled>Até a meia-noite, sem pergunta</button>';
    } else if (st === "silent") {
      cardTitle = "Silenciado até " + clock(state.silenceUntil) + ".";
      button = '<button class="gold" data-act="silence-home">Alterar o silêncio</button>';
    }
    var broke = state.broke && st !== "done"
      ? '<p class="note">A sequência zerou. A maior foi ' + esc(dayWord(state.best)) + ". A semente recomeça.</p>"
      : "";
    var verse = verseToday();
    return top() +
      '<main class="stage">' +
      treeBlock(state.streak || 0, false) +
      "<h1>M 6:33</h1>" +
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
    var marks = [
      ["Semana", "7 dias seguidos"],
      ["Mês", "30 dias seguidos"],
      ["Semestre", "180 dias seguidos"],
      ["Ano", "365 dias seguidos"],
      ["5 anos", "1.825 dias seguidos"],
      ["10 anos", "3.650 dias seguidos"]
    ].map(function (row) {
      return "<li><b>" + esc(row[0]) + "</b><span>" + esc(row[1]) + ". A festa fica maior a cada marco.</span></li>";
    }).join("");
    return top() +
      '<main class="stage">' +
      treeBlock(streak, true) +
      "<h1>" + esc(dayWord(streak)) + "</h1>" +
      '<p class="sub">Maior sequência: ' + esc(dayWord(state.best || 0)) + ". A cada semana de leitura a árvore muda, por dois anos, até a Árvore Iluminada. Se um dia ficar sem leitura, a sequência e a árvore recomeçam. A maior sequência continua guardada.</p>" +
      '<p class="note">Faltam ' + esc(left) + " dias para " + esc(next.label) + ".</p>" +
      '<p class="group-label">Quando a mensagem chega</p>' +
      '<ul class="milestones">' + marks + "</ul>" +
      '<p class="prose">A pergunta volta todo dia, à meia-noite, mesmo que a sequência continue. A sequência só zera se um dia passar sem o sim verdadeiro.</p>' +
      "</main>" + nav("progress") + mascot();
  }

  function about() {
    return top() +
      '<main class="stage"><div class="prose">' +
      "<h1 style=\"text-align:left;margin-left:0\">Sobre</h1>" +
      "<p>O nome <b>M 6:33</b> é o versículo 33 do capítulo 6 do Evangelho de Mateus.</p>" +
      "<blockquote>“Mas buscai primeiro o reino de Deus, e a sua justiça, e todas estas coisas vos serão acrescentadas.”</blockquote>" +
      "<p class=\"ref\">Mateus 6.33</p>" +
      "<p>O app leva esse nome porque a Palavra vem primeiro. Antes dos apps que você escolheu, uma pergunta: você já meditou na Bíblia hoje?</p>" +
      "<p>O dia é o dia do calendário deste celular, da meia-noite até a próxima. Não é uma janela de 24 horas. Leu às 8h ou às 23h50, no dia seguinte a pergunta volta.</p>" +
      "<p>Sim e sim de novo contam o dia e somam a sequência. Se um dia ficar de fora, a sequência zera. A maior sequência fica guardada. Semana, mês, semestre, ano, 5 anos e 10 anos têm uma mensagem, cada uma mais celebrada que a anterior.</p>" +
      "<p>Tudo fica neste aparelho. O protótipo não pede internet para funcionar depois de aberto, e não envia a sua leitura para lugar nenhum.</p>" +
      "<p>Este protótipo ainda não cobre o Instagram de verdade. No Android, isso pede permissões do sistema. Aqui você percorre a pergunta, o silêncio e a árvore. Para ver a sequência crescer, use Ajustes e avance um dia, depois responda de novo.</p>" +
      "<p>App da Bíblia escolhido: " + esc(state.bibleApp || "nenhum") + ".<br>Apps da pergunta: " + esc((state.intercept || []).join(", ") || "nenhum") + ".</p>" +
      "</div></main>" + nav("about");
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

  function celebrateScore(m) {
    var base = { semana: 1, mes: 2.6, semestre: 4.4, ano: 6.2, "5a": 8.2, "10a": 10 }[m.id] || 1;
    return base + Math.min(m.count - 1, 10) * 0.45;
  }

  function comicLine(text, score) {
    return text.split(" ").map(function (word, i) {
      var size = Math.round(16 + score * 2.4 + ((i * 13) % Math.max(2, Math.round(score * 3))));
      var rot = (((i * 17) % 15) - 7) * score * 0.45;
      var y = (((i * 5) % 7) - 3) * score * 0.7;
      return '<span style="font-size:' + size + 'px;transform:rotate(' + rot.toFixed(1) + 'deg) translateY(' + y.toFixed(1) + 'px)">' + esc(word) + "</span>";
    }).join(" ");
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
    var score = celebrateScore(m);
    var wild = score >= 4 ? " wild" : "";
    if (score >= 7) wild += " wilder";
    var titleSize = Math.round(28 + score * 5);
    return '<div class="overlay cele cele-' + esc(m.id) + wild + '">' +
      '<p class="eyebrow">Parabéns</p>' +
      treeBlock(m.streak, true) +
      '<h2 style="font-size:' + titleSize + 'px">' + esc(titles[m.id]) + "</h2>" +
      '<p class="lead comic">' + comicLine(m.message, score) + "</p>" +
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
      '<button class="line" data-act="silence-back">Voltar</button></div>';
  }

  function customScreen() {
    var max = maxSilence();
    return '<div class="overlay"><h2>Valor customizado</h2>' +
      '<p class="help">Horas e minutos. Não passa da meia-noite. Ainda cabem ' + esc(L.formatMinutes(max)) + ".</p>" +
      '<div class="fields"><label><span>Horas</span><input id="hours" inputmode="numeric" value="0"></label>' +
      '<label><span>Minutos</span><input id="mins" inputmode="numeric" value="15"></label></div>' +
      '<p class="warn" id="custom-error"></p>' +
      '<button class="gold" data-act="custom-ok">Silenciar</button>' +
      '<button class="line" data-act="custom-back">Voltar</button></div>';
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
      '<p class="group-label">Aparência</p><div class="chips">' +
      chips(["Como o celular", "Claro", "Escuro"], [({ auto: "Como o celular", light: "Claro", dark: "Escuro" })[state.theme || "auto"]], "theme") +
      "</div>" +
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
    applyTheme();
    root.innerHTML = html + overlayHtml() + talkBubble();
    punchImages();
    paintTrees();
    armClock();
  }

  function applyTheme() {
    var theme = state.theme || "auto";
    document.documentElement.setAttribute("data-theme", theme);
    var dark = theme === "dark" || (theme === "auto" && window.matchMedia("(prefers-color-scheme: dark)").matches);
    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute("content", dark ? "#141210" : "#F7F4EE");
  }

  function talkBubble() {
    if (!bubble) return "";
    var item = SAYINGS[(state.talk || 0) % SAYINGS.length];
    return '<div class="bubble"><p class="kicker">' + esc(item.kind) + "</p><p>" + esc(item.text) + '</p><p class="ref">' + esc(item.ref) + '</p><button class="gold" data-act="mascot">Outro versículo</button><button class="line" data-act="close-talk">Fechar</button></div>';
  }

  var punched = {};
  var treeCache = {};

  function paintTrees() {
    var weekNow = L.weekOf(state.streak || 0);
    var canvases = Array.prototype.slice.call(root.querySelectorAll("canvas[data-week]"));
    var left = canvases.length;
    function done() {
      left -= 1;
      if (left > 0) return;
      window.requestAnimationFrame(function () {
        window.requestAnimationFrame(function () {
          root.querySelectorAll(".tree-wrap.vanish").forEach(function (wrap) {
            wrap.classList.add("play");
          });
        });
      });
    }
    canvases.forEach(function (canvas) {
      drawWeek(canvas, Number(canvas.getAttribute("data-week")), canvas.classList.contains("tree-next"), done);
    });
    if (state.lastSeenWeek !== weekNow) {
      window.setTimeout(function () {
        state.lastSeenWeek = weekNow;
        save();
      }, state.lastSeenWeek == null ? 0 : 1500);
    }
  }

  function drawWeek(canvas, week, silhouette, done) {
    var tree = L.treeForWeek(week);
    var key = tree.file + ":" + week + ":" + (silhouette ? "b" : "c");
    if (treeCache[key]) {
      paintCached(canvas, treeCache[key]);
      if (done) done();
      return;
    }
    var image = new Image();
    image.onload = function () {
      var w = image.naturalWidth;
      var h = image.naturalHeight;
      var off = document.createElement("canvas");
      off.width = w;
      off.height = h;
      var ctx = off.getContext("2d");
      ctx.drawImage(image, 0, 0);
      var pixels = ctx.getImageData(0, 0, w, h);
      var px = pixels.data;
      var i;
      for (i = 0; i < px.length; i += 4) {
        var r = px[i];
        var g = px[i + 1];
        var b = px[i + 2];
        if (r > 226 && g > 214 && b > 190 && (r - b) < 55 && (r - g) < 30) px[i + 3] = 0;
      }
      ctx.putImageData(pixels, 0, 0);
      if (!silhouette) decorateWeek(ctx, w, h, week);
      var framed = cropOpaque(off);
      if (silhouette) {
        var cover = framed.getContext("2d");
        cover.globalCompositeOperation = "source-in";
        cover.fillStyle = "#111";
        cover.fillRect(0, 0, framed.width, framed.height);
      }
      treeCache[key] = framed;
      paintCached(canvas, framed);
      if (done) done();
    };
    image.onerror = function () {
      if (done) done();
    };
    image.src = asset(tree.file);
  }

  function paintCached(canvas, source) {
    var width = canvas.classList.contains("tree-next") ? 140 : 520;
    var height = canvas.classList.contains("tree-next") ? 110 : 420;
    canvas.width = width;
    canvas.height = height;
    var ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, width, height);
    var scale = Math.min(width / source.width, height / source.height);
    var dw = source.width * scale;
    var dh = source.height * scale;
    ctx.drawImage(source, (width - dw) / 2, (height - dh) / 2, dw, dh);
  }

  function cropOpaque(source) {
    var w = source.width;
    var h = source.height;
    var data = source.getContext("2d").getImageData(0, 0, w, h).data;
    var minX = w;
    var minY = h;
    var maxX = 0;
    var maxY = 0;
    var found = false;
    var y;
    var x;
    for (y = 0; y < h; y += 2) {
      for (x = 0; x < w; x += 2) {
        if (data[(y * w + x) * 4 + 3] > 24) {
          found = true;
          if (x < minX) minX = x;
          if (y < minY) minY = y;
          if (x > maxX) maxX = x;
          if (y > maxY) maxY = y;
        }
      }
    }
    if (!found) return source;
    var pad = Math.round(Math.min(w, h) * 0.04);
    minX = Math.max(0, minX - pad);
    minY = Math.max(0, minY - pad);
    maxX = Math.min(w - 1, maxX + pad);
    maxY = Math.min(h - 1, maxY + pad);
    var cw = maxX - minX + 1;
    var ch = maxY - minY + 1;
    var cropped = document.createElement("canvas");
    cropped.width = cw;
    cropped.height = ch;
    cropped.getContext("2d").drawImage(source, minX, minY, cw, ch, 0, 0, cw, ch);
    return cropped;
  }

  function decorateWeek(ctx, w, h, week) {
    if (week <= 0 || week >= L.MAX_WEEK) return;
    if (L.treeForWeek(week).week === week) return;
    var rand = seeded(week * 97 + 13);
    var colors = ["#e85d75", "#f2c14e", "#7c5cdb", "#3cb37a", "#f08a3c", "#4aa3d8", "#fff4c2"];
    var marks = 2 + (week % 8);
    var n;
    ctx.save();
    for (n = 0; n < marks; n += 1) {
      var x = w * (0.22 + rand() * 0.56);
      var y = h * (0.12 + rand() * 0.42);
      ctx.fillStyle = colors[(week + n) % colors.length];
      ctx.beginPath();
      ctx.arc(x, y, 6 + (week % 5) + rand() * 6, 0, Math.PI * 2);
      ctx.fill();
    }
    if (week >= 16) {
      for (n = 0; n < 1 + (week % 4); n += 1) {
        ctx.fillStyle = n % 2 ? "#e23d3d" : "#f0b429";
        ctx.beginPath();
        ctx.arc(w * (0.3 + rand() * 0.4), h * (0.18 + rand() * 0.3), 8 + rand() * 6, 0, Math.PI * 2);
        ctx.fill();
      }
    }
    ctx.restore();
  }

  function seeded(seed) {
    var value = seed % 2147483646;
    if (value <= 0) value += 2147483646;
    return function () {
      value = (value * 16807) % 2147483647;
      return (value - 1) / 2147483646;
    };
  }

  function punchImages() {
    root.querySelectorAll(".tree, .mascot-btn img, .gallery img").forEach(function (img) {
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
    if (!L.silenceFits(minutes, max)) return;
    var amount = minutes;
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
    if (act === "settings") { overlay = "settings"; bubble = false; render(); return; }
    if (act === "mascot") {
      if (bubble) state.talk = (state.talk || 0) + 1;
      bubble = true;
      save();
      render();
      return;
    }
    if (act === "close-talk") { bubble = false; render(); return; }
    if (act === "theme") {
      var picked = el.getAttribute("data-name");
      state.theme = picked === "Claro" ? "light" : picked === "Escuro" ? "dark" : "auto";
      save();
      render();
      return;
    }
    if (act === "silence-home") { silenceOrigin = "home"; overlay = "silence"; render(); return; }
    if (act === "silence-back") { overlay = silenceOrigin === "home" ? null : "priority"; render(); return; }
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
    if (act === "silence-open") { silenceOrigin = "priority"; overlay = "silence"; render(); return; }
    if (act === "custom-back") { overlay = "silence"; render(); return; }
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
      var max = maxSilence();
      var warn = document.getElementById("custom-error");
      if (!L.silenceFits(total, max)) {
        if (warn) {
          warn.textContent = total < 1
            ? "Escolha pelo menos 1 minuto."
            : "Esse tempo passa da meia-noite de hoje. Ainda cabem " + L.formatMinutes(max) + ".";
        }
        return;
      }
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
  if (window.matchMedia) {
    window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", function () {
      if ((state.theme || "auto") === "auto") applyTheme();
    });
  }
  root.addEventListener("input", function (event) {
    if (event.target.id !== "hours" && event.target.id !== "mins") return;
    var hours = Math.max(0, Number(document.getElementById("hours").value) || 0);
    var mins = Math.max(0, Number(document.getElementById("mins").value) || 0);
    var warn = document.getElementById("custom-error");
    if (!warn) return;
    var total = hours * 60 + mins;
    var max = maxSilence();
    warn.textContent = total > max
      ? "Esse tempo passa da meia-noite de hoje. Ainda cabem " + L.formatMinutes(max) + "."
      : "";
  });

  if ("serviceWorker" in navigator && location.protocol.indexOf("http") === 0) {
    navigator.serviceWorker.register("sw.js").catch(function () {});
  }
})();
