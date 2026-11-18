// TypeScript copies of the backend enums (backend/.../common/Enums.java).
// A "union type" like this means the value must be exactly one of these strings,
// so a typo such as "AVAILIBLE" is caught before the app even runs.

export type Role = 'CADDIE' | 'ADMIN';
export type Grade = 'SINGLE' | 'DOUBLE' | 'JUNIOR';
export type BagType = 'SINGLE' | 'DOUBLE';
export type QueueStatus = 'OFF' | 'AVAILABLE' | 'CALLED' | 'OUT' | 'BREAK';

// An "interface" describes the shape of an object, like a Java record/DTO.
export interface QueueEntry {
  position: number;
  caddieId: number;
  name: string;
  grade: Grade;
  isPriority: boolean;
  status: QueueStatus;
  waitingSince: string; // ISO date-time from the backend
}
