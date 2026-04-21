package ru.daniil.product.controller.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.exceptions.UserBlockedExeption;
import ru.daniil.core.request.CreateUpdateProductRequest;
import ru.daniil.core.sharedInterfaces.UserProvider;
import ru.daniil.product.mapper.ProductMapper;
import ru.daniil.product.service.product.ProductProcessorService;
import ru.daniil.product.service.product.ProductService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Api продуктов", description = "API для работы с продуктами")
public class ProductApiController {

    private final ProductService productService;
    private final ProductProcessorService productProcessorService;
    private final UserProvider userProvider;
    private final ProductMapper productMapper;

    public ProductApiController(ProductService productService,
                                ProductProcessorService productProcessorService,
                                UserProvider userProvider,
                                ProductMapper productMapper) {
        this.productService = productService;
        this.productProcessorService = productProcessorService;
        this.userProvider = userProvider;
        this.productMapper = productMapper;
    }

    @PostMapping("/create")
    @Operation(
            summary = "Создание продукта",
            description = "Создаёт 1 продукт по реквесту. "
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Продукт успешно создан",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Запрос был заполнен не корректно",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Пользователю заблокировали возможность выставлять товары на площадке",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера при создании продуктов",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> generateTestProducts(
            @Valid @RequestBody CreateUpdateProductRequest request) {
        JwtAuthenticationToken authentication = (JwtAuthenticationToken)
                SecurityContextHolder.getContext().getAuthentication();

        assert authentication != null;
        Jwt jwt = authentication.getToken();
        String email = jwt.getClaimAsString("email");

        Map<String, Object> response = new HashMap<>();
        try {
            User user = userProvider.getByEmail(email).orElseThrow(
                    () -> new NotFoundException("Пользователь не бул аутентифицирован в системе")
            );
            productProcessorService.create(request, user);

            response.put("message", "Продукт успешно создан");
            return ResponseEntity.ok(response);

        } catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (UserBlockedExeption ex){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (BadRequestException exe){
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/my")
    @Operation(
            summary = "Получение всех продуктов текущего пользователя",
            description = "Возвращает список всех продуктов, принадлежащих авторизованному пользователю, с пагинацией"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список продуктов успешно получен",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> getMyProducts(
            @Parameter(description = "Номер страницы (начиная с 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Количество элементов на странице")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Поле для сортировки (id, name, price, createdAt)")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Направление сортировки (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortDir) {

        Map<String, Object> response = new HashMap<>();
        try {
            JwtAuthenticationToken authentication = (JwtAuthenticationToken)
                    SecurityContextHolder.getContext().getAuthentication();

            assert authentication != null;
            Jwt jwt = authentication.getToken();
            String email = jwt.getClaimAsString("email");

            User user = userProvider.getByEmail(email).orElseThrow();


            Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<Product> products = productService.getBySeller(user.getId(), pageable);

            response.put("products", products.getContent().stream()
                    .map(productMapper::toResponse)
                    .collect(Collectors.toList()));
            response.put("currentPage", products.getNumber());
            response.put("totalItems", products.getTotalElements());
            response.put("totalPages", products.getTotalPages());
            response.put("hasNext", products.hasNext());
            response.put("hasPrevious", products.hasPrevious());

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e){
            response.put("message", "Пожалуйста авторизуйтесь и повторите попытку");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Обновление продукта",
            description = "Обновляет существующий продукт по его ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Продукт успешно обновлён",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Пользователю заблокирована возможность редактирования товаров",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Продукт не найден или не принадлежит текущему пользователю",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> updateProduct(
            @Parameter(description = "ID продукта", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CreateUpdateProductRequest request) {

        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = userProvider.getAuthUser();

            if (currentUser.isTradingBlocked()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Пользователю заблокирована возможность работы с товарами"));
            }

            Product product = productService.getById(id);

            if (!product.getSeller().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Изменять можно только свои товары"));
            }

            Product updatedProduct = productProcessorService.update(id, request);

            response.put("message", "Продукт успешно обновлён");
            response.put("product", productMapper.toResponse(updatedProduct));
            return ResponseEntity.ok(response);

        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Продукт не найден"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Ошибка при обновлении продукта: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{sku}")
    @Operation(
            summary = "Удаление продукта",
            description = "Удаляет продукт по его ID (только свои продукты)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Продукт успешно удалён",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Попытка удалить чужой продукт",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Продукт не найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> deleteProduct(
            @Parameter(description = "ID продукта", required = true)
            @PathVariable String sku) {

        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = userProvider.getAuthUser();
            Product product = productService.getBySku(sku);

            if (!product.getSeller().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Вы можете удалять только свои товары"));
            }

            productProcessorService.delete(product.getId());

            response.put("message", "Продукт успешно удалён");
            return ResponseEntity.ok(response);

        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Продукт не найден"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Ошибка при удалении продукта: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/{sku}/uploadImages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Загрузка изображений продукта",
            description = "Загружает одно или несколько изображений для продукта. " +
                    "Все изображения загружаются с isMain=false. " +
                    "Для назначения главного изображения используйте отдельный эндпоинт"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Изображения успешно загружены",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Продукт с указанным SKU не найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<List<String>> uploadImages(
            @Parameter(description = "Артикул продукта", required = true)
            @PathVariable String sku,

            @RequestPart("files")
            @ArraySchema(schema = @Schema(type = "string", format = "binary"))
            List<MultipartFile> files) {

        try {
            List<String> filenames = productProcessorService.addManyProductImages(sku, files);
            return ResponseEntity.ok(filenames);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Ошибка при загрузке изображений: " + e.getMessage()));
        }
    }
}
