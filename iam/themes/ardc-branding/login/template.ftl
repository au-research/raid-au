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
        //
        // The component reveals itself (opacity 0 -> 1) as soon as it
        // initialises, which can happen before this poll has run -
        // visibly flashing the button flush-right for a moment before it
        // jumps to its corrected position. An inline opacity:0 override
        // can't reliably prevent that: the component appears to replace
        // its own inline `style` attribute wholesale at some point after
        // first mount, which silently wipes out anything set via
        // host.style, including an !important inline override. An
        // external stylesheet rule doesn't live on the element's style
        // attribute at all, so it survives that replacement - hide via a
        // stylesheet instead, and only remove the rule once a fixed
        // safety window has elapsed with the position fix continuously
        // re-applied throughout.
        (function () {
            var host = document.getElementById('ardc-menu');
            if (!host) return;

            var hideStyle = document.createElement('style');
            hideStyle.textContent = '#ardc-menu { opacity: 0 !important; }';
            document.head.appendChild(hideStyle);

            function applyFix() {
                var btn = host.shadowRoot && host.shadowRoot.querySelector('.ardc-header__explore-btn');
                if (!btn) return false;
                // The component's own shadow stylesheet sets `right`
                // with !important, so a plain inline-style assignment
                // loses - setProperty with "important" priority wins.
                btn.style.setProperty('right', '24px', 'important');
                return true;
            }

            var elapsedMs = 0;
            var tickMs = 50;
            var safetyWindowMs = 800;
            var interval = setInterval(function () {
                applyFix();
                elapsedMs += tickMs;
                if (elapsedMs >= safetyWindowMs) {
                    clearInterval(interval);
                    hideStyle.remove();
                }
            }, tickMs);
        })();
    </script>

    <!-- Top Navigation Bar. About/Documentation/raid.org are real,
         external destinations per ARDC's RAiD domain/service guide. -->
    <nav class="top-navbar">
        <div class="nav-container">
            <div class="nav-logo">
                <img src="${url.resourcesPath}/img/raid-logo-mark.svg" class="logo-text" alt="logo">
                <span class="nav-title">${msg("org.header.title")}</span>
            </div>
            <div class="nav-links" id="nav-links">
                <#assign externalLinkIcon>
                    <svg class="nav-link-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                        <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M15 3h6v6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M10 14 21 3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                </#assign>
                <a href="https://ardc.edu.au/services/ardc-identifier-services/raid-research-activity-identifier-service/" target="_blank" rel="noopener noreferrer">About ${externalLinkIcon?no_esc}</a>
                <a href="https://documentation.ardc.edu.au/raid" target="_blank" rel="noopener noreferrer">Documentation ${externalLinkIcon?no_esc}</a>
                <a href="https://raid.org" target="_blank" rel="noopener noreferrer">raid.org ${externalLinkIcon?no_esc}</a>
            </div>
            <!-- Mobile-only equivalent of React's MobileNavMenu: .nav-links
                 has nowhere to go below 899px (see login-ardc.css), so this
                 toggles it open as a dropdown instead of leaving it with no
                 way to reach these links at all. -->
            <button
                type="button"
                class="mobile-nav-toggle"
                id="mobile-nav-toggle"
                aria-label="Toggle navigation menu"
                aria-expanded="false"
                aria-controls="nav-links"
            >
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                    <path d="M4 6h16M4 12h16M4 18h16" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </button>
            <script>
                (function () {
                    var toggle = document.getElementById('mobile-nav-toggle');
                    var links = document.getElementById('nav-links');
                    if (!toggle || !links) return;
                    toggle.addEventListener('click', function () {
                        var isOpen = links.classList.toggle('nav-links--open');
                        toggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
                    });
                })();
            </script>
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
        <!-- Real, external destinations per ARDC's RAiD domain/service
             guide - same URLs as the React app's footer quick-links
             (src/constants/external-links.ts), kept in sync manually
             since this theme has no shared build step with that app. -->
        <ul id="quick-links">
            <li><a href="https://ardc.edu.au/services/ardc-identifier-services/raid-research-activity-identifier-service/" target="_blank" rel="noopener noreferrer">About RAiD</a></li>
            <li><a href="https://raid.org" target="_blank" rel="noopener noreferrer">Visit raid.org</a></li>
            <li><a href="https://documentation.ardc.edu.au/raid" target="_blank" rel="noopener noreferrer">RAiD User Guides</a></li>
            <li><a href="https://metadata.raid.org/" target="_blank" rel="noopener noreferrer">RAiD Schema Documentation</a></li>
            <li><a href="mailto:contact@raid.org">Request Support</a></li>
            <li><a href="mailto:services@ardc.edu.au">Contact the ARDC</a></li>
        </ul>
        <ul id="legal-links">
            <!-- Not msg()-driven: "termsOfUse"/"accessibility" aren't
                 defined anywhere in this theme (render as the literal key
                 name), and "privacyPolicy" is a realm-level localization
                 override that's a full HTML link snippet meant to be
                 dropped inline, not a bare URL - wrapping it in another
                 <a href> put raw HTML inside an href attribute. Same real
                 URLs as React's app-config.json footer.links. -->
            <li><a href="https://ardc.edu.au/terms-and-conditions/" target="_blank" rel="noopener noreferrer">Terms of use</a></li>
            <li><a href="https://ardc.edu.au/accessibility-statement-for-ardc/" target="_blank" rel="noopener noreferrer">Accessibility</a></li>
            <li><a href="https://ardc.edu.au/privacy-policy/" target="_blank" rel="noopener noreferrer">Privacy policy</a></li>
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
