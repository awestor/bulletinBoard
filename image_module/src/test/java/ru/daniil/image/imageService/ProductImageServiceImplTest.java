package ru.daniil.image.imageService;

import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.product.ProductImage;
import ru.daniil.image.repository.ProductImageRepository;
import ru.daniil.image.service.PrepareImageService;
import ru.daniil.image.service.product.ProductImageServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceImplTest {

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private PrepareImageService prepareImageService;

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
        productImage.setName("test.jpg");
        productImage.setIsMain(false);
    }

    @Test
    void saveImage_WithValidFile_ShouldSaveAndReturnFileName() throws IOException {
        String expectedFileName = "generated-uuid.jpg";
        when(prepareImageService.getFileUploadedName(mockFile)).thenReturn(expectedFileName);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[10]));

        String result = productImageService.saveImage(mockFile);

        assertNotNull(result);
        assertEquals(expectedFileName, result);
        assertTrue(Files.exists(tempDir.resolve(expectedFileName)));
        verify(prepareImageService).getFileUploadedName(mockFile);
    }

    @Test
    void saveImage_WhenIOExceptionOccurs_ShouldThrowRuntimeException() throws IOException {
        String expectedFileName = "test.jpg";
        when(prepareImageService.getFileUploadedName(mockFile)).thenReturn(expectedFileName);
        when(mockFile.getInputStream()).thenThrow(new IOException("Stream error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productImageService.saveImage(mockFile));

        assertTrue(exception.getMessage().contains("Возникла проблема при сохранении изображения"));
        assertFalse(Files.exists(tempDir.resolve(expectedFileName)));
    }

    @Test
    void completePath_WithValidFilename_ShouldReturnFullPath() throws IOException {
        String filename = "existing.jpg";
        Path filePath = tempDir.resolve(filename);
        Files.createFile(filePath);

        String result = productImageService.completePath(filename);

        assertNotNull(result);
        assertEquals(filePath.toString(), result);
    }

    @Test
    void completePath_WithNullFilename_ShouldReturnNull() {
        String result = productImageService.completePath(null);

        assertNull(result);
    }

    @Test
    void completePath_WhenFileDoesNotExist_ShouldThrowNotFoundException() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> productImageService.completePath("nonexistent.jpg"));

        assertTrue(exception.getMessage().contains("Изображение продукта не найдено"));
    }

    @Test
    void deleteImage_WithValidFilename_ShouldDeleteImageAndFile() throws IOException {
        String filename = "test.jpg";
        Path filePath = tempDir.resolve(filename);
        Files.createFile(filePath);

        when(productImageRepository.findByName(filename)).thenReturn(Optional.of(productImage));
        doNothing().when(productImageRepository).delete(productImage);

        productImageService.deleteImage(filename);

        verify(productImageRepository).findByName(filename);
        verify(productImageRepository).delete(productImage);
        assertFalse(Files.exists(filePath));
    }

    @Test
    void deleteImage_WhenImageNotFoundInDatabase_ShouldThrowNotFoundException() {
        String filename = "nonexistent.jpg";
        when(productImageRepository.findByName(filename)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> productImageService.deleteImage(filename));

        assertEquals("Данного изображения не найдено в БД", exception.getMessage());
        verify(productImageRepository, never()).delete(any());
    }

    @Test
    void deleteImage_WhenFileDeleteFails_ShouldStillDeleteFromDatabase() throws IOException {
        String filename = "test.jpg";
        Path filePath = tempDir.resolve(filename);
        Files.createFile(filePath);

        filePath.toFile().setWritable(false);

        when(productImageRepository.findByName(filename)).thenReturn(Optional.of(productImage));
        doNothing().when(productImageRepository).delete(productImage);

        assertDoesNotThrow(() -> productImageService.deleteImage(filename));

        verify(productImageRepository).delete(productImage);
    }

    @Test
    void deleteAllImages_ShouldDeleteAllImagesAndFiles() throws IOException {
        Long productId = 1L;
        String filename1 = "image1.jpg";
        String filename2 = "image2.jpg";

        Path filePath1 = tempDir.resolve(filename1);
        Path filePath2 = tempDir.resolve(filename2);
        Files.createFile(filePath1);
        Files.createFile(filePath2);

        ProductImage image1 = new ProductImage();
        image1.setName(filename1);
        ProductImage image2 = new ProductImage();
        image2.setName(filename2);

        List<ProductImage> images = Arrays.asList(image1, image2);

        when(productImageRepository.findByProductId(productId)).thenReturn(images);
        doNothing().when(productImageRepository).deleteByProductId(productId);

        productImageService.deleteAllImages(productId);

        verify(productImageRepository).findByProductId(productId);
        verify(productImageRepository).deleteByProductId(productId);
        assertFalse(Files.exists(filePath1));
        assertFalse(Files.exists(filePath2));
    }

    @Test
    void deleteAllImages_WithEmptyList_ShouldNotDeleteFiles() {
        Long productId = 1L;
        when(productImageRepository.findByProductId(productId)).thenReturn(List.of());
        doNothing().when(productImageRepository).deleteByProductId(productId);

        productImageService.deleteAllImages(productId);

        verify(productImageRepository).findByProductId(productId);
        verify(productImageRepository).deleteByProductId(productId);
    }

    @Test
    void deleteAllInButch_ShouldDeleteAllFromDatabaseAndDirectory() throws IOException {
        String filename1 = "file1.jpg";
        String filename2 = "file2.jpg";

        Files.createFile(tempDir.resolve(filename1));
        Files.createFile(tempDir.resolve(filename2));

        doNothing().when(productImageRepository).deleteAllInBatch();

        assertDoesNotThrow(() -> productImageService.deleteAllinButch());

        verify(productImageRepository).deleteAllInBatch();
        assertFalse(Files.exists(tempDir.resolve(filename1)));
        assertFalse(Files.exists(tempDir.resolve(filename2)));
    }

    @Test
    void deleteAllInButch_WhenDirectoryDoesNotExist_ShouldNotThrowException() {
        Path nonExistentDir = tempDir.resolve("nonexistent");
        ReflectionTestUtils.setField(productImageService, "uploadDir", nonExistentDir.toString());

        doNothing().when(productImageRepository).deleteAllInBatch();

        assertDoesNotThrow(() -> productImageService.deleteAllinButch());

        verify(productImageRepository).deleteAllInBatch();
    }

    @Test
    void deleteAllInButch_WhenIOExceptionOccurs_ShouldStillDeleteFromDatabase() throws IOException {
        String filename = "readonly.jpg";
        Path filePath = tempDir.resolve(filename);
        Files.createFile(filePath);
        filePath.toFile().setWritable(false);

        doNothing().when(productImageRepository).deleteAllInBatch();

        assertDoesNotThrow(() -> productImageService.deleteAllinButch());

        verify(productImageRepository).deleteAllInBatch();
    }

    @Test
    void save_ShouldSaveProductImage() {
        productImageService.save(productImage);

        verify(productImageRepository).save(productImage);
    }

    @Test
    void setMain_ShouldUpdateMainFlag_WhenFileExists() {
        String fileName = "test-image.jpg";

        doNothing().when(productImageRepository).unsetOtherMainImages(fileName);
        doNothing().when(productImageRepository).setMainByFileName(fileName);

        productImageService.setMain(fileName);

        InOrder inOrder = inOrder(productImageRepository);
        inOrder.verify(productImageRepository).unsetOtherMainImages(fileName);
        inOrder.verify(productImageRepository).setMainByFileName(fileName);
        verifyNoMoreInteractions(productImageRepository);
    }

    @Test
    void setMain_ShouldPropagateException_WhenUnsetFails() {
        String fileName = "test-image.jpg";

        doThrow(new DataIntegrityViolationException("DB error"))
                .when(productImageRepository).unsetOtherMainImages(fileName);

        assertThrows(DataIntegrityViolationException.class, () ->
                productImageService.setMain(fileName)
        );

        verify(productImageRepository).unsetOtherMainImages(fileName);
        verify(productImageRepository, never()).setMainByFileName(anyString());
    }
}