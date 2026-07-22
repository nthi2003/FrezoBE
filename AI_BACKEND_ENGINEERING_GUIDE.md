# Frezo Backend — Engineering Guide

> **Mục tiêu:** đưa cho AI (Cursor, Claude, Copilot) trước khi code Java/Spring Boot cho FrezoBE.
> Đây là file **trung tâm**. 3 file còn lại là chuyên đề, được reference lại từ đây.
> Nếu 1 quy tắc trong file này mâu thuẫn với file chuyên đề → **theo file chuyên đề**.

---

## 📚 Bộ tài liệu Backend Engineering (đọc theo thứ tự)

| # | File | Nội dung | Bắt buộc đọc khi... |
|---|------|----------|---------------------|
| 1 | **AI_BACKEND_ENGINEERING_GUIDE.md** | Kiến trúc, package, layer, coding rules, performance, security, logging, enterprise rules | Trước MỌI task backend |
| 2 | [API_DESIGN_STANDARD.md](./API_DESIGN_STANDARD.md) | REST verbs, `ApiResponse<T>`, error schema, pagination, filter, OpenAPI, idempotency | Tạo/sửa Controller, đổi API contract |
| 3 | [SPRING_BOOT_BEST_PRACTICE.md](./SPRING_BOOT_BEST_PRACTICE.md) | DI, JPA, transaction, validation, exception, MapStruct, bean config, testing | Viết Service, Repository, Config, Mapper |
| 4 | [DATABASE_STANDARD.md](./DATABASE_STANDARD.md) | PostgreSQL naming, migration (Flyway), index, FK, UUID, audit, soft delete, query optimization | Tạo/sửa Entity, migration script, tuning query |

**AI đang code phải:**
1. Đọc **ít nhất** file này + file chuyên đề tương ứng với task.
2. Trước khi viết class mới → xem đã có class tương tự trong `module-common` chưa (không tạo trùng).
3. Sau khi viết xong → chạy Self-review Checklist (§14).

---

## 0. Triết lý thiết kế (Design Philosophy)

FrezoBE là **enterprise multi-module system** (26 module Maven), phục vụ HR + CRM + Warehouse + CMS + Task + Automation + Email + AI. Không phải MVP, không phải side-project. Vì vậy:

| Ưu tiên | Đối lập (KHÔNG chọn) |
|---------|----------------------|
| **Rõ ràng** — 1 việc 1 chỗ, đặt tên đúng nghĩa | Ngắn gọn nhất — nhét 3 thứ vào 1 method |
| **Nhất quán** — 1 pattern áp dụng toàn hệ thống | Sáng tạo cục bộ — module này khác module kia |
| **An toàn dữ liệu** — transaction, validation, audit đầy đủ | Fast-path — bỏ qua check để nhanh |
| **Dự đoán được** — cùng input → cùng output, cùng lỗi → cùng error code | Magic — code "thông minh" nhưng khó đọc |
| **Có bằng chứng** — log, audit, trace, error code | "Chạy được là được" |
| **Nghịch đảo dễ** — rollback được, undo được | Destructive by default |

**Tham chiếu chuẩn công nghiệp:** Spring PetClinic (structure), Netflix microservices (resilience), Airbnb Java Style, Google Java Style, RFC 7807 (Problem Details for HTTP APIs), RFC 9110 (HTTP Semantics).

**Không tham chiếu:** blog post random 5+ năm tuổi, tutorial YouTube "Spring Boot in 10 minutes", ChatGPT-generated code không kiểm chứng.

---

## 1. Kiến trúc (Architecture First) ⭐⭐⭐⭐⭐

### 1.1 Mô hình tổng thể — Hexagonal Lite + Feature Module

FrezoBE **không** dùng Hexagonal đầy đủ (ports/adapters/domain/application layer tách biệt) vì overhead quá cao cho team hiện tại. Dùng **Hexagonal Lite**:

```
┌─────────────────────────────────────────────────────────┐
│  module-<domain>-res  (REST Adapter)                    │
│  ├── controller/           ← HTTP inbound               │
│  └── (chỉ orchestration, không business)                │
└──────────────────────┬──────────────────────────────────┘
                       │ gọi qua interface
┌──────────────────────▼──────────────────────────────────┐
│  module-<domain>-bom  (Business Object Model = Domain)  │
│  ├── service/              ← interface (public API)     │
│  ├── service/impl/         ← business logic             │
│  ├── repository/           ← JPA outbound               │
│  ├── entity/               ← JPA @Entity                │
│  ├── mapper/               ← MapStruct DTO ↔ Entity     │
│  ├── dto/request/          ← input DTO + validation     │
│  ├── dto/response/         ← output DTO                 │
│  ├── common/ (enum)        ← domain enums               │
│  └── config/               ← domain-level config        │
└──────────────────────┬──────────────────────────────────┘
                       │ inject
┌──────────────────────▼──────────────────────────────────┐
│  module-common  (Shared Kernel)                         │
│  ├── domain/BaseEntity.java                             │
│  ├── response/ApiResponse.java + PageResponse.java      │
│  ├── exception/AppException.java + GlobalExceptionHandler│
│  ├── mapper/BaseMapper.java                             │
│  └── util/                                              │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│  module-server  (Composition Root)                      │
│  ├── FrezoServerApplication.main()                      │
│  ├── config/ (SecurityConfig, MapStructConfig, ...)     │
│  └── i18n/messages*.properties                          │
└─────────────────────────────────────────────────────────┘
```

**Quy tắc dependency:**
- `res` → `bom` → `common` (1 chiều, KHÔNG ngược)
- `common` KHÔNG import từ bất kỳ module business nào
- `server` là entrypoint, có thể import mọi module
- **Cấm** `bom` module A gọi `bom` module B **trực tiếp qua repository** — phải qua `service` interface

### 1.2 Ranh giới module (Bounded Context — DDD Lite)

Mỗi cặp `module-<domain>-bom` + `module-<domain>-res` = **1 Bounded Context**. Ví dụ:

