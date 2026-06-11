// RAID-461: Shared date helpers for e2e tests

/**
 * A valid embargo expiry date in YYYY-MM-DD format.
 *
 * The API rejects embargo expiry dates more than 18 months in the future
 * (AccessValidator: "Embargo expiry cannot be more than 18 months in the
 * future"), so hardcoded dates eventually start failing. Default to 12
 * months from today to stay comfortably inside the limit.
 */
export function validEmbargoExpiry(monthsFromNow = 12): string {
  const date = new Date();
  date.setMonth(date.getMonth() + monthsFromNow);
  return date.toISOString().slice(0, 10);
}
