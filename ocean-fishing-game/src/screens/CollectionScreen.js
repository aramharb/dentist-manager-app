import React, { useMemo, useState } from 'react';
import { View, Text, FlatList, Pressable, ScrollView, Modal, StyleSheet } from 'react-native';
import {
  SPECIES, COLLECTIBLES, TOTAL_COLLECTIBLES, CATEGORIES, RARITIES, RARITY_ORDER, ZONES,
} from '../data/species';
import { colors, rarityColor } from '../theme';
import SpeciesCard from '../components/SpeciesCard';

export default function CollectionScreen({ save, discovered }) {
  const [filter, setFilter] = useState('all');
  const [open, setOpen] = useState(null);

  const list = useMemo(() => {
    if (filter === 'all') return COLLECTIBLES;
    return SPECIES.filter((s) => s.category === filter);
  }, [filter]);

  const numberOf = (s) => COLLECTIBLES.indexOf(s) + 1;
  const pct = Math.round((discovered / TOTAL_COLLECTIBLES) * 100);

  const header = (
    <View>
      <View style={styles.progressCard}>
        <View style={styles.rowBetween}>
          <Text style={styles.big}>📖 Ocean Book</Text>
          <Text style={styles.big}>
            {discovered}/{TOTAL_COLLECTIBLES}
          </Text>
        </View>
        <View style={styles.bar}>
          <View style={[styles.fill, { width: `${pct}%` }]} />
        </View>
        <View style={styles.pills}>
          {RARITY_ORDER.map((r) => {
            const all = COLLECTIBLES.filter((s) => s.rarity === r);
            const got = all.filter((s) => save.catches[s.id]).length;
            return (
              <View key={r} style={[styles.pill, { borderColor: rarityColor(r) }]}>
                <Text style={[styles.pillText, { color: rarityColor(r) }]}>
                  {got}/{all.length}
                </Text>
                <Text style={styles.pillLabel}>{RARITIES[r].label}</Text>
              </View>
            );
          })}
        </View>
      </View>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chips}>
        <Chip label="🔎 All" active={filter === 'all'} onPress={() => setFilter('all')} />
        {Object.entries(CATEGORIES).map(([id, c]) => (
          <Chip key={id} label={`${c.emoji} ${c.label}`} active={filter === id} onPress={() => setFilter(id)} />
        ))}
      </ScrollView>
    </View>
  );

  return (
    <View style={styles.root}>
      <FlatList
        data={list}
        keyExtractor={(s) => s.id}
        numColumns={3}
        ListHeaderComponent={header}
        contentContainerStyle={{ padding: 12, paddingBottom: 30 }}
        columnWrapperStyle={{ gap: 10 }}
        renderItem={({ item }) => {
          const entry = save.catches[item.id];
          return <Cell species={item} entry={entry} num={numberOf(item)} onPress={() => setOpen(item)} />;
        }}
      />
      <Modal transparent animationType="fade" visible={!!open} onRequestClose={() => setOpen(null)}>
        <Pressable style={styles.backdrop} onPress={() => setOpen(null)}>
          <Pressable style={styles.sheet} onPress={() => {}}>
            <ScrollView>
              {open && save.catches[open.id] ? (
                <SpeciesCard species={open} entry={save.catches[open.id]} />
              ) : open ? (
                <Locked species={open} />
              ) : null}
              <Pressable onPress={() => setOpen(null)} style={styles.close}>
                <Text style={styles.closeText}>Close</Text>
              </Pressable>
            </ScrollView>
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
}

function Cell({ species, entry, num, onPress }) {
  const color = rarityColor(species.rarity);
  const found = !!entry;
  return (
    <Pressable onPress={onPress} style={[styles.cell, { borderColor: found ? color : '#16405e' }]}>
      <Text style={styles.num}>{species.category === 'litter' ? '🧹' : `#${String(num).padStart(2, '0')}`}</Text>
      {found ? (
        <>
          <Text style={styles.cellEmoji}>{species.emoji}</Text>
          <Text style={styles.cellName} numberOfLines={2}>
            {species.name}
          </Text>
          <Text style={[styles.cellCount, { color }]}>×{entry.count}</Text>
        </>
      ) : (
        <>
          <Text style={[styles.cellEmoji, styles.unknown]}>{species.emoji}</Text>
          <Text style={styles.cellName}>???</Text>
          <Text style={[styles.cellCount, { color: color + '99' }]}>{RARITIES[species.rarity].label}</Text>
        </>
      )}
    </Pressable>
  );
}

