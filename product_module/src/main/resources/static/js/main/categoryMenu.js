// Кэш категорий
const categoryCache = new Map();

// Состояние меню
let currentColumns = [];
let hideTimeout = null;
let activeItem = null;

class CategoryMenu {
    constructor(containerId, onCategorySelect) {
        this.container = document.getElementById(containerId);
        this.onCategorySelect = onCategorySelect;
        if (!this.container) {
            console.error(`Контейнер ${containerId} не найден`);
            return;
        }

        this.menuElement = null;
        this.init();
    }

    async init() {
        const rootCategories = await this.loadRootCategories();
        this.renderMenu(rootCategories);
        this.attachEventListeners();
    }

    async loadRootCategories() {
        if (categoryCache.has('root')) {
            return categoryCache.get('root');
        }

        try {
            const response = await fetch('/api/category/root', {
                credentials: 'include'
            });

            if (!response.ok) throw new Error('Ошибка загрузки корневых категорий');

            const categories = await response.json();
            categoryCache.set('root', categories);
            return categories;
        } catch(e) {
            console.error('Ошибка загрузки корневых категорий:', e);
            return [];
        }
    }

    async loadChildCategories(categoryName) {
        if (categoryCache.has(categoryName)) {
            return categoryCache.get(categoryName);
        }

        try {
            const response = await fetch(`/api/category/${encodeURIComponent(categoryName)}/next`, {
                credentials: 'include'
            });

            if (!response.ok) throw new Error(`Ошибка загрузки потомков для ${categoryName}`);

            const categories = await response.json();
            categoryCache.set(categoryName, categories);
            return categories;
        } catch(e) {
            console.error('Ошибка загрузки потомков:', e);
            return [];
        }
    }

    renderMenu(categories, level = 0) {
        if (!this.container) return;

        if (level === 0) {
            this.container.innerHTML = '';
            this.menuElement = document.createElement('div');
            this.menuElement.className = 'categoryMenu';
            this.container.appendChild(this.menuElement);
            currentColumns = [];
        }

        // Удаляем все колонки начиная с текущего уровня
        while (currentColumns.length > level) {
            const col = currentColumns.pop();
            if (col && col.remove) col.remove();
        }

        // Создаем новую колонку
        const column = this.createColumn(categories, level);
        this.menuElement.appendChild(column);
        currentColumns.push(column);

        // Анимация появления
        column.style.animation = 'slideIn 0.2s ease';
    }

    createColumn(categories, level) {
        const column = document.createElement('div');
        column.className = `categoryColumn level${level}`;
        column.dataset.level = level;

        const list = document.createElement('ul');
        list.className = 'categoryList';

        categories.forEach(category => {
            const item = this.createCategoryItem(category, level);
            list.appendChild(item);
        });

        column.appendChild(list);
        return column;
    }

    createCategoryItem(category, level) {
        const li = document.createElement('li');
        li.className = 'categoryItem';
        li.dataset.categoryId = category.id;
        li.dataset.categoryName = category.name;
        li.dataset.categoryLevel = level;
        li.dataset.hasChildren = category.hasChildren || false;

        const link = document.createElement('a');
        link.href = '#';
        link.className = 'categoryLink';
        link.textContent = category.name;

        if (category.hasChildren) {
            const arrow = document.createElement('span');
            arrow.className = 'categoryArrow';
            arrow.textContent = '›';
            link.appendChild(arrow);
        }

        li.appendChild(link);
        this.attachItemEvents(li, category, level);

        return li;
    }

