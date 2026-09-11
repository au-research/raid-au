(function () {
    var signinCard = document.querySelector('.signin-card');
    var loadingCard = document.getElementById('kc-loading-card');
    var loadingTitle = document.getElementById('kc-loading-title');
    var loadingText = document.getElementById('kc-loading-text');
    var cancelLink = document.getElementById('kc-loading-cancel');

    if (signinCard && loadingCard) {
        document.querySelectorAll('.idp-button').forEach(function (button) {
            button.addEventListener('click', function () {
                loadingTitle.textContent = button.getAttribute('data-loading-title') || '';
                loadingText.textContent = button.getAttribute('data-loading-text') || '';
                signinCard.hidden = true;
                loadingCard.hidden = false;
            });
        });

        if (cancelLink) {
            cancelLink.addEventListener('click', function (event) {
                event.preventDefault();
                loadingCard.hidden = true;
                signinCard.hidden = false;
            });
        }
    }

    var helpToggle = document.querySelector('.signin-help-toggle');
    var helpPanel = document.getElementById('signin-help-panel');

    if (helpToggle && helpPanel) {
        helpToggle.addEventListener('click', function () {
            var expanded = helpToggle.getAttribute('aria-expanded') === 'true';
            helpToggle.setAttribute('aria-expanded', String(!expanded));
            helpPanel.hidden = expanded;
        });
    }
})();
