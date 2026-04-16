package ru.daniil.product.config.testBdInit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.enums.CategoryType;
import ru.daniil.product.repository.CategoryRepository;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class CategoryTestDataGenerator {

    private final CategoryRepository categoryRepository;

    public CategoryTestDataGenerator(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void generateCategories() {
        if (categoryRepository.count() > 10) {
            System.out.println("Категории уже существуют в БД. Пропускаем генерацию.");
            return;
        }

        System.out.println("Начинаем генерацию тестовых категорий...");

        // 1. Создаём корневые категории (5 штук)
        List<Category> rootCategories = createRootCategories();
        categoryRepository.saveAll(rootCategories);

        // 2. Для каждой корневой создаём промежуточные категории
        List<Category> allCategories = new ArrayList<>(rootCategories);
        List<Category> intermediateCategories = createIntermediateCategories(rootCategories);
        categoryRepository.saveAll(intermediateCategories);
        allCategories.addAll(intermediateCategories);

        // 3. Для каждой промежуточной создаём конечные категории (LEAF)
        List<Category> leafCategories = createLeafCategories(intermediateCategories);
        categoryRepository.saveAll(leafCategories);
        allCategories.addAll(leafCategories);

        System.out.println("Сгенерировано категорий: " + allCategories.size());
        printStatistics(allCategories);
    }

    private List<Category> createRootCategories() {
        List<Category> roots = new ArrayList<>();

        // Корневые категории (5 штук)
        roots.add(new Category("Электроника", CategoryType.ROOT));
        roots.add(new Category("Одежда и обувь", CategoryType.ROOT));
        roots.add(new Category("Дом и сад", CategoryType.ROOT));
        roots.add(new Category("Спорт и отдых", CategoryType.ROOT));
        roots.add(new Category("Книги и медиа", CategoryType.ROOT));

        return roots;
    }

    private List<Category> createIntermediateCategories(List<Category> rootCategories) {
        List<Category> intermediate = new ArrayList<>();

        Map<String, List<String>> hierarchyMap = new HashMap<>();

        // Определяем структуру категорий
        hierarchyMap.put("Электроника", Arrays.asList("Компьютеры и ноутбуки", "Смартфоны и гаджеты", "Аудио и видео", "Фото и видеокамеры"));
        hierarchyMap.put("Одежда и обувь", Arrays.asList("Мужская одежда", "Женская одежда", "Детская одежда", "Обувь", "Аксессуары"));
        hierarchyMap.put("Дом и сад", Arrays.asList("Мебель", "Бытовая техника", "Посуда и кухонные принадлежности", "Декор и текстиль", "Сад и огород"));
        hierarchyMap.put("Спорт и отдых", Arrays.asList("Фитнес и тренажёры", "Туризм и кемпинг", "Велоспорт", "Зимние виды спорта", "Водные виды спорта"));
        hierarchyMap.put("Книги и медиа", Arrays.asList("Художественная литература", "Научная литература", "Детские книги", "Музыка", "Фильмы и сериалы"));

        for (Category root : rootCategories) {
            List<String> intermediateNames = hierarchyMap.get(root.getName());
            if (intermediateNames != null) {
                for (String name : intermediateNames) {
                    Category category = new Category(name, CategoryType.INTERMEDIATE, root);
                    intermediate.add(category);
                }
            }
        }

        return intermediate;
    }

    private List<Category> createLeafCategories(List<Category> intermediateCategories) {
        List<Category> leafCategories = new ArrayList<>();

        // Для каждой промежуточной категории создаём от 2 до 4 конечных категорий
        Map<String, List<String>> leafMap = new HashMap<>();

        leafMap.put("Компьютеры и ноутбуки", Arrays.asList("Ноутбуки", "Настольные ПК", "Моноблоки", "Комплектующие"));
        leafMap.put("Смартфоны и гаджеты", Arrays.asList("Смартфоны", "Планшеты", "Умные часы", "Наушники"));
        leafMap.put("Аудио и видео", Arrays.asList("Телевизоры", "Акустические системы", "Плееры", "Проекторы"));
        leafMap.put("Фото и видеокамеры", Arrays.asList("Зеркальные камеры", "Беззеркальные камеры", "Экшн-камеры", "Штативы"));

        leafMap.put("Мужская одежда", Arrays.asList("Футболки и поло", "Рубашки", "Джинсы и брюки", "Куртки и пуховики"));
        leafMap.put("Женская одежда", Arrays.asList("Платья", "Блузы и рубашки", "Юбки и брюки", "Верхняя одежда"));
        leafMap.put("Детская одежда", Arrays.asList("Для мальчиков", "Для девочек", "Для новорождённых", "Школьная форма"));
        leafMap.put("Обувь", Arrays.asList("Кроссовки", "Ботинки", "Туфли", "Сандалии"));
        leafMap.put("Аксессуары", Arrays.asList("Сумки и рюкзаки", "Ремни", "Головные уборы", "Часы"));

        leafMap.put("Мебель", Arrays.asList("Кровати", "Диваны и кресла", "Шкафы и комоды", "Столы и стулья"));
        leafMap.put("Бытовая техника", Arrays.asList("Холодильники", "Стиральные машины", "Пылесосы", "Микроволновые печи"));
        leafMap.put("Посуда и кухонные принадлежности", Arrays.asList("Кастрюли и сковороды", "Ножи и разделочные доски", "Столовые приборы", "Посуда для сервировки"));
        leafMap.put("Декор и текстиль", Arrays.asList("Постельное бельё", "Шторы и гардины", "Ковры и паласы", "Декоративные подушки"));
        leafMap.put("Сад и огород", Arrays.asList("Садовый инвентарь", "Семена и рассада", "Удобрения", "Садовая мебель"));

        leafMap.put("Фитнес и тренажёры", Arrays.asList("Беговые дорожки", "Велотренажёры", "Силовые тренажёры", "Коврики и мячи"));
        leafMap.put("Туризм и кемпинг", Arrays.asList("Палатки", "Спальные мешки", "Рюкзаки", "Туристическая посуда"));
        leafMap.put("Велоспорт", Arrays.asList("Горные велосипеды", "Шоссейные велосипеды", "Городские велосипеды", "Аксессуары для велосипедов"));
        leafMap.put("Зимние виды спорта", Arrays.asList("Лыжи", "Сноуборды", "Коньки", "Экипировка"));
        leafMap.put("Водные виды спорта", Arrays.asList("Плавки и купальники", "Очки для плавания", "Надувные лодки", "Спасательные жилеты"));

        leafMap.put("Художественная литература", Arrays.asList("Классика", "Современная проза", "Детективы и триллеры", "Фантастика и фэнтези"));
        leafMap.put("Научная литература", Arrays.asList("Учебники", "Научно-популярные", "Техническая литература", "Бизнес-литература"));
        leafMap.put("Детские книги", Arrays.asList("Сказки", "Познавательные", "Книги с картинками", "Энциклопедии для детей"));
        leafMap.put("Музыка", Arrays.asList("CD и винил", "Музыкальные инструменты", "Нотные издания", "Наушники для музыки"));
        leafMap.put("Фильмы и сериалы", Arrays.asList("Blu-ray и DVD", "Подписки на стриминг", "Киноплакаты", "Сувениры"));

        for (Category intermediate : intermediateCategories) {
            List<String> leafNames = leafMap.get(intermediate.getName());
            if (leafNames != null) {
                for (String leafName : leafNames) {
                    Category leaf = new Category(leafName, CategoryType.LEAF, intermediate);
                    leafCategories.add(leaf);
                }
            } else {
                // Если нет специфических конечных категорий, создаём стандартные
                leafCategories.add(new Category("Стандартные товары", CategoryType.LEAF, intermediate));
                leafCategories.add(new Category("Премиум товары", CategoryType.LEAF, intermediate));
            }
        }

        return leafCategories;
    }

    private void printStatistics(List<Category> allCategories) {
        Map<CategoryType, Long> typeCount = allCategories.stream()
                .collect(Collectors.groupingBy(Category::getType, Collectors.counting()));

        long rootsCount = typeCount.getOrDefault(CategoryType.ROOT, 0L);
        long intermediateCount = typeCount.getOrDefault(CategoryType.INTERMEDIATE, 0L);
        long leafCount = typeCount.getOrDefault(CategoryType.LEAF, 0L);

        System.out.println("\n=== СТАТИСТИКА СОЗДАННЫХ КАТЕГОРИЙ ===");
        System.out.println("Всего категорий: " + allCategories.size());
        System.out.println("Корневых (ROOT): " + rootsCount);
        System.out.println("Промежуточных (INTERMEDIATE): " + intermediateCount);
        System.out.println("Конечных (LEAF): " + leafCount);
        System.out.println("\n=== ПРОВЕРКА ИЕРАРХИИ ===");

        // Проверяем, что у каждой LEAF есть родитель INTERMEDIATE или ROOT
        List<Category> roots = allCategories.stream()
                .filter(c -> c.getType() == CategoryType.ROOT)
                .toList();

        for (Category root : roots) {
            System.out.println("\nКорень: " + root.getName());
            List<Category> children = allCategories.stream()
                    .filter(c -> c.getParent() != null && c.getParent().getId().equals(root.getId()))
                    .toList();
            System.out.println("  Прямых потомков: " + children.size());

            for (Category child : children) {
                if (child.getType() == CategoryType.INTERMEDIATE) {
                    List<Category> grandChildren = allCategories.stream()
                            .filter(c -> c.getParent() != null && c.getParent().getId().equals(child.getId()))
                            .toList();
                    System.out.println("    " + child.getName() + " (INTERMEDIATE) -> LEAF: " + grandChildren.size());
                }
            }
        }
    }

    public void clearCategories() {
        categoryRepository.deleteAll();
        System.out.println("Все категории удалены из БД");
    }
}