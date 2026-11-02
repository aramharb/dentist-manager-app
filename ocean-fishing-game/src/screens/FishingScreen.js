import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { View, Text, Pressable, Animated, Easing, StyleSheet, useWindowDimensions } from 'react-native';
import * as Haptics from 'expo-haptics';
import {
  ZONES, REEL, FACTS_OF_THE_DAY, rollSpecies, rollSize, SPECIES,
} from '../data/species';
import { colors, rarityColor } from '../theme';
import CatchModal from '../components/CatchModal';

const buzz = (kind) => {
  try {
    if (kind === 'success') Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
    else if (kind === 'error') Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
    else Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
  } catch {
    // haptics unavailable (web / simulator)
  }
};

const ZONE_WATER = {
  shore: ['#35b4e8', '#2a9dd6', '#1f86bd', '#176ea3', '#105889', '#0a436b'],
  open: ['#2a8fc8', '#1b7fb8', '#12608f', '#0a4268', '#07304f', '#041f38'],
  deep: ['#0d3a5c', '#092d49', '#062238', '#041a2c', '#031220', '#020a14'],
};
const SKY = { shore: '#8fd3f4', open: '#6fb7e0', deep: '#2b4a66' };

const rand = (a, b) => a + Math.random() * (b - a);

export default function FishingScreen({ save, discovered, onCatch, onEscape, onCast }) {
  const { width } = useWindowDimensions();
  const [layout, setLayout] = useState({ w: width, h: 520 });
  const [zone, setZone] = useState('shore');
  const [phase, setPhase] = useState('idle'); // idle | waiting | bite | reeling | result
  const [reel, setReel] = useState({ p: 0, t: 0 });
  const [banner, setBanner] = useState(null);
  const [result, setResult] = useState(null);
  const [fact, setFact] = useState(() => FACTS_OF_THE_DAY[Math.floor(Math.random() * FACTS_OF_THE_DAY.length)]);

  const phaseRef = useRef('idle');
  const holdRef = useRef(false);
  const fishRef = useRef(null);
  const castAtRef = useRef(0);
  const timers = useRef([]);
  const hookY = useRef(new Animated.Value(0)).current;
  const shake = useRef(new Animated.Value(0)).current;

  const waterY = Math.round(layout.h * 0.3);
  const maxDepth = layout.h - 150;
  const cx = layout.w / 2;
  // Rod: a tilted stick standing on the boat; the line hangs from its tip.
  const ROD_LEN = 100;
  const ROD_ANGLE = (38 * Math.PI) / 180;
  const baseX = cx - 36;
  const baseY = waterY - 8;
  const tipX = baseX + ROD_LEN * Math.sin(ROD_ANGLE);
  const rodY = baseY - ROD_LEN * Math.cos(ROD_ANGLE);
  const lineX = tipX;

  const go = useCallback((p) => {
    phaseRef.current = p;
    setPhase(p);
  }, []);

  const later = (fn, ms) => {
    const id = setTimeout(fn, ms);
    timers.current.push(id);
    return id;
  };
  const clearTimers = () => {
    timers.current.forEach(clearTimeout);
    timers.current = [];
  };
  useEffect(() => clearTimers, []);

  useEffect(() => {
    hookY.setValue(rodY + 10);
  }, [rodY, hookY]);

  const reelIn = useCallback(() => {
    Animated.timing(hookY, { toValue: rodY + 10, duration: 450, easing: Easing.out(Easing.quad), useNativeDriver: false }).start();
  }, [hookY, rodY]);

  const showBanner = useCallback((text, tone = 'bad') => {
    setBanner({ text, tone });
    later(() => setBanner(null), 2200);
  }, []);

  const fail = useCallback((text) => {
    clearTimers();
    holdRef.current = false;
    shake.stopAnimation();
    shake.setValue(0);
    onEscape && onEscape();
    reelIn();
    go('idle');
    setReel({ p: 0, t: 0 });
    showBanner(text);
    buzz('error');
  }, [go, onEscape, reelIn, shake, showBanner]);

  // ---- phase: idle -> waiting ----
  const cast = useCallback(() => {
    if (phaseRef.current !== 'idle') return;
    clearTimers();
    setBanner(null);
    onCast && onCast();
    castAtRef.current = Date.now();
    go('waiting');
    Animated.timing(hookY, {
      toValue: waterY + 8, duration: 550, easing: Easing.out(Easing.cubic), useNativeDriver: false,
    }).start();
    // Pick what will bite now so the reel difficulty matches the fish.
    const species = rollSpecies(zone);
    fishRef.current = { species, size: rollSize(species), startedAt: 0 };
    later(() => {
      if (phaseRef.current !== 'waiting') return;
      go('bite');
      buzz('impact');
      Animated.loop(
        Animated.sequence([
          Animated.timing(shake, { toValue: 1, duration: 70, useNativeDriver: false }),
          Animated.timing(shake, { toValue: -1, duration: 70, useNativeDriver: false }),
        ]),
      ).start();
      Animated.timing(hookY, { toValue: waterY + 34, duration: 160, useNativeDriver: false }).start();
      // Player must grab the rod quickly or the fish spits the hook.
      later(() => {
        if (phaseRef.current === 'bite') fail('It got away… be quicker next time! 💨');
      }, 1500);
    }, rand(1600, 4600));
  }, [fail, go, hookY, onCast, waterY, zone, shake]);

  const startReeling = useCallback(() => {
    clearTimers();
    shake.stopAnimation();
    shake.setValue(0);
    fishRef.current.startedAt = Date.now();
    setReel({ p: 8, t: 0 });
    go('reeling');
  }, [go, shake]);

  // ---- phase: reeling game loop ----
  useEffect(() => {
    if (phase !== 'reeling') return undefined;
    const fish = fishRef.current;
    const cfg = REEL[fish.species.rarity];
    let p = 8;
    let t = 0;
    let last = Date.now();
    let raf;
    const tick = () => {
      const now = Date.now();
      const dt = Math.min(0.05, (now - last) / 1000);
      last = now;
      if (holdRef.current) {
        p += cfg.reel * dt;
        t += cfg.heat * dt;
      } else {
        t -= cfg.cool * dt;
        p -= cfg.pull * dt;
      }
      p = Math.max(2, Math.min(100, p));
      t = Math.max(0, t);
      setReel({ p, t });
      hookY.setValue(waterY + 20 + (1 - p / 100) * (maxDepth - waterY));

      if (t >= 100) return fail('SNAP! 💥 The line broke — let it cool down by releasing.');
      if (now - fish.startedAt > 45000) return fail('The fish got away… 🐟');
      if (p >= 100) return finish();
      raf = requestAnimationFrame(tick);
      return undefined;
    };
    const finish = () => {
      holdRef.current = false;
      const { species, size } = fish;
      const info = onCatch(species, size);
      buzz('success');
      reelIn();
      go('result');
      setReel({ p: 0, t: 0 });
      setResult({ species, size, ...info });
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [phase, fail, go, hookY, maxDepth, onCatch, reelIn, waterY]);

  const onPressIn = () => {
    const ph = phaseRef.current;
    if (ph === 'bite') {
      holdRef.current = true;
      startReeling();
    } else if (ph === 'reeling') {
      holdRef.current = true;
    } else if (ph === 'waiting' && Date.now() - castAtRef.current > 700) {
      fail('Too early! You scared the fish away. 🫣');
    }
  };
  const onPressOut = () => {
    holdRef.current = false;
  };
  const onPress = () => {
    if (phaseRef.current === 'idle') cast();
  };

  const closeResult = () => {
    setResult(null);
    go('idle');
    setFact(FACTS_OF_THE_DAY[Math.floor(Math.random() * FACTS_OF_THE_DAY.length)]);
  };

  const status = {
    idle: 'Tap anywhere to cast 🎣',
    waiting: 'Waiting for a bite…',
    bite: '❗ BITE! Press & HOLD to reel!',
    reeling: 'Hold to reel — release when tension is high!',
    result: '',
  }[phase];

  const fish = fishRef.current;
  const showAura = phase === 'reeling' && fish && reel.p > 55;
  const shadowSize = fish ? 40 + Math.min(1, REEL_SIZE(fish.species)) * 110 : 60;
  const unlockedCount = discovered;

  return (
    <View style={styles.root}>
      <Pressable
        style={styles.scene}
        onLayout={(e) => setLayout({ w: e.nativeEvent.layout.width, h: e.nativeEvent.layout.height })}
        onPressIn={onPressIn}
        onPressOut={onPressOut}
        onPress={onPress}
      >
        <Scene layout={layout} waterY={waterY} zone={zone} />

        {/* boat + rod */}
        <Text style={[styles.boat, { left: cx - 76, top: waterY - 34 }]}>🛶</Text>
        <Text style={[styles.angler, { left: cx - 58, top: waterY - 58 }]}>🧑</Text>
        <View
          pointerEvents="none"
          style={{
            position: 'absolute',
            width: 4,
            height: ROD_LEN,
            borderRadius: 2,
            backgroundColor: '#8a5a2b',
            left: (baseX + tipX) / 2 - 2,
            top: (baseY + rodY) / 2 - ROD_LEN / 2,
            transform: [{ rotate: '38deg' }],
          }}
        />

        {/* fishing line */}
        <Animated.View
          pointerEvents="none"
          style={{
            position: 'absolute',
            left: lineX,
            top: rodY,
            width: 2,
            backgroundColor: 'rgba(255,255,255,0.85)',
            height: hookY.interpolate({ inputRange: [-1000, 5000], outputRange: [-1000 - rodY, 5000 - rodY] }),
            transform: [{ translateX: shake.interpolate({ inputRange: [-1, 1], outputRange: [-3, 3] }) }],
          }}
        />
        {/* bobber / hook */}
        {phase !== 'idle' && phase !== 'result' && (
          <Animated.View
            pointerEvents="none"
            style={{
              position: 'absolute',
              left: lineX - 9,
              top: Animated.subtract(hookY, 9),
              transform: [{ translateX: shake.interpolate({ inputRange: [-1, 1], outputRange: [-3, 3] }) }],
            }}
          >
            {phase === 'reeling' ? (
              <View style={{ alignItems: 'center' }}>
                <Text style={{ fontSize: 18 }}>🪝</Text>
                <View
                  style={{
                    width: shadowSize, height: shadowSize * 0.45, borderRadius: shadowSize, marginTop: 4,
                    backgroundColor: 'rgba(0,10,20,0.55)',
                    borderWidth: showAura ? 3 : 0,
                    borderColor: showAura ? rarityColor(fish.species.rarity) : 'transparent',
                    shadowColor: showAura ? rarityColor(fish.species.rarity) : '#000',
                    shadowOpacity: showAura ? 1 : 0,
                    shadowRadius: 14,
                  }}
                />
              </View>
            ) : (
              <View style={styles.bobber}>
                <View style={styles.bobberTop} />
              </View>
            )}
          </Animated.View>
        )}
        {phase === 'bite' && <Ripples x={lineX} y={waterY + 8} />}

        {/* messages */}
        <View style={styles.msgWrap} pointerEvents="none">
          {banner ? (
            <Text style={[styles.banner, banner.tone === 'bad' && { backgroundColor: 'rgba(160,30,40,0.9)' }]}>{banner.text}</Text>
          ) : phase === 'reeling' ? null : (
            <Text style={[styles.status, phase === 'bite' && { color: colors.accent, fontSize: 24 }]}>{status}</Text>
          )}
          {phase === 'idle' && !banner && (
            <View style={styles.factCard}>
              <Text style={styles.factText}>🌊 {fact}</Text>
            </View>
          )}
        </View>

        {phase === 'reeling' && <Meters p={reel.p} t={reel.t} />}
      </Pressable>

      {/* zone picker */}
      <View style={styles.zones}>
        {Object.entries(ZONES).map(([id, z]) => {
          const locked = unlockedCount < z.unlockAt;
          const active = zone === id;
          return (
            <Pressable
              key={id}
              disabled={locked || phase !== 'idle'}
              onPress={() => setZone(id)}
              style={[styles.zone, active && styles.zoneActive, (locked || (phase !== 'idle' && !active)) && { opacity: 0.45 }]}
            >
              <Text style={styles.zoneEmoji}>{locked ? '🔒' : z.emoji}</Text>
              <Text style={styles.zoneLabel}>{z.label}</Text>
              <Text style={styles.zoneSub}>{locked ? `${z.unlockAt} species` : `${SPECIES.filter((s) => s.zones.includes(id)).length} to find`}</Text>
            </Pressable>
          );
        })}
      </View>

      <CatchModal result={result ? { ...result, entry: save.catches[result.species.id] } : null} discovered={discovered} onClose={closeResult} />
    </View>
  );
}

// 0..1 how big a creature is, for the underwater shadow hint.
function REEL_SIZE(species) {
  const { max, unit } = species.size;
  const cm = unit === 'm' ? max * 100 : max;
  return Math.min(1, Math.log10(Math.max(10, cm)) / 3.4 - 0.25);
}

function Meters({ p, t }) {
  const danger = t > 75;
  return (
    <View style={styles.meters} pointerEvents="none">
      <Text style={styles.hint}>Hold to reel — release when tension is high!</Text>
      <Text style={styles.meterLabel}>🎣 Reel</Text>
      <View style={styles.bar}>
        <View style={[styles.fill, { width: `${p}%`, backgroundColor: colors.good }]} />
      </View>
      <Text style={[styles.meterLabel, danger && { color: colors.danger }]}>{danger ? '⚠️ LINE TENSION — RELEASE!' : '🧵 Line tension'}</Text>
      <View style={styles.bar}>
        <View style={[styles.fill, { width: `${Math.min(100, t)}%`, backgroundColor: t > 75 ? colors.danger : t > 50 ? colors.accent : '#7fd6ff' }]} />
      </View>
    </View>
  );
}

function Ripples({ x, y }) {
  const a = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const loop = Animated.loop(Animated.timing(a, { toValue: 1, duration: 800, useNativeDriver: false }));
    loop.start();
    return () => loop.stop();
  }, [a]);
  const size = a.interpolate({ inputRange: [0, 1], outputRange: [10, 90] });
  return (
    <Animated.View
      pointerEvents="none"
      style={{
        position: 'absolute', left: Animated.subtract(x, Animated.divide(size, 2)), top: Animated.subtract(y, Animated.divide(size, 6)),
        width: size, height: Animated.divide(size, 3), borderRadius: 50, borderWidth: 2, borderColor: 'rgba(255,255,255,0.8)',
        opacity: a.interpolate({ inputRange: [0, 1], outputRange: [1, 0] }),
      }}
    />
  );
}

