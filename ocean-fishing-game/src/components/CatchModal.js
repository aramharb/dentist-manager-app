import React, { useEffect, useRef } from 'react';
import { Modal, View, Text, ScrollView, Pressable, Animated, StyleSheet } from 'react-native';
import SpeciesCard from './SpeciesCard';
import { isLitter, RARITIES, TOTAL_COLLECTIBLES } from '../data/species';
import { colors, rarityColor } from '../theme';

const SPARKS = ['✨', '⭐', '✨', '💫', '✨', '🌟', '✨', '💫'];

function Sparkles({ active }) {
  const anims = useRef(SPARKS.map(() => new Animated.Value(0))).current;
  useEffect(() => {
    if (!active) return;
    const loops = anims.map((a, i) => {
      const loop = Animated.loop(
        Animated.timing(a, { toValue: 1, duration: 1800 + i * 150, useNativeDriver: true }),
      );
      setTimeout(() => loop.start(), i * 120);
      return loop;
    });
    return () => loops.forEach((l) => l.stop());
  }, [active, anims]);
  if (!active) return null;
  return (
    <View pointerEvents="none" style={StyleSheet.absoluteFill}>
      {anims.map((a, i) => (
        <Animated.Text
          key={i}
          style={{
            position: 'absolute',
            left: `${8 + i * 12}%`,
            top: -20,
            fontSize: 22 + (i % 3) * 6,
            opacity: a.interpolate({ inputRange: [0, 0.1, 0.9, 1], outputRange: [0, 1, 1, 0] }),
            transform: [{ translateY: a.interpolate({ inputRange: [0, 1], outputRange: [0, 620] }) }],
          }}
        >
          {SPARKS[i]}
        </Animated.Text>
      ))}
    </View>
  );
}

// Shown right after reeling something in.
export default function CatchModal({ result, discovered, onClose }) {
  const scale = useRef(new Animated.Value(0.6)).current;
  useEffect(() => {
    if (!result) return;
    scale.setValue(0.6);
    Animated.spring(scale, { toValue: 1, friction: 5, tension: 90, useNativeDriver: true }).start();
  }, [result, scale]);

  if (!result) return null;
  const { species, size, isNew, isRecord, entry } = result;
  const litter = isLitter(species);
  const big = species.rarity === 'epic' || species.rarity === 'legendary';
  const complete = !litter && discovered >= TOTAL_COLLECTIBLES;

  let title = litter ? '🧹 Ocean cleaned!' : isNew ? '🎉 NEW DISCOVERY!' : isRecord ? '📏 New personal record!' : 'You caught…';
  if (species.rarity === 'legendary' && isNew) title = '👑 LEGENDARY DISCOVERY!';

  return (
    <Modal transparent animationType="fade" onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <Sparkles active={big} />
        <ScrollView contentContainerStyle={styles.scroll}>
          <Text style={[styles.title, { color: isNew ? colors.accent : colors.text }]}>{title}</Text>
          <Animated.View style={{ transform: [{ scale }], alignSelf: 'stretch' }}>
            <SpeciesCard species={species} entry={entry} size={size}>
              {!litter && (
                <Text style={[styles.progress, { color: rarityColor(species.rarity) }]}>
                  Book: {discovered}/{TOTAL_COLLECTIBLES} discovered
                </Text>
              )}
            </SpeciesCard>
          </Animated.View>
          {complete && <Text style={styles.complete}>🏆 Collection complete — you are a true Ocean Master!</Text>}
          <Pressable onPress={onClose} style={({ pressed }) => [styles.btn, pressed && { opacity: 0.8 }]}>
            <Text style={styles.btnText}>Keep fishing 🎣</Text>
          </Pressable>
        </ScrollView>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(2,14,24,0.92)' },
  scroll: { padding: 20, paddingTop: 60, paddingBottom: 40, alignItems: 'center', maxWidth: 480, alignSelf: 'center', width: '100%' },
  title: { fontSize: 22, fontWeight: '900', marginBottom: 14, textAlign: 'center' },
  progress: { marginTop: 8, fontWeight: '700' },
  complete: { color: colors.accent, fontSize: 16, fontWeight: '800', marginTop: 16, textAlign: 'center' },
  btn: { marginTop: 20, backgroundColor: colors.accent, paddingHorizontal: 32, paddingVertical: 14, borderRadius: 30 },
  btnText: { color: '#06243a', fontWeight: '900', fontSize: 17 },
});
