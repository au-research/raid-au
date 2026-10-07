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

    <!-- ARDC Top Bar -->
    <div class="ardc-top-bar">
        <div class="ardc-top-bar-inner">
            <div class="ardc-explore-menu">
                <a href="https://ardc.edu.au" class="ardc-explore-link">
                    Explore ARDC
                    <svg class="ardc-explore-chevron" width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M3 4.5L6 7.5L9 4.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                </a>
            </div>
        </div>
    </div>

    <!-- Top Navigation Bar -->
    <nav class="top-navbar">
        <div class="nav-container">
            <div class="nav-logo">
                <img src="${url.resourcesPath}/img/RAiD-Strapline.svg" class="logo-text" alt="logo"></img>
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
</body>
</html>
</#macro>
