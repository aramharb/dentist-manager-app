// Every catchable creature, plant and piece of litter in the game.
// zones: 'shore' (Shallows), 'open' (Open Sea), 'deep' (Deep Abyss)

export const RARITIES = {
  common: { label: 'Common', color: '#9fb3c8', weight: 50, emoji: '⚪' },
  uncommon: { label: 'Uncommon', color: '#5ee6a8', weight: 28, emoji: '🟢' },
  rare: { label: 'Rare', color: '#4cc9f0', weight: 13, emoji: '🔵' },
  epic: { label: 'Epic', color: '#b388ff', weight: 7, emoji: '🟣' },
  legendary: { label: 'Legendary', color: '#ffb703', weight: 2, emoji: '🟡' },
};
export const RARITY_ORDER = ['common', 'uncommon', 'rare', 'epic', 'legendary'];

export const CATEGORIES = {
  fish: { label: 'Fish', emoji: '🐟' },
  mammal: { label: 'Mammals', emoji: '🐬' },
  reptile: { label: 'Reptiles', emoji: '🐢' },
  invertebrate: { label: 'Invertebrates', emoji: '🐙' },
  plant: { label: 'Plants & Algae', emoji: '🌿' },
  bird: { label: 'Seabirds', emoji: '🦩' },
  litter: { label: 'Ocean Litter', emoji: '🗑️' },
};

export const ZONES = {
  shore: { label: 'Shallows', emoji: '🏝️', unlockAt: 0, blurb: 'Warm, bright and full of life.' },
  open: { label: 'Open Sea', emoji: '🌊', unlockAt: 6, blurb: 'Big blue water. Big animals.' },
  deep: { label: 'Deep Abyss', emoji: '🌑', unlockAt: 18, blurb: 'Dark, cold and very strange.' },
};

const S = (id, emoji, name, ar, category, rarity, zones, min, max, unit, habitat, status, fact) => ({
  id, emoji, name, ar, category, rarity, zones, size: { min, max, unit }, habitat, status, fact,
});