| Module | Bounded Context | Aggregate Root |
|--------|-----------------|----------------|
| `qtht` | Quản trị hệ thống | User, Role, Menu, Permission, Organization, Department |
| `qlns` | Quản lý nhân sự | Person, Contract, Payroll |
| `customer` | Khách hàng & CRM | Customer, Lead, Opportunity |
| `product` | Sản phẩm | Product, Category, PriceList |
| `warehouse` | Kho | Warehouse, Stock, StockMove |
| `task` | Task management | Task, Project, Sprint |
| `cms` | CMS | Article, Page, Media |
| `email` | Email service | EmailTemplate, EmailQueue |
| `fbautomation` | Facebook automation | FbPage, FbAutomationRule |
| `dmdc` | Danh mục dùng chung | Category, Lookup |
| `qtbv` | Quản trị bán vé (hoặc business tương tự) | (theo module) |
| `auth` | Authentication | UserAuth, JwtToken, RefreshToken |

**Cross-context call:**
- ❌ **KHÔNG:** `CustomerServiceImpl` inject `PersonRepository` (nhảy module)
- ✅ **CÓ:** `CustomerServiceImpl` inject `PersonService` (interface), hoặc dùng **event** (Spring `ApplicationEventPublisher`, tương lai Kafka)

### 1.3 Cấm

| Điều cấm | Vì sao |
|----------|--------|
| **Anemic Domain toàn phần** — entity chỉ có getter/setter, mọi logic ở Service | Business rule bị scatter, khó reuse, khó test |
| **Business logic trong Controller** | Không test được, không reuse được từ scheduler/CLI/event handler |
| **Business logic trong Repository** | Repository = data access thuần túy. Query phức tạp → Specification/QueryDSL/JPQL |
| **Controller → Repository trực tiếp** (bỏ qua Service) | Mất transaction boundary, mất validation |
| **`@Transactional` trong Controller** | Transaction phải khớp với business unit-of-work, nằm ở Service |
| **Static bean / util class chứa state** | Không test được, race condition |
| **Circular dependency giữa 2 module** | Kiến trúc sai — refactor thành 3 module hoặc dùng event |

---

## 2. Package Structure ⭐⭐⭐⭐⭐

### 2.1 Cấu trúc chuẩn cho mỗi module business

```
com.frezo.<domain>/
├── controller/              (chỉ trong -res module)
├── service/
│   ├── XxxService.java              ← interface
│   └── impl/
│       └── XxxServiceImpl.java      ← implementation
├── repository/
│   └── XxxRepository.java           ← extends JpaRepository<E, String>, JpaSpecificationExecutor<E>
├── entity/
│   └── Xxx.java                     ← @Entity extends BaseEntity
├── dto/
│   ├── request/
│   │   ├── CreateXxxRequest.java
│   │   ├── UpdateXxxRequest.java
│   │   └── XxxFilterRequest.java   ← extends PagingBase
│   └── response/
│       └── XxxResponse.java
├── mapper/
│   └── XxxMapper.java               ← @Mapper(config = CentralMapperConfig.class)
├── validator/
│   └── XxxBusinessValidator.java    ← business validation (không phải @NotBlank)
├── specification/
│   └── XxxSpecification.java        ← JPA Criteria dynamic query
├── constant/
│   └── XxxErrorCode.java            ← enum error code i18n key
├── common/
│   └── XxxStatus.java               ← enum domain
├── config/
│   └── XxxProperties.java           ← @ConfigurationProperties
└── event/
    ├── XxxCreatedEvent.java         ← domain event (Spring ApplicationEvent)
    └── XxxEventListener.java
```

### 2.2 Cấm

| ❌ Bad | ✅ Good |
|--------|---------|
| Tất cả module chung 1 package `controller/`, `service/`, `entity/` | Mỗi module riêng theo `com.frezo.<domain>` |
| Package `common/` chứa mọi thứ (DTO, util, config, constants trộn lẫn) | Tách rõ `dto/`, `constant/`, `config/`, `util/` |
| DTO nằm chung với Entity trong `entity/` | Bắt buộc `dto/request/` và `dto/response/` |
| Service class không có interface, `@Autowired impl` khắp nơi | `interface` + `impl/` — cho phép mock trong test |
| `util/` chứa 20+ class không liên quan | Nếu > 5 file → gom theo chức năng (`StringUtil`, `DateUtil`, `SecurityUtil`) |

### 2.3 Naming convention (bắt buộc)

| Loại | Convention | Ví dụ |
|------|------------|-------|
| Package | Tất cả lowercase, tiếng Anh | `com.frezo.customer.repository` |
| Class Entity | Danh từ số ít, PascalCase | `Department`, `Person`, `Contract` |
| Class Repository | `<Entity>Repository` | `DepartmentRepository` |
| Class Service (interface) | `<Domain>Service` | `DepartmentService` |
| Class Service (impl) | `<Domain>ServiceImpl` | `DepartmentServiceImpl` |
| Class Controller | `<Domain>Controller` | `DepartmentController` |
| Class Mapper | `<Domain>Mapper` | `DepartmentMapper` |
| DTO Request | `Create<Domain>Request`, `Update<Domain>Request`, `<Domain>FilterRequest` | `CreateDepartmentRequest` |
| DTO Response | `<Domain>Response`, `<Domain>DetailResponse` (khi cần detail rộng hơn) | `DepartmentResponse`, `DepartmentDetailResponse` |
| Enum status | `<Domain>Status` (UPPER_SNAKE cho value) | `DepartmentStatus.ACTIVE` |
| Constant | `UPPER_SNAKE_CASE` | `MAX_UPLOAD_SIZE_MB` |
| Method boolean | Bắt đầu bằng `is`/`has`/`can`/`should` | `isDeleted()`, `hasPermission()` |
| Method query | `findByX`, `existsByX`, `countByX`, `getX` (khi chắc chắn tồn tại) | `findByCode()` |
| Method command | `create`, `update`, `delete`, `activate`, `deactivate`, `approve`, `reject` | `createDepartment()` |

