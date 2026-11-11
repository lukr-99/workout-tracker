const DAY_MS = 24 * 60 * 60 * 1000;

/** Days as `YYYY-MM-DD` in the owner's time zone, and the little calendar arithmetic the reader needs. */
export class LocalDay {
  static isTimeZone(timeZone: string): boolean {
    try {
      new Intl.DateTimeFormat("en-CA", { timeZone });
      return true;
    } catch {
      return false;
    }
  }

  /** The day an instant falls on in [timeZone]. */
  static of(instant: string | number, timeZone: string): string {
    const parts = new Intl.DateTimeFormat("en-CA", { timeZone, year: "numeric", month: "2-digit", day: "2-digit" }).formatToParts(new Date(instant));
    const part = (type: string) => parts.find((item) => item.type === type)?.value ?? "";
    return `${part("year")}-${part("month")}-${part("day")}`;
  }

  static addDays(day: string, days: number): string {
    return new Date(LocalDay.time(day) + days * DAY_MS).toISOString().slice(0, 10);
  }

  static daysBetween(from: string, to: string): number {
    return Math.round((LocalDay.time(to) - LocalDay.time(from)) / DAY_MS);
  }

  /** The Monday of the day's week (ISO weeks start on Monday). */
  static weekStart(day: string): string {
    const weekday = new Date(LocalDay.time(day)).getUTCDay();
    return LocalDay.addDays(day, -((weekday + 6) % 7));
  }

  static month(day: string): string {
    return day.slice(0, 7);
  }

  static isDay(value: string): boolean {
    // A round trip, because Date.parse rolls 2026-02-30 over to March instead of refusing it.
    return /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(LocalDay.time(value)) && new Date(LocalDay.time(value)).toISOString().startsWith(value);
  }

  private static time(day: string): number {
    return Date.parse(`${day}T00:00:00Z`);
  }
}
