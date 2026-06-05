# Keycloak Login Page Configuration

This guide explains how to configure the RAiD login page via the Keycloak admin console.

## Configuration Steps

### 1. Login to Keycloak and Select Realm

1. Log in to the Keycloak admin console
2. Select the **raid** realm from the realm dropdown

### 2. Configure Theme

1. Navigate to **Realm Settings → Themes** tab
2. Select theme: `raid-custom`
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

### 4. Configure Identity Providers

1. Navigate to **Identity Providers**
2. For each identity provider (Google, AAF, ORCID, etc.):
   - Click on the provider name to edit
   - Set **First Login Flow** to `first broker login`
   - Click **Save**

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