// ---------------- animated sea background ----------------
function Scene({ layout, waterY, zone }) {
  const bands = ZONE_WATER[zone];
  const bandH = (layout.h - waterY) / bands.length;
  const wave = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const loop = Animated.loop(Animated.timing(wave, { toValue: 1, duration: 4000, easing: Easing.linear, useNativeDriver: true }));
    loop.start();
    return () => loop.stop();
  }, [wave]);

  const swimmers = useMemo(
    () => Array.from({ length: 6 }, (_, i) => ({
      emoji: zone === 'deep' ? ['🐟', '🦑', '🪼'][i % 3] : ['🐠', '🐟', '🐡', '🦀', '🐙', '🐬'][i],
      y: waterY + 60 + ((i * 97) % Math.max(100, layout.h - waterY - 220)),
      dur: 14000 + i * 2500,
      size: 18 + (i % 3) * 6,
    })),
    [zone, waterY, layout.h],
  );

  return (
    <View style={StyleSheet.absoluteFill} pointerEvents="none">
      <View style={{ position: 'absolute', left: 0, right: 0, top: 0, height: waterY, backgroundColor: SKY[zone] }} />
      <Text style={{ position: 'absolute', right: 28, top: 20, fontSize: 46, opacity: zone === 'deep' ? 0.5 : 1 }}>{zone === 'deep' ? '🌙' : '☀️'}</Text>
      <Text style={{ position: 'absolute', left: 24, top: 36, fontSize: 40, opacity: 0.9 }}>☁️</Text>
      <Text style={{ position: 'absolute', left: layout.w * 0.55, top: 70, fontSize: 28, opacity: 0.7 }}>☁️</Text>
      {bands.map((c, i) => (
        <View key={i} style={{ position: 'absolute', left: 0, right: 0, top: waterY + i * bandH, height: bandH + 1, backgroundColor: c }} />
      ))}
      {/* waves */}
      <Animated.Text
        style={{
          position: 'absolute', top: waterY - 14, left: -40, fontSize: 26, color: bands[0], letterSpacing: 2, width: layout.w + 120,
          transform: [{ translateX: wave.interpolate({ inputRange: [0, 1], outputRange: [0, -40] }) }],
        }}
        numberOfLines={1}
      >
        {'〰'.repeat(60)}
      </Animated.Text>
      {swimmers.map((s, i) => (
        <Swimmer key={`${zone}-${i}`} {...s} width={layout.w} dir={i % 2 ? 1 : -1} />
      ))}
      <Bubbles layout={layout} waterY={waterY} />
      {zone === 'shore' && <Text style={{ position: 'absolute', bottom: 4, left: 10, fontSize: 34 }}>🪸🌿🪸</Text>}
      {zone === 'shore' && <Text style={{ position: 'absolute', bottom: 2, right: 14, fontSize: 30 }}>🌿🐚</Text>}
      {zone === 'deep' && <Text style={{ position: 'absolute', bottom: 6, right: 20, fontSize: 24, opacity: 0.8 }}>✨</Text>}
    </View>
  );
}

