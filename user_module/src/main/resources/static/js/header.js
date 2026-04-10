function getCookie(name) {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
        const [key, ...rest] = cookie.split('=');
        if (key && key.trim() === name) {
            return rest.length > 0 ? decodeURIComponent(rest.join('=')) : null;
        }
    }
    return null;
}


function getUsername() {
    const username = getCookie('USERNAME');
    console.log('USERNAME cookie:', username);
    return username && username.trim() ? username.trim() : null;
}

function isAuthenticated() {
    return !!getUsername();
}

function getDefaultAvatarSvg() {
    const svgElement = document.getElementById('defaultAvatarSvg');
    if (svgElement) {
        return svgElement.outerHTML;
    }
    return `<svg class="userAvatarDefault" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="50" cy="50" r="50" fill="#667eea"/><circle cx="50" cy="35" r="15" fill="white" fill-opacity="0.9"/><path d="M50 55 C35 55 22 65 20 80 L80 80 C78 65 65 55 50 55Z" fill="white" fill-opacity="0.9"/></svg>`;
}

async function loadUserAvatar(username) {
    if (!username) return null;
    try {
        const response = await fetch(`/api/users/${encodeURIComponent(username)}/avatar`, {
            credentials: 'include'
        });
        if (response.ok) {
            const data = await response.json();
            return data.avatarUrl;
        }
    } catch(e) {
        console.error('Ошибка загрузки аватара:', e);
    }
    return null;
}

async function updateAuthStatus() {
    const authStatus = document.getElementById('authStatus');
    const cabinetLink = document.getElementById('cabinetLink');

    if (!authStatus) return;

    const authenticated = isAuthenticated();
    const username = getUsername();

    console.log('Authenticated:', authenticated, 'Username:', username);

    if (authenticated && username && username !== 'null') {
        if (cabinetLink) {
            cabinetLink.style.display = 'inline-block';
        }

        const defaultSvg = getDefaultAvatarSvg();

        authStatus.innerHTML = `
            <div class="authUser">
                <div class="userAvatarContainer" id="avatarContainer">
                    ${defaultSvg}
                </div>
                <div class="userDropdown">
                    <span class="userName">${escapeHtml(username)}</span>
                    <div class="dropdownMenu">
                        <a href="/cabinet" class="dropdownItem">Профиль</a>
                        <a href="/cabinet/my-products" class="dropdownItem">Мои товары</a>
                        <a href="/cabinet/cart" class="dropdownItem">Корзина</a>
                        <a href="/cabinet/purchases" class="dropdownItem">История покупок</a>
                        <div class="dropdownDivider"></div>
                        <button onclick="window.logout()" class="dropdownItem logoutItem">Выйти</button>
                    </div>
                </div>
            </div>
        `;

        // Загружаем реальный аватар
        const avatarUrl = await loadUserAvatar(username);
        const avatarContainer = document.getElementById('avatarContainer');
        if (avatarContainer && avatarUrl) {
            avatarContainer.innerHTML = `<img src="${avatarUrl}" alt="${username}" class="userAvatarImage" onerror="this.onerror=null; this.parentElement.innerHTML = \`${defaultSvg}\`">`;
        }
    } else {
        if (cabinetLink) {
            cabinetLink.style.display = 'none';
        }

        authStatus.innerHTML = `
            <div class="authButtons">
                <a href="/login" class="btnOutline">Вход</a>
                <a href="/register" class="btnPrimary">Регистрация</a>
            </div>
        `;
    }

    initDropdown();
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function initDropdown() {
    const userDropdown = document.querySelector('.userDropdown');
    if (!userDropdown) return;

    // Удаляем старый обработчик
    const newDropdown = userDropdown.cloneNode(true);
    userDropdown.parentNode.replaceChild(newDropdown, userDropdown);

    newDropdown.addEventListener('click', (e) => {
        e.stopPropagation();
        newDropdown.classList.toggle('active');
    });
}

window.logout = async function() {
    try {
        await fetch('/api/auth/logout', {
            method: 'POST',
            credentials: 'include'
        });
    } catch(e) {
        console.error('Ошибка при выходе:', e);
    }
    window.location.href = '/login?logout=true';
};

// Закрытие дропдауна при клике вне
document.addEventListener('click', () => {
    const dropdown = document.querySelector('.userDropdown');
    if (dropdown) {
        dropdown.classList.remove('active');
    }
});

// Обновляем статус при загрузке
document.addEventListener('DOMContentLoaded', updateAuthStatus);

// Также обновляем при возврате на страницу
window.addEventListener('pageshow', updateAuthStatus);