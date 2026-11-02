import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { CATEGORIES, RARITIES, formatSize } from '../data/species';
import { colors, rarityColor } from '../theme';

// Full "field guide" card for one species. Used after a catch and from the book.
export default function SpeciesCard({ species, entry, size, children }) {
  const color = rarityColor(species.rarity);
  const cat = CATEGORIES[species.category];
  return (
    <View style={[styles.card, { borderColor: color, shadowColor: color }]}>
      <View style={[styles.hero, { backgroundColor: color + '22' }]}>
        <Text style={styles.emoji}>{species.emoji}</Text>
      </View>
      <View style={[styles.badge, { backgroundColor: color }]}>
        <Text style={styles.badgeText}>{RARITIES[species.rarity].label.toUpperCase()}</Text>
      </View>
      <Text style={styles.name}>{species.name}</Text>
      <Text style={styles.ar}>{species.ar}</Text>
      <Text style={styles.cat}>
        {cat.emoji} {cat.label}
      </Text>

      {children}

      <View style={styles.stats}>
        <Stat label={size ? 'This catch' : 'Biggest'} value={size ? formatSize(size) : entry ? `${entry.best} ${entry.unit}` : '—'} />
        <Stat label="Caught" value={entry ? `${entry.count}×` : '—'} />
        <Stat label="Status" value={species.status} small />
      </View>

      <View style={styles.factBox}>
        <Text style={styles.factTitle}>💡 Did you know?</Text>
        <Text style={styles.fact}>{species.fact}</Text>
      </View>
      <Text style={styles.habitat}>📍 {species.habitat}</Text>
    </View>
  );
}

function Stat({ label, value, small }) {
  return (
    <View style={styles.stat}>
      <Text style={styles.statLabel}>{label}</Text>
      <Text style={[styles.statValue, small && { fontSize: 11 }]} numberOfLines={3}>
        {value}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.panel,
    borderRadius: 24,
    borderWidth: 3,
    padding: 18,
    alignItems: 'center',
    shadowOpacity: 0.6,
    shadowRadius: 20,
    shadowOffset: { width: 0, height: 0 },
    elevation: 12,
  },
  hero: { width: 130, height: 130, borderRadius: 65, alignItems: 'center', justifyContent: 'center' },
  emoji: { fontSize: 76 },
  badge: { marginTop: 12, paddingHorizontal: 12, paddingVertical: 3, borderRadius: 10 },
  badgeText: { color: '#041a2b', fontWeight: '800', fontSize: 11, letterSpacing: 1.5 },
  name: { color: colors.text, fontSize: 24, fontWeight: '800', marginTop: 8, textAlign: 'center' },
  ar: { color: colors.accent, fontSize: 18, marginTop: 2, textAlign: 'center' },
  cat: { color: colors.muted, marginTop: 4, fontSize: 13 },
  stats: { flexDirection: 'row', marginTop: 14, gap: 8, alignSelf: 'stretch' },
  stat: { flex: 1, backgroundColor: colors.panelDark, borderRadius: 12, padding: 8, alignItems: 'center', justifyContent: 'center' },
  statLabel: { color: colors.muted, fontSize: 11 },
  statValue: { color: colors.text, fontWeight: '700', fontSize: 15, marginTop: 2, textAlign: 'center' },
  factBox: { marginTop: 14, backgroundColor: colors.panelDark, borderRadius: 14, padding: 12, alignSelf: 'stretch' },
  factTitle: { color: colors.accent, fontWeight: '700', marginBottom: 4 },
  fact: { color: colors.text, fontSize: 14, lineHeight: 20 },
  habitat: { color: colors.muted, marginTop: 10, fontSize: 12, textAlign: 'center' },
});
