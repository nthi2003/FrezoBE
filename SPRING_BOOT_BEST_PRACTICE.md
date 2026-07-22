# Frezo Backend — Spring Boot Best Practice

> Chuẩn Spring Boot / JPA / MapStruct / Validation / Exception / Testing cho FrezoBE.
> Đọc **cùng** [AI_BACKEND_ENGINEERING_GUIDE.md](./AI_BACKEND_ENGINEERING_GUIDE.md) và [DATABASE_STANDARD.md](./DATABASE_STANDARD.md).

Stack (bắt buộc bám sát `pom.xml`):

- Java **21**
- Spring Boot **3.2.0**
- Hibernate **6.3.1.Final**
- MapStruct **1.5.5.Final**
- Lombok **1.18.30**
- SpringDoc OpenAPI **2.5.0**
- JJWT **0.11+**
- Caffeine, EasyExcel 3.3.3, Apache Tika 2.9.2, Bucket4j

---

## 1. Dependency Injection

### 1.1 Constructor Injection ONLY

```java
// ✅ Good
@Service
@RequiredArgsConstructor       // Lombok tạo constructor với các field final
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentMapper departmentMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ...
}
```

```java
// ❌ Bad — field injection
@Service
public class DepartmentServiceImpl {
    @Autowired private DepartmentRepository departmentRepository;   // ❌
    @Autowired private DepartmentMapper departmentMapper;           // ❌
}
```

**Vì sao bắt buộc constructor injection:**
- Test được không cần Spring context (`new DepartmentServiceImpl(mockRepo, mockMapper)`)
- Field bất biến (`final`) — thread-safe
- Cyclic dependency lộ ra compile-time (Spring 3.x throw error)
- Không cần `@Autowired` (Spring 4.3+ tự infer khi có 1 constructor)

### 1.2 Setter injection — chỉ khi bất khả kháng

Rare. Ví dụ: circular dependency chưa refactor được (nhưng nên refactor thành 3 class hoặc event).

### 1.3 `@ConfigurationProperties` > `@Value`

**Rule:** Nếu > 3 property cùng prefix → gom `@ConfigurationProperties`.

```java
// ❌ Bad — @Value scatter
@Component
public class JwtProvider {
    @Value("${app.jwt.secret}") private String secret;
    @Value("${app.jwt.expiration}") private long expiration;
    @Value("${app.jwt.refresh-expiration}") private long refreshExpiration;
    @Value("${app.jwt.issuer}") private String issuer;
    @Value("${app.jwt.audience}") private String audience;
}
```

```java
// ✅ Good — @ConfigurationProperties
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter @Setter
@Validated
public class JwtProperties {
    @NotBlank private String secret;
    @Min(60000) private long expiration = 3600000L;         // 1h default
    @Min(60000) private long refreshExpiration = 604800000L; // 7d default
    @NotBlank private String issuer = "frezo";
    private String audience = "frezo-app";
}

@Service
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtProperties props;

    public String generate(...) {
        Jwts.builder().signWith(SignatureAlgorithm.HS512, props.getSecret()) ...
    }
}
```

Thêm `spring-boot-configuration-processor` deps để IDE autocomplete YAML.

### 1.4 Bean lifecycle

- `@Component` chung
- `@Service` cho business logic
- `@Repository` cho JPA repository (đã kế thừa từ `JpaRepository`, không cần lặp)
- `@RestController` cho REST controller
- `@Configuration` cho config class
- `@Aspect` `@Component` cho AOP

Cấm:
- ❌ `static` field trong bean
- ❌ Mutable shared state trong bean (`private List<X> cache = new ArrayList<>();` → dùng `Cache` bean hoặc `ConcurrentHashMap`)
- ❌ `new` service bên trong service khác (dùng DI)

---

## 2. Configuration

### 2.1 File structure

```
module-server/src/main/resources/
├── application.yml            # config chung (bootstrap)
├── application-dev.yml        # dev override
├── application-staging.yml    # staging override
├── application-prod.yml       # prod override
├── i18n/
│   ├── messages.properties
│   └── messages_vi.properties
├── logback-spring.xml         # log format có MDC/traceId
└── bootstrap.yml              # Spring Cloud Config / Consul (đã dùng Consul)
```

