// TypeScript copies of the backend enums (backend/.../common/Enums.java).
// A "union type" like this means the value must be exactly one of these strings,
// so a typo such as "AVAILIBLE" is caught before the app even runs.

export type Role = 'CADDIE' | 'ADMIN';
export type Grade = 'SINGLE' | 'DOUBLE' | 'JUNIOR';
export type BagType = 'SINGLE' | 'DOUBLE';
// OFF = not checked in / finished for today. FOOD and BACK_LATER keep their place in line.
// CALLED and OUT are set by the admin when giving out bags.
export type QueueStatus = 'OFF' | 'AVAILABLE' | 'FOOD' | 'BACK_LATER' | 'CALLED' | 'OUT';
export type Placement = 'TOP_OF_GROUP' | 'BOTTOM_OF_GROUP';

// An "interface" describes the shape of an object, like a Java record/DTO.
// These mirror the records in backend/.../auth/AuthDtos.java and caddie/CaddieDtos.java.

export interface User {
  id: number;
  name: string;
  phone: string;
  role: Role;
  mustChangePassword: boolean;
  grade: Grade | null; // `| null`: admins have no grade
  priority: boolean | null;
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface Caddie {
  id: number;
  userId: number;
  name: string;
  phone: string;
  grade: Grade;
  priority: boolean;
  listPosition: number; // 0 once they've left
  passwordSet: boolean;
  active: boolean; // false = left the club
}

export interface CaddieEdit {
  name: string;
  phone: string;
  grade: Grade;
  priority: boolean;
}

/** A caddie master's account (Admins tab). */
export interface Admin {
  id: number;
  name: string;
  phone: string;
  passwordSet: boolean;
}

export interface AdminWithSetupCode {
  admin: Admin;
  setupCode: string;
}

export interface CaddieWithSetupCode {
  caddie: Caddie;
  setupCode: string;
}

export interface NewCaddie {
  name: string;
  phone: string;
  grade: Grade;
  priority: boolean;
  placement: Placement;
}

// Mirrors backend/.../queue/QueueDtos.java. Times are ISO strings in UTC, e.g.
// "2026-11-22T10:45:00Z"; `new Date(text)` turns them into the phone's local time.

/** The caddie's own day - they see their number, not who else is here. */
export interface MyDay {
  caddieId: number;
  checkedIn: boolean;
  status: QueueStatus;
  backAt: string | null;
  lineNumber: number | null; // "You're 5th in line"; null if not checked in or out on course
  bagsThisWeek: number;
  clubPosition: number;
  locationRequired: boolean;
  // While status is CALLED
  calledBagType: BagType | null;
  calledAt: string | null;
  callAccepted: boolean; // they've tapped "On my way"
}

/** Order and figures both come from the daily update at `updatesAt` ("16:00"). */
export interface ClubList {
  countsUpTo: string; // a date, e.g. "2026-11-25"
  updatesAt: string;
  totalBags: number; // whole club on countsUpTo
  days: string[]; // Monday..Friday of the week shown
  dayTotals: number[]; // whole club on each of `days` - the Total row
  weekTotal: number; // whole club this week, weekend included
  rows: ClubListRow[];
}

export interface ClubListRow {
  position: number;
  caddieId: number;
  name: string;
  grade: Grade;
  priority: boolean;
  bagsByDay: number[]; // one per entry in ClubList.days
  weekTotal: number; // includes the weekend - this is what orders the list
}

export interface BoardEntry {
  caddieId: number;
  name: string;
  grade: Grade;
  priority: boolean;
  status: QueueStatus;
  backAt: string | null;
  checkedInAt: string;
  bagsThisWeek: number;
  clubPosition: number;
  lineNumber: number | null;
  moved: boolean; // admin moved them by hand today (holds until their next bag)
  missedCalls: number;
  // While CALLED
  calledBagType: BagType | null;
  calledAt: string | null;
  callAccepted: boolean;
  // While OUT: the bag they're carrying, so it can be corrected
  currentLoopId: number | null;
  currentBagType: BagType | null;
  currentDoubleLoop: boolean; // marked as a double loop (counts twice)
  sentOutAt: string | null;
  printPosition: number; // printed list: everyone numbered, out on course where they'll be when back
  bagsByDay: number[]; // bags each day of Board.weekDays
}

export interface Board {
  date: string;
  totalBagsToday: number;
  entries: BoardEntry[]; // already in today's line order
  awayToday: string[]; // names with an approved "not coming in" for today
  openIssues: number; // issue reports waiting in the Inbox
  weekDays: string[]; // Monday-Friday of this week (plus the weekend once it's here), for the printout
}

// ---- Not coming in (backend/.../absence) ----
export type AbsenceStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface Absence {
  id: number;
  caddieId: number;
  caddieName: string;
  date: string; // "2026-11-27"
  reason: string | null;
  status: AbsenceStatus;
  submittedAt: string;
  decidedAt: string | null;
}

export interface MyAbsences {
  earliestDate: string; // the first date that's far enough ahead (24h notice)
  requests: Absence[];
}

export interface Inbox {
  pending: Absence[];
  recentlyDecided: Absence[];
}

// ---- Issue reports (backend/.../issue/IssueReports) ----
export type IssueType = 'SLOW_PLAY' | 'RANGER' | 'GOLFER_PROBLEM' | 'GOLFER_GOING_IN' | 'MISSING_ITEM';

export interface Issue {
  id: number;
  caddieName: string;
  type: IssueType;
  hole: number | null;
  message: string | null;
  createdAt: string;
  resolved: boolean;
  resolvedBy: string | null; // admin's name
  resolvedAt: string | null;
}

/** Issue history for a week (weekStart set) or all time (weekStart null). */
export interface IssueSummary {
  weekStart: string | null;
  total: number;
  // Partial<...> = some keys may be missing: types nobody reported aren't listed
  byType: Partial<Record<IssueType, number>>;
  byCaddie: { caddieId: number; name: string; reports: number }[]; // most reports first
  issues: Issue[]; // newest first (all time: newest 200)
}

// ---- Attendance (backend/.../availability/AttendanceReport) ----
export interface AttendanceRow {
  caddieId: number;
  name: string;
  grade: Grade;
  priority: boolean;
  checkedInAt: (string | null)[]; // one per day Mon-Sun; null = didn't come in
  daysWorked: number;
}

export interface AttendanceWeek {
  weekStart: string;
  days: string[]; // Monday..Sunday
  dayTotals: number[]; // caddies in each day
  caddiesIn: number; // different caddies in at least once
  rows: AttendanceRow[];
}

// ---- History (backend/.../loop/HistoryService) ----
export interface HistoryLoop {
  id: number;
  date: string;
  bagType: BagType;
  doubleLoop: boolean; // went round twice; counts twice
  sentOutAt: string | null;
  returnedAt: string | null;
}

export interface HistoryWeek {
  weekStart: string;
  bags: number; // a double counts as 2, a double loop twice
  loops: number;
  daysWorked: number;
  loopRows: HistoryLoop[];
}

export interface History {
  totalBags: number;
  totalDaysWorked: number;
  weeks: HistoryWeek[];
}
