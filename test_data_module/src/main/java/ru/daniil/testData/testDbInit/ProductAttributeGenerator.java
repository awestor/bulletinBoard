package ru.daniil.testData.testDbInit;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component
public class ProductAttributeGenerator {

    private static final Random random = new Random();

    public Map<String, String> generateAttributes(String categoryName, String productType) {
        Map<String, String> attributes = new HashMap<>();

        attributes.put("brand", getRandomBrand());
        attributes.put("country", getRandomCountry());
        attributes.put("warranty", getRandomWarranty());

        switch (categoryName) {
            case "Ноутбуки":
            case "Компьютеры и ноутбуки":
                attributes.put("processor", getRandomProcessor());
                attributes.put("ram", getRandomRam());
                attributes.put("storage", getRandomStorage());
                attributes.put("screen_size", getRandomScreenSize());
                attributes.put("os", getRandomOS());
                break;

            case "Смартфоны":
            case "Смартфоны и гаджеты":
                attributes.put("screen_diagonal", getRandomDiagonal());
                attributes.put("camera_mp", getRandomCamera());
                attributes.put("battery", getRandomBattery());
                attributes.put("memory", getRandomPhoneMemory());
                attributes.put("color", getRandomColor());
                break;

            case "Наушники":
                attributes.put("connection_type", random.nextBoolean() ? "Bluetooth" : "Wired");
                attributes.put("noise_cancellation", random.nextBoolean() ? "Yes" : "No");
                attributes.put("microphone", random.nextBoolean() ? "Yes" : "No");
                break;

            case "Телевизоры":
                attributes.put("screen_size", getRandomTVSize());
                attributes.put("resolution", getRandomResolution());
                attributes.put("smart_tv", random.nextBoolean() ? "Yes" : "No");
                break;

            case "Футболки и поло":
            case "Рубашки":
                attributes.put("material", getRandomMaterial());
                attributes.put("size", getRandomSize());
                attributes.put("color", getRandomColor());
                break;

            case "Джинсы и брюки":
                attributes.put("material", "Cotton");
                attributes.put("size", getRandomSize());
                attributes.put("fit", getRandomFit());
                attributes.put("color", getRandomColor());
                break;

            case "Кровати":
                attributes.put("size", getRandomBedSize());
                attributes.put("material", getRandomFurnitureMaterial());
                attributes.put("orthopedic_base", random.nextBoolean() ? "Yes" : "No");
                break;

            case "Диваны и кресла":
                attributes.put("type", random.nextBoolean() ? "Direct" : "Corner");
                attributes.put("material", getRandomFurnitureMaterial());
                attributes.put("transformation", random.nextBoolean() ? "Yes" : "No");
                break;

            case "Беговые дорожки":
                attributes.put("max_load", getRandomMaxLoad());
                attributes.put("motor_power", getRandomMotorPower());
                attributes.put("folding", random.nextBoolean() ? "Yes" : "No");
                break;

            case "Книги":
            case "Классика":
            case "Фантастика и фэнтези":
            case "Детективы и триллеры":
                attributes.put("author", getRandomAuthor());
                attributes.put("pages", String.valueOf(random.nextInt(300) + 100));
                attributes.put("publisher", getRandomPublisher());
                attributes.put("year", String.valueOf(random.nextInt(24) + 2000));
                attributes.put("cover_type", random.nextBoolean() ? "Hardcover" : "Paperback");
                break;

            default:
                // Атрибуты по умолчанию
                attributes.put("material", getRandomMaterial());
                attributes.put("color", getRandomColor());
                attributes.put("weight", String.valueOf(random.nextInt(5000) + 100) + "g");
                break;
        }

        if (productType.contains("Премиум")) {
            attributes.put("premium", "Yes");
            attributes.put("luxury_packaging", "Yes");
        } else if (productType.contains("Эконом")) {
            attributes.put("economy", "Yes");
            attributes.put("basic_model", "Yes");
        }

        return attributes;
    }

    /**
     * Генераторы случайных значений
     * для разнообразия продуктов
     */

    private String getRandomBrand() {
        String[] brands = {"Apple", "Samsung", "Sony", "LG", "Xiaomi", "Huawei", "Nike", "Adidas", "IKEA", "Bosch"};
        return brands[random.nextInt(brands.length)];
    }

    private String getRandomCountry() {
        String[] countries = {"China", "USA", "Germany", "Japan", "South Korea", "Vietnam", "Italy", "France"};
        return countries[random.nextInt(countries.length)];
    }

    private String getRandomWarranty() {
        String[] warranties = {"6 months", "1 year", "2 years", "3 years", "5 years"};
        return warranties[random.nextInt(warranties.length)];
    }

