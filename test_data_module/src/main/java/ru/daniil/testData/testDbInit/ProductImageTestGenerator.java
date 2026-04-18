package ru.daniil.testData.testDbInit;

import ie.briandouglas.placeholder_image.PlaceholderImage;
import ie.briandouglas.placeholder_image.PlaceholderImageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.other.SimpleMultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProductImageTestGenerator {

    private static final Logger methodLogger = LoggerFactory.getLogger("INFO-LOGGER");
    private static final int IMAGE_WIDTH = 400;
    private static final int IMAGE_HEIGHT = 400;

    /**
     * Генерирует список изображений для продукта
     * @param productName название продукта
     * @param categoryName категория (для разных цветов фона)
     * @param count количество изображений (1-3)
     */
    public List<MultipartFile> generateImagesForProduct(String productName, String categoryName, int count) {
        List<MultipartFile> images = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            try {
                BufferedImage image = createPlaceholderImage(productName, categoryName, i);
                MultipartFile multipartFile = convertToMultipartFile(image, productName, i);
                images.add(multipartFile);
            } catch (Exception e) {
                methodLogger.error("Ошибка генерации изображения для {}: {}", productName, e.getMessage());
            }
        }

        return images;
    }

    /**
     * Генерирует одно изображение через библиотеку PlaceholderImage
     */
    private BufferedImage createPlaceholderImage(String productName, String categoryName, int index) {
        Color backgroundColor = generateColorFromString(categoryName, index);

        // Текст на изображении, но возможно уберу и сделаю так, чтобы у всех изображений был 1 из n фонов,
        // где n - количество категорий
        String displayText = productName.length() > 20
                ? productName.substring(0, 17) + "..."
                : productName;

        if (index > 0) {
            displayText = displayText + " [" + (index + 1) + "]";
        }

        PlaceholderImage placeholderImage = new PlaceholderImage();
        PlaceholderImageOptions options = PlaceholderImageOptions.builder()
                .width(IMAGE_WIDTH)
                .height(IMAGE_HEIGHT)
                .text(displayText)
                .backgroundColor(backgroundColor)
                .font(new Font("Arial", Font.BOLD, 24))
                .build();

        return placeholderImage.generate(options);
    }

    /**
     * Генерирует цвет на основе названия категории и индекса
     * @return цвет в виде объекта Color
     */
    private Color generateColorFromString(String input, int index) {
        String hashSource = input + "_" + index;
        int hash = hashSource.hashCode();

        // Используется HSL
        // хотя сказать честно можно обойтись и просто RGB
        float hue = Math.abs(hash % 360) / 360.0f;

        // Насыщенность: 0.5-0.8
        float saturation = 0.5f + (Math.abs((hash >> 8) % 30) / 100.0f);

        // Яркость: 0.5-0.7
        float lightness = 0.5f + (Math.abs((hash >> 16) % 20) / 100.0f);

        return hslToRgb(hue, saturation, lightness);
    }

    /**
     * Конвертирует HSL в RGB
     * @param hue HSL значение
     * @param saturation насыщенность
     * @param lightness яркость
     * @return RGB цвет
     */
    private Color hslToRgb(float hue, float saturation, float lightness) {
        float chroma = (1 - Math.abs(2 * lightness - 1)) * saturation;
        float huePrime = hue * 6;
        float x = chroma * (1 - Math.abs(huePrime % 2 - 1));

        float r1, g1, b1;

        int hueSegment = (int) huePrime;
        switch (hueSegment) {
            case 0:
                r1 = chroma; g1 = x; b1 = 0;
                break;
            case 1:
                r1 = x; g1 = chroma; b1 = 0;
                break;
            case 2:
                r1 = 0; g1 = chroma; b1 = x;
                break;
            case 3:
                r1 = 0; g1 = x; b1 = chroma;
                break;
            case 4:
                r1 = x; g1 = 0; b1 = chroma;
                break;
            default:
                r1 = chroma; g1 = 0; b1 = x;
                break;
        }

        float m = lightness - chroma / 2;

        int r = Math.round((r1 + m) * 255);
        int g = Math.round((g1 + m) * 255);
        int b = Math.round((b1 + m) * 255);

        return new Color(r, g, b);
    }

    /**
     * Конвертирует BufferedImage в MultipartFile
     * @param image BufferedImage изображение
     * @param productName название продукта
     * @param index индекс
     * @return файл в формате MultipartFile
     */
    private MultipartFile convertToMultipartFile(BufferedImage image, String productName, int index){
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            String filename = sanitizeFilename(productName) + "_" + (index + 1) + ".png";

            return new SimpleMultipartFile(
                    "images",
                    filename,
                    "image/png",
                    imageBytes
            );
        } catch (IOException e){
            throw new RuntimeException("Ошибка преобразования файла");
        }
    }

    /**
     * Очищает имя файла от недопустимых символов
     * @param name название продукта
     * @return название файла
     */
    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Zа-яА-Я0-9]", "_")
                .substring(0, Math.min(50, name.length()));
    }
}