function Swimmer({ emoji, y, dur, size, width, dir }) {
  const x = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const loop = Animated.loop(Animated.timing(x, { toValue: 1, duration: dur, easing: Easing.linear, useNativeDriver: true }));
    loop.start();
    return () => loop.stop();
  }, [x, dur]);
  const from = dir > 0 ? -60 : width + 60;
  const to = dir > 0 ? width + 60 : -60;
  return (
    <Animated.Text
      style={{
        position: 'absolute', top: y, left: 0, fontSize: size, opacity: 0.35,
        transform: [{ translateX: x.interpolate({ inputRange: [0, 1], outputRange: [from, to] }) }, { scaleX: dir > 0 ? -1 : 1 }],
      }}
    >
      {emoji}
    </Animated.Text>
  );
}

function Bubbles({ layout, waterY }) {
  const items = useMemo(() => Array.from({ length: 8 }, (_, i) => ({ x: (i * 0.13 + 0.07) * layout.w, d: 5000 + i * 700, s: 6 + (i % 4) * 3 })), [layout.w]);
  return items.map((b, i) => <Bubble key={i} {...b} top={waterY} bottom={layout.h} />);
}
function Bubble({ x, d, s, top, bottom }) {
  const a = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const loop = Animated.loop(Animated.timing(a, { toValue: 1, duration: d, easing: Easing.linear, useNativeDriver: true }));
    loop.start();
    return () => loop.stop();
  }, [a, d]);
  return (
    <Animated.View
      style={{
        position: 'absolute', left: x, top: 0, width: s, height: s, borderRadius: s, borderWidth: 1.5,
        borderColor: 'rgba(255,255,255,0.5)',
        opacity: a.interpolate({ inputRange: [0, 0.1, 0.9, 1], outputRange: [0, 0.8, 0.8, 0] }),
        transform: [{ translateY: a.interpolate({ inputRange: [0, 1], outputRange: [bottom, top + 10] }) }],
      }}
    />
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  scene: { flex: 1, overflow: 'hidden' },
  angler: { position: 'absolute', fontSize: 30 },
  boat: { position: 'absolute', fontSize: 72 },
  bobber: { width: 18, height: 18, borderRadius: 9, backgroundColor: '#fff', overflow: 'hidden', borderWidth: 1, borderColor: '#d33' },
  bobberTop: { height: 9, backgroundColor: '#e63946' },
  msgWrap: { position: 'absolute', left: 16, right: 16, bottom: 14, alignItems: 'center' },
  status: {
    color: '#fff', fontSize: 18, fontWeight: '800', textAlign: 'center',
    textShadowColor: 'rgba(0,0,0,0.7)', textShadowRadius: 6, textShadowOffset: { width: 0, height: 1 },
  },
  banner: { color: '#fff', fontWeight: '800', fontSize: 15, textAlign: 'center', paddingHorizontal: 14, paddingVertical: 8, borderRadius: 14, overflow: 'hidden' },
  factCard: { marginTop: 8, backgroundColor: 'rgba(3,28,46,0.72)', borderRadius: 14, padding: 10, maxWidth: 420 },
  factText: { color: colors.text, fontSize: 13, lineHeight: 18, textAlign: 'center' },
  meters: { position: 'absolute', left: 20, right: 20, bottom: 14, backgroundColor: 'rgba(3,28,46,0.75)', borderRadius: 16, padding: 12 },
  hint: { color: colors.accent, fontWeight: '800', fontSize: 13, textAlign: 'center', marginBottom: 8 },
  meterLabel: { color: colors.text, fontWeight: '700', fontSize: 12, marginBottom: 4 },
  bar: { height: 14, backgroundColor: 'rgba(255,255,255,0.15)', borderRadius: 8, overflow: 'hidden', marginBottom: 8 },
  fill: { height: '100%', borderRadius: 8 },
  zones: { flexDirection: 'row', gap: 8, padding: 8, backgroundColor: colors.panelDark },
  zone: { flex: 1, alignItems: 'center', paddingVertical: 6, borderRadius: 14, backgroundColor: colors.panel, borderWidth: 2, borderColor: 'transparent' },
  zoneActive: { borderColor: colors.accent },
  zoneEmoji: { fontSize: 20 },
  zoneLabel: { color: colors.text, fontWeight: '800', fontSize: 12 },
  zoneSub: { color: colors.muted, fontSize: 10 },
});
