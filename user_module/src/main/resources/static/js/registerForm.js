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
            btn.textContent = btnId === 'googleRegisterBtn' ? 'Зарегистрироваться через Google' : 'Зарегистрироваться';
        }
    }

    async function handleOAuthCallback() {
        const code = getQueryParam("code");
        const provider = getQueryParam("state");

        if (code) {
            console.log("Получен код для регистрации от:", provider);

            try {
                setButtonLoading('googleRegisterBtn', true);

                const response = await fetch("/api/auth/token", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ code, provider, isRegistration: true })
                });

                if (!response.ok) {
                    throw new Error(`Ошибка регистрации: ${response.status}`);
                }

                const tokens = await response.json();
                window.TokenManager.saveTokens(tokens);
                showMessage('Регистрация через Google успешна!', 'success');

                setTimeout(() => {
                    window.location.href = '/cabinet';
                }, 1500);

            } catch (err) {
                console.error("Ошибка OAuth регистрации:", err);
                showMessage('Ошибка регистрации: ' + err.message);
            } finally {
                setButtonLoading('googleRegisterBtn', false);
                window.history.replaceState({}, document.title, window.location.pathname);
            }
            return true;
        }
        return false;
    }

    async function handleLocalRegister(userData) {
        const response = await fetch("/api/auth/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(userData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || "Ошибка регистрации");
        }

        const tokens = await response.json();
        window.TokenManager.saveTokens(tokens);
    }

    (async function() {
        const handled = await handleOAuthCallback();
        if (handled) return;

        const registerForm = document.getElementById('registerForm');
        if (registerForm) {
            registerForm.addEventListener('submit', async (e) => {
                e.preventDefault();

                const name = document.getElementById('name').value;
                const email = document.getElementById('email').value;
                const password = document.getElementById('password').value;
                const confirmPassword = document.getElementById('confirmPassword').value;

                if (!name || !email || !password) {
                    showMessage('Заполните все поля');
                    return;
                }

                if (password !== confirmPassword) {
                    showMessage('Пароли не совпадают');
                    return;
                }

                if (password.length < 6) {
                    showMessage('Пароль должен быть не менее 6 символов');
                    return;
                }

                setButtonLoading('registerBtn', true);
                try {
                    await handleLocalRegister({ name, email, password });
                    showMessage('Регистрация успешна!', 'success');
                    setTimeout(() => {
                        window.location.href = '/cabinet';
                    }, 1000);
                } catch (error) {
                    showMessage(error.message);
                } finally {
                    setButtonLoading('registerBtn', false);
                }
            });
        }

        const googleRegisterBtn = document.getElementById('googleRegisterBtn');
        if (googleRegisterBtn) {
            googleRegisterBtn.addEventListener('click', () => {
                window.location.href = 'http://localhost:9090/realms/bulletin-board/protocol/openid-connect/auth?client_id=bulletin-board-client&response_type=code&redirect_uri=http://localhost:8080/register&state=google&kc_idp_hint=google';
            });
        }
    })();
});