**Cấm naming vô nghĩa:**
- ❌ `getData()`, `saveData()`, `process()`, `handle()`, `doSomething()`, `execute()` (khi không rõ ngữ cảnh)
- ❌ `DTO`, `VO`, `Bean`, `Info` chung chung — phải nói rõ Request/Response
- ❌ Viết tắt tự chế: `deptSvc`, `custRepo`, `usrDto` — dùng tên đầy đủ

---

## 3. Layer Responsibility ⭐⭐⭐⭐⭐

### 3.1 Trách nhiệm từng layer

```
┌─────────────────────────────────────────────────────────┐
│ Controller                                              │
│ ─ Nhận HTTP request                                     │
│ ─ Validate DTO (@Valid → tự động qua GlobalHandler)     │
│ ─ Extract user context (từ SecurityContextHolder)       │
│ ─ Gọi 1 method service                                  │
│ ─ Wrap kết quả bằng ApiResponse<T>                      │
│ ─ KHÔNG try-catch (để GlobalExceptionHandler xử lý)     │
│ ─ KHÔNG mapping entity→dto (Service đã trả DTO)         │
│ ─ KHÔNG loop, không if-else business                    │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Service                                                 │
│ ─ Business logic                                        │
│ ─ Transaction boundary (@Transactional)                 │
│ ─ Business validation (không phải @NotBlank)            │
│ ─ Orchestration nhiều repository/service                │
│ ─ Publish domain event                                  │
│ ─ Gọi Mapper để convert entity ↔ DTO                    │
│ ─ Ném AppException với ErrorCode                        │
│ ─ KHÔNG return entity ra ngoài (chỉ DTO)                │
│ ─ KHÔNG chứa HTTP concerns (HttpStatus, HttpHeaders)    │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Repository                                              │
│ ─ CRUD entity                                           │
│ ─ Query: derived method / @Query JPQL / Specification   │
│ ─ Projection (nếu cần)                                  │
│ ─ KHÔNG business logic                                  │
│ ─ KHÔNG if-else (trừ dynamic query trong Specification) │
│ ─ KHÔNG validate                                        │
│ ─ KHÔNG mapping                                         │
│ ─ KHÔNG @Transactional (kế thừa từ Service)             │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ Mapper (MapStruct)                                      │
│ ─ Entity ↔ Request/Response DTO                         │
│ ─ Không chứa business logic (chỉ mapping thuần)         │
│ ─ Xử lý null-safe, list-safe                            │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ Validator (Business)                                    │
│ ─ Rules không thể express bằng @NotBlank/@Size          │
│ ─ Ví dụ: "Ngày kết thúc phải sau ngày bắt đầu",         │
│         "Không được xóa department đang có nhân viên"   │
│ ─ Ném AppException với ErrorCode chuẩn                  │
└─────────────────────────────────────────────────────────┘
```

### 3.2 Bad → Good

<details>
<summary><b>❌ Bad — Controller làm việc của Service</b></summary>

```java
@GetMapping("/combobox")
public ApiResponse<List<ComboboxResponse>> getCombobox() {
    DepartmentFilterRequest filter = new DepartmentFilterRequest();
    filter.setPageNumber(0);
    filter.setPageSize(Integer.MAX_VALUE);                                 // ❌ page hack
    Map<String, Object> data = departmentService.all(filter);              // ❌ Map<String,Object>
    List<DepartmentResponse> items = (List<DepartmentResponse>) data.get("items"); // ❌ unchecked cast
    List<ComboboxResponse> comboboxes = items.stream()                     // ❌ mapping ở controller
        .map(d -> new ComboboxResponse(d.getId(), d.getName()))
        .toList();
    return ApiResponse.success(comboboxes);
}
```
</details>

<details>
<summary><b>✅ Good — Controller chỉ orchestration</b></summary>

```java
@GetMapping("/combobox")
@Operation(summary = "Danh sách phòng ban dạng combobox (id + name)")
public ApiResponse<List<ComboboxResponse>> getCombobox() {
    return ApiResponse.success(departmentService.combobox());
}
```
Service:
```java
@Override
@Transactional(readOnly = true)
public List<ComboboxResponse> combobox() {
    return departmentRepository.findAllActiveProjection();  // Projection ở Repository
}
```
Repository (Projection):
```java
public interface DepartmentRepository extends JpaRepository<Department, String>, ... {
    @Query("select new com.frezo.qtht.dto.response.ComboboxResponse(d.id, d.name) " +
           "from Department d where d.isDeleted = false and d.status = 'ACTIVE'")
    List<ComboboxResponse> findAllActiveProjection();
}
```
</details>

---

## 4. Coding Rules ⭐⭐⭐⭐⭐

### 4.1 Cấm tuyệt đối

| Cấm | Vì sao | Thay bằng |
|-----|--------|-----------|
| `Object` làm return type / param type "cho tiện" | Mất type-safety | Generic `<T>` hoặc DTO cụ thể |
| `Map<String, Object>` làm response type | FE không parse được, IDE không autocomplete | DTO class hoặc `Map<String, ConcreteType>` |
| `// TODO`, `// FIXME`, `// XXX` không có ticket ID | Rác vĩnh viễn | Nếu bắt buộc: `// TODO(FREZO-123): mô tả cụ thể + deadline` |
| Code demo / test data hardcode trong production code | Lộ credential, nhiễu prod | `@Profile("dev")` hoặc file seed riêng |
| `throw new RuntimeException("...")` | Mất context, mất error code | `throw new AppException(ErrorCode.XXX, args)` |
| `try { ... } catch (Exception e) { throw e; }` | No-op, chỉ tăng cyclomatic | Xóa try-catch |
| `try { ... } catch (Exception e) { log.error(e); }` không rethrow | Nuốt lỗi, prod im lặng chết | Rethrow hoặc convert sang `AppException` |
| `Optional.get()` không check | NPE runtime | `.orElseThrow(() -> new AppException(...))` |
| `Objects.isNull(x)` / `Objects.nonNull(x)` | Thừa | `x == null` / `x != null` |
| `if (a) { if (b) { if (c) { ... } } }` (nested > 3) | Khó đọc | Early return / extract method |
| Return `null` từ public method | Khách hàng phải null-check khắp nơi | `Optional<T>` hoặc ném exception |
| Magic number: `if (status == 3)` | Không rõ nghĩa | `if (status == ContractStatus.APPROVED.getCode())` |
| Magic string: `if (role.equals("ADMIN"))` | Typo → bug silent | Enum hoặc constant |
| Duplicate code > 6 dòng | Sửa 1 nơi quên nơi khác | Extract method / util |
| Method mutation param: `void doSomething(List<X> input) { input.clear(); }` | Side effect ẩn | Return giá trị mới, tôn trọng immutability |
| `System.out.println` | Không log level, không structured | `log.debug/info/warn/error` |
| `printStackTrace()` | Log về stderr, không control được | `log.error("msg", ex)` |
| Field injection `@Autowired private XxxService svc;` | Không test được không có Spring, cyclic dễ ẩn | Constructor injection + `private final` |
| `@Value("${...}")` rải rác 10+ chỗ | Config scatter | `@ConfigurationProperties` gom 1 class |

