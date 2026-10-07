<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false displayMessage=!messagesPerField.existsError('username','password'); section>
    <#if section = "header">
        <!-- Header rendered in template.ftl -->

    <#elseif section = "form">

        <!-- ==================== SIGN-IN CARD ==================== -->
        <div class="signin-card">
            <img src="${url.resourcesPath}/img/RAiD-Strapline.svg" class="signin-card__logo" alt="RAiD - Research Activity Identifier">
            <h1 class="signin-card__title">${msg("signinTitle")}</h1>
            <p class="signin-card__text">${msg("signinText")}</p>

            <#if social.providers??>
                <div class="idp-buttons">
                    <#list social.providers as p>
                        <a href="${p.loginUrl}"
                           class="idp-button idp-${p.alias}"
                           id="social-${p.alias}"
                           data-idp-alias="${p.alias}"
                           data-loading-title="<#if p.alias == 'orcid'>${msg('idp.orcid.loadingTitle')}<#elseif p.alias == 'google'>${msg('idp.google.loadingTitle')}<#else>${msg('idp.aaf.loadingTitle')}</#if>"
                           data-loading-text="<#if p.alias == 'orcid'>${msg('idp.orcid.loadingText')}<#elseif p.alias == 'google'>${msg('idp.google.loadingText')}<#else>${msg('idp.aaf.loadingText')}</#if>">
                            <span class="idp-icon">
                                <#if p.alias == "google">
                                    <svg viewBox="0 0 18 18" focusable="false" aria-hidden="true">
                                        <path fill="#4285F4" d="M17.64 9.2045c0-.6381-.0573-1.2518-.1636-1.8409H9v3.4814h4.8436c-.2086 1.125-.8427 2.0782-1.7959 2.7164v2.2581h2.9087c1.7018-1.5668 2.6836-3.874 2.6836-6.615z"/>
                                        <path fill="#34A853" d="M9 18c2.43 0 4.4673-.8059 5.9564-2.1805l-2.9087-2.2581c-.8059.54-1.8368.8591-3.0477.8591-2.3441 0-4.3282-1.5832-5.0359-3.7104H.9573v2.3318C2.4382 15.9832 5.4818 18 9 18z"/>
                                        <path fill="#FBBC05" d="M3.9641 10.71c-.18-.54-.2827-1.1168-.2827-1.71s.1027-1.17.2827-1.71V4.9582H.9573C.3477 6.1732 0 7.5477 0 9s.3477 2.8268.9573 4.0418L3.9641 10.71z"/>
                                        <path fill="#EA4335" d="M9 3.5795c1.3214 0 2.5077.4541 3.4405 1.346l2.5813-2.5814C13.4632.8918 11.4259 0 9 0 5.4818 0 2.4382 2.0168.9573 4.9582L3.9641 7.29C4.6718 5.1627 6.6559 3.5795 9 3.5795z"/>
                                    </svg>
                                <#elseif p.alias == "aaf">
                                    <svg class="MuiSvgIcon-root MuiSvgIcon-fontSizeMedium css-vubbuv"
                                         focusable="false"
                                         aria-hidden="true"
                                         viewBox="0 0 512 512">
                                         <path fill="currentColor"
                                             d="M380.37 28.839l-27.24 100.215-64-48 17.405-34.46-83.863 8.079-13.541
                                             42.38-35.512-25.482-67.16 85.62-83.008 48.593 34.81 156.752 38.87 6.518
                                              112-64 74.38 52.082 21.62-28.094 32 72.012L424 415.452l64.549-126.398-6.014-64.703-65.404-79.297-36.762-116.215z"
                                         />
                                         <path fill="currentColor"
                                             d="M358 442 l-6 10 -12 14 -4 16 4 14 14 10 22 2 18-6 10-14 4-18 -8-16 -16-10 -26-2z"
                                         />
                                    </svg>
                                <#elseif p.alias == "aaf-saml">
                                    <svg class="MuiSvgIcon-root MuiSvgIcon-fontSizeMedium css-vubbuv"
                                         focusable="false"
                                         aria-hidden="true"
                                         viewBox="0 0 512 512">
                                         <path fill="currentColor"
                                             d="M380.37 28.839l-27.24 100.215-64-48 17.405-34.46-83.863 8.079-13.541
                                             42.38-35.512-25.482-67.16 85.62-83.008 48.593 34.81 156.752 38.87 6.518
                                              112-64 74.38 52.082 21.62-28.094 32 72.012L424 415.452l64.549-126.398-6.014-64.703-65.404-79.297-36.762-116.215z"
                                         />
                                         <path fill="currentColor"
                                             d="M358 442 l-6 10 -12 14 -4 16 4 14 14 10 22 2 18-6 10-14 4-18 -8-16 -16-10 -26-2z"
                                         />
                                    </svg>
                                <#elseif p.alias == "orcid">
                                    <svg focusable="false" aria-hidden="true" viewBox="0 0 256 256">
                                        <path fill="#A6CE39" d="M256,128c0,70.7-57.3,128-128,128C57.3,256,0,198.7,0,128C0,57.3,57.3,0,128,0C198.7,0,256,57.3,256,128z">
                                        </path>
                                        <g fill="#fff">
                                        <path class="icon" d="M86.3,186.2H70.9V79.1h15.4v107.1z">
                                        </path>
                                        <path class="icon"
                                            d="M108.9,79.1h41.6c39.6,0,57,28.3,57,53.6c0,27.5-21.5,53.6-56.8,53.6h-41.8V79.1zM124.3,172.4h24.5c34.9,0,42.9-26.5,42.9-39.7c0-21.5-13.7-39.7-43.7-39.7h-23.7V172.4z">
                                        </path>
                                        <path d="M88.7,56.8c0,5.5-4.5,10.1-10.1,10.1s-10.1-4.6-10.1-10.1c0-5.6,4.5-10.1,10.1-10.1S88.7,51.3,88.7,56.8z">
                                        </path>
                                        </g>
                                    </svg>
                                </#if>
                            </span>
                            <span class="idp-button__body">
                                <span class="idp-button__label">${p.displayName!}</span>
                                <span class="idp-button__helper">
                                    <#if p.alias == "orcid">${msg('idp.orcid.helperText')}<#elseif p.alias == "google">${msg('idp.google.helperText')}<#else>${msg('idp.aaf.helperText')}</#if>
                                </span>
                            </span>
                            <svg class="idp-button__chevron" aria-hidden="true" viewBox="0 0 12 12" width="12" height="12" fill="none">
                                <path d="M4.5 3L7.5 6L4.5 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                        </a>
                    </#list>
                </div>
            </#if>

            <button type="button" class="signin-help-toggle" aria-expanded="false" aria-controls="signin-help-panel">
                ${msg('signinHelpToggle')}
                <svg class="signin-help-toggle__chevron" aria-hidden="true" viewBox="0 0 12 12" width="12" height="12" fill="none">
                    <path d="M3 4.5L6 7.5L9 4.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </button>
            <div id="signin-help-panel" class="signin-help-panel" hidden>
                ${msg('signinHelpText')?no_esc}
            </div>

            <div class="signin-footnote">
                <p>By signing in you agree to the ${msg("servicePolicy")?no_esc} and ${msg("privacyPolicy")?no_esc}.</p>
            </div>
        </div>

        <!-- ==================== LOADING / REDIRECTING STATE ==================== -->
        <div id="kc-loading-card" class="loading-card" hidden aria-live="polite">
            <img src="${url.resourcesPath}/img/RAiD-Strapline.svg" class="loading-card__logo" alt="RAiD">
            <span class="loading-card__spinner" aria-hidden="true"></span>
            <h2 id="kc-loading-title" class="loading-card__title"></h2>
            <p id="kc-loading-text" class="loading-card__text"></p>
            <a href="#" id="kc-loading-cancel" class="loading-card__cancel">${msg('loadingCancelLink')}</a>
        </div>

        <script src="${url.resourcesPath}/js/login.js" defer></script>

    </#if>

