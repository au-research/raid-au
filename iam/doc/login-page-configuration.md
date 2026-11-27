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

### 3. Localization overrides: all the login page text and links

Every piece of text and every link on the login page comes from a message key, so a realm can change it in the admin console with no change to the theme:

1. Click the **Localization** tab, then **Realm overrides**.
2. Add a key and a value. A realm override beats the default shipped in the theme. Anything left empty is not shown.

Both themes ship defaults in `login/messages/messages_en.properties`. `raid-custom` has neutral defaults that name no agency and show no links. `ardc-branding` supplies ARDC's values for the header, footer and policy links, so the ARDC theme works with no overrides at all. A test (`ThemeMessagesTest`) fails if a template uses a key that no bundle defines, or holds text or a link of its own.

Values go through Keycloak's `MessageFormat` processing. A literal apostrophe in a value must be written as `''` (doubled), otherwise it, and anything MessageFormat treats as quoted after it, is silently dropped from the rendered text. HTML links are allowed where the table says so.

**Page and sign-in card**

| Key | What it sets | `raid-custom` default |
|-----|--------------|-----------------------|
| `pageTitle` | The browser tab title | `RAiD Sign-in` |
| `signinTitle` | The heading on the sign-in card | `RAiD Sign-in` |
| `signinText` | The line under the heading | `Please select your preferred sign-in method` |
| `welcomeTitle` | The title beside the logo in the header. Not shown when empty. | empty |
| `welcomeText` | Small text under the card. HTML links allowed. Not shown when empty. | empty |
| `servicePolicy`, `privacyPolicy` | HTML links in the footnote "By signing in you agree to the … and …". The footnote is left out when both are empty. | empty |
| `signinFootnotePrefix`, `signinFootnoteJoin`, `signinFootnoteSuffix` | The words around those two links | `By signing in you agree to the`, `and`, `.` |

**Header and footer (`ardc-branding`)**

| Key | What it sets |
|-----|--------------|
| `headerLinkAboutText` and `headerLinkAboutUrl`, `headerLinkDocsText` and `headerLinkDocsUrl`, `headerLinkRaidText` and `headerLinkRaidUrl` | The three header links. A link with no text or no address is not shown. |
| `megaMenuHost` | The name the ARDC mega-menu shows for this app |
| `footerQuickLinks` | HTML list items (`<li><a href="…">…</a></li>`) for the footer's quick links |
| `supportEmail`, `footerSupportText` | The Request Support link: the helpdesk address and its label |
| `contact`, `footerContactText` | The contact link: the contact address and its label |
| `termsOfUse`, `accessibility`, `privacy_Policy` | The addresses of the legal links. Note the spelling `privacy_Policy`, with an underscore. |
| `footerTermsText`, `footerAccessibilityText`, `footerPrivacyText` | The labels of those legal links |
| `footerAcnText`, `footerAcnUrl` | The registration number and its link |

ARDC's values: `supportEmail` is `services@ardc.edu.au` (the helpdesk) and `contact` is `contact@ardc.edu.au`.

**Accessibility text**

| Key | What it sets | Default |
|-----|--------------|---------|
| `logoAlt` | Alt text of the header logo | `RAiD logo` |
| `strapLineAlt` | Alt text of the logo on the sign-in card | `RAiD - Research Activity Identifier` |
| `loadingLogoAlt` | Alt text of the logo on the redirecting screen | `RAiD` |
| `navToggleLabel` | Label of the mobile menu button | `Toggle navigation menu` |

**Sign-in providers, loading and help text**

| Key | Default |
|-----|---------|
| `idp.aaf.helperText` | `Australian universities and research organisations.` |
| `idp.orcid.helperText` | `Recommended for international users.` |
| `idp.google.helperText` | `For users without an institutional or ORCID account.` |
| `idp.aaf.loadingTitle` | `Redirecting to your institution…` |
| `idp.aaf.loadingText` | `You'll sign in through the Australian Access Federation, then return to RAiD.` |
| `idp.orcid.loadingTitle` | `Redirecting to ORCID…` |
| `idp.orcid.loadingText` | `You'll sign in through ORCID, then return to RAiD.` |
| `idp.google.loadingTitle` | `Redirecting to Google…` |
| `idp.google.loadingText` | `You'll sign in through Google, then return to RAiD.` |
| `loadingCancelLink` | `Taking too long? Cancel and choose another option` |
| `signinHelpToggle` | `Which sign-in option is right for me?` |
| `signinHelpText` | `If you have an account with an Australian university or research organisation, choose Australian Access Federation. If you're an international researcher, choose ORCID. Otherwise, choose Google.` |

**Read by other parts of the system, not by the theme**

| Key | Read by |
|-----|---------|
| `groupSelectorAccessMessage` | The web app's group selector, which fetches it from the realm's localization |
| `federatedIdentityConfirmReauthenticateMessage` | Keycloak's own sign-in flow |

**Retired:** `badge` is no longer displayed (the sign-in page redesign, RAID-843, removed it). The old `org.*` keys, `loginTitle`, `loginInstruction` and `help.tooltip` are gone, and any existing override of them is a harmless no-op that can be removed.

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
- These configurations allow agencies to customize the login page without code changes