    attachItemEvents(item, category, level) {
        let loadTimer = null;

        // При наведении мыши
        item.addEventListener('mouseenter', async (e) => {
            if (hideTimeout) {
                clearTimeout(hideTimeout);
                hideTimeout = null;
            }

            if (activeItem) {
                activeItem.classList.remove('active');
            }
            activeItem = item;
            item.classList.add('active');

            if (category.hasChildren && !this.menuElement.querySelector(`.categoryColumn.level${level + 1}`)) {
                loadTimer = setTimeout(async () => {
                    item.classList.add('loading');
                    const children = await this.loadChildCategories(category.name);
                    item.classList.remove('loading');

                    if (children && children.length > 0) {
                        this.renderMenu(children, level + 1);
                    }
                }, 300);
            }
        });

        // При уходе мыши
        item.addEventListener('mouseleave', () => {
            if (loadTimer) {
                clearTimeout(loadTimer);
                loadTimer = null;
            }

            if (activeItem === item) {
                item.classList.remove('active');
                activeItem = null;
            }

            if (hideTimeout) {
                clearTimeout(hideTimeout);
            }
            hideTimeout = setTimeout(() => {
                this.resetToRoot();
            }, 5000);
        });

        // Клик по категории - обновляем фильтры без перезагрузки
        item.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();

            if (this.onCategorySelect) {
                this.onCategorySelect(category);
            }

            // Закрываем меню
            if (this.container) {
                this.container.style.display = 'none';
                const menuButton = document.querySelector('.categoryMenuButton');
                if (menuButton) menuButton.classList.remove('active');
            }

            this.resetToRoot();
        });
    }

    resetToRoot() {
        while (currentColumns.length > 1) {
            const col = currentColumns.pop();
            if (col && col.remove) col.remove();
        }

        if (activeItem) {
            activeItem.classList.remove('active');
            activeItem = null;
        }

        if (hideTimeout) {
            clearTimeout(hideTimeout);
            hideTimeout = null;
        }
    }

    attachEventListeners() {
        document.addEventListener('click', (e) => {
            if (this.menuElement && !this.menuElement.contains(e.target) &&
                !e.target.classList.contains('categoryMenuButton')) {
                if (this.container) {
                    this.container.style.display = 'none';
                }
                const menuButton = document.querySelector('.categoryMenuButton');
                if (menuButton) menuButton.classList.remove('active');
            }
        });

        if (this.menuElement) {
            this.menuElement.addEventListener('click', (e) => {
                e.stopPropagation();
            });
        }
    }
}

// Инициализация
document.addEventListener('DOMContentLoaded', () => {
    const filterGroup = document.querySelector('.filterGroup');
    const categorySelect = document.getElementById('categoryFilter');

    if (filterGroup && categorySelect) {
        const menuContainer = document.createElement('div');
        menuContainer.id = 'categoryMenuContainer';
        menuContainer.className = 'categoryMenuContainer';

        categorySelect.style.display = 'none';
        categorySelect.parentNode.insertBefore(menuContainer, categorySelect.nextSibling);

        const menuButton = document.createElement('button');
        menuButton.className = 'categoryMenuButton';
        menuButton.innerHTML = '📁 Выбрать категорию <span class="menuArrow">▼</span>';

        categorySelect.parentNode.insertBefore(menuButton, menuContainer);

        let categoryMenu = null;

        menuButton.addEventListener('click', (e) => {
            e.stopPropagation();
            const isVisible = menuContainer.style.display === 'block';

            if (!isVisible) {
                menuContainer.style.display = 'block';
                menuButton.classList.add('active');

                if (!categoryMenu) {
                    categoryMenu = new CategoryMenu('categoryMenuContainer', (category) => {
                        // Обновляем select без перезагрузки страницы
                        if (categorySelect) {
                            categorySelect.value = category.id;
                            // Триггерим событие change для применения фильтров
                            const event = new Event('change');
                            categorySelect.dispatchEvent(event);
                        }
                        // Закрываем меню
                        menuContainer.style.display = 'none';
                        menuButton.classList.remove('active');
                    });
                }
            } else {
                menuContainer.style.display = 'none';
                menuButton.classList.remove('active');
                if (categoryMenu) {
                    categoryMenu.resetToRoot();
                }
            }
        });

        document.addEventListener('click', (e) => {
            if (!menuContainer.contains(e.target) && e.target !== menuButton) {
                menuContainer.style.display = 'none';
                menuButton.classList.remove('active');
                if (categoryMenu) {
                    categoryMenu.resetToRoot();
                }
            }
        });
    }
});