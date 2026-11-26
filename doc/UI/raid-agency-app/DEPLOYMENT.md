# RAiD Agency App — Deployment Guide

The app is configured at runtime via a single `app-config.json` file fetched on every page load. This means environment-specific values (API URLs, Keycloak config, branding) can be updated **without rebuilding or redeploying** the application.

---

## app-config.json Structure

```json
{
  "keycloak": {
    "url": "https://iam.{env}.raid.org.au",
    "realm": "raid",
    "clientId": "your-client-id"
  },
  "apiBaseUrl": "https://api.{env}.raid.org.au",
  "environment": "{env}",
  "supportEmail": "contact@example.org",
  "services": {
    "orcid": "https://orcid.{env}.example.org",
    "staticProd": "https://static.prod.example.org",
    "staticBase": "https://static.{env}.example.org"
  },
  "app": {
    "orcid": {
      "placeholder": "Enter ORCID iD or ISNI",
      "helpText": "Enter a valid ORCID iD e.g. 0000-0002-1825-0097"
    }
  }
}
```

This is the neutral layout. Copy [app-config.template.json](../../../raid-agency-app/app-config-examples/app-config.template.json) to start. For an agency with its own branding, add the `theme`, `header` and `footer` sections described in [Branding](#branding).

**Required to start the app:** `keycloak.url`, `keycloak.realm`, `keycloak.clientId`, `apiBaseUrl`, `environment`, `supportEmail`. `supportEmail` is the helpdesk address shown to people who need help, including in the banner that appears when branding cannot be loaded. If any is missing or invalid, the app shows an error banner instead of loading.

**Required for full functionality:** `services.orcid`, `services.staticProd`, `services.staticBase`, `app.orcid.placeholder`, `app.orcid.helpText`. If any is missing or invalid, the app still loads and shows a warning banner under the header.

URL fields must be absolute `http` or `https` URLs. See [Troubleshooting configuration banners](#troubleshooting-configuration-banners).

**Optional fields:**
- `services.invite` — omit if the invite feature is disabled
- `googleAnalytics` — omit it, or either id in it, if you do not use Google Analytics. Nothing is loaded and no banner is shown. See [Google Analytics](#google-analytics-optional).
- `contactEmail` — a second contact address, if the agency has one besides `supportEmail`
- `theme`, `header`, `footer` — see [Branding](#branding). Omit them for the neutral look.

---

## Google Analytics (optional)

Google Analytics is off unless you add this block:

```json
"googleAnalytics": {
  "measurementId": "G-XXXXXXXXXX",
  "measurementIdDemo": "G-YYYYYYYYYY"
}
```

- `measurementId` is used on production sites (the build is a production build or the host name contains `prod`).
- `measurementIdDemo` is used on hosts whose name contains `demo`.
- Every other host, including local development, never loads Google Analytics.
- Either id, or the whole block, can be left out. A missing id switches tracking off for that environment and raises no banner.
- An id must look like a Google tag id, for example `G-ABC123XYZ`. Anything else is ignored.

---

## Branding

The app shows a neutral RAiD look by default. It applies an agency's branding only when the Keycloak realm uses a custom login theme.

### How the app decides

On every page load the app asks Keycloak which login theme the realm uses for this client:

`GET {keycloak.url}/realms/{keycloak.realm}/theme?client_id={keycloak.clientId}`

The answer is `{"loginTheme": "<name>"}` or `{"loginTheme": null}`. The app shows a spinner until it knows. There is no timeout and no cache.

| Answer | What the app shows |
|--------|--------------------|
| `raid-custom` or `null` | The neutral look, with a simple footer showing the support address (and the contact address if set). The `theme`, `header` and `footer` sections of `app-config.json` are ignored. |
| Any other theme, for example `ardc-branding` | The sections in `app-config.json`, merged over the neutral look. A field you leave out keeps its neutral value. |
| The lookup fails (network error, any non-2xx answer, a body that is not JSON, or no `loginTheme` field) | The neutral look, plus a warning banner: "We could not load this site's branding, so the default RAiD look is shown." The banner has a **Reload** button and the `supportEmail` address. |

The theme endpoint is part of the Keycloak provider in `iam/`. Deploy it to the realm before you release a web app that depends on it. Until then, every visitor sees the failed lookup banner. The browser also logs a CORS error, because Keycloak's own 404 page has no CORS headers. The client's **Web origins** must include the web app's origin.

### The branding sections

These sit at the top level of `app-config.json`, after the services and `app` sections. See [app-config.ardc.example.json](../../../raid-agency-app/app-config-examples/app-config.ardc.example.json) for a full example.

| Field | Values | Notes |
|-------|--------|-------|
| `theme` | MUI palette, typography, shape and mode | `palette.primary`, `secondary`, `background`, `error`, `warning`, `info`, `success`, `text`; `typography.fontFamily`, `fontSize`; `shape.borderRadius`; `mode` |
| `header.title` | text | Shown beside the logo |
| `header.logo` | `light`, `dark`, `alt`, `height` | `light` shows in light mode, `dark` in dark mode |
| `header.navLinks` | list of `{ "label", "path" }` | Shown after Home and Your RAiDs. There are no default links. |
| `header.menu` | `none` or `ardc` | `ardc` mounts the ARDC mega menu and loads its script. `none` loads no script. |
| `footer.variant` | `generic` or `ardc` | `ardc` uses the ARDC footer component and loads its script. `generic` is a simple footer that shows the links below, the `supportEmail` address and, if set, the `contactEmail` address. It loads no script. |
| `footer.links` | list of `{ "label", "path" }` | Optional. A link flagged `"contact": true` is not listed in the footer. |

Every link needs a `label`, and its `path` must be an `https` or `mailto` address. A link that fails this is dropped and named in the warning banner. Web links open in a new tab.

### Where the branding lives

Branding belongs only in the external `app-config.json` for each environment (S3, a volume mount or `APP_CONFIG_URL`). Do not add it to `public/app-config.json`, which is only the local development default. It holds no ARDC addresses or analytics IDs, and a test keeps it that way. The CI end-to-end tests also read it, so keep `keycloak` and `apiBaseUrl` pointing at the local stack.

### Moving from the old layout

Earlier releases read branding from a `branding` wrapper (`branding.header`, `branding.footer`, `branding.content`, `branding.theme`). This release ignores it and shows an amber banner: "Some configuration is no longer used: branding." Do this once for each environment:

1. Move `theme`, `header` and `footer` from inside `branding` to the top level.
2. Rewrite `header.logo.src` as `header.logo.light` and `header.logo.dark`.
3. Rewrite the footer: use `footer.variant` and `footer.links`. The old `footer.main`, `copyright`, `showSocialLinks` and `socialLinks` are no longer used.
4. Delete the `branding` key, including `branding.content` and `branding.default`.
5. Add `supportEmail` if it is missing. The app does not start without it.
6. Check that the realm's login theme is the one you expect, then deploy the file and reload the page.

---

## AWS S3 + CloudFront

This is the current ARDC deployment method. The app is served as a static site from S3 via CloudFront.

### Prerequisites

- AWS CLI installed and configured
- Access to the target environment's S3 bucket and CloudFront distribution
- The correct `app-config.{env}.json` file

### Environment → Resource Mapping

| Environment | S3 Bucket | URL |
|-------------|-----------|-----|
| test | `test-raid-ui-deployment` | `https://app.test.raid.org.au` |
| demo | `demo-raid-ui-deployment` | `https://app.demo.raid.org.au` |
| stage | `stage-raid-ui-deployment` | `https://app.stage.raid.org.au` |
| prod | `prod-raid-ui-deployment` | `https://app.prod.raid.org.au` |

### Initial Deployment

**1. Build the app**
```sh
npm ci
npm run build
```

**2. Upload build artifacts to S3**
```sh
aws s3 sync dist/ s3://{env}-raid-ui-deployment/ --delete --exclude "app-config.json"
```

> `app-config.json` is excluded from `dist/` by the build and excluded from the sync — the S3 config is never overwritten by a deployment.

**3. Upload app-config.json (first time only)**
```sh
aws s3 cp config/app-config.{env}.json s3://{env}-raid-ui-deployment/app-config.json
```

**4. Invalidate CloudFront cache**
```sh
aws cloudfront create-invalidation \
  --distribution-id {DISTRIBUTION_ID} \
  --paths "/*"
```

**5. Verify**
```sh
curl https://app.{env}.raid.org.au/app-config.json
```

### Updating Config Only (no redeploy)

**1. Edit the config file**

Download, edit, and re-upload via AWS Console, or use CloudShell:
```sh
# In AWS CloudShell
aws s3 cp s3://{env}-raid-ui-deployment/app-config.json app-config.json
nano app-config.json
aws s3 cp app-config.json s3://{env}-raid-ui-deployment/app-config.json
```

**2. Invalidate CloudFront cache**
```sh
aws cloudfront create-invalidation \
  --distribution-id {DISTRIBUTION_ID} \
  --paths "/app-config.json"
```

Changes take effect on the next page load.

---

## Docker

For registration agencies deploying the app in a Docker container.

### Prerequisites

- Docker installed
- `app-config.json` prepared with your environment's values (copy `app-config-examples/app-config.template.json`)

### Build the Image

```sh
docker build -t raid-agency-app:latest .
```

### Option A — Volume Mount (Recommended)

Place your `app-config.json` on the host machine and mount it into the container:

```sh
docker run -d \
  -p 80:80 \
  -v /path/to/your/app-config.json:/usr/share/nginx/html/app-config.json:ro \
  --name raid-agency-app \
  raid-agency-app:latest
```

Or using Docker Compose — uncomment Option A in `docker-compose.yaml`:

```yaml
services:
  raid-agency-app:
    image: raid-agency-app:latest
    ports:
      - "80:80"
    volumes:
      - ./app-config.json:/usr/share/nginx/html/app-config.json:ro
```

```sh
docker compose up -d
```

### Option B — External Config URL

If your config is hosted at an external URL (your own S3, CDN, or any HTTPS endpoint), pass it as an environment variable. Nginx will proxy `/app-config.json` to that URL:

```sh
docker run -d \
  -p 80:80 \
  -e APP_CONFIG_URL=https://your-storage.example.com/app-config.json \
  --name raid-agency-app \
  raid-agency-app:latest
```

Or using Docker Compose — uncomment Option B in `docker-compose.yaml`:

```yaml
services:
  raid-agency-app:
    image: raid-agency-app:latest
    ports:
      - "80:80"
    environment:
      APP_CONFIG_URL: https://your-storage.example.com/app-config.json
```

```sh
docker compose up -d
```

### Verify

```sh
curl http://localhost/app-config.json
```

### Updating Config Only (no redeploy)

**Option A:** Replace the file at the mounted host path — takes effect on next page load, no restart needed.

**Option B:** Update the file at the external URL — takes effect on next page load, no restart needed.

---

## Kubernetes

For registration agencies deploying the app in a Kubernetes cluster.

### Prerequisites

- `kubectl` configured for the target cluster
- `app-config.json` prepared with your environment's values
- Docker image pushed to a registry accessible by the cluster

### Option A — ConfigMap (Recommended)

**1. Create the namespace**
```sh
kubectl create namespace raid
```

**2. Create a ConfigMap from your config file**
```sh
kubectl create configmap raid-app-config \
  --from-file=app-config.json=/path/to/your/app-config.json \
  --namespace raid
```

**3. Create a deployment manifest (`deployment.yaml`)**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: raid-agency-app
  namespace: raid
spec:
  replicas: 1
  selector:
    matchLabels:
      app: raid-agency-app
  template:
    metadata:
      labels:
        app: raid-agency-app
    spec:
      containers:
        - name: raid-agency-app
          image: your-registry/raid-agency-app:latest
          ports:
            - containerPort: 80
          volumeMounts:
            - name: app-config
              mountPath: /usr/share/nginx/html/app-config.json
              subPath: app-config.json
      volumes:
        - name: app-config
          configMap:
            name: raid-app-config
---
apiVersion: v1
kind: Service
metadata:
  name: raid-agency-app
  namespace: raid
spec:
  selector:
    app: raid-agency-app
  ports:
    - port: 80
      targetPort: 80
  type: LoadBalancer
```

**4. Apply the manifest**
```sh
kubectl apply -f deployment.yaml
```

**5. Verify**
```sh
kubectl get pods -n raid
curl http://{EXTERNAL_IP}/app-config.json
```

### Option B — External Config URL

If your config is hosted at an external HTTPS URL, set `APP_CONFIG_URL` in the deployment:

```yaml
containers:
  - name: raid-agency-app
    image: your-registry/raid-agency-app:latest
    env:
      - name: APP_CONFIG_URL
        value: https://your-storage.example.com/app-config.json
```

### Updating Config Only (no pod restart required for Option B)

**Option A — Update the ConfigMap**
```sh
kubectl create configmap raid-app-config \
  --from-file=app-config.json=/path/to/your/updated/app-config.json \
  --namespace raid \
  --dry-run=client -o yaml | kubectl apply -f -
```

Kubernetes automatically propagates ConfigMap updates to mounted volumes within ~1 minute — no pod restart required. If you need the change to take effect immediately:
```sh
kubectl rollout restart deployment/raid-agency-app --namespace raid
kubectl rollout status deployment/raid-agency-app --namespace raid
```

**Option B — Update file at external URL**

Update the file at the external URL — takes effect on next page load, no pod restart needed.

---

## Quick Reference

| Deployment | Config location | Update without redeploy? |
|------------|-----------------|--------------------------|
| AWS S3 + CloudFront | S3 bucket root | Yes — upload + CloudFront invalidation |
| Docker volume mount | Host file path | Yes — replace file |
| Docker external URL | Any HTTPS URL | Yes — update file at URL |
| Kubernetes ConfigMap | ConfigMap + pod volume | Yes — update ConfigMap (auto-propagates ~1 min) |
| Kubernetes external URL | Any HTTPS URL | Yes — update file at URL |

---

## Troubleshooting configuration banners

The app checks `app-config.json` every time it loads. Banners name the affected fields only, never their values. The browser console logs the full detail.

**Red banner: "This site is not configured correctly"**

The app cannot start. Only a minimal header and the banner are shown, on every page.

- *"its configuration file could not be loaded"*: `/app-config.json` is missing, unreachable or not valid JSON. Behind CloudFront, a missing file returns the app's HTML page, which produces this message. Check with `curl https://app.<env>.<domain>/app-config.json`; it must return JSON.
- *"Missing: …" or "Invalid: …"*: a field required to start the app is absent, empty, or not a valid URL.

**Amber banner: "Some configuration is missing" (or "invalid")**

The app runs, but the listed features may not work. The banner appears under the header on every page. When signed out, the login page pauses with a **Continue to log in** button so the banner can be read.

**Amber banner: "Some configuration is no longer used"**

`app-config.json` still has the old `branding` key. Follow [Moving from the old layout](#moving-from-the-old-layout).

**Amber banner: "We could not load this site's branding"**

The app could not look up the realm's login theme, so it shows the neutral look. The configuration file is fine. Reload the page. If it continues, check that `{keycloak.url}/realms/{keycloak.realm}/theme?client_id={keycloak.clientId}` returns JSON from the browser, that Keycloak has the theme endpoint deployed, and that the client's **Web origins** include the web app's origin. On the login page the app pauses with a **Continue to log in** button so the banner can be read.

**To fix a banner about `app-config.json`:** correct `app-config.json` for the environment using the steps above for your deployment type, then reload the page. No rebuild is needed.
