// Génère icon.png (256x256) pour weebo-bridge-notify : cloche blanche + pastille
// ambre sur dégradé violet/indigo, coins arrondis. PNG encodé à la main (zlib natif).
const zlib = require('zlib');
const fs = require('fs');

const S = 256;      // taille finale
const SS = 4;       // supersampling anti-aliasing

// --- géométrie (coordonnées en espace 256) ---
function roundedRectAlpha(x, y) {
  const m = 10, r = 58, x0 = m, y0 = m, x1 = S - m, y1 = S - m;
  if (x < x0 || x > x1 || y < y0 || y > y1) return 0;
  const cx = Math.min(Math.max(x, x0 + r), x1 - r);
  const cy = Math.min(Math.max(y, y0 + r), y1 - r);
  const dx = x - cx, dy = y - cy;
  return dx * dx + dy * dy <= r * r ? 1 : 0;
}

function inBell(x, y) {
  const dx = x - 128;
  // dôme (demi-cercle r=52 centré en y=118)
  if (y <= 118 && dx * dx + (y - 118) * (y - 118) <= 52 * 52) return true;
  // corps évasé de y=118 à 166 (demi-largeur 52 -> 70)
  if (y > 118 && y <= 166) {
    const t = (y - 118) / 48;
    if (Math.abs(dx) <= 52 + 18 * t * t) return true;
  }
  // barre inférieure arrondie (capsule)
  const capD = distToSegment(x, y, 62, 172, 194, 172);
  if (capD <= 9) return true;
  // battant
  const cdx = x - 128, cdy = y - 196;
  if (cdx * cdx + cdy * cdy <= 13 * 13) return true;
  return false;
}

function distToSegment(px, py, ax, ay, bx, by) {
  const abx = bx - ax, aby = by - ay;
  const t = Math.max(0, Math.min(1, ((px - ax) * abx + (py - ay) * aby) / (abx * abx + aby * aby)));
  const dx = px - (ax + t * abx), dy = py - (ay + t * aby);
  return Math.sqrt(dx * dx + dy * dy);
}

function inBadge(x, y) {
  const dx = x - 186, dy = y - 74;
  return dx * dx + dy * dy <= 30 * 30;
}

function samplePixel(x, y) {
  const a = roundedRectAlpha(x, y);
  if (!a) return [0, 0, 0, 0];
  // dégradé violet -> indigo
  const t = y / S;
  let r = 124 + (49 - 124) * t;
  let g = 58 + (46 - 58) * t;
  let b = 237 + (129 - 237) * t;
  if (inBell(x, y)) { r = 250; g = 250; b = 252; }
  if (inBadge(x, y)) { r = 251; g = 191; b = 36; }
  return [r, g, b, 255];
}

// --- rendu supersamplé ---
const raw = Buffer.alloc(S * (S * 4 + 1));
for (let y = 0; y < S; y++) {
  raw[y * (S * 4 + 1)] = 0; // filtre PNG "None"
  for (let x = 0; x < S; x++) {
    let r = 0, g = 0, b = 0, a = 0;
    for (let sy = 0; sy < SS; sy++) {
      for (let sx = 0; sx < SS; sx++) {
        const p = samplePixel(x + (sx + 0.5) / SS, y + (sy + 0.5) / SS);
        r += p[0]; g += p[1]; b += p[2]; a += p[3];
      }
    }
    const n = SS * SS, off = y * (S * 4 + 1) + 1 + x * 4;
    raw[off] = Math.round(r / n);
    raw[off + 1] = Math.round(g / n);
    raw[off + 2] = Math.round(b / n);
    raw[off + 3] = Math.round(a / n);
  }
}

// --- encodage PNG ---
const CRC_TABLE = [];
for (let i = 0; i < 256; i++) {
  let c = i;
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
  CRC_TABLE[i] = c >>> 0;
}
function crc32(buf) {
  let c = 0xffffffff;
  for (const byte of buf) c = CRC_TABLE[(c ^ byte) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body));
  return Buffer.concat([len, body, crc]);
}
const ihdr = Buffer.alloc(13);
ihdr.writeUInt32BE(S, 0);
ihdr.writeUInt32BE(S, 4);
ihdr[8] = 8;  // profondeur
ihdr[9] = 6;  // RGBA
const png = Buffer.concat([
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
  chunk('IHDR', ihdr),
  chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
  chunk('IEND', Buffer.alloc(0)),
]);
fs.writeFileSync(process.argv[2] || 'icon.png', png);
console.log('icon.png écrit (' + png.length + ' octets)');
