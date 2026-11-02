import { RARITIES } from './data/species';

export const colors = {
  sky: '#7cc6ee',
  sun: '#ffe08a',
  water: ['#2a9dd6', '#1b7fb8', '#12608f', '#0a4268', '#052b47', '#031c2e'],
  panel: '#0b3552',
  panelDark: '#06243a',
  text: '#eaf6ff',
  muted: '#8fb4cc',
  accent: '#ffd166',
  danger: '#ff6b6b',
  good: '#5ee6a8',
};

export const rarityColor = (r) => RARITIES[r].color;
