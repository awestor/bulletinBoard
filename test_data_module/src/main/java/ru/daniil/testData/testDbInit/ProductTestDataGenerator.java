package ru.daniil.testData.testDbInit;

import jakarta.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.product.Category;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.CreateProductRequest;
import ru.daniil.product.service.category.CategoryService;
import ru.daniil.product.service.product.ProductProcessorService;
import ru.daniil.product.service.product.ProductService;
import ru.daniil.user.service.user.UserService;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProductTestDataGenerator {

    private static final Logger infoLogger = LoggerFactory.getLogger("INFO-LOGGER");
    private static final Random random = new Random();

    private final CategoryService categoryService;
    private final ProductService productService;
    private final ProductProcessorService productProcessorService;
    private final ProductAttributeGenerator attributeGenerator;
    private final ProductImageTestGenerator testImageGenerator;
    private final UserTestDataGenerator userTestDataGenerator;
    private final UserService userService;

    public ProductTestDataGenerator(CategoryService categoryService,
                                    ProductService productService, ProductProcessorService productProcessorService,
                                    ProductAttributeGenerator attributeGenerator,
                                    ProductImageTestGenerator testImageGenerator,
                                    UserTestDataGenerator userTestDataGenerator,
                                    UserService userService) {
        this.categoryService = categoryService;
        this.productService = productService;
        this.productProcessorService = productProcessorService;
        this.attributeGenerator = attributeGenerator;
        this.testImageGenerator = testImageGenerator;
        this.userTestDataGenerator = userTestDataGenerator;
        this.userService = userService;
    }

    @Transactional
    public void generateProducts() {
        long existingProducts = productService.count();
        if (existingProducts > 50) {
            infoLogger.info("Продукты уже существуют в БД ({} шт.). Пропускаем генерацию.", existingProducts);
            return;
        }

        List<Category> leafCategories = categoryService.getLeafCategories();

        if (leafCategories.isEmpty()) {
            infoLogger.warn("Нет конечных категорий (LEAF). Сначала выполните генерацию категорий.");
            return;
        }

        infoLogger.info("Найдено {} конечных категорий", leafCategories.size());

        List<String> sellerLogins = ensureSellersExist(leafCategories.size());
        infoLogger.info("Доступно {} продавцов", sellerLogins.size());

        Map<String, List<Category>> sellerCategoriesMap =
                distributeCategoriesToSellers(sellerLogins, leafCategories);

        try {
            List<Product> allProducts = generateProductsForSellers(sellerCategoriesMap);
            infoLogger.info("Сгенерировано продуктов: {}", allProducts.size());
            printStatistics(allProducts, sellerCategoriesMap);
        } catch (Exception e){
            // уже было прологировано
        }
    }

    /**
     * Гарантирует наличие достаточного количества продавцов
     * @param requiredMinCategories минимальное количество категорий (нужно столько же или меньше продавцов)
     * @return список логинов продавцов
     */
    private List<String> ensureSellersExist(int requiredMinCategories) {
        List<String> existingSellers = userTestDataGenerator.getGeneratedTestUsersList();

        // Всегда должен быть хотя бы один продавец (FIRST_SELLER)
        String firstSeller = userTestDataGenerator.getFirstSellerLogin();
        if (!existingSellers.contains(firstSeller)) {
            existingSellers = userTestDataGenerator.generateTestSellers(1);
        }

        int currentSellersCount = existingSellers.size();
        int targetSellersCount = Math.min(requiredMinCategories, 10);

        if (currentSellersCount < targetSellersCount) {
            int needToCreate = targetSellersCount - currentSellersCount;
            infoLogger.info("Недостаточно продавцов. Создаём ещё {} продавцов", needToCreate);
            List<String> newSellers = userTestDataGenerator.generateTestSellers(needToCreate);
            existingSellers.addAll(newSellers);
        }

        return existingSellers;
    }

    /**
     * Распределяет категории между продавцами
     * Каждая категория закрепляется за одним продавцом
     * @param sellerLogins никнейм продавца
     * @param categories категории
     * @return список категорий привязанных к продавцу
     */
    private Map<String, List<Category>> distributeCategoriesToSellers(
            List<String> sellerLogins, List<Category> categories) {
        Map<String, List<Category>> sellerCategoriesMap = new HashMap<>();

        for (String login : sellerLogins) {
            sellerCategoriesMap.put(login, new ArrayList<>());
        }

        for (int i = 0; i < categories.size(); i++) {
            String sellerLogin = sellerLogins.get(i % sellerLogins.size());
            sellerCategoriesMap.get(sellerLogin).add(categories.get(i));
        }

        infoLogger.info("Распределение категорий по продавцам:");
        for (Map.Entry<String, List<Category>> entry : sellerCategoriesMap.entrySet()) {
            infoLogger.info("  Продавец {}: {} категорий",
                    entry.getKey(), entry.getValue().size());
        }

        return sellerCategoriesMap;
    }

    /**
     * Генерирует продукты для каждого продавца по его категориям
     * @param sellerCategoriesMap список категорий привязанных к продавцу
     * @return список продуктов
     */
    private List<Product> generateProductsForSellers(Map<String, List<Category>> sellerCategoriesMap) {
        List<Product> allProducts = new ArrayList<>();

        for (Map.Entry<String, List<Category>> entry : sellerCategoriesMap.entrySet()) {
            String sellerLogin = entry.getKey();
            List<Category> categories = entry.getValue();

            infoLogger.info("Генерация продуктов для продавца {} ({} категорий)", sellerLogin, categories.size());

            User seller = userService.getByLogin(sellerLogin).orElseThrow(
                    () -> new NotFoundException("Возникла ошибка при генерации продуктов")
            );
            try {
                for (Category category : categories) {
                    int productsCount = generateCountProducts(category.getName());
                    List<Product> products = createProductsForCategory(category, seller, productsCount);
                    allProducts.addAll(products);

                    infoLogger.debug("  Категория '{}': создано {} продуктов",
                            category.getName(), products.size());
                }
            } catch (Exception e){
                infoLogger.error("При генерации продуктов для категории возникла ошибка: {}", e.getMessage());
                throw e;
            }
        }

        return allProducts;
    }

    /**
     * Создаёт продукты для конкретной категории и продавца
     * @param category категория
     * @param seller продавец
     * @param count количество
     * @return список продуктов, что он продаёт
     */
    private List<Product> createProductsForCategory(Category category, User seller, int count) {
        List<Product> products = new ArrayList<>();

        List<CreateProductRequest> templates = getProductTemplatesForCategory(category.getName(), category);

        try {
            for (int i = 0; i < count; i++) {
                CreateProductRequest template = templates.isEmpty()
                        ? getDefaultTemplate(category)
                        : templates.get(i % templates.size());

                String fullProductName = generateProductName(template, i);

                int imagesCount = generateRandomInt(1, 3);
                List<MultipartFile> images = testImageGenerator.generateImagesForProduct(
                        fullProductName,
                        category.getName(),
                        imagesCount
                );

                template.setName(fullProductName);
                template.setImages(images);
                template.setStockQuantity(generateRandomInt(10, 100));
                template.setAttributes(
                        attributeGenerator.generateAttributes(category.getName(), template.getName())
                );

                Product product = productProcessorService.create(template, seller);
                products.add(product);
                infoLogger.debug("Создан продукт: {} (продавец: {}, категория: {})",
                        product.getName(), seller.getLogin(), category.getName());
            }
        } catch (Exception e) {
            infoLogger.error("Ошибка создания продукта для категории {}: {}",
                    category.getName(), e.getMessage());
            throw e;
        }

        return products;
    }

    private String generateProductName(CreateProductRequest template, int index) {
        List<String> modifiers = Arrays.asList("Профессиональный", "Базовый", "Премиум", "Эконом", "Ультра");
        String modifier = modifiers.get(index % modifiers.size());
        return modifier + " " + template.getName() + (index > 0 ? " " + (index + 1) : "");
    }

    private Integer generateRandomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    /**
     * Генерирует лимит от 5 до 15 на продукты в зависимости от хеша названия категории
     * @param categoryName название категории
     * @return количество генерируемых продуктов
     */
    private Integer generateCountProducts(String categoryName) {
        int hash = Math.abs(categoryName.hashCode());
        return 5 + (hash % 11);
    }

    private List<CreateProductRequest> getProductTemplatesForCategory(String categoryName, Category category) {
        List<CreateProductRequest> templates = new ArrayList<>();

        // Электроника
        switch (categoryName) {
            case "Ноутбуки" -> {
                templates.add(createProductRequest(category, "Ноутбук", 50000, "Мощный ноутбук для работы и игр"));
                templates.add(createProductRequest(category, "Ультрабук", 70000, "Лёгкий и тонкий ноутбук для бизнеса"));
                templates.add(createProductRequest(category, "Игровой ноутбук", 120000, "Высокопроизводительный игровой ноутбук"));
            }
            case "Смартфоны" -> {
                templates.add(createProductRequest(category, "Смартфон", 25000, "Современный смартфон с отличной камерой"));
                templates.add(createProductRequest(category, "Флагманский смартфон", 60000, "Топовый смартфон с лучшими характеристиками"));
            }
            case "Планшеты" ->
                    templates.add(createProductRequest(category, "Планшет", 20000, "Универсальный планшет для работы и развлечений"));
            case "Наушники" ->
                    templates.add(createProductRequest(category, "Наушники", 5000, "Беспроводные наушники с шумоподавлением"));
            case "Телевизоры" -> {
                templates.add(createProductRequest(category, "Телевизор", 40000, "Smart TV с 4K разрешением"));
                templates.add(createProductRequest(category, "OLED телевизор", 90000, "Премиальный телевизор с идеальным чёрным цветом"));
            }

            // Одежда
            case "Футболки и поло" -> {
                templates.add(createProductRequest(category, "Футболка", 1500, "Хлопковая футболка повседневная"));
                templates.add(createProductRequest(category, "Поло", 2500, "Классическое поло из качественного трикотажа"));
            }
            case "Рубашки" -> {
                templates.add(createProductRequest(category, "Рубашка", 3000, "Классическая рубашка из хлопка"));
                templates.add(createProductRequest(category, "Спортивная рубашка", 2800, "Удобная рубашка для активного отдыха"));
            }
            case "Джинсы и брюки" -> {
                templates.add(createProductRequest(category, "Джинсы", 4000, "Классические джинсы прямого кроя"));
                templates.add(createProductRequest(category, "Брюки чинос", 3500, "Удобные брюки для офиса"));
            }
            case "Платья" -> {
                templates.add(createProductRequest(category, "Платье", 5000, "Элегантное платье для особых случаев"));
                templates.add(createProductRequest(category, "Повседневное платье", 3500, "Удобное платье для каждого дня"));
            }
            case "Аксессуары" -> {
                templates.add(createProductRequest(category, "Сумка", 3000, "Стильная сумка повседневная"));
                templates.add(createProductRequest(category, "Ремень", 1500, "Кожаный ремень с пряжкой"));
                templates.add(createProductRequest(category, "Часы", 8000, "Классические наручные часы"));
            }

            // Дом и сад
            case "Кровати" -> {
                templates.add(createProductRequest(category, "Кровать", 15000, "Удобная кровать с ортопедическим основанием"));
                templates.add(createProductRequest(category, "Двуспальная кровать", 25000, "Просторная кровать для двоих"));
            }
            case "Диваны и кресла" -> {
                templates.add(createProductRequest(category, "Диван", 30000, "Угловой диван с механизмом трансформации"));
                templates.add(createProductRequest(category, "Кресло", 12000, "Мягкое кресло для отдыха"));
            }
            case "Холодильники" -> {
                templates.add(createProductRequest(category, "Холодильник", 35000, "Двухкамерный холодильник с No Frost"));
                templates.add(createProductRequest(category, "Маленький холодильник", 15000, "Компактный холодильник для дачи"));
            }
            case "Стиральные машины" -> {
                templates.add(createProductRequest(category, "Стиральная машина", 25000, "Автоматическая стиральная машина"));
                templates.add(createProductRequest(category, "Стиральная машина с сушкой", 40000, "2 в 1: стирка и сушка"));
            }
            case "Посуда и кухонные принадлежности" -> {
                templates.add(createProductRequest(category, "Набор кастрюль", 5000, "Качественный набор кастрюль"));
                templates.add(createProductRequest(category, "Сковорода", 2000, "Антипригарная сковорода"));
                templates.add(createProductRequest(category, "Набор ножей", 3000, "Профессиональные кухонные ножи"));
            }
            case "Сад и огород" -> {
                templates.add(createProductRequest(category, "Газонокосилка", 12000, "Электрическая газонокосилка"));
                templates.add(createProductRequest(category, "Триммер", 5000, "Аккумуляторный триммер для травы"));
                templates.add(createProductRequest(category, "Семена", 300, "Набор семян овощей и цветов"));
            }

            // Спорт и отдых
            case "Беговые дорожки" ->
                    templates.add(createProductRequest(category, "Беговая дорожка", 45000, "Электрическая беговая дорожка для дома"));
            case "Велотренажёры" ->
                    templates.add(createProductRequest(category, "Велотренажёр", 25000, "Магнитный велотренажёр с компьютером"));
            case "Палатки" -> {
                templates.add(createProductRequest(category, "Палатка", 8000, "Туристическая палатка на 4 человека"));
                templates.add(createProductRequest(category, "Экспедиционная палатка", 15000, "Профессиональная палатка для сложных условий"));
            }
            case "Туризм и кемпинг" -> {
                templates.add(createProductRequest(category, "Рюкзак", 4000, "Туристический рюкзак 60л"));
                templates.add(createProductRequest(category, "Спальник", 3000, "Спальный мешок для трёх сезонов"));
                templates.add(createProductRequest(category, "Коврик туристический", 1500, "Изолирующий коврик для палатки"));
            }
            case "Велоспорт" -> {
                templates.add(createProductRequest(category, "Горный велосипед", 30000, "Велосипед с амортизацией"));
                templates.add(createProductRequest(category, "Шлем велосипедный", 2500, "Лёгкий защитный шлем"));
            }

            // Книги и медиа
            case "Классика" -> {
                templates.add(createProductRequest(category, "Война и мир", 800, "Лев Толстой. Бессмертная классика"));
                templates.add(createProductRequest(category, "Преступление и наказание", 700, "Фёдор Достоевский"));
                templates.add(createProductRequest(category, "Мастер и Маргарита", 750, "Михаил Булгаков"));
            }
            case "Фантастика и фэнтези" -> {
                templates.add(createProductRequest(category, "Гарри Поттер", 900, "Джоан Роулинг. Полный сборник"));
                templates.add(createProductRequest(category, "Властелин колец", 1200, "Джон Толкин. Трилогия"));
                templates.add(createProductRequest(category, "Дюна", 850, "Фрэнк Герберт"));
            }
            case "Детективы и триллеры" -> {
                templates.add(createProductRequest(category, "Убийство в Восточном экспрессе", 650, "Агата Кристи"));
                templates.add(createProductRequest(category, "Оно", 700, "Стивен Кинг"));
            }
            case "Научная литература" -> {
                templates.add(createProductRequest(category, "Краткая история времени", 800, "Стивен Хокинг"));
                templates.add(createProductRequest(category, "Думай медленно... решай быстро", 750, "Даниэль Канеман"));
            }
            case "Детские книги" -> {
                templates.add(createProductRequest(category, "Колобок", 300, "Русская народная сказка"));
                templates.add(createProductRequest(category, "Винни Пух", 500, "Алан Милн"));
                templates.add(createProductRequest(category, "Маленький принц", 450, "Антуан де Сент-Экзюпери"));
            }

            // Если категория не найдена в шаблонах - возвращается дефолтный шаблон
            default -> {
                templates.add(createProductRequest(category, "Стандартный товар", 1000, "Качественный товар по доступной цене"));
                templates.add(createProductRequest(category, "Премиум товар", 3000, "Товар повышенного качества"));
            }
        }

        return templates;
    }

    /**
     * Вспомогательный метод для создания запроса продукта
     * @param category категория
     * @param name название товара
     * @param price цена товара
     * @param description описание
     * @return CreateProductRequest
     */
    private CreateProductRequest createProductRequest(Category category, String name, int price, String description) {
        return CreateProductRequest.builder()
                .name(name)
                .description(description)
                .price(BigDecimal.valueOf(price))
                .categoryId(category.getId())
                .stockQuantity(generateRandomInt(10, 100))
                .images(null)
                .attributes(new HashMap<>())
                .build();
    }

    private CreateProductRequest getDefaultTemplate(Category category) {
        return createProductRequest(
                category, "Стандартный товар",
                1500, "Базовый товар для тестирования"
        );
    }

    /**
     * Выводит расширенную статистику с распределением по продавцам
     * @param products список продуктов
     * @param sellerCategoriesMap список продавцов
     */
    private void printStatistics(List<Product> products, Map<String, List<Category>> sellerCategoriesMap) {
        if (products.isEmpty()) return;

        Map<String, Long> productsBySeller = products.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getSeller().getLogin(),
                        Collectors.counting()
                ));

        infoLogger.info("\n=== СТАТИСТИКА СОЗДАННЫХ ПРОДУКТОВ ===");
        infoLogger.info("Всего продуктов: {}", products.size());
        infoLogger.info("\nРаспределение по продавцам:");

        productsBySeller.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    int categoriesCount = sellerCategoriesMap.getOrDefault(entry.getKey(), List.of()).size();
                    infoLogger.info("  {} -> {} продуктов ({} категорий)",
                            entry.getKey(), entry.getValue(), categoriesCount);
                });

        Map<String, Long> productsByCategory = products.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCategory().getName(),
                        Collectors.counting()
                ));

        infoLogger.info("\nТоп-10 категорий по количеству продуктов:");
        productsByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry ->
                        infoLogger.info("  {} -> {} продуктов", entry.getKey(), entry.getValue())
                );
    }

    public void clearProducts() {
        try {
            long countBefore = productService.count();
            productProcessorService.deleteAll();
            infoLogger.info("Удалено продуктов: {} (было: {})", productService.count(), countBefore);
        } catch (Exception e) {
            infoLogger.error("Ошибка при массовом удалении продуктов: {}", e.getMessage());
            throw new RuntimeException("Не удалось очистить таблицу продуктов", e);
        }
    }
}