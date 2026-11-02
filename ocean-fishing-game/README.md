# 🌊 Ocean Catch

An educational fishing game for mobile (iOS / Android, built with Expo + React Native).
Tap to cast, **hold to reel**, and fill your Ocean Book with sea animals, plants, seabirds and
facts — including a few Mediterranean / Tunisian specials (red coral of Tabarka, loggerhead
turtles of Kuriat, flamingos, the invasive blue crab…).

## How to play
- **Tap** the sea to cast. Wait for the bobber to dip — **BITE!**
- **Press & hold** to reel. Holding raises *line tension*; **release** to let it cool.
  If tension hits 100% the line snaps. Rarer creatures fight harder.
- Every catch shows a field-guide card with a fun fact, size and conservation status.
- Pull up litter (bags, bottles, ghost nets) to "clean the ocean" and learn how to help.
- Zones: 🏝️ Shallows → 🌊 Open Sea (6 species) → 🌑 Deep Abyss (18 species).
- Collection book: 48 species in 5 rarities, with undiscovered silhouettes and zone hints.
  Progress is saved on the device.

## Run it
```bash
cd ocean-fishing-game
npm install
npx expo start        # scan the QR code with Expo Go (iOS/Android)
npm run web           # or try it in the browser
npm test              # validates the species data and rarity odds
```

## Adding creatures
Add one line to `src/data/species.js` (`S(id, emoji, name, arabicName, category, rarity, zones, minSize, maxSize, unit, habitat, status, fact)`).
The book, odds and zone pools update automatically. `npm test` checks the data.

## Ideas for next steps
Sound effects, real illustrations instead of emoji, French/Arabic UI language toggle,
daily quests, bait & rod upgrades, a quiz after each catch.
