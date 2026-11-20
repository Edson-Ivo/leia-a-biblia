(function (root) {
  "use strict";

  var TIERS = [
    { id: "10a", days: 3650, name: "10 anos" },
    { id: "5a", days: 1825, name: "5 anos" },
    { id: "ano", days: 365, name: "um ano" },
    { id: "semestre", days: 180, name: "um semestre" },
    { id: "mes", days: 30, name: "um mês" },
    { id: "semana", days: 7, name: "uma semana" }
  ];

  var MAX_WEEK = 104;
  var TREES = [
    { week: 0, file: "tree-00-semente.jpg", name: "Semente" },
    { week: 1, file: "tree-01-broto.jpg", name: "Broto" },
    { week: 2, file: "tree-c02-botao.jpg", name: "Botão" },
    { week: 4, file: "tree-c03-flores.jpg", name: "Flores" },
    { week: 7, file: "tree-c04-rosa.jpg", name: "Florada rosa" },
    { week: 11, file: "tree-c05-violeta.jpg", name: "Violeta" },
    { week: 16, file: "tree-c06-frutos.jpg", name: "Primeiros frutos" },
    { week: 22, file: "tree-c07-pomar.jpg", name: "Pomar" },
    { week: 30, file: "tree-c08-vermelho.jpg", name: "Flores e frutos" },
    { week: 40, file: "tree-c09-ouro.jpg", name: "Copa de ouro" },
    { week: 52, file: "tree-c10-lanternas.jpg", name: "Lanternas" },
    { week: 68, file: "tree-c11-crepusculo.jpg", name: "Crepúsculo" },
    { week: 104, file: "tree-c12-iluminada.jpg", name: "Árvore Iluminada" }
  ];

  function pad(n) {
    return String(n).padStart(2, "0");
  }

  function dateKey(date) {
    return date.getFullYear() + "-" + pad(date.getMonth() + 1) + "-" + pad(date.getDate());
  }

  function parseKey(key) {
    var parts = key.split("-").map(Number);
    return new Date(parts[0], parts[1] - 1, parts[2], 12, 0, 0, 0);
  }

  function addDays(key, n) {
    var dt = parseKey(key);
    dt.setDate(dt.getDate() + n);
    return dateKey(dt);
  }

  function freshState() {
    return {
      onboarded: false,
      dayOffset: 0,
      bibleApp: "YouVersion",
      intercept: ["Instagram", "TikTok", "YouTube"],
      lastCompleted: null,
      streak: 0,
      best: 0,
      broke: false,
      lastCustomMinutes: null,
      silenceUntil: null,
      silenceDate: null,
      theme: "auto",
      lastSeenWeek: null,
      talk: 0
    };
  }

  function reconcile(state, today) {
    var next = Object.assign({}, state);
    var yesterday = addDays(today, -1);
    if (next.lastCompleted && next.lastCompleted !== today && next.lastCompleted !== yesterday) {
      if ((next.streak || 0) > 0) next.broke = true;
      next.streak = 0;
    }
    if (next.silenceDate && next.silenceDate !== today) {
      next.silenceUntil = null;
      next.silenceDate = null;
    }
    return next;
  }

  function silenceActive(state, today, nowMs) {
    return Boolean(
      state.silenceUntil &&
      state.silenceDate === today &&
      nowMs < state.silenceUntil
    );
  }

  function dayStatus(state, today, nowMs) {
    if (state.lastCompleted === today) return "done";
    if (silenceActive(state, today, nowMs)) return "silent";
    return "open";
  }

  function milestoneFor(streak) {
    var i;
    for (i = 0; i < TIERS.length; i += 1) {
      var tier = TIERS[i];
      if (streak >= tier.days && streak % tier.days === 0) {
        return {
          id: tier.id,
          days: tier.days,
          count: streak / tier.days,
          streak: streak,
          message: milestoneMessage(tier.id, streak / tier.days)
        };
      }
    }
    return null;
  }

  function milestoneMessage(id, count) {
    if (id === "semana") {
      if (count === 1) {
        return "Uma semana inteira na Palavra. Sete dias seguidos. Que bom que você ficou.";
      }
      return count + " semanas seguidas. A leitura não saiu do seu dia. Continue.";
    }
    if (id === "mes") {
      if (count === 1) {
        return "Um mês completo. Trinta dias meditando na Bíblia. Deus abençoe essa constância, e que a vontade dEle seja feita.";
      }
      return count + " meses seguidos na Palavra. Isso já é um caminho firme. Parabéns.";
    }
    if (id === "semestre") {
      if (count === 1) {
        return "Seis meses sem largar a Escritura. Parabéns, de coração. Pouca coisa boa se sustenta tanto tempo. A sua se sustentou.";
      }
      return count + " semestres seguidos. Meio ano atrás de meio ano, a Palavra ficou. Que alegria. Parabéns.";
    }
    if (id === "ano") {
      if (count === 1) {
        return "Um ano inteiro. Trezentos e sessenta e cinco dias na Bíblia. O Senhor abençoe a sua vida, a sua casa e o dia de amanhã. Parabéns, de verdade.";
      }
      return count + " anos seguidos, dia após dia. Isso é fidelidade. Que Deus guarde o que Ele começou em você. Parabéns.";
    }
    if (id === "5a") {
      if (count === 1) {
        return "Cinco anos. Uma árvore gigante não aparece numa semana. A sua sequência chegou lá. Glória a Deus por cada um desses dias. Parabéns.";
      }
      return count * 5 + " anos seguidos na Palavra. Uma vida inteira inclinada para a Escritura. Parabéns. O Senhor guarde o que é dEle em você.";
    }
    if (count === 1) {
      return "Dez anos. Dia após dia, a Palavra ficou com você. Isso é uma vida, não um esforço de semana. Parabéns. Que os próximos dias sejam tão cheios quanto estes.";
    }
    return count * 10 + " anos. Dia após dia, sem soltar a Bíblia. Poucas coisas duram assim. Parabéns. Guarde este dia, e que Deus abençoe os que ainda vêm.";
  }

  function complete(state, today) {
    if (state.lastCompleted === today) {
      return { state: state, already: true, milestone: null };
    }
    var yesterday = addDays(today, -1);
    var streak = state.lastCompleted === yesterday ? (state.streak || 0) + 1 : 1;
    var next = Object.assign({}, state, {
      lastCompleted: today,
      streak: streak,
      best: Math.max(state.best || 0, streak),
      broke: false,
      silenceUntil: null,
      silenceDate: null
    });
    return { state: next, already: false, milestone: milestoneFor(streak) };
  }

  function minutesUntil(nowMs, endMs) {
    return Math.floor((endMs - nowMs) / 60000);
  }

  function endOfLocalDay(now) {
    var end = new Date(now.getTime());
    end.setHours(24, 0, 0, 0);
    return end;
  }

  function silenceChoices(lastCustom, maxMinutes) {
    var choices = [];
    if (
      lastCustom &&
      lastCustom !== 5 &&
      lastCustom !== 10 &&
      lastCustom !== 30 &&
      lastCustom >= 1 &&
      lastCustom <= maxMinutes
    ) {
      choices.push({ minutes: lastCustom, kind: "last" });
    }
    [5, 10, 30].forEach(function (minutes) {
      if (minutes <= maxMinutes) choices.push({ minutes: minutes, kind: "preset" });
    });
    if (maxMinutes >= 1) choices.push({ minutes: null, kind: "custom" });
    return choices;
  }

  function weekOf(streak) {
    var week = Math.floor((streak || 0) / 7);
    if (week < 0) week = 0;
    if (week > MAX_WEEK) week = MAX_WEEK;
    return week;
  }

  function treeForWeek(week) {
    var chosen = TREES[0];
    var i;
    for (i = 0; i < TREES.length; i += 1) {
      if (week >= TREES[i].week) chosen = TREES[i];
    }
    return chosen;
  }

  function treeFor(streak) {
    return treeForWeek(weekOf(streak));
  }

  function silenceFits(minutes, maxMinutes) {
    return minutes >= 1 && minutes <= maxMinutes;
  }

  function formatMinutes(total) {
    var h = Math.floor(total / 60);
    var m = total % 60;
    if (h && m) return h + " h " + m + " min";
    if (h === 1) return "1 hora";
    if (h) return h + " horas";
    return m + " min";
  }

  var api = {
    TIERS: TIERS,
    TREES: TREES,
    MAX_WEEK: MAX_WEEK,
    weekOf: weekOf,
    treeForWeek: treeForWeek,
    silenceFits: silenceFits,
    dateKey: dateKey,
    parseKey: parseKey,
    addDays: addDays,
    freshState: freshState,
    reconcile: reconcile,
    silenceActive: silenceActive,
    dayStatus: dayStatus,
    milestoneFor: milestoneFor,
    milestoneMessage: milestoneMessage,
    complete: complete,
    minutesUntil: minutesUntil,
    endOfLocalDay: endOfLocalDay,
    silenceChoices: silenceChoices,
    treeFor: treeFor,
    formatMinutes: formatMinutes
  };
  root.LeiaLogic = api;
  if (typeof module !== "undefined" && module.exports) {
    module.exports = { LeiaLogic: api };
  }
})(typeof globalThis !== "undefined" ? globalThis : this);
