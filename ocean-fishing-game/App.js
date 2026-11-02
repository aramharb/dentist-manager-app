import React, { useCallback, useEffect, useRef, useState } from 'react';
import { View, Text, Pressable, StyleSheet, ActivityIndicator } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import FishingScreen from './src/screens/FishingScreen';
import CollectionScreen from './src/screens/CollectionScreen';
import { emptySave, loadSave, recordCatch, writeSave } from './src/storage';
import { TOTAL_COLLECTIBLES, isLitter, SPECIES_BY_ID } from './src/data/species';
import { colors } from './src/theme';

export default function App() {
  const [tab, setTab] = useState('fish');
  const [save, setSave] = useState(null);
  const saveRef = useRef(emptySave());

  useEffect(() => {
    loadSave().then((s) => {
      saveRef.current = s;
      setSave(s);
    });
  }, []);

  const commit = useCallback((next) => {
    saveRef.current = next;
    setSave(next);
    writeSave(next);
  }, []);

  const onCatch = useCallback((species, size) => {
    const { save: next, isNew, isRecord } = recordCatch(saveRef.current, species, size);
    commit(next);
    return { isNew, isRecord };
  }, [commit]);
  const onCast = useCallback(() => commit({ ...saveRef.current, casts: (saveRef.current.casts || 0) + 1 }), [commit]);
  const onEscape = useCallback(() => commit({ ...saveRef.current, escaped: (saveRef.current.escaped || 0) + 1 }), [commit]);

  if (!save) {
    return (
      <View style={[styles.root, { alignItems: 'center', justifyContent: 'center' }]}>
        <ActivityIndicator color={colors.accent} size="large" />
      </View>
    );
  }

  const ids = Object.keys(save.catches);
  const discovered = ids.filter((id) => SPECIES_BY_ID[id] && !isLitter(SPECIES_BY_ID[id])).length;
  const litter = ids.filter((id) => SPECIES_BY_ID[id] && isLitter(SPECIES_BY_ID[id])).reduce((n, id) => n + save.catches[id].count, 0);

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.root} edges={['top', 'bottom']}>
        <StatusBar style="light" />
        <View style={styles.hud}>
          <Text style={styles.title}>🌊 Ocean Catch</Text>
          <View style={styles.hudStats}>
            <Text style={styles.stat}>📖 {discovered}/{TOTAL_COLLECTIBLES}</Text>
            <Text style={styles.stat}>🧹 {litter}</Text>
          </View>
        </View>

        <View style={{ flex: 1 }}>
          {/* Keep the fishing screen mounted so a cast isn't lost when peeking at the book. */}
          <View style={[styles.fill, tab !== 'fish' && styles.hidden]}>
            <FishingScreen save={save} discovered={discovered} onCatch={onCatch} onCast={onCast} onEscape={onEscape} />
          </View>
          {tab === 'book' && (
            <View style={[StyleSheet.absoluteFill, { backgroundColor: colors.panelDark }]}>
              <CollectionScreen save={save} discovered={discovered} />
            </View>
          )}
        </View>

        <View style={styles.tabs}>
          <Tab label="🎣 Fish" active={tab === 'fish'} onPress={() => setTab('fish')} />
          <Tab label="📖 Book" active={tab === 'book'} onPress={() => setTab('book')} />
        </View>
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

function Tab({ label, active, onPress }) {
  return (
    <Pressable onPress={onPress} style={[styles.tab, active && styles.tabActive]}>
      <Text style={[styles.tabText, active && { color: colors.accent }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.panelDark },
  fill: { flex: 1 },
  hidden: { opacity: 0, pointerEvents: 'none' },
  hud: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 8, backgroundColor: colors.panelDark },
  title: { color: colors.text, fontSize: 18, fontWeight: '900' },
  hudStats: { flexDirection: 'row', gap: 10 },
  stat: { color: colors.accent, fontWeight: '800', backgroundColor: colors.panel, paddingHorizontal: 10, paddingVertical: 4, borderRadius: 12, overflow: 'hidden' },
  tabs: { flexDirection: 'row', backgroundColor: colors.panelDark, borderTopWidth: 1, borderTopColor: '#12395a' },
  tab: { flex: 1, alignItems: 'center', paddingVertical: 14 },
  tabActive: { backgroundColor: colors.panel },
  tabText: { color: colors.muted, fontWeight: '800', fontSize: 15 },
});