    private String getRandomProcessor() {
        String[] processors = {"Intel Core i3", "Intel Core i5", "Intel Core i7", "Intel Core i9", "AMD Ryzen 5", "AMD Ryzen 7", "AMD Ryzen 9"};
        return processors[random.nextInt(processors.length)];
    }

    private String getRandomRam() {
        String[] ram = {"4GB", "8GB", "16GB", "32GB", "64GB"};
        return ram[random.nextInt(ram.length)];
    }

    private String getRandomStorage() {
        String[] storage = {"256GB SSD", "512GB SSD", "1TB SSD", "2TB HDD", "1TB HDD + 256GB SSD"};
        return storage[random.nextInt(storage.length)];
    }

    private String getRandomScreenSize() {
        String[] sizes = {"13.3\"", "14\"", "15.6\"", "17.3\"", "16\""};
        return sizes[random.nextInt(sizes.length)];
    }

    private String getRandomOS() {
        String[] os = {"Windows 11", "Windows 10", "macOS", "Linux", "No OS"};
        return os[random.nextInt(os.length)];
    }

    private String getRandomDiagonal() {
        String[] diagonals = {"5.5\"", "6.1\"", "6.5\"", "6.7\"", "6.9\""};
        return diagonals[random.nextInt(diagonals.length)];
    }

    private String getRandomCamera() {
        String[] cameras = {"12MP", "48MP", "64MP", "108MP", "50MP"};
        return cameras[random.nextInt(cameras.length)];
    }

    private String getRandomBattery() {
        String[] batteries = {"3000mAh", "4000mAh", "4500mAh", "5000mAh", "6000mAh"};
        return batteries[random.nextInt(batteries.length)];
    }

    private String getRandomPhoneMemory() {
        String[] memory = {"64GB", "128GB", "256GB", "512GB", "1TB"};
        return memory[random.nextInt(memory.length)];
    }

    private String getRandomColor() {
        String[] colors = {"Black", "White", "Red", "Blue", "Green", "Yellow", "Silver", "Gold", "Space Gray"};
        return colors[random.nextInt(colors.length)];
    }

    private String getRandomTVSize() {
        String[] sizes = {"32\"", "43\"", "50\"", "55\"", "65\"", "75\"", "85\""};
        return sizes[random.nextInt(sizes.length)];
    }

    private String getRandomResolution() {
        String[] resolutions = {"HD Ready", "Full HD", "4K UHD", "8K UHD"};
        return resolutions[random.nextInt(resolutions.length)];
    }

    private String getRandomMaterial() {
        String[] materials = {"Cotton", "Polyester", "Wool", "Leather", "Nylon", "Linen", "Silk"};
        return materials[random.nextInt(materials.length)];
    }

    private String getRandomSize() {
        String[] sizes = {"XS", "S", "M", "L", "XL", "XXL"};
        return sizes[random.nextInt(sizes.length)];
    }

    private String getRandomFit() {
        String[] fits = {"Slim", "Regular", "Loose", "Skinny", "Straight"};
        return fits[random.nextInt(fits.length)];
    }

    private String getRandomBedSize() {
        String[] sizes = {"Single", "Double", "Queen", "King"};
        return sizes[random.nextInt(sizes.length)];
    }

    private String getRandomFurnitureMaterial() {
        String[] materials = {"Wood", "Metal", "Leather", "Fabric", "Plastic", "Glass"};
        return materials[random.nextInt(materials.length)];
    }

    private String getRandomMaxLoad() {
        String[] loads = {"100kg", "120kg", "150kg", "180kg", "200kg"};
        return loads[random.nextInt(loads.length)];
    }

    private String getRandomMotorPower() {
        String[] powers = {"1.5 HP", "2.0 HP", "2.5 HP", "3.0 HP", "3.5 HP"};
        return powers[random.nextInt(powers.length)];
    }

    private String getRandomAuthor() {
        String[] authors = {"Джордж Оруэлл", "Лев Толстой", "Фёдор Достоевский", "Джейн Остин", "Эрнест Хемингуэй",
                "Стивен Кинг", "Джоан Роулинг", "Джордж Мартин", "Агата Кристи", "Артур Конан Дойл"};
        return authors[random.nextInt(authors.length)];
    }

    private String getRandomPublisher() {
        String[] publishers = {"Эксмо", "АСТ", "Питер", "Манн, Иванов и Фербер", "Альпина Паблишер",
                "Penguin Books", "HarperCollins", "Simon & Schuster"};
        return publishers[random.nextInt(publishers.length)];
    }
}