**Cấm:**
- ❌ Hardcode secret trong file YAML commit git (dùng `${JWT_SECRET}` env var)
- ❌ Config theo host (`if (host == "prod")`) — dùng profile
- ❌ Đọc config từ file bên ngoài path tuỳ ý (chỉ dùng Spring config binding)

### 2.2 Profile

- `dev`: local dev, DEBUG log, Swagger public, mock external service
- `staging`: production-like, INFO log, Swagger basic auth
- `prod`: strict, WARN log, Swagger disabled, monitoring enabled

Bật: `-Dspring.profiles.active=prod` hoặc env `SPRING_PROFILES_ACTIVE=prod`.

### 2.3 Consul (đã dùng)

- Config động qua Consul KV store
- Health check `/actuator/health`
- Không bỏ Spring Boot config hoàn toàn — Consul override chứ không thay thế

---

## 3. JPA / Hibernate

### 3.1 Entity

**Bắt buộc:**
- Extend `BaseEntity` (`id` UUID String + audit + `isDeleted`)
- `@Entity` + `@Table(name = "snake_case_plural")` — bảng số nhiều
- `@Column` chỉ định `nullable`, `length`, `columnDefinition` khi cần
- KHÔNG dùng `@Data` (equals/hashCode với lazy loading + auto-generated ID → nguy hiểm). Dùng `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`.

```java
@Entity
@Table(name = "departments",
    indexes = {
        @Index(name = "idx_departments_organization_id", columnList = "organization_id"),
        @Index(name = "idx_departments_parent_id", columnList = "parent_id"),
        @Index(name = "idx_departments_code", columnList = "code")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_departments_org_code", columnNames = {"organization_id", "code"})
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Department extends BaseEntity {

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DepartmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Person manager;

    @Version                           // optimistic locking khi có concurrent edit
    @Column(name = "version", nullable = false)
    private Long version;
}
```

### 3.2 Fetch Type — LAZY mặc định

| Relation | Default JPA | Bắt buộc dùng |
|----------|-------------|---------------|
| `@ManyToOne` | EAGER | **LAZY** |
| `@OneToOne` | EAGER | **LAZY** |
| `@OneToMany` | LAZY | LAZY (đã đúng) |
| `@ManyToMany` | LAZY | LAZY (đã đúng) |

**Cấm `FetchType.EAGER` toàn cục.** Khi cần load relation → dùng `@EntityGraph` per-query hoặc `JOIN FETCH` trong JPQL.

### 3.3 Query — 3 cấp độ

**Cấp 1: Derived query** (đơn giản)
```java
Optional<Department> findByIdAndIsDeletedFalse(String id);
boolean existsByOrganizationIdAndCode(String orgId, String code);
List<Department> findByStatusIn(Collection<DepartmentStatus> statuses);
```

**Cấp 2: `@Query` JPQL** (query cố định phức tạp hoặc cần projection)
```java
@Query("""
    select new com.frezo.qtht.dto.response.DepartmentResponse(
        d.id, d.code, d.name, o.name, p.name, m.name, d.status
    )
    from Department d
    left join d.organization o
    left join d.parent p
    left join d.manager m
    where d.isDeleted = false
      and (:keyword is null or lower(d.name) like lower(concat('%', :keyword, '%')))
      and (:status is null or d.status = :status)
""")
Page<DepartmentResponse> searchProjection(
    @Param("keyword") String keyword,
    @Param("status") DepartmentStatus status,
    Pageable pageable
);
```

**Cấp 3: Specification** (filter động, nhiều điều kiện optional)
```java
public class DepartmentSpec {
    public static Specification<Department> notDeleted() {
        return (root, q, cb) -> cb.isFalse(root.get("isDeleted"));
    }
    public static Specification<Department> keyword(String kw) {
        if (!StringUtils.hasText(kw)) return null;
        String like = "%" + kw.toLowerCase() + "%";
        return (root, q, cb) -> cb.or(
            cb.like(cb.lower(root.get("name")), like),
            cb.like(cb.lower(root.get("code")), like)
        );
    }
    public static Specification<Department> status(DepartmentStatus s) {
        return s == null ? null : (root, q, cb) -> cb.equal(root.get("status"), s);
    }
}

// Service
Specification<Department> spec = Specification
    .where(DepartmentSpec.notDeleted())
    .and(DepartmentSpec.keyword(filter.getKeyword()))
    .and(DepartmentSpec.status(filter.getStatus()));
Page<Department> page = repo.findAll(spec, filter.toPageable());
```

