// Слайдер с популярными товарами
let sliderScrollInterval = null;

async function loadSliderProducts() {
    const sliderContainer = document.getElementById('featuredSlider');
    if (!sliderContainer) return;

    // Показываем скелетон (фиксированное место)
    sliderContainer.innerHTML = `
        <div class="loading" style="min-height: 280px; display: flex; align-items: center; justify-content: center;">
            Загрузка популярных товаров...
        </div>
    `;

    try {
        const response = await fetch('/api/products/featured', {
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error('Ошибка загрузки слайдера');
        }

        const products = await response.json();

        if (!products || products.length === 0) {
            sliderContainer.innerHTML = '<div class="emptyState">Нет популярных товаров</div>';
            return;
        }

        sliderContainer.innerHTML = products.map(product => `
            <div class="slideCard" data-product-id="${product.id}" onclick="location.href='/product/${product.id}'">
                <img src="${product.image || '/images/placeholder.jpg'}"
                     alt="${escapeHtml(product.name)}"
                     class="slideImage"
                     loading="lazy"
                     onerror="this.src='/images/placeholder.jpg'">
                <div class="slideInfo">
                    <div class="slideName">${escapeHtml(product.name)}</div>
                    <div class="slidePrice">${formatPrice(product.price)} ₽</div>
                </div>
            </div>
        `).join('');

        // Добавляем кнопки навигации после загрузки контента
        addSliderNavigation();

    } catch(e) {
        console.error('Ошибка загрузки слайдера:', e);
        sliderContainer.innerHTML = '<div class="emptyState">Ошибка загрузки популярных товаров</div>';
    }
}

function addSliderNavigation() {
    const sliderContainer = document.querySelector('.sliderContainer');
    if (!sliderContainer) return;

    // Удаляем существующие кнопки, если есть
    const existingNav = sliderContainer.querySelector('.sliderNav');
    if (existingNav) existingNav.remove();

    const sliderTrack = document.getElementById('featuredSlider');
    if (!sliderTrack || sliderTrack.children.length === 0) return;

    // Создаем кнопки навигации
    const navDiv = document.createElement('div');
    navDiv.className = 'sliderNav';
    navDiv.innerHTML = `
        <button class="sliderNavBtn sliderNavPrev" aria-label="Предыдущий">‹</button>
        <button class="sliderNavBtn sliderNavNext" aria-label="Следующий">›</button>
    `;

    sliderContainer.style.position = 'relative';
    sliderContainer.appendChild(navDiv);

    const prevBtn = navDiv.querySelector('.sliderNavPrev');
    const nextBtn = navDiv.querySelector('.sliderNavNext');

    // Прокрутка влево
    prevBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        sliderTrack.scrollBy({
            left: -300,
            behavior: 'smooth'
        });
    });

    // Прокрутка вправо
    nextBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        sliderTrack.scrollBy({
            left: 300,
            behavior: 'smooth'
        });
    });

    // Автоматическая прокрутка (опционально)
    startAutoScroll(sliderTrack);

    // Останавливаем автопрокрутку при наведении
    sliderTrack.addEventListener('mouseenter', () => stopAutoScroll());
    sliderTrack.addEventListener('mouseleave', () => startAutoScroll(sliderTrack));
}

function startAutoScroll(sliderTrack) {
    if (sliderScrollInterval) clearInterval(sliderScrollInterval);

    sliderScrollInterval = setInterval(() => {
        if (sliderTrack && !sliderTrack.matches(':hover')) {
            const scrollAmount = sliderTrack.clientWidth;
            const maxScroll = sliderTrack.scrollWidth - sliderTrack.clientWidth;

            if (sliderTrack.scrollLeft + scrollAmount >= maxScroll) {
                // Дошли до конца - возвращаемся в начало
                sliderTrack.scrollTo({ left: 0, behavior: 'smooth' });
            } else {
                sliderTrack.scrollBy({ left: scrollAmount, behavior: 'smooth' });
            }
        }
    }, 5000);
}

function stopAutoScroll() {
    if (sliderScrollInterval) {
        clearInterval(sliderScrollInterval);
        sliderScrollInterval = null;
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatPrice(price) {
    return new Intl.NumberFormat('ru-RU').format(price);
}

// Инициализация
document.addEventListener('DOMContentLoaded', () => {
    loadSliderProducts();
});

// Экспортируем для возможного переиспользования
window.loadSliderProducts = loadSliderProducts;