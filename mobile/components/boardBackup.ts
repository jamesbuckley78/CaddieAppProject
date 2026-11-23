import { Platform } from 'react-native';

import type { Board, BoardEntry } from '@/types';

import { clockTime, dayLabel } from './format';
import { tagColors } from './theme';

// Paper backup for the admin board (docs/PLAN.md, "Server or wifi down"). Web only: the
// caddie master runs the board in a PC/tablet browser, where localStorage and the browser's
// own print dialog are free. On phones these do nothing.

const KEY = 'caddie.boardCopy';

export type SavedBoard = { board: Board; savedAt: string };

export const canPrint = Platform.OS === 'web';

/** Keep the latest board in the browser, so it can still be shown and printed offline. */
export function saveBoardCopy(board: Board) {
  if (!canPrint) return;
  try {
    localStorage.setItem(KEY, JSON.stringify({ board, savedAt: new Date().toISOString() }));
  } catch {
    // storage full or blocked (private browsing) - the live board still works
  }
}

/** On sign-out: the shared PC in the caddie shack shouldn't keep today's list. */
export function clearBoardCopy() {
  if (!canPrint) return;
  try {
    localStorage.removeItem(KEY);
  } catch {
    // nothing stored - nothing to clear
  }
}

export function loadBoardCopy(): SavedBoard | null {
  if (!canPrint) return null;
  try {
    const value = localStorage.getItem(KEY);
    return value ? (JSON.parse(value) as SavedBoard) : null;
  } catch {
    return null;
  }
}

/**
 * The printable list as a complete HTML page, shown in the app's PrintPreview
 * (components/PrintPreview.tsx), which prints exactly this page. Printing only - the board
 * itself looks different.
 *
 * One table of everyone checked in, numbered in order. Caddies out on course are slotted in
 * where they're expected to be once they're back (printPosition). Each row has the coloured
 * P/S/D/J letter beside the name, the bags for each day of the week so far, and the week total.
 */
export function printableHtml(board: Board, savedAt: string) {
  // ?? fallbacks: a copy saved by an older version of the app doesn't have these fields
  const days = board.weekDays ?? [];
  const place = (e: BoardEntry) => e.printPosition ?? e.lineNumber ?? 0;
  const entries = [...board.entries].sort((a, b) => place(a) - place(b));
  const counted = (i: number) => days[i] <= board.date; // later days in the week stay blank
  const bags = (e: BoardEntry, i: number) => (e.bagsByDay ?? [])[i] ?? 0;

  const header = days.map((d) => `<th class="num">${DAY_LETTERS[new Date(d + 'T12:00').getDay()]}</th>`).join('');
  const rows = entries
    .map((e) => {
      const letter = e.priority ? 'P' : e.grade === 'DOUBLE' ? 'D' : e.grade === 'JUNIOR' ? 'J' : 'S';
      const dayCells = days.map((_, i) => `<td class="num">${counted(i) ? bags(e, i) : ''}</td>`).join('');
      const out = e.status === 'OUT' ? ' <span class="out">out on course</span>' : '';
      return `<tr><td class="num">${place(e)}</td>
<td><span class="tag" style="background:${tagColors[letter]}">${letter}</span>${escape(e.name)}${out}</td>
${dayCells}<td class="num"><b>${e.bagsThisWeek}</b></td></tr>`;
    })
    .join('');
  const totals = days
    .map((_, i) => `<td class="num">${counted(i) ? entries.reduce((sum, e) => sum + bags(e, i), 0) : ''}</td>`)
    .join('');
  const weekTotal = entries.reduce((sum, e) => sum + e.bagsThisWeek, 0);

  return `<!doctype html><html><head><meta charset="utf-8"><title>Caddie list ${escape(board.date)}</title>
<style>
  body { font-family: Arial, sans-serif; margin: 24px; background: #fff; }
  table { border-collapse: collapse; width: 100%; }
  th, td { border: 1px solid #999; padding: 4px 8px; text-align: left; font-size: 13px; }
  th { background: #eee; }
  .num { text-align: center; width: 32px; }
  .tag { display: inline-block; width: 16px; margin-right: 6px; border-radius: 3px; color: #fff; font-size: 11px;
         font-weight: bold; text-align: center; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  .out { color: #666; font-style: italic; font-size: 12px; }
  tr.total td { font-weight: bold; border-top: 2px solid #333; }
  .note { color: #444; font-size: 12px; }
</style></head><body>
<h1>Caddie list - ${escape(dayLabel(board.date))}</h1>
<p>As of ${escape(clockTime(savedAt))} · ${entries.length} checked in · ${board.totalBagsToday} bags today.
${board.awayToday.length ? `Off today: ${escape(board.awayToday.join(', '))}.` : ''}</p>
<table>
<tr><th class="num">#</th><th>Name</th>${header}<th class="num">Wk</th></tr>
${rows}
<tr class="total"><td></td><td>Total</td>${totals}<td class="num">${weekTotal}</td></tr>
</table>
<p class="note">P priority · S single · D double · J junior. Bags per day (a double counts as 2); Wk = this week
including the weekend. Caddies out on course are placed where they're expected to be once they're back.</p>
</body></html>`;
}

// Date.getDay(): 0 = Sunday ... 6 = Saturday
const DAY_LETTERS = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

/** Names go into HTML, so characters like < and & must not be read as markup. */
function escape(text: string) {
  return text.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);
}
