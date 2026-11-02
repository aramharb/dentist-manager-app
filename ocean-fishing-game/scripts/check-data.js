// Sanity checks for the species data and game logic. Run: npm test
// species.js uses ES modules; load it through a tiny transform.
const fs = require('fs');
const path = require('path');
const vm = require('vm');


const src = fs.readFileSync(path.join(__dirname, '..', 'src/data/species.js'), 'utf8')
  .replace(/export const (\w+)/g, 'var $1 = exports.$1')
  .replace(/export function (\w+)/g, 'exports.$1 = $1; function $1');
const sandbox = { exports: {}, Math, Object, console };
vm.runInNewContext(src, sandbox);
const D = sandbox.exports;

let failed = 0;
const check = (ok, msg) => { if (!ok) { failed++; console.error('FAIL:', msg); } };

const ids = new Set();
for (const s of D.SPECIES) {
  check(!ids.has(s.id), `duplicate id ${s.id}`);
  ids.add(s.id);
  check(D.RARITIES[s.rarity], `${s.id}: bad rarity`);
  check(D.CATEGORIES[s.category], `${s.id}: bad category`);
  check(s.zones.length && s.zones.every((z) => D.ZONES[z]), `${s.id}: bad zones`);
  check(s.size.min < s.size.max, `${s.id}: bad size range`);
  check(s.fact && s.fact.length > 30 && s.ar && s.emoji && s.habitat && s.status, `${s.id}: missing text`);
}
for (const z of Object.keys(D.ZONES)) {
  const pool = D.SPECIES.filter((s) => s.zones.includes(z));
  check(pool.length >= 8, `zone ${z} has too few species (${pool.length})`);
}
// Every collectible must be catchable somewhere, and the unlock chain must be reachable.
const reachable = (z) => D.COLLECTIBLES.filter((s) => s.zones.includes(z)).length;
check(reachable('shore') >= D.ZONES.open.unlockAt, 'cannot unlock Open Sea from Shallows');
check(reachable('shore') + reachable('open') >= D.ZONES.deep.unlockAt, 'cannot unlock Deep Abyss');

// Rarity odds: roll many times and check the distribution is sensible.
const counts = {};
let n = 50000;
for (let i = 0; i < n; i++) {
  const s = D.rollSpecies('open');
  counts[s.rarity] = (counts[s.rarity] || 0) + 1;
}
console.log('open sea rarity %:', Object.fromEntries(Object.entries(counts).map(([k, v]) => [k, +(v / n * 100).toFixed(1)])));
check(counts.common > counts.uncommon && counts.uncommon > counts.rare && counts.rare > counts.epic && counts.epic > counts.legendary,
  'rarity ordering');

console.log(`${D.SPECIES.length} entries, ${D.TOTAL_COLLECTIBLES} collectible species, ${D.SPECIES.length - D.TOTAL_COLLECTIBLES} litter`);
if (failed) { console.error(failed + ' check(s) failed'); process.exit(1); }
console.log('All checks passed');
