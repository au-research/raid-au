# API Tokens and Refresh Tokens

This guide demonstrates how to request an access token using the password grant type and how to exchange a refresh token for a new access token using curl. In the demo environment access tokens are valid for 24 hours but in the production environment access tokens are only valid for 10 minutes. See [Token and Session Lifetimes](token-lifetimes.md) for full details across all environments. In order to use this method you will need to request a client id/secret and username/password at contact@raid.org.au.

## Request an access token
To request an access token using the password grant type, use the following curl command:

```
curl --location --request POST 'https://iam.demo.raid.org.au/auth/realms/raid/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'client_id=<CLIENT_ID>' \
--data-urlencode 'client_secret=<CLIENT_SECRET>' \
--data-urlencode 'username=<USERNAME>' \
--data-urlencode 'password=<PASSWORD>'
```
Replace the placeholders with your actual Keycloak server URL, realm name, client ID, client secret, username, and password. This will return both an access token and a refresh token similar to...
```
{
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJuZkMtdmoybkE4aGVEN25oMkFDeWI5Nk5ZYUVkTlZuQWVHV2x3bUw0bFEwIn0.eyJleHAiOjE3MjE5NzI0NDIsImlhdCI6MTcyMTk2NTI0MiwianRpIjoiYjg2MDY1NDUtNmJhMC00ODQ0LWFlMmItODg3MzBlMDRmMTEyIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDAxL3JlYWxtcy9yYWlkIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImZmMzM5YTI2LTc1NGMtNGY5Zi1iM2NhLWM0YTdlYjkzYTJhMyIsInR5cCI6IkJlYXJlciIsImF6cCI6InJhaWQtYXBpIiwic2Vzc2lvbl9zdGF0ZSI6IjM2Nzg3NDhiLTcyZGQtNDAzMy04YmMzLWJiOTNkZWQ3MWFmYyIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZ3JvdXAtYWRtaW4iLCJvZmZsaW5lX2FjY2VzcyIsInNlcnZpY2UtcG9pbnQtdXNlciIsInVtYV9hdXRob3JpemF0aW9uIiwiZGVmYXVsdC1yb2xlcy1yYWlkIiwib3BlcmF0b3IiXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6IiIsInNpZCI6IjM2Nzg3NDhiLTcyZGQtNDAzMy04YmMzLWJiOTNkZWQ3MWFmYyIsInNlcnZpY2VfcG9pbnRfZ3JvdXBfaWQiOiIxNjliZDNmMy1kZDQyLTRhYzAtYjg5YS1mYjQ5NjQ4ZTVlZmYifQ.TBt7SuIPXP0MklrdT56OGFj_vtySUCkbD8uhnCyAu-2LBXhC6zvYQgXBagmBQIFdCn1mfrlYKHdfP0zUYp5YVLz4xEf6Tlahy8Q7IntkAN4rxxdZDBG2spkPBwpEvqO8iITZKwsJPxzZEKpGusWLSBnENXSgXQY4Cy3gFHyrcKKVkGelsflL8SFDIkRWdxEFZudraMYL6hQNSngAj1h4Gxodwm1505sQMsno94zg4Nip1bZITUI--RKj9DuuVGIsv4NZM7x2yJW27pEZObJnk-L1kY3fU4iXnNY7wuD2y8s1f9UYTB7OTFKDdPlYtHU8w-Qj5nm3BNAts77ORzS_Fg",
    "expires_in": 7199,
    "refresh_expires_in": 7199,
    "refresh_token": "eyJhbGciOiJIUzUxMiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIzMDg0MGVhNi1hYjdiLTRmN2UtOGFlNS02MTI5Yzk1YTlhYzEifQ.eyJleHAiOjE3MjE5NzI0NDIsImlhdCI6MTcyMTk2NTI0MiwianRpIjoiNGFiMDFkYWUtMDc4MC00MzFlLTk1MmQtODkwOWZiMDBlMTJiIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDAxL3JlYWxtcy9yYWlkIiwiYXVkIjoiaHR0cDovL2xvY2FsaG9zdDo4MDAxL3JlYWxtcy9yYWlkIiwic3ViIjoiZmYzMzlhMjYtNzU0Yy00ZjlmLWIzY2EtYzRhN2ViOTNhMmEzIiwidHlwIjoiUmVmcmVzaCIsImF6cCI6InJhaWQtYXBpIiwic2Vzc2lvbl9zdGF0ZSI6IjM2Nzg3NDhiLTcyZGQtNDAzMy04YmMzLWJiOTNkZWQ3MWFmYyIsInNjb3BlIjoiIiwic2lkIjoiMzY3ODc0OGItNzJkZC00MDMzLThiYzMtYmI5M2RlZDcxYWZjIn0.1yIh9d-0Bt_X2lsIPZTcBbIRqEbwOB6RRMKNWCsrnV2bHYg2pVxdilZHzgv2_hFGryQqp4bDWR-jXIA1PZt1gw",
    "token_type": "Bearer",
    "not-before-policy": 0,
    "session_state": "3678748b-72dd-4033-8bc3-bb93ded71afc",
    "scope": ""
}
```

## Exchange Refresh Token for a New Access Token
To exchange a refresh token for a new access token, use the following curl command:

```
curl --location --request POST 'https://iam.test.raid.org.au/auth/realms/raid/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=refresh_token' \
--data-urlencode 'client_id=<CLIENT_ID>' \
--data-urlencode 'client_secret=<CLIENT_SECRET>' \
--data-urlencode 'refresh_token=<REFRESH_TOKEN>'
```

Replace the placeholders with your actual Keycloak server URL, realm name, client ID, client secret, and refresh token.

## Further reading
* https://auth0.com/blog/refresh-tokens-what-are-they-and-when-to-use-them/