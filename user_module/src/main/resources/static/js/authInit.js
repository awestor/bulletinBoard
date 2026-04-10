(function() {
    // Страницы, доступные без авторизации
    const publicPaths = ['/login', '/register', '/error'];
    const currentPath = window.location.pathname;

    // Проверяем, нужна ли авторизация
    const requiresAuth = !publicPaths.some(path => currentPath.startsWith(path));

    if (!requiresAuth) {
        console.log("Публичная страница");
        return;
    }

    function getCookie(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
        return null;
    }

    // Проверяем наличие USERNAME cookie
    const username = getCookie('USERNAME');

    if (!username) {
        console.log("Не авторизован, редирект на /login");
        window.location.href = '/login';
    } else {
        console.log("Авторизован как:", username);
    }
})();