function Locked({ species }) {
  const color = rarityColor(species.rarity);
  return (
    <View style={[styles.locked, { borderColor: color }]}>
      <Text style={[styles.lockedEmoji]}>❓</Text>
      <Text style={[styles.lockedTitle, { color }]}>{RARITIES[species.rarity].label} creature</Text>
      <Text style={styles.lockedText}>Not discovered yet. Keep fishing to find it!</Text>
      <Text style={styles.lockedHint}>
        🧭 Hint — where to look:{'\n'}
        {species.zones.map((z) => `${ZONES[z].emoji} ${ZONES[z].label}`).join('  •  ')}
      </Text>
    </View>
  );
}

function Chip({ label, active, onPress }) {
  return (
    <Pressable onPress={onPress} style={[styles.chip, active && styles.chipActive]}>
      <Text style={[styles.chipText, active && { color: '#06243a' }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.panelDark },
  progressCard: { backgroundColor: colors.panel, borderRadius: 18, padding: 14 },
  rowBetween: { flexDirection: 'row', justifyContent: 'space-between' },
  big: { color: colors.text, fontSize: 20, fontWeight: '900' },
  bar: { height: 12, backgroundColor: 'rgba(255,255,255,0.12)', borderRadius: 8, marginTop: 10, overflow: 'hidden' },
  fill: { height: '100%', backgroundColor: colors.accent, borderRadius: 8 },
  pills: { flexDirection: 'row', gap: 6, marginTop: 12 },
  pill: { flex: 1, borderWidth: 1.5, borderRadius: 10, paddingVertical: 4, alignItems: 'center' },
  pillText: { fontWeight: '800', fontSize: 13 },
  pillLabel: { color: colors.muted, fontSize: 9 },
  chips: { paddingVertical: 12, gap: 8 },
  chip: { paddingHorizontal: 14, paddingVertical: 7, borderRadius: 18, backgroundColor: colors.panel },
  chipActive: { backgroundColor: colors.accent },
  chipText: { color: colors.text, fontWeight: '700', fontSize: 13 },
  cell: { flex: 1 / 3, marginBottom: 10, backgroundColor: colors.panel, borderRadius: 16, borderWidth: 2, alignItems: 'center', padding: 8, minHeight: 120, maxWidth: '33%' },
  num: { alignSelf: 'flex-start', color: colors.muted, fontSize: 10 },
  cellEmoji: { fontSize: 40, marginTop: 2 },
  unknown: { opacity: 0.18 },
  cellName: { color: colors.text, fontSize: 11, fontWeight: '700', textAlign: 'center', marginTop: 4 },
  cellCount: { fontSize: 11, fontWeight: '800', marginTop: 2 },
  backdrop: { flex: 1, backgroundColor: 'rgba(2,14,24,0.85)', justifyContent: 'center', padding: 18 },
  sheet: { maxHeight: '90%', width: '100%', maxWidth: 480, alignSelf: 'center' },
  close: { alignSelf: 'center', marginTop: 14, backgroundColor: colors.accent, paddingHorizontal: 30, paddingVertical: 12, borderRadius: 26 },
  closeText: { color: '#06243a', fontWeight: '900', fontSize: 16 },
  locked: { backgroundColor: colors.panel, borderWidth: 3, borderRadius: 24, padding: 24, alignItems: 'center' },
  lockedEmoji: { fontSize: 70 },
  lockedTitle: { fontSize: 20, fontWeight: '900', marginTop: 8 },
  lockedText: { color: colors.text, marginTop: 8, textAlign: 'center' },
  lockedHint: { color: colors.accent, marginTop: 14, textAlign: 'center', lineHeight: 22 },
});