export const SPECIES = [
  // ---------- COMMON ----------
  S('sardine', '🐟', 'Sardine', 'سردين', 'fish', 'common', ['shore', 'open'], 15, 25, 'cm',
    'Atlantic & Mediterranean', 'Least Concern',
    'Sardines swim in schools of millions. When a predator attacks they swirl into a spinning "bait ball" to confuse it.'),
  S('anchovy', '🐟', 'Anchovy', 'الأنشوجة', 'fish', 'common', ['shore', 'open'], 10, 20, 'cm',
    'Coastal waters', 'Least Concern',
    'Tiny but mighty: anchovies are a main food of whales, seabirds and big fish, so the whole ocean food web depends on them.'),
  S('mackerel', '🐟', 'Atlantic Mackerel', 'ماكريل', 'fish', 'common', ['open'], 25, 40, 'cm',
    'Open water, all seasons', 'Least Concern',
    'Mackerel almost never stop swimming. Moving keeps water flowing over their gills so they can breathe.'),
  S('seabream', '🐟', 'Gilt-head Sea Bream', 'دوراد', 'fish', 'common', ['shore'], 30, 50, 'cm',
    'Mediterranean seagrass & sand', 'Least Concern',
    'It is born male and many become female later in life. Its strong jaws crush mussels and shells.'),
  S('mullet', '🐟', 'Red Mullet', 'سلطان إبراهيم', 'fish', 'common', ['shore'], 15, 30, 'cm',
    'Sandy sea floors', 'Least Concern',
    'Two whiskers (barbels) under its chin are covered in taste sensors. It "tastes" the sand to find buried prey.'),
  S('bluecrab', '🦀', 'Atlantic Blue Crab', 'السلطعون الأزرق', 'invertebrate', 'common', ['shore'], 12, 20, 'cm',
    'Lagoons & estuaries', 'Invasive in the Mediterranean',
    'Its Latin name means "beautiful swimmer". It arrived in the Mediterranean by ship and is now spreading along the Tunisian coast.'),
  S('shrimp', '🦐', 'Common Shrimp', 'قمبري', 'invertebrate', 'common', ['shore'], 5, 10, 'cm',
    'Sandy shallows', 'Least Concern',
    "A shrimp's heart sits in its head region, and it walks on tiny legs while using its tail to jump backwards when scared."),
  S('sealettuce', '🥬', 'Sea Lettuce', 'خس البحر', 'plant', 'common', ['shore'], 0.1, 0.4, 'm',
    'Rocky shores & lagoons', 'Least Concern',
    'This green alga is only two cells thick and is edible. In nutrient-polluted water it can bloom and smother the shore.'),
  S('moonjelly', '🪼', 'Moon Jellyfish', 'قنديل البحر', 'invertebrate', 'common', ['shore', 'open'], 10, 40, 'cm',
    'Drifting in every ocean', 'Least Concern',
    'A jellyfish is about 95% water and has no brain, no heart and no bones — yet it has existed for over 500 million years.'),
  S('wipeout_bag', '🛍️', 'Plastic Bag', 'كيس بلاستيكي', 'litter', 'common', ['shore', 'open'], 20, 60, 'cm',
    'Everywhere', 'Pollution',
    'Turtles mistake floating bags for jellyfish and eat them. Roughly 8 million tonnes of plastic enter the ocean every year. Bring a reusable bag!'),
  S('lanternfish', '🐟', 'Lanternfish', 'سمكة الفانوس', 'fish', 'common', ['deep'], 3, 12, 'cm',
    'Twilight zone, 200–1000 m', 'Least Concern',
    'Lanternfish carry rows of glowing organs and are among the most numerous vertebrates on Earth. Every night billions swim up to feed — the biggest migration on the planet.'),

  // ---------- UNCOMMON ----------
  S('clownfish', '🐠', 'Clownfish', 'سمكة المهرج', 'fish', 'uncommon', ['shore'], 8, 11, 'cm',
    'Coral reefs, inside anemones', 'Least Concern',
    'A coat of slime protects it from the anemone\'s stings. All clownfish are born male; the boss of the group turns female.'),
  S('pufferfish', '🐡', 'Pufferfish', 'السمكة المنتفخة', 'fish', 'uncommon', ['shore', 'open'], 15, 60, 'cm',
    'Reefs & warm coasts', 'Varies by species',
    'It gulps water to puff up like a ball. Many species carry a poison far deadlier than cyanide — never touch or eat one.'),
  S('octopus', '🐙', 'Common Octopus', 'أخطبوط', 'invertebrate', 'uncommon', ['shore'], 30, 90, 'cm',
    'Rocky seabeds', 'Least Concern',
    'An octopus has three hearts and blue blood. It can squeeze through any hole bigger than its beak and change colour in a blink.'),
  S('squid', '🦑', 'European Squid', 'حبّار', 'invertebrate', 'uncommon', ['open'], 20, 50, 'cm',
    'Open water', 'Least Concern',
    'Squid fly through the water by jet propulsion, squirting water out of their body, and can shoot a cloud of ink to escape.'),
  S('lobster', '🦞', 'European Lobster', 'جراد البحر', 'invertebrate', 'uncommon', ['shore'], 25, 50, 'cm',
    'Rocky crevices', 'Least Concern',
    'Live lobsters are blue-brown — they only turn red when cooked. They can regrow a lost claw over several moults.'),
  S('oyster', '🦪', 'Oyster', 'محار', 'invertebrate', 'uncommon', ['shore'], 6, 15, 'cm',
    'Reefs & shallow bays', 'Varies by species',
    'One oyster can filter around 190 litres of water a day. Oyster reefs clean the sea and protect coasts from waves.'),
  S('starfish', '⭐', 'Sea Star', 'نجمة البحر', 'invertebrate', 'uncommon', ['shore'], 10, 25, 'cm',
    'Rocky shores & seabeds', 'Least Concern',
    'A sea star has no brain and no blood. It pumps seawater through its body, and can regrow a lost arm.'),
  S('flamingo', '🦩', 'Greater Flamingo', 'النحام الوردي', 'bird', 'uncommon', ['shore'], 110, 150, 'cm',
    'Salt lakes & lagoons (e.g. Tunisia)', 'Least Concern',
    'Flamingos are born grey. They turn pink from the carotenoid pigments in the tiny shrimp and algae they filter from the water.'),
  S('mangrove', '🌳', 'Mangrove', 'المانغروف', 'plant', 'uncommon', ['shore'], 3, 15, 'm',
    'Tropical coasts', 'Many species threatened',
    'Mangroves grow in salt water, are nurseries for baby fish, and store several times more carbon than most land forests.'),
  S('lionfish', '🐠', 'Lionfish', 'سمكة الأسد', 'fish', 'uncommon', ['shore'], 20, 35, 'cm',
    'Reefs', 'Invasive in the Mediterranean',
    'Its venomous spines protect it. It came from the Red Sea through the Suez Canal and now harms native fish in the Mediterranean.'),
  S('posidonia', '🌿', 'Neptune Grass (Posidonia)', 'بوسيدونيا', 'plant', 'uncommon', ['shore'], 0.3, 1, 'm',
    'Mediterranean seabed', 'Protected species',
    'Not an alga but a true flowering plant. Its meadows make oxygen, shelter baby fish and one colony is estimated to be around 100,000 years old.'),
  S('wipeout_bottle', '🧴', 'Plastic Bottle', 'قارورة بلاستيكية', 'litter', 'uncommon', ['shore', 'open'], 15, 30, 'cm',
    'Everywhere', 'Pollution',
    'A plastic bottle can take around 450 years to break down — and even then it only becomes microplastics. Recycle it!'),
  S('dumbo', '🐙', 'Dumbo Octopus', 'أخطبوط دامبو', 'invertebrate', 'uncommon', ['deep'], 20, 40, 'cm',
    'Down to 4000 m', 'Least Concern',
    'It "flaps" ear-like fins to glide along the sea floor. Dumbo octopuses live deeper than any other octopus.'),
  S('deepnet', '🕸️', 'Ghost Net', 'شبكة صيد مهجورة', 'litter', 'uncommon', ['open', 'deep'], 1, 6, 'm',
    'Drifting', 'Pollution',
    'Lost fishing nets keep trapping animals for years — "ghost fishing". You just removed one. Thank you!'),

  // ---------- RARE ----------
  S('redcoral', '🪸', 'Red Coral', 'المرجان الأحمر', 'invertebrate', 'rare', ['shore'], 5, 30, 'cm',
    'Dark rocky caves, Mediterranean', 'Endangered',
    'A colony of tiny animals, not a plant. It grows less than a millimetre a year, which is why overharvesting (as once off Tabarka) hurts it so much.'),
  S('tuna', '🐟', 'Bluefin Tuna', 'التونة الحمراء', 'fish', 'rare', ['open'], 100, 250, 'cm',
    'Atlantic & Mediterranean', 'Least Concern (recovering)',
    'A warm-blooded-ish giant: it keeps its muscles warmer than the water, swims at up to 70 km/h and crosses entire oceans.'),
  S('greenturtle', '🐢', 'Green Sea Turtle', 'السلحفاة الخضراء', 'reptile', 'rare', ['shore', 'open'], 80, 120, 'cm',
    'Warm seas', 'Endangered',
    'Adults graze on seagrass like underwater cows. Females return to the very beach where they hatched to lay their eggs.'),
  S('loggerhead', '🐢', 'Loggerhead Turtle', 'السلحفاة ضخمة الرأس', 'reptile', 'rare', ['shore', 'open'], 70, 110, 'cm',
    'Mediterranean (nests in Tunisia)', 'Vulnerable',
    "Sand temperature decides the babies' sex: warmer nests make more females. Loggerheads nest on Tunisian beaches such as the Kuriat islands."),
  S('dolphin', '🐬', 'Bottlenose Dolphin', 'الدلفين', 'mammal', 'rare', ['shore', 'open'], 2, 4, 'm',
    'Coastal & open seas', 'Least Concern',
    'Dolphins sleep with half their brain at a time and call each other with personal "signature whistles" — like names.'),
  S('moray', '🐍', 'Moray Eel', 'الموراي', 'fish', 'rare', ['shore'], 0.8, 1.5, 'm',
    'Rocky reefs', 'Least Concern',
    'A second set of jaws hidden in its throat shoots forward to grab prey and drag it down. It breathes by constantly opening its mouth.'),
  S('swordfish', '🐟', 'Swordfish', 'أبو سيف', 'fish', 'rare', ['open'], 2, 4, 'm',
    'Open ocean', 'Least Concern',
    'Its "sword" is a flat bill used to slash at prey. A heater organ by its eyes keeps them warm so it sees clearly in cold deep water.'),
  S('sunfish', '🐟', 'Ocean Sunfish', 'سمكة الشمس', 'fish', 'rare', ['open', 'deep'], 1.8, 3, 'm',
    'Open ocean', 'Vulnerable',
    'The heaviest bony fish in the world (up to ~2 tonnes) eats mostly jellyfish, and a female can lay about 300 million eggs.'),
  S('otter', '🦦', 'Sea Otter', 'قضاعة البحر', 'mammal', 'rare', ['shore'], 1, 1.5, 'm',
    'Pacific kelp forests', 'Endangered',
    'Otters use rocks as tools to crack shells, and hold hands while sleeping so they don\'t drift apart. They have the densest fur of any animal.'),
  S('anglerfish', '🐟', 'Anglerfish', 'سمكة الصنارة', 'fish', 'rare', ['deep'], 20, 100, 'cm',
    'Deep sea', 'Least Concern',
    'Only females have the glowing lure on their head. The tiny males bite on and fuse to her body for life.'),
  S('wipeout_can', '🥫', 'Old Can', 'علبة معدنية', 'litter', 'rare', ['shore', 'open'], 8, 15, 'cm',
    'Everywhere', 'Pollution',
    'An aluminium can can last up to ~200 years in the sea, but recycling one saves almost all the energy needed to make a new one.'),

  // ---------- EPIC ----------
  S('monkseal', '🦭', 'Mediterranean Monk Seal', 'فقمة الراهب', 'mammal', 'epic', ['shore'], 2, 2.4, 'm',
    'Caves & remote Mediterranean coasts', 'Endangered',
    'One of the rarest mammals on Earth — only around 700–800 remain. Mothers raise pups in hidden sea caves.'),
  S('manta', '🐟', 'Giant Oceanic Manta Ray', 'شيطان البحر', 'fish', 'epic', ['open'], 3, 7, 'm',
    'Open ocean', 'Endangered',
    'The biggest ray, with wings up to 7 m wide, and the largest brain of any fish. It filter-feeds on plankton and can leap out of the water.'),
  S('greatwhite', '🦈', 'Great White Shark', 'القرش الأبيض العظيم', 'fish', 'epic', ['open'], 4, 6, 'm',
    'Cool coastal seas', 'Vulnerable',
    "A shark's skeleton is made of flexible cartilage, not bone. It senses the faint electricity of living prey and grows thousands of teeth over a lifetime."),
  S('hammerhead', '🦈', 'Great Hammerhead', 'القرش المطرقة', 'fish', 'epic', ['open'], 3, 5, 'm',
    'Warm seas', 'Critically Endangered',
    'The wide "hammer" spreads its senses and gives it an almost 360° view — and it loves hunting stingrays buried in sand.'),
  S('orca', '🐋', 'Orca', 'الأوركا', 'mammal', 'epic', ['open'], 6, 8, 'm',
    'All oceans', 'Data deficient (varies by population)',
    'Orcas are actually the largest dolphins. They live in family pods led by grandmothers, and each pod has its own dialect.'),
  S('humpback', '🐳', 'Humpback Whale', 'الحوت الأحدب', 'mammal', 'epic', ['open'], 12, 16, 'm',
    'All oceans', 'Least Concern',
    'Males sing long, complex songs that change over the years and spread between groups. They migrate thousands of kilometres each year.'),
  S('leatherback', '🐢', 'Leatherback Turtle', 'السلحفاة الجلدية', 'reptile', 'epic', ['open', 'deep'], 1.5, 2.2, 'm',
    'Open ocean', 'Vulnerable',
    'The largest sea turtle has no hard shell, only leathery skin. It dives deeper than 1000 m, mostly to eat jellyfish.'),
  S('penguin', '🐧', 'Emperor Penguin', 'البطريق الإمبراطوري', 'bird', 'epic', ['open'], 100, 130, 'cm',
    'Antarctic ice', 'Near Threatened',
    'It dives over 500 m deep and holds its breath for more than 20 minutes. Dads keep the egg warm on their feet through the Antarctic winter.'),
  S('giantsquid', '🦑', 'Giant Squid', 'الحبار العملاق', 'invertebrate', 'epic', ['deep'], 8, 13, 'm',
    'Deep ocean', 'Least Concern',
    'It has the biggest eyes in the animal kingdom — as large as a dinner plate. It was only filmed alive in the wild in 2012.'),
  S('vampiresquid', '🦑', 'Vampire Squid', 'حبار مصاص الدماء', 'invertebrate', 'epic', ['deep'], 20, 30, 'cm',
    'Oxygen-poor deep water', 'Least Concern',
    'Neither a vampire nor a squid! It eats drifting "marine snow" and, when scared, turns itself inside out and lights up.'),
  S('spermwhale', '🐳', 'Sperm Whale', 'حوت العنبر', 'mammal', 'epic', ['deep'], 11, 16, 'm',
    'Deep ocean', 'Vulnerable',
    'The biggest toothed predator on Earth has the largest brain of any animal. It dives over 1000 m to hunt giant squid.'),

  // ---------- LEGENDARY ----------
  S('bluewhale', '🐋', 'Blue Whale', 'الحوت الأزرق', 'mammal', 'legendary', ['open'], 24, 30, 'm',
    'All oceans', 'Endangered',
    'The largest animal that has ever lived. Its heart weighs as much as a motorbike and its tongue as much as an elephant.'),
  S('whaleshark', '🦈', 'Whale Shark', 'قرش الحوت', 'fish', 'legendary', ['open'], 5, 12, 'm',
    'Warm tropical seas', 'Endangered',
    'The biggest fish alive is a gentle filter-feeder. The spots on its back are unique, like human fingerprints.'),
  S('coelacanth', '🐟', 'Coelacanth', 'سيلاكانث', 'fish', 'legendary', ['deep'], 1.4, 2, 'm',
    'Deep caves, Indian Ocean', 'Critically Endangered',
    'Scientists thought it died out with the dinosaurs — until a living one was caught in 1938. A true "living fossil".'),
  S('colossalsquid', '🦑', 'Colossal Squid', 'الحبار الهائل', 'invertebrate', 'legendary', ['deep'], 4, 10, 'm',
    'Antarctic deep sea', 'Least Concern',
    'Heavier than the giant squid, with swivelling hooks on its tentacles and the largest eyes ever measured.'),
  S('megalodon', '🦷', 'Megalodon Tooth', 'ميغالودون', 'fish', 'legendary', ['deep'], 10, 18, 'cm',
    'Extinct (fossil)', 'Extinct ~3.6 million years ago',
    'A fossil tooth of a shark that may have reached 15 m or more. Teeth like this are found on seabeds and beaches around the world.'),
];