### 4.2 Ưu tiên

| Ưu tiên | Ví dụ |
|---------|-------|
| **Early return** | `if (list.isEmpty()) return List.of();` |
| **Immutable data** | `record UserView(String id, String name) {}`, `List.copyOf(...)`, `Collections.unmodifiableList(...)` |
| **Stream khi phù hợp** (không lạm dụng cho vòng lặp có side effect) | `list.stream().filter(...).map(...).toList()` |
| **Guard clause** | Kiểm tra input xấu → throw sớm, giữ happy path phẳng |
| **Explicit type** khi type inference gây khó đọc | `Map<String, List<UserResponse>> grouped = ...` thay vì `var grouped = ...` |
| **`record` cho DTO đơn giản** (Java 21 hỗ trợ tốt) | `public record ComboboxResponse(String id, String name) {}` |
| **`sealed class` cho hierarchy đóng** | `sealed interface PaymentResult permits Success, Failure {}` |

### 4.3 Kích thước code

| Đơn vị | Giới hạn cứng | Nếu vượt |
|--------|---------------|----------|
| Method | ≤ **40 dòng** (không tính blank + comment) | Extract method con |
| Class | ≤ **400 dòng** | Tách class theo trách nhiệm |
| Controller | ≤ **200 dòng** | Tách controller theo sub-resource |
| Service | ≤ **500 dòng** (đã nới cho business phức tạp) | Tách service theo use case |
| Parameter list | ≤ **5 params** | Gom thành DTO |
| Cyclomatic complexity per method | ≤ **10** | Refactor |
| Nested if/for depth | ≤ **3** | Early return / extract |

**Cách check nhanh:** cài SonarLint / IntelliJ Inspection profile.

---

## 5. SOLID + DDD Lite

### 5.1 SOLID áp dụng thực tế

| Nguyên lý | Áp dụng trong FrezoBE |
|-----------|----------------------|
| **S**ingle Responsibility | 1 Service = 1 aggregate root. Không tạo `UserAndRoleAndPermissionService` |
| **O**pen/Closed | Thêm feature = thêm class mới (Strategy), không sửa class cũ. Ví dụ: `NotificationChannel` interface + `EmailChannel`, `SmsChannel`, `PushChannel` |
| **L**iskov | Impl của interface phải giữ contract. Không throw exception không declared |
| **I**nterface Segregation | `ReadonlyRepository` vs `WritableRepository` nếu cần. `XxxService` interface không chứa method chỉ 1 client dùng |
| **D**ependency Inversion | Service depend on **interface** của Repository (JpaRepository là interface rồi). Không new object trong service |

### 5.2 DDD Lite — không dogmatic

Chúng ta **KHÔNG** làm Full DDD (Aggregate, Value Object, Domain Event, Repository interface trong domain layer, Application Service riêng). Nhưng bắt buộc:

1. **Entity có method behavior** — không chỉ getter/setter (avoid Anemic Domain toàn phần):
   ```java
   @Entity
   public class Contract extends BaseEntity {
       private ContractStatus status;
       private LocalDate signedDate;

       public void approve(String approverId) {
           if (this.status != ContractStatus.PENDING_APPROVAL) {
               throw new AppException(ErrorCode.CONTRACT_INVALID_STATE);
           }
           this.status = ContractStatus.APPROVED;
           this.signedDate = LocalDate.now();
           this.updatedBy = approverId;
       }
   }
   ```
2. **State machine tường minh** — dùng enum + transition map (xem `contractStatus.ts` bên FE). Không dùng `int status = 3`.
3. **Domain event** — dùng `ApplicationEventPublisher` cho intra-module, chuẩn bị Kafka cho cross-module.
4. **Aggregate boundary** — 1 transaction chỉ modify 1 aggregate. Cross-aggregate → dùng event (eventual consistency).

---

## 6. Performance Guideline ⭐⭐⭐⭐⭐

