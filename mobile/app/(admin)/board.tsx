import { Link } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, useWindowDimensions, View } from 'react-native';

import { adminBackIn, callCaddie, changeBagType, markDoubleLoop, moveInLine, noReply, sendOut } from '@/api/board';
import { fetchBoard } from '@/api/queue';
import { canPrint, loadBoardCopy, printableHtml, saveBoardCopy } from '@/components/boardBackup';
import { Button } from '@/components/Button';
import { CaddieTag } from '@/components/CaddieTag';
import { ErrorText } from '@/components/ErrorText';
import { PrintPreview } from '@/components/PrintPreview';
import { clockTime, statusText } from '@/components/format';
import { Screen } from '@/components/Screen';
import { colors } from '@/components/theme';
import { useLiveData } from '@/hooks/useLiveData';
import type { BagType, Board, BoardEntry } from '@/types';

const PAGE = 15; // rows shown per table before "Show 15 more"

type TableKey = 'SINGLES' | 'DOUBLES' | 'OUT';

export default function AdminBoard() {
  // useWindowDimensions re-draws the screen when it's resized/rotated, so the layout can switch
  const { width } = useWindowDimensions();
  const narrow = width < 760;

  const { data: live, error, setData } = useLiveData(fetchBoard, 10000);
  // The copy saved in this browser last time (web only). useState(() => ...) reads it once,
  // when the screen first opens, rather than on every re-draw.
  const [saved] = useState(loadBoardCopy);
  // Offline straight after a page reload: show the saved copy until the server answers
  const board = live ?? (error ? saved?.board : null) ?? null;
  const showingSaved = !live && board !== null;
  // The printable page while the preview is open; null = closed
  const [previewHtml, setPreviewHtml] = useState<string | null>(null);

  // Save every fresh board, so there's always a recent copy to fall back on and print
  useEffect(() => {
    if (live) saveBoardCopy(live);
  }, [live]);
  const [tab, setTab] = useState<TableKey>('SINGLES');
  // Record<K, V> = an object with one V per key - here how many rows each table shows
  const [limits, setLimits] = useState<Record<TableKey, number>>({ SINGLES: PAGE, DOUBLES: PAGE, OUT: PAGE });
  const [openRow, setOpenRow] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  /** Runs one board action and shows the updated board it returns. */
  async function act(action: () => Promise<Board>) {
    setActionError(null);
    setBusy(true);
    try {
      setData(await action());
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Something went wrong');
    } finally {
      setBusy(false);
    }
  }

  const entries = board?.entries ?? [];
  // One queue, filtered (CLAUDE.md). Juniors can take both, so they're in both tables.
  const waiting = entries.filter((e) => e.status !== 'OUT');
  const tables: Record<TableKey, BoardEntry[]> = {
    SINGLES: waiting.filter((e) => e.grade !== 'DOUBLE'),
    DOUBLES: waiting.filter((e) => e.grade !== 'SINGLE'),
    OUT: entries.filter((e) => e.status === 'OUT'),
  };
  const titles: Record<TableKey, string> = { SINGLES: 'Singles', DOUBLES: 'Doubles', OUT: 'Out on course' };

  function table(key: TableKey) {
    return (
      <Table
        key={key}
        title={titles[key]}
        kind={key}
        entries={tables[key]}
        limit={limits[key]}
        onMore={() => setLimits({ ...limits, [key]: limits[key] + PAGE })}
        openRow={openRow}
        onToggle={(id) => setOpenRow(openRow === id ? null : id)}
        busy={busy}
        act={act}
      />
    );
  }

  return (
    <Screen>
      {previewHtml && <PrintPreview html={previewHtml} onClose={() => setPreviewHtml(null)} />}
      {showingSaved && saved && (
        <Text style={styles.offline}>
          Can't reach the server - showing the list saved at {clockTime(saved.savedAt)}. Print it if you need to work
          from paper.
        </Text>
      )}
      {board && board.openIssues > 0 && (
        // Link = a tappable route change (expo-router), here to the Inbox tab
        <Link href="/inbox" style={styles.issues}>
          {board.openIssues} open {board.openIssues === 1 ? 'issue' : 'issues'} from caddies - tap to see them in the
          Inbox
        </Link>
      )}
      <View style={styles.summary}>
        <Text style={styles.summaryText}>{board?.totalBagsToday ?? 0} bags today</Text>
        <Text style={styles.muted}>
          {waiting.length} waiting · {tables.OUT.length} out on course · tap a name for options
        </Text>
        {board && board.awayToday.length > 0 && (
          <Text style={styles.muted}>Off today (approved): {board.awayToday.join(', ')}</Text>
        )}
        {canPrint && board && (
          <View style={styles.printRow}>
            <Button
              label="Print preview"
              variant="secondary"
              onPress={() => {
                const asOf = showingSaved && saved ? saved.savedAt : new Date().toISOString();
                setPreviewHtml(printableHtml(board, asOf));
              }}
            />
          </View>
        )}
      </View>
      <ErrorText message={error ?? actionError} />

      {narrow ? (
        <>
          {/* Phone: one table at a time */}
          <View style={styles.segments}>
            {(['SINGLES', 'DOUBLES', 'OUT'] as TableKey[]).map((key) => (
              <Pressable
                key={key}
                onPress={() => setTab(key)}
                accessibilityRole="tab"
                accessibilityState={{ selected: tab === key }}
                style={[styles.segment, tab === key && styles.segmentOn]}>
                <Text style={[styles.segmentText, tab === key && styles.segmentTextOn]}>
                  {titles[key]} ({tables[key].length})
                </Text>
              </Pressable>
            ))}
          </View>
          {table(tab)}
        </>
      ) : (
        <>
          {/* Tablet / PC: singles and doubles side by side */}
          <View style={styles.sideBySide}>
            {table('SINGLES')}
            {table('DOUBLES')}
          </View>
          {tables.OUT.length > 0 && table('OUT')}
        </>
      )}
    </Screen>
  );
}

