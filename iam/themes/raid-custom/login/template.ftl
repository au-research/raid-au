<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>${msg("pageTitle")}</title>

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

    <!-- Top Navigation Bar -->
    <nav class="top-navbar">
        <div class="nav-container">
            <div class="nav-logo">
                <img src="${url.resourcesPath}/img/raid-logo-light.svg" class="logo-text" alt="${msg('logoAlt')}">
                <#assign welcomeTitle = msg("welcomeTitle")>
                <#if welcomeTitle?has_content>
                    <span class="nav-title">${welcomeTitle}</span>
                </#if>
            </div>
        </div>
    </nav>

    <!-- Main Content -->
    <div class="page-container">
        <div class="content-wrapper">
            <#-- Display messages -->
            <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="alert alert-${message.type}">
                    <span class="alert-message">${kcSanitize(message.summary)?no_esc}</span>
                </div>
            </#if>

            <#nested "form">

            <#-- Text under the card, set by the realm (HTML links allowed). -->
            <#assign welcomeText = msg("welcomeText")>
            <#if welcomeText?has_content>
                <p class="welcome-text">${welcomeText?no_esc}</p>
            </#if>
        </div>
    </div>
</body>
</html>
</#macro>
