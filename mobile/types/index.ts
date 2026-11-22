// TypeScript copies of the backend enums (backend/.../common/Enums.java).
// A "union type" like this means the value must be exactly one of these strings,
// so a typo such as "AVAILIBLE" is caught before the app even runs.

export type Role = 'CADDIE' | 'ADMIN';
export type Grade = 'SINGLE' | 'DOUBLE' | 'JUNIOR';
export type BagType = 'SINGLE' | 'DOUBLE';
export type QueueStatus = 'OFF' | 'AVAILABLE' | 'CALLED' | 'OUT' | 'BREAK';

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
}

export interface QueueEntry {
  position: number;
  caddieId: number;
  name: string;
  grade: Grade;
  isPriority: boolean;
  status: QueueStatus;
  waitingSince: string; // ISO date-time from the backend
}
