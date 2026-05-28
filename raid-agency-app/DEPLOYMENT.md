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
  "googleAnalytics": {
    "measurementId": "G-XXXXXXXXXX"
  },
  "services": {
    "orcid": "https://orcid.{env}.example.org",
    "staticProd": "https://static.prod.example.org",
    "staticBase": "https://static.{env}.example.org"
  },
  "branding": {}
}
```

**Required fields:** `keycloak`, `apiBaseUrl`, `environment`, `supportEmail`, `services.orcid`, `services.staticProd`, `services.staticBase`

**Optional fields:**
- `services.invite` — omit if the invite feature is disabled
- `googleAnalytics` — omit or leave empty if not used
- `branding` — omit entirely to use built-in defaults, or override individual fields

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
- `app-config.json` prepared with your environment's values (use `public/app-config.json` as a template)

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