*(Chi tiết JPA xem [SPRING_BOOT_BEST_PRACTICE.md §3](./SPRING_BOOT_BEST_PRACTICE.md#3-jpa--hibernate).)*

### 6.1 Danh sách check bắt buộc

| Chủ đề | Bắt buộc | Cấm |
|--------|----------|-----|
| **N+1 query** | Dùng `@EntityGraph` hoặc `JOIN FETCH` khi load list có relation | Loop gọi `entity.getRelation()` trong service |
| **FetchType** | `LAZY` mặc định cho MỌI `@ManyToOne`, `@OneToMany`, `@ManyToMany` | `FetchType.EAGER` (trừ khi có lý do đo được) |
| **Pagination** | **Bắt buộc** cho mọi endpoint list. Default `pageSize=20`, max `pageSize=100` | Trả full list không giới hạn |
| **Projection** | Dùng interface projection / DTO projection cho combobox, dropdown, list view chỉ cần vài field | Load full entity chỉ để lấy `id + name` |
| **Batch insert/update** | `spring.jpa.properties.hibernate.jdbc.batch_size=50`, `order_inserts=true`, `order_updates=true`. Dùng `saveAll()` với chunk | Loop `save()` 1000 lần |
| **Index** | Query column WHERE / ORDER BY / JOIN đều phải có index (xem `DATABASE_STANDARD.md §5`) | `LIKE '%xxx%'` mà không có GIN index |
| **`SELECT *`** | Chỉ select field cần thiết (Projection) | Load full entity 30 cột cho grid 5 cột |
| **Cache** | Caffeine (đã có deps) cho lookup ít thay đổi (Menu, Role, Permission, DanhMucDungChung) | Cache DTO đang được modify thường xuyên |
| **Async** | `@Async` + `TaskExecutor` cho email, notification, export lớn | Chạy sync 30s task chặn HTTP thread |
| **Connection pool** | HikariCP mặc định, tune `maximum-pool-size` theo tải | Mở connection thủ công |
| **Streaming lớn** | Response `application/octet-stream` cho export XLSX, PDF | Load full 100MB vào memory rồi return byte[] |

### 6.2 Ví dụ N+1 — thực tế trong codebase

<details>
<summary><b>❌ Bad</b></summary>

```java
List<Department> departments = departmentRepository.findAll();
return departments.stream()
    .map(d -> new DepartmentResponse(
        d.getId(),
        d.getName(),
        d.getOrganization().getName(),   // ❌ N+1: mỗi department 1 query lấy organization
        d.getParent() != null ? d.getParent().getName() : null,  // ❌ N+1 nữa
        d.getManager().getName()         // ❌ N+1 nữa
    ))
    .toList();
```
</details>

<details>
<summary><b>✅ Good — EntityGraph</b></summary>

```java
public interface DepartmentRepository extends JpaRepository<Department, String>, ... {
    @EntityGraph(attributePaths = {"organization", "parent", "manager"})
    @Query("select d from Department d where d.isDeleted = false")
    Page<Department> findAllWithRelations(Specification<Department> spec, Pageable pageable);
}
```
</details>

<details>
<summary><b>✅ Better — DTO Projection (không cần load Entity)</b></summary>

```java
@Query("""
    select new com.frezo.qtht.dto.response.DepartmentResponse(
        d.id, d.code, d.name, o.name, p.name, m.name, d.status)
    from Department d
    left join d.organization o
    left join d.parent p
    left join d.manager m
    where d.isDeleted = false
""")
Page<DepartmentResponse> searchProjection(Specification<Department> spec, Pageable pageable);
```
</details>

---

## 7. Security Guideline ⭐⭐⭐⭐⭐

### 7.1 Authentication

| Yêu cầu | Chi tiết |
|---------|----------|
| **JWT** | JJWT 0.11+, algorithm **HS512** (đã dùng), secret ≥ **64 bytes**, đọc từ env var (KHÔNG hardcode default trong code) |
| **Access token TTL** | 1 giờ (`3600000ms`) — đã đúng |
| **Refresh token TTL** | 7 ngày — đã đúng, lưu DB (revocable), rotate mỗi lần refresh |
| **Token blacklist** | Bảng `jwt_blacklist` khi user logout / password change — đã có |
| **Password hash** | **BCrypt** (`BCryptPasswordEncoder`, strength ≥ 10). ❌ **CẤM `NoOpPasswordEncoder`** kể cả "tạm thời" |
| **CSRF** | Tắt (đã đúng vì stateless JWT) |
| **CORS** | ❌ **CẤM** `allowedOrigins("*")` production. Dùng whitelist env var: `app.cors.allowed-origins=https://app.frezo.vn,https://admin.frezo.vn` |

### 7.2 Authorization (RBAC)

| Yêu cầu | Chi tiết |
|---------|----------|
| **Roles** | Bảng `role` + `user_role` (many-to-many) — đã có |
| **Permissions** | Bảng `permission` (code + api_path + action) + `role_permission` — đã có |
| **Check permission** | `@CheckPermission(api = "/qtht/department", action = "VIEW")` — aspect đã có. **Bắt buộc bật lại** (đang bị comment-out) |
| **Method-level security** | Bật `@EnableMethodSecurity(prePostEnabled = true)` để dùng `@PreAuthorize` khi cần |
| **Admin bypass** | User có role `SUPER_ADMIN` → aspect skip check (đã cần implement) |
| **Data-level (row-level)** | Multi-tenant / branch-level: mọi query của resource X phải filter by `tenantId` / `branchId` từ `SecurityContextHolder` |

### 7.3 Data protection

| Yêu cầu | Chi tiết |
|---------|----------|
| **Không log secret** | Password, JWT, API key, CCCD, credit card → KHÔNG được `log.info()` |
| **Không trả stack trace** cho client | `GlobalExceptionHandler` chỉ trả `message` + `errorCode`, stack chỉ log server |
| **Không trả exception raw** | Không `throw new RuntimeException(sqlException.getMessage())` — có thể lộ schema |
| **Encrypt PII** | Số CCCD, số điện thoại, số tài khoản → cân nhắc AES-256 column-level encryption (Hibernate `@ColumnTransformer` hoặc envelope encryption) |
| **Audit log** | Mọi thao tác nhạy cảm (login, permission change, delete) → ghi audit (đã có `ApiLogAspect`), trace được ai làm gì lúc nào |
| **Mask response** | Số điện thoại, email, CCCD trả về FE dạng masked (`0987***456`), có endpoint reveal riêng có audit |
| **Rate limit** | Bucket4j (đã có deps) — login endpoint bắt buộc: 5 request / 5 phút / IP |
| **Idempotency** | POST tạo mới có tiền → require header `Idempotency-Key` (xem `API_DESIGN_STANDARD.md §7`) |
| **SQL injection** | Bắt buộc parameterized query. ❌ CẤM concat string vào JPQL/native. ✅ Dùng `:param` binding |
| **File upload** | Whitelist MIME type (Apache Tika detect thật, không tin extension), max size, virus scan (tương lai) |

### 7.4 Anti-pattern hiện tại phải fix

| Anti-pattern | Vị trí | Fix |
|--------------|--------|-----|
| `NoOpPasswordEncoder` | `SecurityConfig` | BCrypt strength 12, migration hash password cũ |
| `@Autowired UserDetailsService` (field injection) | `SecurityConfig` | Constructor injection |
| `allowedOrigins("*")` | `SecurityConfig.corsConfigurationSource` | Whitelist env var |
| JWT secret default hardcode `"SecretKeyForFTechBackend..."` | `JwtTokenProvider` | Bắt buộc set env var, fail-fast nếu default |
| `DriverManager.getConnection(...)` trong `main()` để CREATE DATABASE | `FrezoServerApplication` | Xóa, dùng Flyway `V1__init.sql` |
| `@CheckPermission` bị comment-out toàn bộ controllers | `DepartmentController`, `PersonController`, ... | Bật lại đồng loạt, config `SUPER_ADMIN` bypass |
| Không có `@EnableMethodSecurity` | `SecurityConfig` | Thêm để `@PreAuthorize` hoạt động |

---

## 8. Logging

| Level | Khi nào dùng |
|-------|--------------|
| `TRACE` | Chi tiết cực nhỏ (debug protocol) — chỉ bật local |
| `DEBUG` | Step-by-step luồng service — bật dev, tắt prod |
| `INFO` | Sự kiện quan trọng: user login, order created, batch job start/end |
| `WARN` | Bất thường nhưng recover được: retry, fallback, deprecated API dùng |
| `ERROR` | Lỗi cần điều tra: exception không expected, external service fail |

### 8.1 Format & MDC

```java
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    public void placeOrder(CreateOrderRequest req) {
        log.info("Placing order for customer={} items={}", req.customerId(), req.items().size());
        try {
            // ...
        } catch (PaymentException ex) {
            log.error("Payment failed for order customer={}", req.customerId(), ex);   // ✅ pass ex, không dùng {}
            throw new AppException(ErrorCode.PAYMENT_FAILED, ex);
        }
    }
}
```

**Bắt buộc:**
- Log arg qua placeholder `{}`, KHÔNG concat string (`log.info("x=" + x)`)
- Log exception qua param thứ 2, KHÔNG `.getMessage()` (mất stack trace)
- Log KHÔNG chứa: password, token, secret, số CCCD, số thẻ, OTP
- Mỗi request có `correlationId` (aka `traceId`) trong MDC → propagate qua log của cả stack
- Filter setup MDC: `TraceIdFilter extends OncePerRequestFilter` đọc header `X-Correlation-Id`, nếu không có thì generate UUID
- Response luôn set header `X-Correlation-Id` để client debug được

### 8.2 Cấm

- ❌ `System.out.println` / `printStackTrace`
- ❌ `log.info("SQL: " + query)` production (log SQL qua `hibernate.SQL` logger, chỉ bật dev)
- ❌ `log.debug` với string.format expensive khi guard sai:
  ```java
  log.debug("Result: " + heavyToString(obj));   // ❌ evaluate cả khi debug tắt
  log.debug("Result: {}", heavyToString(obj));   // ✅ lazy
  ```
- ❌ `catch (Exception e) { log.error(e); }` không có message

---

## 9. Enterprise Rules ⭐⭐⭐⭐⭐

Đây là những rule mà AI **thường không tự đề xuất** nhưng bắt buộc cho enterprise system.

### 9.1 Audit Log

| Rule | Chi tiết |
|------|----------|
| Mọi mutation (create/update/delete) trên aggregate quan trọng → ghi audit | Aspect `AuditAspect` + annotation `@Auditable(action = "CREATE", resource = "DEPARTMENT")` |
| Audit lưu: `who`, `when`, `action`, `resource`, `resourceId`, `oldValue` (JSON), `newValue` (JSON), `ip`, `userAgent`, `traceId` | Bảng `audit_log` — đã có `ApiLogAspect` nhưng cần chuẩn hóa |
| Audit KHÔNG bị xóa (chỉ archive) | Retention 2-7 năm tùy compliance |

### 9.2 Soft Delete

| Rule | Chi tiết |
|------|----------|
| Mọi entity business → có `is_deleted BOOLEAN DEFAULT FALSE` (đã có ở `BaseEntity`) | Thêm `deleted_at TIMESTAMPTZ`, `deleted_by VARCHAR(36)` |
| Query mặc định filter `is_deleted = false` | Dùng Hibernate `@Where(clause = "is_deleted = false")` hoặc Specification helper |
| Endpoint DELETE thực chất set `is_deleted = true` | Hard delete chỉ có ở data retention job |
| Restore endpoint (nếu cần) → `PUT /xxx/{id}/restore` set `is_deleted = false` | Cần permission cao |

### 9.3 Optimistic Locking

| Rule | Chi tiết |
|------|----------|
| Entity có concurrent edit (Order, Contract, Product Stock, User Profile) → thêm `@Version private Long version` | Hibernate tự bump, throw `OptimisticLockException` khi conflict |
| GlobalExceptionHandler convert → HTTP 409 Conflict + errorCode `CONCURRENT_MODIFICATION` | FE hiển thị "Có người khác vừa sửa, vui lòng tải lại" |

### 9.4 Idempotency

| Rule | Chi tiết |
|------|----------|
| Mọi POST tạo resource có ảnh hưởng tiền / stock → yêu cầu header `Idempotency-Key` | UUID do client sinh |
| Server lưu `(idempotency_key, response_hash, expires_at)` 24h | Request trùng → trả lại response cũ |

### 9.5 Rate Limit

| Endpoint | Rule |
|----------|------|
| `POST /auth/login` | 5 req / 5 phút / IP + username |
| `POST /auth/forgot-password` | 3 req / giờ / email |
| Mọi API authenticated | 100 req / phút / user (tune theo pricing plan) |
| Export lớn (`GET /xxx/export`) | 1 req / phút / user |

Dùng **Bucket4j** (đã có deps). Header response: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`. Vượt → HTTP 429.

### 9.6 Retry & Circuit Breaker

| Cấu hình | Chi tiết |
|----------|----------|
| Retry cho external call (email, SMS, payment, AI OCR) | `Spring Retry` (`@Retryable(maxAttempts=3, backoff=@Backoff(delay=1000, multiplier=2))`) |
| Circuit breaker cho service không ổn định | `Resilience4j` (bổ sung deps) — chưa có, cần thêm |
| Timeout mọi outbound HTTP | Connect 3s, read 10s |

### 9.7 Caching

| Loại | Cache | TTL |
|------|-------|-----|
| User permissions | Caffeine `perm:userId` | 5 phút, evict khi role/permission thay đổi |
| Menu tree | Caffeine `menu:tree` | 10 phút, evict manual |
| DanhMucDungChung (lookup) | Caffeine | 30 phút |
| JWT public key (nếu chuyển RS256) | Caffeine | 1 giờ |

Config qua `@EnableCaching` + `@Cacheable`/`@CacheEvict`. Tránh cache DTO đang được modify.

### 9.8 Distributed Lock (khi lên multi-instance)

- Scheduler (`@Scheduled`) chạy 1 lần / instance → dùng **ShedLock** (bảng `shedlock` trong DB) hoặc Redis Redisson.
- Race-condition trên stock: lock `stock:{productId}` Redis 5s.

### 9.9 Event & Outbox

| Rule | Chi tiết |
|------|----------|
| Intra-module event | Spring `ApplicationEventPublisher` + `@EventListener` |
| Cross-module / cross-service | Chuẩn bị **Kafka** — publish qua `OutboxTable` (event lưu cùng transaction), có `OutboxRelay` scheduler đọc DB → publish Kafka → mark sent. Đảm bảo **exactly-once semantic** |
| Event schema | Avro / JSON Schema versioned. KHÔNG breaking change |

---

## 10. Chuẩn i18n & Error Code

### 10.1 Bắt buộc: Enum ErrorCode tập trung

**Vấn đề hiện tại:** i18n key literal rải rác 30+ chỗ, `AppException + QTHTException + AuthException` trùng chức năng.

**Fix:** 1 enum tập trung per module + interface chung.

```java
// module-common
public interface ErrorCode {
    String key();          // i18n key
    HttpStatus status();
    String defaultMessage();
}

// module-qtht-bom
public enum QthtErrorCode implements ErrorCode {
    DEPARTMENT_NOT_FOUND("invalid.department.entity.not.found", HttpStatus.NOT_FOUND, "Phòng ban không tồn tại"),
    DEPARTMENT_CODE_EXISTS("department.code.already.exists", HttpStatus.CONFLICT, "Mã phòng ban đã tồn tại"),
    DEPARTMENT_HAS_MEMBERS("department.cannot.delete.has.members", HttpStatus.CONFLICT, "Không xoá được phòng ban đang có nhân viên"),
    // ...
    ;
    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
    // constructor + getters
}
```

Sử dụng:
```java
throw new AppException(QthtErrorCode.DEPARTMENT_NOT_FOUND, departmentId);
```

**Bỏ:** `QTHTException`, `AuthException` (chỉ giữ `AppException` + `ErrorCode`).

### 10.2 Message convention

- `messages.properties` (default = English)
- `messages_vi.properties` (Vietnamese)
- Key format: `<layer>.<domain>.<what>` — ví dụ:
  - `exception.department.not.found=Phòng ban {0} không tồn tại`
  - `validation.person.email.invalid=Email {0} không hợp lệ`
  - `success.order.created=Đơn hàng {0} đã tạo thành công`
- Args positional `{0}`, `{1}` — không dùng name `{name}` (MessageSource chuẩn Java)

---

## 11. Self-Review Checklist

Trước khi PR / commit, chạy qua checklist này:

### 11.1 Architecture
- [ ] Không có business logic trong Controller
- [ ] Không có business logic trong Repository
- [ ] Service depend on interface (không phải Impl trực tiếp)
- [ ] Cross-module gọi qua Service interface, không nhảy Repository
- [ ] Package nằm đúng `com.frezo.<domain>.{controller|service|...}`

### 11.2 API
- [ ] URL kebab-case, plural noun (`/departments` không `/department`)
- [ ] Response wrap trong `ApiResponse<T>` (KHÔNG dùng `Response` cũ, KHÔNG `Map<String,Object>`)
- [ ] Pagination trả `PageResponse<T>` (KHÔNG `Map` với keys `items/total/pageNumber`)
- [ ] Validation `@Valid` cho MỌI `@RequestBody`
- [ ] Có `@Operation` OpenAPI cho mọi endpoint

### 11.3 Security
- [ ] Endpoint có `@CheckPermission` (hoặc `permitAll()` public rõ ràng)
- [ ] Không log password / token / secret
- [ ] Password BCrypt (KHÔNG NoOp / plain)
- [ ] Query dùng parameterized binding, không concat SQL

### 11.4 Data
- [ ] Entity extend `BaseEntity` (có UUID + audit + soft delete)
- [ ] `@ManyToOne` / `@OneToMany` là `LAZY` (KHÔNG EAGER)
- [ ] Query list có `Pageable`
- [ ] Query có relation → `@EntityGraph` hoặc DTO projection (không N+1)
- [ ] Migration script Flyway `V<yyyyMMddHHmm>__<desc>.sql`
- [ ] Index cho column WHERE / ORDER BY / FK

### 11.5 Code quality
- [ ] Method ≤ 40 dòng, Class ≤ 400 dòng, Controller ≤ 200 dòng, Service ≤ 500 dòng
- [ ] Không có `TODO`/`FIXME` không có ticket ID
- [ ] Không `System.out.println`, `printStackTrace`
- [ ] Không `Optional.get()` mà không check
- [ ] Không nested if > 3 tầng
- [ ] Không `Object`/`Map<String,Object>` làm public API
- [ ] Không field injection `@Autowired`
- [ ] Không try-catch nuốt exception
- [ ] `log.error("msg", ex)` khi log exception (không `.getMessage()`)

### 11.6 Test (khi bắt đầu bổ sung test)
- [ ] Unit test cho service dùng Mockito
- [ ] Integration test cho controller dùng `@SpringBootTest` + `MockMvc` + Testcontainers PostgreSQL (KHÔNG H2)
- [ ] Coverage service ≥ 80%

---

## 12. Prompt Template cho AI

Khi nhờ Cursor / Claude / Copilot code, dùng template này:

<details>
<summary><b>Template — Tạo endpoint mới</b></summary>

```
Task: Tạo endpoint <mô tả>

Module: com.frezo.<domain>
Aggregate: <Entity>

Yêu cầu:
- Đọc: FrezoBE/AI_BACKEND_ENGINEERING_GUIDE.md + API_DESIGN_STANDARD.md
- Response: ApiResponse<T> hoặc PageResponse<T> (KHÔNG Response cũ, KHÔNG Map)
- Validation: @Valid trong DTO
- Exception: dùng AppException + <Domain>ErrorCode enum
- Permission: thêm @CheckPermission(api = "...", action = "...")
- Transaction: @Transactional (readOnly = true nếu query)
- Mapper: MapStruct, không map manual
- Pagination: bắt buộc nếu là list

Files cần tạo/sửa:
- Controller: com/frezo/<domain>/controller/XxxController.java
- Service: com/frezo/<domain>/service/XxxService.java + impl/XxxServiceImpl.java
- Repository: com/frezo/<domain>/repository/XxxRepository.java
- DTO: dto/request/CreateXxxRequest.java + dto/response/XxxResponse.java
- Mapper: mapper/XxxMapper.java
- ErrorCode: constant/<Domain>ErrorCode.java (thêm entry nếu chưa có)
- i18n: module-server/src/main/resources/i18n/messages_vi.properties

Sau khi code xong:
- Chạy Self-review Checklist §11
```
</details>

<details>
<summary><b>Template — Fix N+1 / performance</b></summary>

```
Task: Fix N+1 trong <method>

Đọc:
- AI_BACKEND_ENGINEERING_GUIDE.md §6 (Performance)
- SPRING_BOOT_BEST_PRACTICE.md §3 (JPA)

Yêu cầu:
- Chạy log SQL (bật hibernate.SQL DEBUG local) đếm số query
- Dùng @EntityGraph HOẶC JOIN FETCH HOẶC DTO Projection
- Không đổi API contract (response y hệt)
- Đo lại số query sau fix (mục tiêu: từ N+1 xuống 1-3 query)

Nếu là combobox / dropdown → prefer DTO Projection
Nếu là detail page nhiều relation → prefer @EntityGraph
Nếu là list + filter phức tạp → prefer Specification + @EntityGraph
```
</details>

<details>
<summary><b>Template — Thêm module mới</b></summary>

```
Task: Thêm module <domain>

Đọc:
- AI_BACKEND_ENGINEERING_GUIDE.md §1-3 (Architecture, Package, Layer)

Yêu cầu:
1. Tạo 2 Maven module: module-<domain>-bom + module-<domain>-res
2. Package: com.frezo.<domain>
3. Thêm vào FrezoBE/pom.xml <modules>
4. Thêm dependency 2 module vào module-server/pom.xml
5. Thêm @ComponentScan basePackage vào FrezoServerApplication (nếu cần)
6. Thêm @EntityScan basePackage vào FrezoServerApplication
7. Thêm mapper package vào MapStructConfig @ComponentScan
8. Tạo <Domain>ErrorCode enum trong constant/
9. Tạo <Domain>Properties nếu có config riêng

Không tạo circular dependency với module khác.
```
</details>

---

## 13. Migration Path (từ code hiện tại → chuẩn)

Codebase hiện có 5 anti-patterns lớn cần fix theo thứ tự:

| Priority | Anti-pattern | Effort | Rủi ro |
|----------|--------------|--------|--------|
| P0 | `NoOpPasswordEncoder` → BCrypt + migrate hash | 1 day + migration script | Cao (touch auth) |
| P0 | CORS `*` → whitelist env | 30 min | Thấp |
| P0 | JWT secret hardcode default → env-only, fail-fast | 30 min | Trung |
| P1 | Bật lại `@CheckPermission` toàn bộ controllers + `@EnableMethodSecurity` | 2 days | Cao (touch mọi endpoint) |
| P1 | Bỏ `Response<T>` (util/web), unify về `ApiResponse<T>` | 3 days | Trung (touch nhiều controller) |
| P1 | Bỏ `QTHTException`, `AuthException`, unify về `AppException` + `ErrorCode` enum | 2 days | Trung |
| P2 | `Map<String,Object>` → `PageResponse<T>` | 3 days | Trung |
| P2 | Thêm Flyway `V1__init.sql` từ current schema, tắt `ddl-auto=update` | 1 day | Cao (schema drift) |
| P2 | Thêm `@Version` cho entity có concurrent edit | 1 day + migration | Trung |
| P3 | Thêm `TraceIdFilter` + MDC | 2 hours | Thấp |
| P3 | Xóa `DriverManager.getConnection` trong `main()` | 10 min | Thấp |
| P3 | Thêm test (JUnit 5 + Testcontainers) — start với `module-common` + `module-qtht-bom` | 5+ days | Thấp |

Chi tiết từng anti-pattern xem file chuyên đề tương ứng.

---

## 14. Tham chiếu

- [API_DESIGN_STANDARD.md](./API_DESIGN_STANDARD.md) — REST + `ApiResponse<T>` + pagination + OpenAPI + idempotency
- [SPRING_BOOT_BEST_PRACTICE.md](./SPRING_BOOT_BEST_PRACTICE.md) — DI + JPA + transaction + validation + exception + mapper + testing
- [DATABASE_STANDARD.md](./DATABASE_STANDARD.md) — PostgreSQL + naming + migration + index + FK + audit + query optimization
- [FrezoFE/FE_UI_UX_STANDARD.md](../FrezoFE/FE_UI_UX_STANDARD.md) — FE contract (biết BE trả gì để FE hiển thị đúng)

---

*File này là source of truth cho backend engineering. Cập nhật khi có quyết định kiến trúc mới (thêm module, đổi tech stack, thêm pattern). PR sửa file này cần có ít nhất 1 senior approve.*
