// Защита от ошибок
(function() {
    console.log("mainProductList.js загружен");

    // Проверяем наличие необходимых элементов
    function checkElements() {
        const productsGrid = document.getElementById('productsGrid');
        if (!productsGrid) {
            console.error("Элемент productsGrid не найден!");
            return false;
        }
        return true;
    }

    // Загрузка и отображение списка товаров
    let currentPage = 1;
    let isLoading = false;
    let hasMore = true;
    let currentFilters = {};

    window.loadProducts = async function(filters = null, reset = true) {
        console.log("loadProducts вызван", { filters, reset });

        const productsGrid = document.getElementById('productsGrid');
        if (!productsGrid) {
            console.error("productsGrid не найден");
            return;
        }

        if (reset) {
            currentPage = 1;
            hasMore = true;
            productsGrid.innerHTML = '<div class="loading">Загрузка...</div>';
        }

        if (isLoading) return;
        isLoading = true;

        try {
            const params = new URLSearchParams();

            if (filters) {
                if (filters.search) params.append('search', filters.search);
                if (filters.category) params.append('category', filters.category);
                if (filters.priceMin) params.append('priceMin', filters.priceMin);
                if (filters.priceMax) params.append('priceMax', filters.priceMax);
            } else if (window.currentFilters) {
                currentFilters = window.currentFilters;
            }

            params.append('page', currentPage);
            params.append('size', 12);

            console.log("Запрос товаров:", params.toString());

            const response = await fetch(`/api/products?${params.toString()}`, {
                credentials: 'include'
            });

            if (!response.ok) {
                throw new Error('Ошибка загрузки товаров');
            }

            const data = await response.json();
            const products = data.content || data || [];
            const totalPages = data.totalPages || 1;

            if (reset) {
                productsGrid.innerHTML = '';
            }

            if (!products || products.length === 0) {
                if (currentPage === 1) {
                    productsGrid.innerHTML = '<div class="emptyState">Товары не найдены</div>';
                }
                hasMore = false;
                return;
            }

            products.forEach(product => {
                const productCard = createProductCard(product);
                productsGrid.appendChild(productCard);
            });

            hasMore = currentPage < totalPages;

            if (hasMore && reset) {
                setupInfiniteScroll();
            }

        } catch(e) {
            console.error('Ошибка загрузки товаров:', e);
            if (reset) {
                productsGrid.innerHTML = '<div class="emptyState">Ошибка загрузки товаров. Попробуйте позже.</div>';
            }
        } finally {
            isLoading = false;
        }
    };

    // Остальной код...
})();