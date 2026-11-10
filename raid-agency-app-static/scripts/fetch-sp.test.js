import { describe, expect, it, vi } from 'vitest';
import { addServicePointNameToRaidData, fetchAllServicePoints } from './fetch-sp.js';

const config = {
  apiEndpoint: 'https://api.example.org',
  bearerToken: 'token-123',
  enableCaching: false,
};

const publicServicePoints = [
  { id: 20000001, name: 'Example Service Point', identifierOwner: 'https://ror.org/038sjwq14' },
  { id: 20000002, name: 'Second Service Point', identifierOwner: 'https://ror.org/02stey378' },
];

const respondWith = (body) => vi.fn().mockResolvedValue({ data: JSON.stringify(body) });

describe('fetchAllServicePoints', () => {
  it('requests the public service point read with the bearer token', async () => {
    const makeRequestWithRetry = respondWith(publicServicePoints);

    await fetchAllServicePoints({ makeRequestWithRetry, config });

    expect(makeRequestWithRetry).toHaveBeenCalledTimes(1);
    const [url, options] = makeRequestWithRetry.mock.calls[0];
    expect(url).toBe('https://api.example.org/service-point/public');
    expect(options.method).toBe('GET');
    expect(options.headers.Authorization).toBe('Bearer token-123');
  });

  it('maps each service point id to its name', async () => {
    const makeRequestWithRetry = respondWith(publicServicePoints);

    const map = await fetchAllServicePoints({ makeRequestWithRetry, config });

    expect(Array.from(map.entries())).toEqual([
      ['20000001', 'Example Service Point'],
      ['20000002', 'Second Service Point'],
    ]);
  });

  it('skips entries without an id or name', async () => {
    const makeRequestWithRetry = respondWith([{ id: 20000001 }, { name: 'No id' }, publicServicePoints[1]]);

    const map = await fetchAllServicePoints({ makeRequestWithRetry, config });

    expect(Array.from(map.keys())).toEqual(['20000002']);
  });
});

describe('addServicePointNameToRaidData', () => {
  it('adds the service point name to each RAiD from the public read', async () => {
    const makeRequestWithRetry = respondWith(publicServicePoints);
    const stats = { failedServicePointFetches: 0 };
    const raids = [
      { identifier: { owner: { servicePoint: 20000001 } } },
      { identifier: { owner: { servicePoint: 99999999 } } },
    ];

    const result = await addServicePointNameToRaidData(raids, makeRequestWithRetry, config, stats);

    expect(makeRequestWithRetry.mock.calls[0][0]).toBe('https://api.example.org/service-point/public');
    expect(result[0].identifier.owner.servicePointName).toBe('Example Service Point');
    expect(result[1].identifier.owner.servicePointName).toBeUndefined();
    expect(stats.totalServicePoints).toBe(2);
    expect(stats.failedServicePointFetches).toBe(0);
  });

  it('counts a failed fetch and leaves the RAiDs unchanged', async () => {
    const makeRequestWithRetry = vi.fn().mockRejectedValue(new Error('403 Forbidden'));
    const stats = { failedServicePointFetches: 0 };
    const raids = [{ identifier: { owner: { servicePoint: 20000001 } } }];

    const result = await addServicePointNameToRaidData(raids, makeRequestWithRetry, config, stats);

    expect(stats.failedServicePointFetches).toBe(1);
    expect(result[0].identifier.owner.servicePointName).toBeUndefined();
  });
});
