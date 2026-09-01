# Keycloak Login Page Configuration

This guide explains how to configure the RAiD login page via the Keycloak admin console.

## Configuration Steps

### 1. Login to Keycloak and Select Realm

1. Log in to the Keycloak admin console
2. Select the **raid** realm from the realm dropdown

### 2. Configure Theme

1. Navigate to **Realm Settings → Themes** tab
2. Select a login theme:
   - `raid-custom` — the generic default (blue accent), for Registration Agencies other than ARDC
   - `ardc-branding` — ARDC's own branding (purple accent, ARDC/NCRIS footer), a thin child theme of `raid-custom` that inherits its structure and overrides only colours and the header/footer
3. Click **Save**

### 3. Add Localization Overrides

1. Click on the **Localization** tab
2. Add the following key-value pairs under **Realm overrides**:

| Key | Value |
|-----|-------|
| `welcomeTitle` | `ARDC Research Activity Identifier (RAiD) Service` |
| `privacyPolicy` | `<a href="https://ardc.edu.au/privacy-policy/">ARDC privacy policy</a>` |
| `servicePolicy` | `<a href="https://documentation.ardc.edu.au/raid/raid-service-policy">RAiD Service Policy</a>` |
| `badge` | `dev` |
| `welcomeText` | `To learn more, access the <a href="https://documentation.ardc.edu.au/raid/">RAiD documentation</a> <br/> Maintained by the <a href="https://ardc.edu.au/">ARDC</a>` |
| `signinText` | `Please select your preferred sign-in method` |
| `signinTitle` | `RAiD Sign-in` |
| `federatedIdentityConfirmReauthenticateMessage` | `A user with the same details already exists. Authenticate using your original login button. This will link your account with your {0} login.` |
| `groupSelectorAccessMessage` | `To use RAiD you must belong to a 'Service Point'; please request access to the appropriate Service Point in the list below.` |
| `contact` | `services@ardc.edu.au` |
| `termsOfUse` | `https://ardc.edu.au/terms-and-conditions/` |
| `privacyPolicy` | `https://ardc.edu.au/privacy-policy/` |
| `accessibility` | `https://ardc.edu.au/accessibility-statement-for-ardc/` |

> The `badge` value should reflect the environment — e.g. `dev`, `test`, `stage`, `prod`.
> **Deprecated**: the sign-in page redesign (RAID-843) removed the badge from the rendered page in both themes. This key is no longer displayed anywhere; any existing override is now a harmless no-op and can be removed.

### 3a. Optional overrides — provider helper text and loading-state copy

Unlike the keys above, the following ship with sensible English defaults (in `raid-custom`'s `messages_en.properties`, inherited by `ardc-branding`), so the page works without any configuration here. Add a realm override only to change the shipped wording for a specific realm:

| Key | Shipped default |
|-----|-----------------|
| `idp.aaf.helperText` | `Australian universities and research organisations.` |
| `idp.orcid.helperText` | `Recommended for international researchers.` |
| `idp.google.helperText` | `For users without an institutional or ORCID account.` |
| `idp.aaf.loadingTitle` | `Redirecting to your institution…` |
| `idp.aaf.loadingText` | `You'll sign in through the Australian Access Federation, then return to RAiD.` |
| `idp.orcid.loadingTitle` | `Redirecting to ORCID…` |
| `idp.orcid.loadingText` | `You'll sign in through ORCID, then return to RAiD.` |
| `idp.google.loadingTitle` | `Redirecting to Google…` |
| `idp.google.loadingText` | `You'll sign in through Google, then return to RAiD.` |
| `loadingCancelLink` | `Taking too long? Cancel and choose another option` |
| `signinHelpToggle` | `Which sign-in option is right for me?` |
| `signinHelpText` | `If you have an account with an Australian university or research organisation, choose Institution. If you're an international researcher, choose ORCID. Otherwise, choose Google.` |

> Realm override values go through Keycloak's `MessageFormat` processing. A literal apostrophe in an override value must be written as `''` (doubled), otherwise it — and anything MessageFormat treats as quoted after it — will be silently dropped from the rendered text.

### 4. Configure Identity Providers

1. Navigate to **Identity Providers**
2. For each identity provider (Google, AAF, ORCID, etc.):
   - Click on the provider name to edit
   - Set **First Login Flow** to `first broker login`
   - Click **Save**

> ORCID needs attribute mappers as well, otherwise the ORCID iD is written into the
> user's **First Name** and used as their username. See
> [orcid-identity-provider.md](orcid-identity-provider.md).

> The bold label on each sign-in button (e.g. "Sign in with your Institution (AAF)") is that identity provider's own **Display name** field, set on its Identity Providers settings page — not a theme message key. The smaller helper text underneath it is the theme's `idp.*.helperText` key (see 3a above); Keycloak has no built-in "helper text" field on an identity provider.

### 5. Configure Authentication Flow

1. Navigate to the **Authentication** tab
2. Select the **First Broker Login** flow
3. Disable the following executions:
   - Review Profile
   - Confirm Link Existing Account
4. Click **Save**

## Notes

- HTML anchor tags in localization values are rendered as clickable links on the login page
- The `badge` localization key can be updated per environment without redeploying the theme
- These configurations allow agencies to customize the login page without code changes