**Native SQL — chỉ khi JPQL không làm được** (recursive CTE, window function, JSONB operator).

### 3.4 EntityGraph — chống N+1

```java
public interface DepartmentRepository extends JpaRepository<Department, String>, JpaSpecificationExecutor<Department> {

    @EntityGraph(attributePaths = {"organization", "parent", "manager"})
    Optional<Department> findWithRelationsById(String id);

    @EntityGraph(attributePaths = {"organization", "parent", "manager"})
    @Override
    Page<Department> findAll(Specification<Department> spec, Pageable pageable);
}
```

**Cấu hình phát hiện N+1 dev:**
```yaml
# application-dev.yml
spring:
  jpa:
    properties:
      hibernate:
        session:
          events:
            log:
              LOG_QUERIES_SLOWER_THAN_MS: 100
```

Dùng thư viện `hibernate-types` hoặc `hypersistence-utils` bật `n+1 detector` trong test.

### 3.5 Batch operations

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
        batch_versioned_data: true
```

Service:
```java
@Transactional
public void bulkCreate(List<CreateDepartmentRequest> reqs) {
    List<Department> entities = reqs.stream().map(mapper::toEntity).toList();
    // Chia chunk 50 để Hibernate flush + clear session
    Lists.partition(entities, 50).forEach(chunk -> {
        repo.saveAll(chunk);
        em.flush();
        em.clear();
    });
}
```

### 3.6 Projection

**Interface Projection** (nhanh gọn, không cần constructor):
```java
public interface DepartmentSlim {
    String getId();
    String getName();
    String getCode();
}

