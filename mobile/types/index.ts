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
  listPosition: number;
  passwordSet: boolean;
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
}

export interface Board {
  date: string;
  totalBagsToday: number;
  entries: BoardEntry[]; // already in today's line order
}
