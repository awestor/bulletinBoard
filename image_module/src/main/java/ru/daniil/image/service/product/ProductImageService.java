package ru.daniil.image.service.product;

import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.product.ProductImage;

import java.util.List;

public interface ProductImageService {

    List<String> getProductImages(String sku);
    /**
     * Сохраняет изображение на локальном устройстве
     * @param file MultipartFile файл
     * @return название, что было использовано для сохранения файла
     * @throws RuntimeException ошибка в случае неудачного сохранения
     */
    String saveImage(MultipartFile file) throws RuntimeException;

    /**
     * Дополняет название из БД чтобы можно было использовать на фронте
     * @param filename название файла
     * @return полный путь до файла или NotFoundException
     */
    String completePath(String filename);

    /**
     * Удаляет один файл из локального хранилища и БД для одного продукта
     * @param filename название изображения
     */
    void deleteImage(String filename);

    /**
     * Удаляет все файлы из локального хранилища и БД для одного продукта
     * @param productId название продукта
     */
    void deleteAllImages(Long productId);

    /**
     * Также, как и у продуктов - очень опасный метод, что удалит все сохранённые изображения в БД
     * Не знаю как изолировать все эти опасные методы по этому они в соответствующих сервисах
     */
    void deleteAllinButch();

    /**
     * Сохраняет запись о файле в БД
     * @param productImage название изображения
     */
    void save(ProductImage productImage);

    /**
     * Устанавливает флаг главенства на одном файле
     * @param fileName название файла
     */
    void setMain(String fileName);
}
