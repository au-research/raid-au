/**
 * TokenManager
 *
 * Manages OAuth 2.0 client_credentials tokens for build scripts.
 * Automatically re-fetches a new token when the current one is within
 * 60 seconds of expiry, so long-running scripts never send an expired token.
 */

export class TokenManager {
  constructor({ iamEndpoint, clientId, clientSecret, makeRequestWithRetry }) {
    this.iamEndpoint = iamEndpoint;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.makeRequestWithRetry = makeRequestWithRetry;
    this.accessToken = null;
    this.expiresAt = 0;
  }

  async getValidToken() {
    const bufferMs = 60_000;
    if (!this.accessToken || Date.now() >= this.expiresAt - bufferMs) {
      await this._fetchNewToken();
    }
    return this.accessToken;
  }

  async _fetchNewToken() {
    const tokenUrl = `${this.iamEndpoint}/realms/raid/protocol/openid-connect/token`;
    const bodyParams = `grant_type=client_credentials&client_id=${encodeURIComponent(this.clientId)}`;
    const body = this.clientSecret
      ? `${bodyParams}&client_secret=${encodeURIComponent(this.clientSecret)}`
      : bodyParams;

    const response = await this.makeRequestWithRetry(tokenUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(body),
      },
      body,
    });

    const tokenData = JSON.parse(response.data);
    if (!tokenData.access_token) {
      throw new Error('No access token in response');
    }

    this.accessToken = tokenData.access_token;
    const expiresInSeconds = tokenData.expires_in || 300;
    this.expiresAt = Date.now() + expiresInSeconds * 1000;
    console.log(`Token acquired for client '${this.clientId}' (expires in ${expiresInSeconds}s).`);
  }
}