</@layout.registrationLayout>
        <div class="raid-footer">
            <!-- Logos Row -->
            <div class="raid-footer-logos">
                <div class="footer-logo" id="footer-logo-raid">
                    <img src="${url.resourcesPath}/img/RAiD-Strapline.svg" class="logo-text" alt="RAiD logo"></img>
                </div>
                <div class="footer-logo" id="footer-logo-ardc">
                    <img src="https://object-store.rc.nectar.org.au/v1/AUTH_9dbba2bab9754d389ec1829fc61b06ae/web-images/ardc-logo.svg" class="logo-text" alt="ARDC logo"></img>

                </div>
                <div class="footer-logo" id="footer-logo-aus-gov">
                    <img src="${url.resourcesPath}/img/combined-logos.svg" class="logo-text" alt="NCRIS Logo"></img>
                </div>
            </div>

            <!-- Description -->
            <div class="raid-footer-description">
                <p>
                    RAiD global is run by an international consortium led by the Australian Research Data Commons
                    (ARDC). The ARDC is enabled by the Australian Government's National Collaborative Research
                    Infrastructure Strategy (NCRIS). A global network of partners deliver RAiD services within their regions,
                    including the Australian Research Data Commons for Australia.
                </p>
            </div>

            <!-- Footer Links -->
            <div class="raid-footer-links">
                <a href="mailto:${msg("contact")}" class="footer-link">${msg("contact")}</a>
                <a href="${msg("termsOfUse")}" class="footer-link">Terms of use</a>
                <a href="${msg("accessibility")}" class="footer-link">Accessibility</a>
                <a href="${msg("privacyPolicy")}" class="footer-link">Privacy policy</a>
            </div>

            <!-- Acknowledgement of Country -->
            <div class="raid-footer-acknowledgement">
                <p>
                    We acknowledge and celebrate the First Australians on whose traditional lands we live and work, and we pay our respects to Elders past, present and emerging
                </p>
            </div>
        </div>