@Query("select d.id as id, d.name as name, d.code as code from Department d where d.isDeleted = false")
List<DepartmentSlim> findAllSlim();
```

**DTO Projection** (constructor JPQL):
```java
@Query("select new com.frezo.qtht.dto.response.ComboboxResponse(d.id, d.name) from Department d where ...")
List<ComboboxResponse> findComboboxes();
```

**Chọn:** Interface cho query nhỏ, DTO cho response chính thức của API.

### 3.7 Cascade

- `CascadeType.ALL` **chỉ** khi child KHÔNG có ý nghĩa độc lập (composition, không phải aggregation). Ví dụ: `Order` cascade tới `OrderLine`.
- Cấm `CascadeType.ALL` cho `@ManyToOne` hướng ngược (dễ delete nhầm parent).
- `orphanRemoval = true` — dùng cẩn thận, chỉ khi chắc chắn.

### 3.8 Cấm

- ❌ `spring.jpa.hibernate.ddl-auto: update` production → chuyển Flyway
- ❌ `spring.jpa.open-in-view: true` (mặc định Spring Boot bật — nên tắt, gây LazyInitException ẩn)
- ❌ `entityManager.createQuery(...).getResultList()` gọi trực tiếp trong service (dùng Repository)
- ❌ Trả `Entity` ra ngoài Service (chỉ trả DTO)
- ❌ Modify entity không trong transaction (dirty check không chạy)

---

## 4. Transaction

### 4.1 Rule

| Layer | `@Transactional` |
|-------|------------------|
| Controller | ❌ Không |
| Service (public method) | ✅ Có |
| Repository | ❌ Không (kế thừa từ Service) |

### 4.2 Cấu hình chi tiết

```java
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    @Override
    @Transactional(readOnly = true)                                      // ✅ query
    public DepartmentDetailResponse detail(String id) { ... }

    @Override
    @Transactional                                                       // ✅ write default
    public DepartmentResponse create(CreateDepartmentRequest req) { ... }

    @Override
    @Transactional(rollbackFor = Exception.class)                        // ✅ rollback cả checked exception
    public void bulkImport(MultipartFile file) throws IOException { ... }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,               // ✅ tách transaction cho audit
                   noRollbackFor = AuditException.class)
    public void writeAudit(...) { ... }
}
```

### 4.3 Rollback rules

- Mặc định: rollback khi **RuntimeException** hoặc **Error**, KHÔNG rollback checked exception
- `AppException extends RuntimeException` → rollback tự động ✅
- Nếu method throw checked exception (IOException, SQLException...) → thêm `rollbackFor = Exception.class`

### 4.4 Cấm

- ❌ `@Transactional` trên method `private` (Spring proxy không intercept)
- ❌ Self-invocation: `this.otherMethod()` không kích hoạt proxy (dùng `AopContext.currentProxy()` hoặc tách class)
- ❌ Long-running transaction (> 5s) — sẽ khóa row lâu, deadlock
- ❌ Gọi external HTTP trong transaction (giữ DB connection open trong khi chờ network)
- ❌ `@Transactional(readOnly = true)` cho write → data không persist

---

## 5. Validation

### 5.1 Bean Validation trong DTO

```java
public record CreateDepartmentRequest(
    @NotBlank(message = "{validation.department.code.required}")
    @Size(min = 2, max = 32, message = "{validation.department.code.size}")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "{validation.department.code.pattern}")
    String code,

    @NotBlank(message = "{validation.department.name.required}")
    @Size(max = 255)
    String name,

    @NotNull(message = "{validation.department.organizationId.required}")
    String organizationId,

    String parentId,       // optional

    String managerId,      // optional

    @NotNull
    DepartmentStatus status
) {}
```

**Bắt buộc:**
- Message qua `{key}` → đọc `ValidationMessages.properties` / `messages*.properties` (i18n)
- KHÔNG hardcode tiếng Việt trong annotation `@NotBlank(message = "Không được để trống")`
- `record` khi DTO chỉ có field + validation (Java 21)
- Custom validator (`@Constraint`) cho rule đặc biệt (CCCD 12 số VN, số điện thoại VN, mã số thuế)

### 5.2 Controller — `@Valid`

```java
@PostMapping
@CheckPermission(api = "/v1/departments", action = "CREATE")
public ApiResponse<DepartmentResponse> create(
    @RequestBody @Valid CreateDepartmentRequest req    // ← @Valid bắt buộc
) {
    return ApiResponse.created(departmentService.create(req));
}
```

Query param / path variable: `@Validated` ở class + annotation trên param:
```java
@RestController
@Validated
public class ... {
    @GetMapping("/{id}")
    public ApiResponse<...> detail(
        @PathVariable @Pattern(regexp = "^[0-9a-f-]{36}$") String id
    ) { ... }
}
```

### 5.3 Business validation — Service

```java
@Override
@Transactional
public DepartmentResponse create(CreateDepartmentRequest req) {
    // 1. Business validation
    if (departmentRepository.existsByOrganizationIdAndCode(req.organizationId(), req.code())) {
        throw new AppException(QthtErrorCode.DEPARTMENT_CODE_EXISTS, req.code());
    }
    Organization org = organizationRepository.findById(req.organizationId())
        .orElseThrow(() -> new AppException(QthtErrorCode.ORGANIZATION_NOT_FOUND, req.organizationId()));

    // 2. Build entity
    Department entity = departmentMapper.toEntity(req);
    entity.setOrganization(org);

    // 3. Persist
    Department saved = departmentRepository.save(entity);

    // 4. Event
    eventPublisher.publishEvent(new DepartmentCreatedEvent(saved.getId()));

    // 5. Return DTO
    return departmentMapper.toResponse(saved);
}
```

**Rule:**
- Validation format (không null, không blank, đúng regex, đúng size) → trong DTO annotation
- Validation business (unique, cross-field, state machine) → trong Service
- Validation liên quan tới security (permission) → aspect `@CheckPermission`

### 5.4 Group Validation (nếu cần Create/Update dùng chung DTO)

```java
public interface OnCreate {}
public interface OnUpdate {}

public class DepartmentRequest {
    @NotBlank(groups = {OnCreate.class})                    // Bắt buộc khi create
    private String code;

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})   // Bắt buộc cả 2
    private String name;
}

@PostMapping
public ... create(@RequestBody @Validated(OnCreate.class) DepartmentRequest req) { ... }

