document.addEventListener('DOMContentLoaded', function() {

    function getQueryParam(param) {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(param);
    }

    function showMessage(text, type = 'error') {
        const msgDiv = document.getElementById('message');
        if (!msgDiv) return;
        msgDiv.textContent = text;
        msgDiv.className = `message ${type}`;
        msgDiv.style.display = 'block';
        setTimeout(() => {
            msgDiv.style.display = 'none';
        }, 5000);
    }

    function setButtonLoading(btnId, isLoading) {
        const btn = document.getElementById(btnId);
        if (!btn) return;
        if (isLoading) {
            btn.disabled = true;
            btn.textContent = 'Загрузка...';
        } else {
            btn.disabled = false;
            btn.textContent = btnId === 'googleLoginBtn' ? 'Войти через Google' : 'Войти';
        }
    }

    async function handleOAuthCallback() {
        const code = getQueryParam("code");
        const provider = getQueryParam("state");

        if (code) {
            console.log("Получен код авторизации от провайдера:", provider);

            try {
                setButtonLoading('googleLoginBtn', true);

                const response = await fetch("/api/auth/token", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: 'include', // Принимаем cookie
                    body: JSON.stringify({ code, provider })
                });

                if (!response.ok) {
                    throw new Error(`Ошибка обмена кода: ${response.status}`);
                }

                showMessage('Авторизация успешна!', 'success');

                setTimeout(() => {
                    window.location.href = '/';
                }, 1500);

            } catch (err) {
                console.error("Ошибка OAuth:", err);
                showMessage('Ошибка авторизации: ' + err.message);
            } finally {
                setButtonLoading('googleLoginBtn', false);
                window.history.replaceState({}, document.title, window.location.pathname);
            }
            return true;
        }
        return false;
    }

    async function handleLocalLogin(username, password) {
        if (!username || !username.trim()) {
            throw new Error("Введите email или логин");
        }

        if (!password || !password.trim()) {
            throw new Error("Введите пароль");
        }

        const loginRequest = {
            username: username.trim(),
            password: password.trim()
        };

        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: 'include', // Принимаем и отправляем cookie
            body: JSON.stringify(loginRequest)
        });

        if (!response.ok) {
            let errorMessage = "Ошибка входа";
            try {
                const error = await response.json();
                errorMessage = error.message || error.error || `Ошибка ${response.status}`;
            } catch(e) {
                errorMessage = `Ошибка сервера: ${response.status}`;
            }
            throw new Error(errorMessage);
        }

        showMessage('Вход выполнен успешно!', 'success');

        // Просто перенаправляем - cookie отправится автоматически
        setTimeout(() => {
            window.location.href = '/';
        }, 1000);
    }

    (async function() {
        const handled = await handleOAuthCallback();
        if (handled) return;

        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.get('error') === 'true') {
            showMessage('Неверный логин или пароль');
        } else if (urlParams.get('logout') === 'true') {
            showMessage('Вы вышли из системы', 'success');
        }

        const loginForm = document.getElementById('loginForm');
        if (loginForm) {
            loginForm.addEventListener('submit', async (e) => {
                e.preventDefault();

                const usernameInput = document.getElementById('username');
                const passwordInput = document.getElementById('password');

                const username = usernameInput ? usernameInput.value : '';
                const password = passwordInput ? passwordInput.value : '';

                setButtonLoading('loginBtn', true);
                try {
                    await handleLocalLogin(username, password);
                } catch (error) {
                    showMessage(error.message);
                } finally {
                    setButtonLoading('loginBtn', false);
                }
            });
        }

        const googleBtn = document.getElementById('googleLoginBtn');
        if (googleBtn) {
            googleBtn.addEventListener('click', () => {
                window.location.href = 'http://localhost:9090/realms/bulletin-board/protocol/openid-connect/auth?client_id=bulletin-board-client&response_type=code&redirect_uri=http://localhost:8080/login&state=google&kc_idp_hint=google';
            });
        }
    })();
});