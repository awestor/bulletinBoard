package ru.daniil.image.imageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.image.repository.UserImageRepository;
import ru.daniil.image.service.user.UserImageServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserImageServiceImplTest {

    @Mock
    private UserImageRepository userImageRepository;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private UserImageServiceImpl userImageService;

    @TempDir
    Path tempDir;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_FILENAME = "test-avatar.jpg";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userImageService, "uploadDir", tempDir.toString());
    }

    @Test
    void saveImage_WithValidFile_ShouldSaveAndReturnPath() throws IOException {
        when(mockFile.getOriginalFilename()).thenReturn("avatar.jpg");
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[10]));

        doNothing().when(userImageRepository).saveImage(anyString(), eq(TEST_EMAIL));

        String result = userImageService.saveImage(mockFile, TEST_EMAIL);

        assertNotNull(result);
        assertTrue(result.startsWith(tempDir.toString()));

        Path savedFile = Path.of(result);
        assertTrue(Files.exists(savedFile));

        verify(userImageRepository).saveImage(anyString(), eq(TEST_EMAIL));
    }

    @Test
    void saveImage_WhenIOExceptionOccurs_ShouldThrowRuntimeException() throws IOException {
        when(mockFile.getOriginalFilename()).thenReturn("avatar.jpg");
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getInputStream()).thenThrow(new IOException("Stream error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userImageService.saveImage(mockFile, TEST_EMAIL));

        assertTrue(exception.getMessage().contains("Возникла проблема при сохранении изображения"));

        verify(userImageRepository, never()).saveImage(any(), any());
    }

    @Test
    void saveImage_WhenDirectoryDoesNotExist_ShouldCreateDirectory() throws IOException {
        Path nonExistentDir = tempDir.resolve("uploads/users");
        ReflectionTestUtils.setField(userImageService, "uploadDir", nonExistentDir.toString());

        when(mockFile.getOriginalFilename()).thenReturn("avatar.jpg");
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[10]));

        doNothing().when(userImageRepository).saveImage(anyString(), eq(TEST_EMAIL));

        String result = userImageService.saveImage(mockFile, TEST_EMAIL);

        assertTrue(Files.exists(nonExistentDir));
        assertNotNull(result);
    }

    @Test
    void completePath_WithValidFilename_ShouldReturnFullPath() throws IOException {
        Path filePath = tempDir.resolve(TEST_FILENAME);
        Files.createFile(filePath);

        String result = userImageService.completePath(TEST_FILENAME);

        assertNotNull(result);
        assertEquals(filePath.toString(), result);
    }

    @Test
    void completePath_WithNullFilename_ShouldReturnNull() {
        String result = userImageService.completePath(null);

        assertNull(result);
    }

    @Test
    void completePath_WhenFileDoesNotExist_ShouldThrowBadCredentialsException() {
        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> userImageService.completePath("nonexistent.jpg"));

        assertTrue(exception.getMessage().contains("Изображение пользовательского аватара не найдено"));
    }

    @Test
    void completePath_WhenFilenameIsEmpty_ShouldReturnNull() {
        String name = userImageService.completePath("");
        assertNull(name);
    }

    @Test
    void deleteUserAvatar_WithValidData_ShouldDeleteImageAndClearRepository() throws IOException {
        Path filePath = tempDir.resolve(TEST_FILENAME);
        Files.createFile(filePath);

        doNothing().when(userImageRepository).clearImage(TEST_EMAIL);

        userImageService.deleteUserAvatar(TEST_EMAIL, TEST_FILENAME);

        assertFalse(Files.exists(filePath));
        verify(userImageRepository).clearImage(TEST_EMAIL);
    }

    @Test
    void deleteUserAvatar_WhenFileDoesNotExist_ShouldStillClearRepository() {
        doNothing().when(userImageRepository).clearImage(TEST_EMAIL);

        assertDoesNotThrow(() -> userImageService.deleteUserAvatar(TEST_EMAIL, TEST_FILENAME));

        verify(userImageRepository).clearImage(TEST_EMAIL);
    }

    @Test
    void deleteUserAvatar_WhenFileDeleteFails_ShouldThrowExceptionAndNotClearRepository() throws IOException {
        Path filePath = tempDir.resolve(TEST_FILENAME);
        Files.createFile(filePath);

        filePath.toFile().setWritable(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userImageService.deleteUserAvatar(TEST_EMAIL, TEST_FILENAME));

        assertTrue(exception.getMessage().contains("Не удалось удалить файл изображения"));

        verify(userImageRepository, never()).clearImage(TEST_EMAIL);
    }

    @Test
    void saveImage_CacheableAnnotation_ShouldBePresent() throws NoSuchMethodException {
        var method = UserImageServiceImpl.class.getMethod("saveImage", MultipartFile.class, String.class);
        assertTrue(method.isAnnotationPresent(Cacheable.class));

        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        assertEquals("userImages", cacheable.value()[0]);
        assertEquals("#fileName", cacheable.key());
    }

    @Test
    void completePath_CacheableAnnotation_ShouldBePresent() throws NoSuchMethodException {
        var method = UserImageServiceImpl.class.getMethod("completePath", String.class);
        assertTrue(method.isAnnotationPresent(Cacheable.class));

        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        assertEquals("userImages", cacheable.value()[0]);
        assertEquals("#fileName", cacheable.key());
    }

    @Test
    void deleteUserAvatar_CacheEvictAnnotation_ShouldBePresent() throws NoSuchMethodException {
        var method = UserImageServiceImpl.class.getMethod("deleteUserAvatar", String.class, String.class);
        assertTrue(method.isAnnotationPresent(CacheEvict.class));

        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);
        assertEquals("userImages", cacheEvict.value()[0]);
        assertEquals("#fileName", cacheEvict.key());
    }

    @Test
    void saveImage_ShouldGenerateUniqueFilename() throws IOException {
        when(mockFile.getOriginalFilename()).thenReturn("avatar.jpg");
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[10]));

        doNothing().when(userImageRepository).saveImage(anyString(), eq(TEST_EMAIL));

        String result1 = userImageService.saveImage(mockFile, TEST_EMAIL);
        String result2 = userImageService.saveImage(mockFile, TEST_EMAIL);

        assertNotEquals(result1, result2);

        verify(userImageRepository, times(2)).saveImage(anyString(), eq(TEST_EMAIL));
    }
}
