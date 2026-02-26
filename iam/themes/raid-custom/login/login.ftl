<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false displayMessage=!messagesPerField.existsError('username','password'); section>
    <#if section = "header">
        <!-- Header card moved outside to template.ftl for navigation -->

    <#elseif section = "form">
        <!-- RAiD Info Card -->
        <div class="raid-info-card">
            <p class="raid-title pb-1">
                ${msg("welcomeTitle")?no_esc}
                <span class="demo-badge">${msg("badge")?no_esc}</span>
            </p>
            <p class="raid-description pb-1 pt-1">
                ${msg("welcomeText")?no_esc}
            </p>
            <div class="raid-links">
                ${msg("privacyPolicy")?no_esc}
                ${msg("servicePolicy")?no_esc}
            </div>
        </div>

        <!-- Login Card -->
        <div class="login-card">
            <p class="raid-title pb-0">${msg("signinTitle")}</h2>
            <p class="raid-description pb-1">${msg("signinText")}</p>
            <#if social.providers??>
                <div class="idp-buttons pt-0">
                    <#list social.providers as p>
                        <a href="${p.loginUrl}"
                           class="idp-button idp-${p.alias}"
                           id="social-${p.alias}">
                            <span class="idp-icon">
                                <#if p.alias == "google">
                                    <svg class=""
                                        focusable="false"
                                        aria-hidden="true"
                                        viewBox="0 0 24 24"
                                        data-testid="GoogleIcon">
                                        <path d="M12.545,10.239v3.821h5.445c-0.712,2.315-2.647,3.972-5.445,3.972c-3.332,
                                            0-6.033-2.701-6.033-6.032s2.701-6.032,6.033-6.032c1.498,0,2.866,0.549,3.921,
                                            1.453l2.814-2.814C17.503,2.988,15.139,2,12.545,2C7.021,2,2.543,6.477,2.543,
                                            12s4.478,10,10.002,10c8.396,0,10.249-7.85,9.426-11.748L12.545,10.239z">
                                        </path></svg>
                                <#elseif p.alias == "aaf">
                                    <svg class="MuiSvgIcon-root MuiSvgIcon-fontSizeMedium css-vubbuv"
                                         focusable="false"
                                         aria-hidden="true"
                                         viewBox="0 0 512 512">
                                         <path fill="currentColor"
                                             d="M380.37 28.839l-27.24 100.215-64-48 17.405-34.46-83.863 8.079-13.541
                                             42.38-35.512-25.482-67.16 85.62-83.008 48.593 34.81 156.752 38.87 6.518
                                              112-64 74.38 52.082 21.62-28.094 32 72.012L424 415.452l64.549-126.398-6.014-64.
                                              703-65.404-79.297-36.762-116.215zm-14.75 411.238l15.099 43.084 20.412-2.107
                                              11.435-35.864-46.947-5.113z"
                                         >
                                          </path>
                                    </svg>
                                <#elseif p.alias == "aaf-saml">
                                    <svg class="MuiSvgIcon-root MuiSvgIcon-fontSizeMedium css-vubbuv"
                                         focusable="false"
                                         aria-hidden="true"
                                         viewBox="0 0 512 512">
                                         <path fill="currentColor"
                                             d="M380.37 28.839l-27.24 100.215-64-48 17.405-34.46-83.863 8.079-13.541
                                             42.38-35.512-25.482-67.16 85.62-83.008 48.593 34.81 156.752 38.87 6.518
                                              112-64 74.38 52.082 21.62-28.094 32 72.012L424 415.452l64.549-126.398-6.014-64.
                                              703-65.404-79.297-36.762-116.215zm-14.75 411.238l15.099 43.084 20.412-2.107
                                              11.435-35.864-46.947-5.113z"
                                         >
                                          </path>
                                    </svg>
                                <#elseif p.alias == "orcid">
                                    <svg class="MuiSvgIcon-root MuiSvgIcon-fontSizeMedium css-vubbuv"
                                        focusable="false" aria-hidden="true" viewBox="0 0 256 256">
                                        <path d="M256,128c0,70.7-57.3,128-128,128C57.3,256,0,198.7,0,128C0,57.3,57.3,0,128,0C198.7,0,256,57.3,256,128z">
                                        </path>
                                        <g fill="#666">
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
                            <span class="idp-text">${p.displayName!}</span>
                        </a>
                    </#list>
                </div>
            </#if>
        </div>
    </#if>
</@layout.registrationLayout>