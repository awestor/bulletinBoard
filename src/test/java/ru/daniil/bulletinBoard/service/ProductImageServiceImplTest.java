package ru.daniil.bulletinBoard.service;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.bulletinBoard.entity.base.product.ProductImage;
import ru.daniil.bulletinBoard.repository.product.ProductImageRepository;
import ru.daniil.bulletinBoard.service.product.image.ProductImageServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceImplTest {

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private ProductImageServiceImpl productImageService;

    @TempDir
    Path tempDir;

    private ProductImage productImage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productImageService, "uploadDir", tempDir.toString());

        productImage = new ProductImage();
        productImage.setId(1L);
        productImage.setPath("test.jpg");
        productImage.setIsMain(false);
    }

    @Test
    void saveImage_WithValidFile_ShouldSaveAndReturnFileName() throws IOException {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(5L * 1024 * 1024);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[10]));

        String result = productImageService.saveImage(mockFile);

        assertNotNull(result);
        assertTrue(result.endsWith(".jpg"));
        assertTrue(Files.exists(tempDir.resolve(result)));
    }

    @Test
    void saveImage_WithNullFile_ShouldThrowException() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> productImageService.saveImage(null));

        assertEquals("Необходимо загрузить хотя бы одно изображение", exception.getMessage());
    }

    @Test
    void saveImage_WithEmptyFile_ShouldThrowException() {
        when(mockFile.isEmpty()).thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> productImageService.saveImage(mockFile));

        assertEquals("Необходимо загрузить хотя бы одно изображение", exception.getMessage());
    }

    @Test
    void saveImage_WithFileTooLarge_ShouldThrowException() {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(15L * 1024 * 1024);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> productImageService.saveImage(mockFile));

        assertTrue(exception.getMessage().contains("превышает максимальный размер"));
    }

    @Test
    void saveImage_WithUnsupportedContentType_ShouldThrowException() {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(5L * 1024 * 1024);
        when(mockFile.getContentType()).thenReturn("image/bmp");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> productImageService.saveImage(mockFile));

        assertTrue(exception.getMessage().contains("неподдерживаемый тип"));
    }

    @Test
    void saveImage_WithFileWithoutExtension_ShouldSaveWithUuidOnly() throws IOException {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(5L * 1024 * 1024);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getOriginalFilename()).thenReturn("test");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[10]));

        String result = productImageService.saveImage(mockFile);

        assertNotNull(result);
        assertFalse(result.contains("."));
    }

    @Test
    void saveImage_WhenDirectoryDoesNotExist_ShouldCreateDirectory() throws IOException {
        Path newPath = tempDir.resolve("subdir/newdir");
        ReflectionTestUtils.setField(productImageService, "uploadDir", newPath.toString());

        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(5L * 1024 * 1024);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[10]));

        String result = productImageService.saveImage(mockFile);

        assertNotNull(result);
        assertTrue(Files.exists(newPath));
    }

    @Test
    void saveImage_WhenIOExceptionOccurs_ShouldPropagateException() throws IOException {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(5L * 1024 * 1024);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getInputStream()).thenThrow(new IOException("Stream error"));

        assertThrows(IOException.class, () -> productImageService.saveImage(mockFile));
    }

    @Test
    void save_ShouldSaveProductImage() {
        productImageService.save(productImage);

        verify(productImageRepository).save(productImage);
    }
}