type TableProps = {
  title: string;
  kind: TableKey;
  entries: BoardEntry[];
  limit: number;
  onMore: () => void;
  openRow: number | null;
  onToggle: (caddieId: number) => void;
  busy: boolean;
  act: (action: () => Promise<Board>) => void;
};

function Table({ title, kind, entries, limit, onMore, openRow, onToggle, busy, act }: TableProps) {
  const shown = entries.slice(0, limit); // the next `limit` in line
  return (
    <View style={styles.table}>
      <Text style={styles.heading}>
        {title} ({entries.length})
      </Text>
      {entries.length === 0 && <Text style={styles.muted}>Nobody here.</Text>}
      {shown.map((e) => (
        <Row
          key={e.caddieId}
          entry={e}
          kind={kind}
          open={openRow === e.caddieId}
          onToggle={() => onToggle(e.caddieId)}
          busy={busy}
          act={act}
        />
      ))}
      {entries.length > limit && (
        <Button
          label={`Show ${Math.min(PAGE, entries.length - limit)} more (${entries.length - limit} left)`}
          variant="secondary"
          onPress={onMore}
        />
      )}
    </View>
  );
}

type RowProps = {
  entry: BoardEntry;
  kind: TableKey;
  open: boolean;
  onToggle: () => void;
  busy: boolean;
  act: (action: () => Promise<Board>) => void;
};

function Row({ entry, kind, open, onToggle, busy, act }: RowProps) {
  const away = entry.status === 'FOOD' || entry.status === 'BACK_LATER';
  const called = entry.status === 'CALLED';
  return (
    <View style={[styles.rowWrap, called && styles.rowCalled, open && styles.rowOpen]}>
      <Pressable onPress={onToggle} accessibilityRole="button" style={styles.row}>
        <Text style={styles.position}>{entry.lineNumber ?? '–'}</Text>
        <View style={{ flex: 1, gap: 2 }}>
          <View style={styles.nameLine}>
            <CaddieTag priority={entry.priority} grade={entry.grade} />
            <Text style={styles.name} numberOfLines={1}>
              {entry.name}
            </Text>
            {entry.moved && <Text style={styles.chip}>moved</Text>}
            {entry.missedCalls > 0 && <Text style={styles.chip}>missed {entry.missedCalls}</Text>}
          </View>
          <Text style={styles.muted}>{detailLine(entry)}</Text>
        </View>
        <Text style={[styles.status, away && styles.statusAway, called && styles.statusCalled]}>
          {called ? 'Called' : statusText(entry.status, entry.backAt)}
        </Text>
      </Pressable>
      {open && <RowActions entry={entry} kind={kind} busy={busy} act={act} />}
    </View>
  );
}

/** The small grey line under a name. */
function detailLine(e: BoardEntry) {
  if (e.status === 'CALLED') {
    const bag = e.calledBagType === 'DOUBLE' ? 'Double' : 'Single';
    return `${bag} · called ${clockTime(e.calledAt)} · ${e.callAccepted ? 'on the way' : 'waiting for reply'}`;
  }
  if (e.status === 'OUT') {
    const bag = (e.currentBagType === 'DOUBLE' ? 'Double' : 'Single') + (e.currentDoubleLoop ? ' · double loop' : '');
    return `${bag} · out since ${clockTime(e.sentOutAt)} · ${e.bagsThisWeek} this week`;
  }
  return `${e.bagsThisWeek} ${e.bagsThisWeek === 1 ? 'bag' : 'bags'} this week · in since ${clockTime(e.checkedInAt)}`;
}

