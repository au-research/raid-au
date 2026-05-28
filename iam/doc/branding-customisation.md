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

### Copy the existing theme as a starting point

```sh
cp -r themes/raid-custom themes/my-new-theme
```

### Update the theme name

Edit `themes/my-new-theme/theme.properties` and update the parent if needed:

```properties
parent=keycloak
```

### Customise the theme

| Asset | Location |
|-------|----------|
| Styles | `login/resources/css/login.css` |
| HTML structure | `login/template.ftl` |
| Login form | `login/login.ftl` |
| Images / logo | `login/resources/img/` |
| Text overrides | `login/messages/messages_en.properties` |

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

Per-environment text (titles, badge label, policy links) can be set in the Keycloak admin console without touching the theme files — see [login-page-configuration.md](login-page-configuration.md).
