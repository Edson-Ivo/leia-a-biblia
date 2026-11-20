const assert = require("assert");
const { LeiaLogic: L } = require("./logic.js");

function state(partial) {
  return Object.assign(L.freshState(), partial);
}

const day1 = "2026-11-20";
const day2 = "2026-11-21";
const day3 = "2026-11-22";

let s = state({});
let r = L.complete(s, day1);
assert.strictEqual(r.state.streak, 1);
assert.strictEqual(r.state.best, 1);
assert.strictEqual(r.state.lastCompleted, day1);
assert.strictEqual(r.milestone, null);

r = L.complete(r.state, day1);
assert.strictEqual(r.already, true);
assert.strictEqual(r.state.streak, 1);

r = L.complete(r.state, day2);
assert.strictEqual(r.state.streak, 2);
assert.strictEqual(r.state.lastCompleted, day2);
assert.strictEqual(L.reconcile(r.state, day3).streak, 2, "o dia seguinte ainda dá tempo de continuar");

let skipped = L.reconcile(L.complete(state({}), day1).state, day3);
assert.strictEqual(skipped.streak, 0);
assert.strictEqual(skipped.broke, true);
assert.strictEqual(skipped.best, 1);
r = L.complete(skipped, day3);
assert.strictEqual(r.state.streak, 1);
assert.strictEqual(r.state.best, 1);
assert.strictEqual(r.state.broke, false);

let alive = L.complete(state({}), day1).state;
alive = L.reconcile(alive, day2);
assert.strictEqual(alive.streak, 1, "ler ontem mantém a sequência viva hoje, em qualquer hora");
assert.strictEqual(L.dayStatus(alive, day2, Date.now()), "open");

let doneMorning = L.complete(state({}), day1).state;
assert.strictEqual(L.dayStatus(doneMorning, day1, Date.now()), "done");
assert.strictEqual(L.dayStatus(L.reconcile(doneMorning, day2), day2, Date.now()), "open");

assert.strictEqual(L.milestoneFor(6), null);
assert.strictEqual(L.milestoneFor(7).id, "semana");
assert.strictEqual(L.milestoneFor(14).id, "semana");
assert.strictEqual(L.milestoneFor(14).count, 2);
assert.strictEqual(L.milestoneFor(30).id, "mes");
assert.strictEqual(L.milestoneFor(180).id, "semestre");
assert.strictEqual(L.milestoneFor(210).id, "mes");
assert.strictEqual(L.milestoneFor(365).id, "ano");
assert.strictEqual(L.milestoneFor(1825).id, "5a");
assert.strictEqual(L.milestoneFor(3650).id, "10a");
assert.ok(L.milestoneMessage("10a", 1).length > L.milestoneMessage("semana", 1).length);

const choices = L.silenceChoices(75, 200);
assert.deepStrictEqual(choices.map((c) => c.kind), ["last", "preset", "preset", "preset", "custom"]);
assert.strictEqual(choices[0].minutes, 75);
assert.deepStrictEqual(
  L.silenceChoices(10, 200).map((c) => c.minutes),
  [5, 10, 30, null]
);
assert.deepStrictEqual(
  L.silenceChoices(75, 20).map((c) => c.minutes),
  [5, 10, null]
);
assert.deepStrictEqual(L.silenceChoices(null, 0), []);

let silenced = state({ silenceUntil: 1_000, silenceDate: day1 });
assert.strictEqual(L.silenceActive(silenced, day1, 500), true);
assert.strictEqual(L.silenceActive(silenced, day1, 1000), false);
assert.strictEqual(L.reconcile(silenced, day2).silenceUntil, null);

assert.strictEqual(L.weekOf(0), 0);
assert.strictEqual(L.weekOf(6), 0);
assert.strictEqual(L.weekOf(7), 1);
assert.strictEqual(L.treeFor(0).name, "Semente");
assert.strictEqual(L.treeFor(7).name, "Broto");
assert.strictEqual(L.treeFor(14).name, "Botão");
assert.strictEqual(L.weekOf(728), 104);
assert.strictEqual(L.treeFor(728).name, "Árvore Iluminada");
assert.strictEqual(L.treeFor(900).name, "Árvore Iluminada");
assert.strictEqual(L.treeForWeek(8).file, L.treeForWeek(9).file);
assert.notStrictEqual(L.treeForWeek(10).file, L.treeForWeek(11).file);
assert.strictEqual(L.silenceFits(30, 20), false);
assert.strictEqual(L.silenceFits(20, 20), true);
assert.strictEqual(L.silenceFits(0, 20), false);

assert.strictEqual(L.addDays("2026-11-20", 1), "2026-11-21");
assert.strictEqual(L.formatMinutes(75), "1 h 15 min");
assert.strictEqual(L.formatMinutes(60), "1 hora");
assert.strictEqual(L.formatMinutes(5), "5 min");

const end = L.endOfLocalDay(new Date(2026, 10, 20, 23, 50, 0));
assert.strictEqual(L.minutesUntil(new Date(2026, 10, 20, 23, 50, 0).getTime(), end.getTime()), 10);

console.log("logic ok");
