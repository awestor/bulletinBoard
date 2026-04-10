window.TokenManager = (function() {

    function saveTokens(tokens) {
        console.log("Сохраняем токены в localStorage для API запросов");

        const accessToken = tokens.accessToken || tokens.access_token;
        const tokenType = tokens.type || tokens.token_type || 'Bearer';

        if (accessToken) {
            // Сохраняем только для API запросов (не для навигации)
            localStorage.setItem('accessToken', accessToken);
            localStorage.setItem('tokenType', tokenType);

            // Устанавливаем заголовок для fetch
            setAuthHeader(accessToken, tokenType);
            return true;
        }
        return false;
    }

    function loadTokensFromStorage() {
        const token = localStorage.getItem('accessToken');
        const tokenType = localStorage.getItem('tokenType') || 'Bearer';

        if (token) {
            console.log("Токен загружен из localStorage");
            setAuthHeader(token, tokenType);
            return true;
        }
        return false;
    }

    function setAuthHeader(token, tokenType = 'Bearer') {
        if (!token) return;

        if (!window.originalFetch) {
            window.originalFetch = window.fetch;
        }

        window.fetch = function(url, options = {}) {
            const skipAuth = url.includes('/api/auth/') ||
                            url.includes('/login') ||
                            url.includes('/register') ||
                            url.includes('/css/') ||
                            url.includes('/js/') ||
                            url.includes('/images/');

            if (!skipAuth && token) {
                options = options || {};
                options.headers = options.headers || {};
                options.headers['Authorization'] = `${tokenType} ${token}`;
            }

            return window.originalFetch(url, options);
        };
    }

    function getAccessToken() {
        return localStorage.getItem('accessToken');
    }

    function logout() {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('tokenType');

        // Удаляем HttpOnly cookie через API
        fetch('/api/auth/logout', {
            method: 'POST',
            credentials: 'include' // Важно для работы с cookie
        }).finally(() => {
            if (window.originalFetch) {
                window.fetch = window.originalFetch;
            }
            window.location.href = '/login?logout=true';
        });
    }

    return {
        saveTokens,
        loadTokensFromStorage,
        getAccessToken,
        logout,
        setAuthHeader
    };
})();