@PutMapping("/{id}")
public ... update(@RequestBody @Validated(OnUpdate.class) DepartmentRequest req) { ... }
```

**Nhưng:** ưu tiên tách `CreateXxxRequest` + `UpdateXxxRequest` thay vì group. Rõ ràng hơn.

---

## 6. Exception Handling

### 6.1 `AppException` + `ErrorCode` enum — 1 exception duy nhất

```java
// module-common
@Getter
public class AppException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object[] args;

    public AppException(ErrorCode errorCode, Object... args) {
        super(errorCode.key());
        this.errorCode = errorCode;
        this.args = args;
    }

    public AppException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode.key(), cause);
        this.errorCode = errorCode;
        this.args = args;
    }
}

public interface ErrorCode {
    String key();               // i18n key
    HttpStatus status();
    String defaultMessage();
}

// module-common — common error code
public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("error.invalid.request", HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    UNAUTHORIZED("error.unauthorized", HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"),
    FORBIDDEN("error.forbidden", HttpStatus.FORBIDDEN, "Không có quyền"),
    NOT_FOUND("error.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy"),
    CONFLICT("error.conflict", HttpStatus.CONFLICT, "Xung đột dữ liệu"),
    CONCURRENT_MODIFICATION("error.concurrent.modification", HttpStatus.CONFLICT, "Có người khác vừa sửa"),
    RATE_LIMIT_EXCEEDED("error.rate.limit", HttpStatus.TOO_MANY_REQUESTS, "Quá nhiều request"),
    INTERNAL_ERROR("error.internal", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống"),
    INVALID_SORT_FIELD("error.invalid.sort.field", HttpStatus.BAD_REQUEST, "Trường sắp xếp không hợp lệ");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
    // constructor + getters
}
```

Mỗi module có enum riêng: `QthtErrorCode`, `CustomerErrorCode`, `WarehouseErrorCode`...

### 6.2 GlobalExceptionHandler chuẩn

```java
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleApp(AppException ex, HttpServletRequest req, Locale locale) {
        String msg;
        try {
            msg = messageSource.getMessage(ex.getErrorCode().key(), ex.getArgs(), locale);
        } catch (NoSuchMessageException nsm) {
            msg = ex.getErrorCode().defaultMessage();
            log.warn("Missing i18n key: {}", ex.getErrorCode().key());
        }
        log.warn("AppException [{}] at {}: {}", ex.getErrorCode().key(), req.getRequestURI(), msg);
        return ResponseEntity.status(ex.getErrorCode().status())
            .body(ApiResponse.error(ex.getErrorCode(), msg, req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String,String>>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req, Locale locale) {
        Map<String,String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a,b) -> a));
        String msg = messageSource.getMessage("validation.failed", null, locale);
        return ResponseEntity.badRequest()
            .body(ApiResponse.validationError(errors, msg, req.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex, ...) {
        log.error("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(CommonErrorCode.CONFLICT, ...));
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ApiResponse<Void>> handleOptimistic(Exception ex, ...) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(CommonErrorCode.CONCURRENT_MODIFICATION, ...));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, ...) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(CommonErrorCode.FORBIDDEN, ...));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest req, Locale locale) {
        log.error("Unhandled exception at {}", req.getRequestURI(), ex);   // ✅ log full stack
        String msg = messageSource.getMessage("error.internal", null, locale);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(CommonErrorCode.INTERNAL_ERROR, msg, req.getRequestURI()));
    }
}
```

### 6.3 Cấm

- ❌ `try { ... } catch (Exception e) { throw e; }` — no-op
- ❌ `catch (Exception e) { log.error(e.getMessage()); }` — mất stack trace
- ❌ `catch (Exception e) { throw new RuntimeException(e); }` — mất error code
- ❌ Return `null` từ public method (dùng `Optional` hoặc throw)
- ❌ `Optional.get()` không check (dùng `.orElseThrow(() -> new AppException(...))`)
- ❌ Throw exception raw ra client (`new RuntimeException(sqlEx.getMessage())` → lộ schema)
- ❌ Silent catch — bắt xong không log, không xử lý

---

## 7. MapStruct

### 7.1 Central config

```java
// module-common/mapper/CentralMapperConfig.java
@MapperConfig(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface CentralMapperConfig {}
```

Mọi mapper:
```java
@Mapper(config = CentralMapperConfig.class,
        uses = {OrganizationMapper.class, PersonMapper.class})
public interface DepartmentMapper {

    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "parentName",       source = "parent.name")
    @Mapping(target = "managerName",      source = "manager.name")
    DepartmentResponse toResponse(Department entity);

    List<DepartmentResponse> toResponseList(List<Department> entities);

    @Mapping(target = "id", ignore = true)                     // ID sinh bởi BaseEntity @PrePersist
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "organization", ignore = true)          // set thủ công trong service
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "manager", ignore = true)
    Department toEntity(CreateDepartmentRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateDepartmentRequest req, @MappingTarget Department entity);
}
```

### 7.2 BaseMapper generic (khi list mapping nhiều)

```java
public interface BaseMapper<D, E> {
    D toDto(E entity);
    E toEntity(D dto);

