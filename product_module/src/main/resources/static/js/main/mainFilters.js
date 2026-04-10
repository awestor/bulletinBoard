let currentFilters = {
    search: '',
    category: '',
    priceMin: '',
    priceMax: ''
};

async function applyFilters() {
    const searchValue = document.getElementById('searchInput')?.value || '';
    const categoryValue = document.getElementById('categoryFilter')?.value || '';
    const priceMinValue = document.getElementById('priceMin')?.value || '';
    const priceMaxValue = document.getElementById('priceMax')?.value || '';

    currentFilters = {
        search: searchValue,
        category: categoryValue,
        priceMin: priceMinValue,
        priceMax: priceMaxValue
    };

    window.currentFilters = currentFilters;

    // Сохраняем в sessionStorage
    sessionStorage.setItem('productFilters', JSON.stringify(currentFilters));

    // Обновляем товары без перезагрузки страницы
    if (window.loadProducts) {
        await window.loadProducts(currentFilters, true);
    }

    // Обновляем слайдер (опционально, если нужно фильтровать и слайдер)
    if (window.loadSliderProducts && !categoryValue && !searchValue) {
        await window.loadSliderProducts();
    }
}

function loadSavedFilters() {
    const savedFilters = sessionStorage.getItem('productFilters');
    if (savedFilters) {
        try {
            const filters = JSON.parse(savedFilters);
            if (document.getElementById('searchInput')) {
                document.getElementById('searchInput').value = filters.search || '';
            }
            if (document.getElementById('categoryFilter')) {
                document.getElementById('categoryFilter').value = filters.category || '';
            }
            if (document.getElementById('priceMin')) {
                document.getElementById('priceMin').value = filters.priceMin || '';
            }
            if (document.getElementById('priceMax')) {
                document.getElementById('priceMax').value = filters.priceMax || '';
            }
            currentFilters = filters;
            window.currentFilters = currentFilters;
        } catch(e) {
            console.error('Ошибка загрузки фильтров:', e);
        }
    }
}

// Функция для сброса фильтров
window.resetFilters = function() {
    if (document.getElementById('searchInput')) {
        document.getElementById('searchInput').value = '';
    }
    if (document.getElementById('categoryFilter')) {
        document.getElementById('categoryFilter').value = '';
    }
    if (document.getElementById('priceMin')) {
        document.getElementById('priceMin').value = '';
    }
    if (document.getElementById('priceMax')) {
        document.getElementById('priceMax').value = '';
    }

    currentFilters = {
        search: '',
        category: '',
        priceMin: '',
        priceMax: ''
    };

    window.currentFilters = currentFilters;
    sessionStorage.removeItem('productFilters');

    if (window.loadProducts) {
        window.loadProducts(currentFilters, true);
    }
};

// Инициализация
document.addEventListener('DOMContentLoaded', () => {
    console.log("mainFilters.js инициализация");

    loadSavedFilters();

    const applyBtn = document.getElementById('applyFiltersBtn');
    if (applyBtn) {
        applyBtn.addEventListener('click', applyFilters);
    } else {
        console.warn("Кнопка applyFiltersBtn не найдена");
    }

    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        let timeout;
        searchInput.addEventListener('input', () => {
            clearTimeout(timeout);
            timeout = setTimeout(applyFilters, 500);
        });
    }

    const categorySelect = document.getElementById('categoryFilter');
    if (categorySelect) {
        categorySelect.addEventListener('change', () => {
            applyFilters();
        });
    }
});