function RowActions({ entry, kind, busy, act }: Omit<RowProps, 'open' | 'onToggle'>) {
  const [moveTo, setMoveTo] = useState('');
  const id = entry.caddieId;

  if (entry.status === 'CALLED') {
    return (
      <View style={styles.actions}>
        <Button label="Sent out" onPress={() => act(() => sendOut(id))} loading={busy} />
        <Button label="No reply" variant="secondary" onPress={() => act(() => noReply(id))} disabled={busy} />
      </View>
    );
  }

  if (entry.status === 'OUT') {
    const other: BagType = entry.currentBagType === 'DOUBLE' ? 'SINGLE' : 'DOUBLE';
    return (
      <View style={styles.actions}>
        <Button label="Back in" onPress={() => act(() => adminBackIn(id))} loading={busy} />
        {entry.currentLoopId != null && (
          <Button
            label={`Was a ${other === 'DOUBLE' ? 'double' : 'single'}`}
            variant="secondary"
            onPress={() => act(() => changeBagType(entry.currentLoopId!, other))}
            disabled={busy}
          />
        )}
        {entry.currentLoopId != null && (
          <Button
            label={entry.currentDoubleLoop ? 'Not a double loop' : 'Double loop'}
            variant="secondary"
            onPress={() => act(() => markDoubleLoop(entry.currentLoopId!, !entry.currentDoubleLoop))}
            disabled={busy}
          />
        )}
      </View>
    );
  }

  // Waiting: call (bag types this caddie can take - doubles can take a single, but rarely),
  // or move them in the line
  const bags: BagType[] =
    entry.grade === 'SINGLE' ? ['SINGLE'] : entry.grade === 'DOUBLE' ? ['DOUBLE', 'SINGLE'] : kind === 'DOUBLES' ? ['DOUBLE', 'SINGLE'] : ['SINGLE', 'DOUBLE'];
  return (
    <View style={styles.actionsColumn}>
      <View style={styles.actions}>
        {bags.map((bag, i) => (
          <Button
            key={bag}
            label={`Call - ${bag === 'DOUBLE' ? 'double' : 'single'}`}
            variant={i === 0 ? 'primary' : 'secondary'}
            onPress={() => act(() => callCaddie(id, bag))}
            disabled={busy}
          />
        ))}
      </View>
      <View style={styles.moveRow}>
        <Text style={styles.muted}>Move to number</Text>
        <TextInput
          value={moveTo}
          onChangeText={setMoveTo}
          keyboardType="number-pad"
          placeholder={String(entry.lineNumber ?? '')}
          style={styles.moveInput}
        />
        <Button
          label="Move"
          variant="secondary"
          disabled={busy || !Number(moveTo)}
          onPress={() => act(() => moveInLine(id, Number(moveTo)))}
        />
        {entry.moved && (
          <Button label="Undo move" variant="secondary" disabled={busy} onPress={() => act(() => moveInLine(id, null))} />
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  summary: { gap: 2 },
  summaryText: { fontSize: 20, fontWeight: '700', color: colors.text },
  printRow: { flexDirection: 'row', marginTop: 6 },
  offline: { backgroundColor: colors.highlight, color: colors.text, padding: 10, borderRadius: 8, fontWeight: '600' },
  issues: { backgroundColor: colors.danger, color: '#fff', padding: 10, borderRadius: 8, fontWeight: '700' },
  muted: { color: colors.muted, fontSize: 13 },
  segments: { flexDirection: 'row', backgroundColor: colors.card, borderRadius: 8, borderWidth: 1, borderColor: colors.border },
  segment: { flex: 1, minHeight: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 8 },
  segmentOn: { backgroundColor: colors.primary },
  segmentText: { fontSize: 13, fontWeight: '600', color: colors.text, textAlign: 'center' },
  segmentTextOn: { color: colors.primaryText },
  sideBySide: { flexDirection: 'row', gap: 12, alignItems: 'flex-start' },
  table: {
    flex: 1,
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 10,
    padding: 12,
    gap: 6,
  },
  heading: { fontSize: 18, fontWeight: '600', color: colors.text },
  rowWrap: { borderTopWidth: 1, borderTopColor: colors.border },
  rowCalled: { backgroundColor: colors.highlight },
  rowOpen: { backgroundColor: colors.stripe },
  row: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 8, paddingHorizontal: 4 },
  position: { width: 28, fontSize: 18, fontWeight: '700', color: colors.text, textAlign: 'right' },
  nameLine: { flexDirection: 'row', alignItems: 'center', gap: 6, flexWrap: 'wrap' },
  name: { fontSize: 16, fontWeight: '600', color: colors.text, flexShrink: 1 },
  chip: {
    fontSize: 11,
    color: colors.muted,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 4,
    paddingHorizontal: 4,
  },
  status: { fontSize: 13, fontWeight: '600', color: colors.primary },
  statusAway: { color: colors.danger },
  statusCalled: { color: '#b25a00' },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, paddingHorizontal: 4, paddingBottom: 10 },
  actionsColumn: { gap: 4 },
  moveRow: { flexDirection: 'row', alignItems: 'center', flexWrap: 'wrap', gap: 8, paddingHorizontal: 4, paddingBottom: 10 },
  moveInput: {
    width: 64,
    minHeight: 44,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    paddingHorizontal: 8,
    fontSize: 16,
    backgroundColor: colors.card,
    color: colors.text,
  },
});