    default List<D> toDtoList(List<E> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDto).toList();
    }
}
```

### 7.3 Cấm

- ❌ Map manual bằng tay (`return new DepartmentResponse(entity.getId(), entity.getName(), ...)`) — trừ khi logic map quá phức tạp cần method riêng
- ❌ Bỏ `@Mapping(target = "...", ignore = true)` cho field cố ý bỏ qua → mapstruct warning turn on
- ❌ `componentModel = "default"` (không quản lý qua Spring)

---

## 8. Lombok

### 8.1 Sử dụng

| Annotation | Khi nào | Cấm |
|-----------|---------|-----|
| `@Getter @Setter` | Entity, DTO có mutation | — |
| `@Data` | DTO đơn giản, KHÔNG dùng cho Entity | Entity (equals/hashCode dựa vào tất cả field → nguy hiểm với lazy) |
| `@Builder` | Entity, DTO nhiều field optional | Class có validation logic trong constructor (Builder bỏ qua) |
| `@NoArgsConstructor` | Entity (JPA yêu cầu) | — |
| `@AllArgsConstructor` | Entity, DTO khi cần constructor full | — |
| `@RequiredArgsConstructor` | **Bắt buộc** Service/Controller (DI với `final` field) | — |
| `@Slf4j` | **Bắt buộc** Service/Controller/Aspect/Filter | — |
| `@Value` (Lombok) | Immutable class (record là thay thế tốt hơn ở Java 21) | Trong `spring-boot-configuration-processor` context |
| `@EqualsAndHashCode(callSuper = true)` | Khi extend `BaseEntity` VÀ cần equals — nhưng thường KHÔNG cần | — |

**Ưu tiên Java 21 `record`** cho DTO thay vì `@Data`:
```java
public record DepartmentResponse(
    String id,
    String code,
    String name,
    String organizationName,
    DepartmentStatus status
) {}
```

Record immutable, có `equals/hashCode/toString` sẵn, không cần Lombok.

---

## 9. AOP — Cross-cutting concerns

Đã dùng: `@CheckPermission`, `@ApiLog`. Chuẩn hóa thêm:

| Aspect | Trigger | Mục đích |
|--------|---------|----------|
| `CheckPermissionAspect` | `@CheckPermission` | RBAC — throw `AccessDeniedException` nếu không có quyền |
| `AuditAspect` | `@Auditable` | Ghi audit log CRUD |
| `IdempotencyAspect` | `@Idempotent` | Check `Idempotency-Key` header, cache response 24h |
| `RateLimitAspect` | `@RateLimit(limit, duration)` | Bucket4j check |
| `PerformanceAspect` | `@LogExecutionTime` | Log method chậm > threshold |
| `CacheableAspect` | `@Cacheable` (Spring native) | Caffeine cache |

**Cấm:**
- Aspect thay đổi return value ngầm (chỉ log / check / cache)
- Aspect throw exception khác kiểu spec (phải là `AppException` hoặc Spring standard)
- Chain nhiều aspect > 3 lớp (khó debug)

---

## 10. Testing

### 10.1 Stack bắt buộc

- **JUnit 5** (`spring-boot-starter-test` đã có)
- **Mockito** (đi kèm)
- **AssertJ** (đi kèm) — assertion đọc tự nhiên
- **Testcontainers PostgreSQL** — bắt buộc cho integration test, KHÔNG H2 (semantic khác Postgres)
- **RestAssured** — cho API E2E
- **WireMock** — mock external service

### 10.2 Cấu trúc test

```
src/test/java/com/frezo/<domain>/
├── unit/
│   └── service/
│       └── DepartmentServiceImplTest.java    ← Mockito, không Spring context
├── integration/
│   ├── DepartmentControllerIT.java           ← @SpringBootTest + Testcontainers
│   └── DepartmentRepositoryIT.java           ← @DataJpaTest + Testcontainers
└── fixture/
    └── DepartmentFixtures.java               ← builders cho test data
