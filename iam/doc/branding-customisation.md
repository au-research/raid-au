# Branding & Customisation Guide

## 1. React App — Custom Branding via `app-config.json`

The React app reads branding at runtime from `app-config.json`. No rebuild is required — update the config file and reload the page.

Add a `branding` key to your `app-config.json` with only the fields you want to override. Anything omitted falls back to the defaults in `src/config/DefaultConfig.ts`.

```json
{
  "keycloak": { "..." : "..." },
  "apiBaseUrl": "https://api.example.org",
  "branding": {
    "header": {
      "title": "My Organisation",
      "logo": { "src": "/my-logo.svg", "alt": "My Logo" }
    },
    "footer": {
      "copyright": "© 2025 My Organisation"
    },
    "theme": {
      "palette": {
        "primary": { "main": "#2a2f8f" },
        "secondary": { "main": "#e65100" }
      }
    }
  }
}
```

### Configurable branding fields

See `src/config/Appconfig.ts` for the full type definitions. The main overridable sections are:

| Section | Key fields |
|---------|-----------|
| `header` | `title`, `subtitle`, `logo.src`, `logo.alt`, `logo.height`, `navLinks` |
| `footer` | `copyright`, `links`, `main.logos`, `main.text`, `showSocialLinks`, `socialLinks` |
| `content.landingPage` | `heroTitle`, `heroSubtitle`, `showHero` |
| `theme.palette` | `primary`, `secondary`, `background`, `error`, `warning`, `info`, `success`, `text` |
| `theme.typography` | `fontFamily`, `fontSize` |
| `theme.shape` | `borderRadius` |

### Deployment

- **AWS S3**: upload the updated `app-config.json` to the S3 bucket and invalidate the CloudFront cache — see [DEPLOYMENT.md](../../raid-agency-app/DEPLOYMENT.md)
- **Docker / Kubernetes**: replace the mounted config file or update the file at the external URL — see [DEPLOYMENT.md](../../raid-agency-app/DEPLOYMENT.md)

---

## 2. Keycloak — Creating a New Theme

`raid-custom` is the generic default theme (structure, accessibility, responsive layout) and is designed to be forked. `ardc-branding` is a working example of this: a thin child theme that inherits `raid-custom`'s structure and overrides only colours and its own header/footer — see its `theme.properties`, `resources/css/login-ardc.css`, and `login.ftl`.

### Option A — fork raid-custom as a thin child theme (recommended for a colour/branding-only variant)

Set your new theme's parent directly to `raid-custom`:

```properties
parent=raid-custom
import=common/keycloak
styles=css/login.css css/login-my-theme.css
```

Do **not** create your own `login.css` — omit it entirely so it falls back to `raid-custom`'s file (Keycloak resolves each file in `styles=` independently: child-then-parent). Add a second CSS file (loaded after the first) containing only your `:root` colour-token overrides (`--brand-primary`, `--focus-ring`, `--spinner-track`, etc. — see `raid-custom/resources/css/login.css` for the full token list) plus anything structurally unique to your theme (a different header/footer, say). If you don't need any structural additions beyond colours, `login.ftl`/`template.ftl` can be omitted too and both will fall back to `raid-custom`'s.

### Option B — copy the theme as a fully independent starting point

```sh
cp -r themes/raid-custom themes/my-new-theme
```

Edit `themes/my-new-theme/theme.properties` and set `parent=keycloak` instead of `raid-custom` if you want zero coupling to future `raid-custom` changes. This duplicates everything, so any later fix or accessibility improvement to `raid-custom` won't reach your theme automatically.

### A key difference between CSS/FTL and message bundles

- **CSS, `.ftl` templates, and other `resources/` files (images, JS) resolve as a whole file**: if your theme has its own copy, the parent's version isn't used at all — even for the parts you didn't change. There's no partial/fragment override.
- **Message bundles (`messages_en.properties`) merge per key** across the parent chain: your theme only needs to define the keys that genuinely differ from the parent's default; everything else falls through automatically.

This is why `ardc-branding`'s `messages_en.properties` is nearly empty (everything currently matches `raid-custom`'s defaults) while its `login.ftl` is a near-complete copy of `raid-custom`'s (plus its own footer markup) — CSS/FTL changes to `raid-custom` must be manually re-applied to any child theme that has its own copy of that same file.

### Customise the theme

| Asset | Location | Resolves as |
|-------|----------|-------------|
| Shared structure/styles | `login/resources/css/login.css` | whole file — omit to inherit |
| Your own colours/overrides | `login/resources/css/login-my-theme.css` (or similar, listed in `styles=`) | whole file — always your own |
| HTML structure | `login/template.ftl` | whole file — omit to inherit |
| Login form | `login/login.ftl` | whole file — omit to inherit, but any structural change to the parent's card must be manually mirrored if you keep your own copy |
| Images / logo | `login/resources/img/` | whole file per filename — omit a file to inherit that one specific asset |
| Text overrides | `login/messages/messages_en.properties` | merges per key — only list what differs |

### Register the theme in Keycloak

1. Ensure the theme folder is mounted/deployed to Keycloak's `themes/` directory
2. Log in to the Keycloak admin console
3. Navigate to **Realm Settings → Themes**
4. Select your new theme from the **Login Theme** dropdown
5. Click **Save**

### Docker Compose volume mount

If running Keycloak in Docker, mount the theme directory:

```yaml
volumes:
  - ./themes/my-new-theme:/opt/keycloak/themes/my-new-theme
```

Restart the Keycloak container to pick up the new theme.

### Localization overrides

Per-environment text (titles, policy links, provider helper text, loading-state copy) can be set in the Keycloak admin console without touching the theme files — see [login-page-configuration.md](login-page-configuration.md).
