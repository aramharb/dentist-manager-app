import AsyncStorage from '@react-native-async-storage/async-storage';

const KEY = 'oceancatch:v1';

// { catches: { [speciesId]: { count, best, bestUnit, first } }, casts: number }
export const emptySave = () => ({ catches: {}, casts: 0, escaped: 0 });

export async function loadSave() {
  try {
    const raw = await AsyncStorage.getItem(KEY);
    if (!raw) return emptySave();
    return { ...emptySave(), ...JSON.parse(raw) };
  } catch {
    return emptySave();
  }
}

export async function writeSave(save) {
  try {
    await AsyncStorage.setItem(KEY, JSON.stringify(save));
  } catch {
    // Saving is best-effort; the game keeps working in memory.
  }
}

// Pure helper so it can be tested: returns { save, isNew, isRecord }.
export function recordCatch(save, species, size, now = Date.now()) {
  const prev = save.catches[species.id];
  const isNew = !prev;
  const isRecord = !!prev && size.value > prev.best;
  const entry = {
    count: (prev?.count || 0) + 1,
    best: prev ? Math.max(prev.best, size.value) : size.value,
    unit: size.unit,
    first: prev?.first || now,
  };
  return {
    save: { ...save, catches: { ...save.catches, [species.id]: entry } },
    isNew,
    isRecord,
  };
}