export const SPECIES_BY_ID = Object.fromEntries(SPECIES.map((s) => [s.id, s]));

// Litter is cleaned up, not "collected": it doesn't count toward the species total.
export const COLLECTIBLES = SPECIES.filter((s) => s.category !== 'litter');
export const TOTAL_COLLECTIBLES = COLLECTIBLES.length;

export const isLitter = (s) => s.category === 'litter';

// Pick a rarity first (weighted), then a species of that rarity available in the zone,
// so each tier keeps its intended odds regardless of how many species it holds.
export function rollSpecies(zone, rng = Math.random, luck = 0) {
  const pool = SPECIES.filter((s) => s.zones.includes(zone));
  const tiers = RARITY_ORDER.filter((r) => pool.some((s) => s.rarity === r));
  const weights = tiers.map((r) => {
    const w = RARITIES[r].weight;
    return luck && r !== 'common' ? w * (1 + luck) : w;
  });
  const total = weights.reduce((a, b) => a + b, 0);
  let roll = rng() * total;
  let tier = tiers[tiers.length - 1];
  for (let i = 0; i < tiers.length; i++) {
    roll -= weights[i];
    if (roll <= 0) { tier = tiers[i]; break; }
  }
  const options = pool.filter((s) => s.rarity === tier);
  return options[Math.floor(rng() * options.length)];
}

