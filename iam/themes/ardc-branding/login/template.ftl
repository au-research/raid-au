<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>${msg("loginTitle")} - ${msg("org.name")}</title>

    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />

    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
</head>

<body class="${properties.kcBodyClass!}">

    <!-- ARDC Mega-menu (centrally managed by ARDC - see ARDC Web Components
         Developer Documentation). Real content/height only arrive once
         megamenu.min.js runs; onerror hides the element if that script
         can't load rather than leaving a dead placeholder on screen.
         The wrapping div gives the component's transparent bottom edge a
         solid white background to sit on (doc §3.5) - without it, this
         page's own tinted body background (--surface-tint) shows through
         as a faint grey/washed-out seam under the button. -->
    <div class="ardc-megamenu-wrap">
        <ardc-megamenu
            id="ardc-menu"
            data-host="RAiD App"
            data-content-width="full"
            style="display: block; opacity: 0; min-height: 20px;"
        ></ardc-megamenu>
    </div>
    <script
        src="https://ardc.edu.au/wp-content/plugins/ardc-custom-web-components/dist/loaders/megamenu.min.js"
        defer
        data-no-optimize
        onerror="document.getElementById('ardc-menu').style.display='none'"
    ></script>
    <script>
        // The trigger button itself (inside the component's shadow root)
        // is position:absolute; right:0, so it's flush against the true
        // viewport edge regardless of any padding set on its parents -
        // padding only affects normal-flow children, not absolutely-
        // positioned ones. Its own `right` is what needs overriding, and
        // that content renders asynchronously, so this polls briefly
        // rather than assuming it exists on page load.
        (function () {
            var attempts = 0;
            function applyInset() {
                var host = document.getElementById('ardc-menu');
                var btn = host && host.shadowRoot && host.shadowRoot.querySelector('.ardc-header__explore-btn');
                if (btn) {
                    // The component's own shadow stylesheet sets `right`
                    // with !important, so a plain inline-style assignment
                    // loses - setProperty with "important" priority wins.
                    btn.style.setProperty('right', '24px', 'important');
                    return true;
                }
                return false;
            }
            if (applyInset()) return;
            var interval = setInterval(function () {
                attempts += 1;
                if (applyInset() || attempts > 40) {
                    clearInterval(interval);
                }
            }, 250);
        })();
    </script>

    <!-- Top Navigation Bar.
         TODO: About/Documentation currently point at this dev
         environment's React app / raid.org.au as placeholders - the
         Keycloak theme has no config mechanism today for the React
         app's actual per-environment origin (dev/test/demo/prod), and
         no real Documentation destination exists yet anywhere in the
         repo. Both need real, environment-aware targets before this
         ships beyond local dev. -->
    <nav class="top-navbar">
        <div class="nav-container">
            <div class="nav-logo">
                <img src="${url.resourcesPath}/img/raid-logo-mark.svg" class="logo-text" alt="logo">
                <span class="nav-title">${msg("org.header.title")}</span>
            </div>
            <div class="nav-links">
                <#assign externalLinkIcon>
                    <svg class="nav-link-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                        <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M15 3h6v6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M10 14 21 3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                </#assign>
                <a href="http://localhost:7080/about-raid" target="_blank" rel="noopener noreferrer">About ${externalLinkIcon?no_esc}</a>
                <a href="https://www.raid.org.au" target="_blank" rel="noopener noreferrer">Documentation ${externalLinkIcon?no_esc}</a>
                <a href="https://www.raid.org.au" target="_blank" rel="noopener noreferrer">raid.org ${externalLinkIcon?no_esc}</a>
            </div>
        </div>
    </nav>

    <!-- Main Content -->
    <div class="page-container">
        <div class="content-wrapper pt-1">
            <#-- Display messages -->
            <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="alert alert-${message.type}">
                    <span class="alert-message">${kcSanitize(message.summary)?no_esc}</span>
                </div>
            </#if>

            <#nested "form">
        </div>
    </div>

    <!-- ARDC Footer (centrally managed by ARDC - see ARDC Web Components
         Developer Documentation). data-show-ardc-logo is set because RAiD
         carries its own RAiD-branded logo, not an ARDC-branded one, per
         the docs' ARDC Logo Visibility Rule. -->
    <ardc-footer
        class="ardc-footer ardc-custom-component"
        data-content-width="normal"
        data-show-ardc-logo
        style="opacity: 0;"
    >
        <ul id="quick-links">
            <li><a href="mailto:${msg("contact")}">${msg("contact")}</a></li>
        </ul>
        <ul id="legal-links">
            <li><a href="${msg("termsOfUse")}">Terms of use</a></li>
            <li><a href="${msg("accessibility")}">Accessibility</a></li>
            <li><a href="${msg("privacyPolicy")}">Privacy policy</a></li>
        </ul>
        <a id="ardc-footer-acn" href="https://www.acnc.gov.au/charity/charities/eca273f3-f5be-e911-a98a-000d3ad02a61/profile">ACN 633 798 857</a>
    </ardc-footer>
    <script
        src="https://ardc.edu.au/wp-content/plugins/ardc-custom-web-components/dist/loaders/footer.min.js"
        defer
        data-no-optimize
        onerror="document.querySelector('ardc-footer').style.display='none'"
    ></script>
    <script>
        // The footer's own inner wrapper (inside its shadow root) needs
        // extra top padding; that's inside the ARDC-hosted component's
        // shadow DOM, so it can't be reached with a normal stylesheet
        // selector. Its content renders asynchronously once footer.min.js
        // finishes its own init, so this polls briefly rather than
        // assuming it exists on page load. Selecting by tag name, not the
        // "ardc-footer" class - the component's own script strips the
        // light-DOM class attribute once it upgrades the element.
        (function () {
            var attempts = 0;
            function applyPadding() {
                var host = document.querySelector('ardc-footer');
                var inner = host && host.shadowRoot && host.shadowRoot.querySelector('.ardc-footer__footer');
                if (inner) {
                    inner.style.paddingTop = '30px';
                    return true;
                }
                return false;
            }
            if (applyPadding()) return;
            var interval = setInterval(function () {
                attempts += 1;
                if (applyPadding() || attempts > 40) {
                    clearInterval(interval);
                }
            }, 250);
        })();
    </script>
</body>
</html>
</#macro>
