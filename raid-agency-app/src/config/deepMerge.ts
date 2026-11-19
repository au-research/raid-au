type PlainObject = Record<string, unknown>;

function isPlainObject(value: unknown): value is PlainObject {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

/**
 * Merges `source` over `target` and returns a new object. Nested plain objects
 * merge field by field. Arrays and every other value in `source` replace the
 * value in `target`. Fields that are `undefined` in `source` are skipped.
 */
export function deepMerge<T extends object>(target: T, source: Partial<T>): T {
  const targetFields = target as PlainObject;
  const sourceFields = source as PlainObject;
  const result: PlainObject = { ...targetFields };

  for (const key of Object.keys(sourceFields)) {
    const sourceVal = sourceFields[key];
    const targetVal = targetFields[key];

    if (isPlainObject(sourceVal) && isPlainObject(targetVal)) {
      result[key] = deepMerge(targetVal, sourceVal);
    } else if (sourceVal !== undefined) {
      result[key] = sourceVal;
    }
  }

  return result as T;
}
