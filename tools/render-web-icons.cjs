#!/usr/bin/env node
// Renders the browser edition's icons from the Android launcher icon, so both stay the same.
//
// Source: app/src/main/res/drawable/ic_launcher_foreground.xml (adaptive-icon foreground, 108dp)
// on the fill of app/src/main/res/drawable/ic_launcher_background.xml.
// Output: web/src/wasmJsMain/resources/apple-touch-icon.png and icons/ (PNG + SVG).
//
// Needs Node.js and Playwright with Chromium (npm i -g playwright; npx playwright install chromium).
// Run from the repository root:  NODE_PATH="$(npm root -g)" node tools/render-web-icons.cjs
'use strict';

const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const root = path.resolve(__dirname, '..');
const res = path.join(root, 'app/src/main/res');
const out = path.join(root, 'web/src/wasmJsMain/resources');

function attribute(element, name) {
  const match = element.match(new RegExp(`android:${name}="([^"]*)"`));
  return match ? match[1] : null;
}

// Android colors are #RGB, #RRGGBB or #AARRGGBB; SVG wants the alpha separately.
function color(value) {
  if (!value) return { fill: 'none' };
  const hex = value.replace('#', '');
  if (hex.length === 8) {
    const alpha = parseInt(hex.slice(0, 2), 16) / 255;
    return alpha === 0 ? { fill: 'none' } : { fill: `#${hex.slice(2)}`, opacity: alpha };
  }
  return { fill: value };
}

function foregroundPaths() {
  const xml = fs.readFileSync(path.join(res, 'drawable/ic_launcher_foreground.xml'), 'utf8');
  const elements = xml.match(/<path[\s\S]*?\/>/g) || [];
  return elements.map((element) => {
    const fill = color(attribute(element, 'fillColor'));
    const stroke = color(attribute(element, 'strokeColor'));
    const parts = [`d="${attribute(element, 'pathData')}"`, `fill="${fill.fill}"`];
    if (fill.opacity !== undefined) parts.push(`fill-opacity="${fill.opacity}"`);
    if (stroke.fill !== 'none') {
      parts.push(`stroke="${stroke.fill}"`, `stroke-width="${attribute(element, 'strokeWidth') || 1}"`);
    }
    return `<path ${parts.join(' ')}/>`;
  }).join('');
}

function background() {
  const xml = fs.readFileSync(path.join(res, 'drawable/ic_launcher_background.xml'), 'utf8');
  const match = xml.match(/android:fillColor="([^"]+)"/);
  if (!match) throw new Error('fillColor not found in ic_launcher_background.xml');
  return match[1].trim();
}

// Android launchers show the middle 72 of the 108 units; "full" keeps all 108 for maskable icons,
// whose platform mask cuts into the edges. Rounded corners only where no platform mask applies.
function svg({ full, rounded }) {
  const box = full ? '0 0 108 108' : '18 18 72 72';
  const offset = full ? 0 : 18;
  const size = full ? 108 : 72;
  const radius = rounded ? 16 : 0;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${box}">` +
    `<rect x="${offset}" y="${offset}" width="${size}" height="${size}" rx="${radius}" fill="${background()}"/>` +
    `${foregroundPaths()}</svg>\n`;
}

const outputs = [
  { file: 'apple-touch-icon.png', px: 180, full: false, rounded: false }, // iOS rounds it itself
  { file: 'icons/icon-192.png', px: 192, full: false, rounded: true },
  { file: 'icons/icon-512.png', px: 512, full: false, rounded: true },
  { file: 'icons/icon-maskable-512.png', px: 512, full: true, rounded: false },
];

(async () => {
  fs.mkdirSync(path.join(out, 'icons'), { recursive: true });
  fs.writeFileSync(path.join(out, 'icons/favicon.svg'), svg({ full: false, rounded: true }));
  const browser = await chromium.launch();
  try {
    for (const { file, px, full, rounded } of outputs) {
      const page = await browser.newPage({ viewport: { width: px, height: px } });
      const markup = svg({ full, rounded }).replace('<svg ', `<svg width="${px}" height="${px}" `);
      await page.setContent(`<html><body style="margin:0;background:transparent">${markup}</body></html>`);
      await page.locator('svg').screenshot({ path: path.join(out, file), omitBackground: true });
      await page.close();
      console.log(`wrote ${file}`);
    }
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