export function rollSize(species, rng = Math.random) {
  const { min, max, unit } = species.size;
  const v = min + (max - min) * Math.pow(rng(), 1.4);
  const rounded = max < 10 ? Math.round(v * 10) / 10 : Math.round(v);
  return { value: rounded, unit };
}

export const formatSize = (size) => `${size.value} ${size.unit}`;

// Reeling difficulty by rarity (units per second).
export const REEL = {
  common: { reel: 40, heat: 32, cool: 55, pull: 3 },
  uncommon: { reel: 32, heat: 32, cool: 50, pull: 4 },
  rare: { reel: 26, heat: 34, cool: 45, pull: 5 },
  epic: { reel: 22, heat: 38, cool: 42, pull: 5 },
  legendary: { reel: 18, heat: 40, cool: 40, pull: 5 },
};

export const FACTS_OF_THE_DAY = [
  'The ocean covers about 71% of Earth and makes around half of the oxygen we breathe.',
  'More than 80% of the ocean has never been explored or even seen by humans.',
  'The deepest point, the Mariana Trench, is nearly 11 km down — deeper than Everest is tall.',
  'Coral reefs cover less than 1% of the sea floor but shelter about 25% of all marine species.',
  'Plankton drift in the ocean and feed almost everything — from tiny fish to the blue whale.',
  'Sound travels about four times faster in water than in air, which is why whales can "talk" over long distances.',
  'The Mediterranean is home to around 17,000 species — many found nowhere else.',
];