```

### 10.3 Ví dụ unit test service

```java
@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock private DepartmentRepository departmentRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private DepartmentServiceImpl service;

    @Test
    @DisplayName("create() throw DEPARTMENT_CODE_EXISTS khi code trùng")
    void create_shouldThrow_whenCodeExists() {
        // Given
        var req = new CreateDepartmentRequest("IT", "Phòng IT", "org-1", null, null, DepartmentStatus.ACTIVE);
        when(departmentRepository.existsByOrganizationIdAndCode("org-1", "IT")).thenReturn(true);

        // When + Then
        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(AppException.class)
            .hasFieldOrPropertyWithValue("errorCode", QthtErrorCode.DEPARTMENT_CODE_EXISTS);

        verifyNoInteractions(eventPublisher);
    }
}
```

### 10.4 Integration test — Testcontainers

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DepartmentControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("frezo_test")
        .withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void createDepartment_shouldReturn201() throws Exception {
        var req = new CreateDepartmentRequest("IT", "Phòng IT", "org-1", null, null, DepartmentStatus.ACTIVE);
        mvc.perform(post("/v1/departments")
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwt)
                .content(om.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.code").value("IT"));
    }
}
```

### 10.5 Coverage target

| Layer | Coverage tối thiểu |
|-------|---------------------|
| Service | 80% |
| Controller | 60% (chủ yếu happy path + validation error) |
| Repository | 40% (chỉ query phức tạp, không test derived method) |
| Mapper | 0% (MapStruct generated) |
| Overall | 70% |

Config JaCoCo trong `pom.xml`, fail build nếu dưới threshold.

### 10.6 Cấm

- ❌ Test bằng H2 (semantic khác Postgres: JSON, UUID, upsert, window function)
- ❌ Test dùng `@MockBean` khắp nơi (chỉ mock external, không mock Repository trong integration test)
- ❌ Test dependency lẫn nhau (test A phải chạy trước test B)
- ❌ `@Disabled` không có lý do trong PR
- ❌ Sleep trong test (`Thread.sleep(1000)`) — dùng `Awaitility`

---

## 11. Actuator & Monitoring

Bắt buộc bật:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus, loggers
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true                    # /actuator/health/liveness + /readiness cho K8s
  metrics:
    tags:
      application: frezo-backend
      environment: ${SPRING_PROFILES_ACTIVE}
```

- `/actuator/health` — dùng cho K8s liveness/readiness probe
- `/actuator/prometheus` — scrape metric
- `/actuator/loggers` — đổi log level runtime
- **Bảo vệ:** chỉ expose port riêng, không authenticated bên ngoài (hoặc dùng `SecurityFilterChain` riêng)

Bổ sung `micrometer-registry-prometheus` deps.

---

## 12. Bootstrap Application

```java
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableMethodSecurity(prePostEnabled = true)         // Bật @PreAuthorize
@EnableConfigurationProperties
@ComponentScan(basePackages = "com.frezo")
@EntityScan(basePackages = "com.frezo")              // auto-scan mọi @Entity
public class FrezoServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FrezoServerApplication.class, args);
    }
}
```

**Cấm:**
- ❌ Thủ công liệt kê `@EntityScan(basePackages = {"com.frezo.qtht.entity", "com.frezo.customer.entity", ...})` — dùng `"com.frezo"` root, để Spring auto-scan (giảm lỗi thiếu module mới)
- ❌ `DriverManager.getConnection(...)` trong `main()` để CREATE DATABASE — dùng Flyway `V1__init.sql` hoặc infra script

---

*File này focus vào Spring Boot / JPA / MapStruct / Testing. Cross-reference với `AI_BACKEND_ENGINEERING_GUIDE.md` (§ Architecture, § Security, § Enterprise Rules) và `DATABASE_STANDARD.md` (§ Migration, § Index).*
