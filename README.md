com.server.board
├─ BoardApplication.java
├─ config
│  └─ DataSourceConfig.java            # SQLite DataSource + JdbcTemplate
├─ controller
│  └─ TestController.java              # /testDB, /insertName, /updateName 등 API
├─ service
│  └─ TestService.java                 # 트랜잭션/비즈니스 로직
├─ repository
│  ├─ TestRepository.java              # 인터페이스
│  └─ TestRepositoryJdbc.java          # JdbcTemplate로 SQL 실행 (쿼리 보관소)
├─ domain
│  ├─ dto
│  │  ├─ NameRow.java                  # GET 응답 DTO (id, name)
│  │  ├─ CreateNameRequest.java        # POST 바디 DTO
│  │  └─ UpdateNameRequest.java        # PATCH/PUT 바디 DTO
│  └─ model                            # (추후 엔티티/도메인 클래스 필요 시)
└─ exception
├─ NotFoundException.java           # 필요 시
└─ GlobalExceptionHandler.java      # @RestControllerAdvice (선택)



===================================================================

BoardApplication.java
스프링 부트 진입점. 애플리케이션 컨텍스트를 만들고(IOC 컨테이너), 아래 컴포넌트들을 스캔/등록.

config/
DataSourceConfig: DataSource(SQLite 연결)와 JdbcTemplate 빈을 등록.
→ 이후 모든 DB 접근은 JdbcTemplate을 통해 수행.

controller/
TestController: HTTP 요청을 받는 레이어. URL 매핑, 요청 파라미터/바디 파싱, 응답 작성(JSON).
비즈니스 로직은 직접 하지 않고 Service에 위임.

service/
TestService: 도메인 규칙/트랜잭션/검증을 담당.
여러 Repository 호출 조합, 권한·검증 로직 위치.
DB 세부사항(SQL)은 모름(관심사 분리).

repository/
TestRepository 인터페이스: “무엇을 할 수 있는지” 계약.
TestRepositoryJdbc 구현체: 실제 SQL이 있는 곳. JdbcTemplate 사용해서 쿼리 실행, Row 매핑.

domain/dto/
API 입·출력용 데이터 구조. (예: NameRow, CreateNameRequest, UpdateNameRequest)

Controller ↔ Service/Repository 사이에서 타입 안정성과 가독성↑.
exception/
도메인/애플리케이션 예외와 전역 예외 처리기(@RestControllerAdvice)를 둬서, 에러를 JSON 포맷으로 깔끔히 응답.

===========================================================================

애플리케이션 기동 시 내부 동작 순서

1. main() 실행 → SpringApplication.run()

2. 컴포넌트 스캔
com.server.board 패키지 하위에서 @Configuration, @RestController, @Service, @Repository, @Component 등을 찾아 빈으로 등록.

3. 설정 빈 등록
DataSourceConfig의 @Bean 메서드 실행 → DataSource, JdbcTemplate 생성.

4. 의존성 주입(DI)
TestRepositoryJdbc(JdbcTemplate) 생성 → TestService(TestRepository) 생성 → TestController(TestService) 생성.

5. 웹 레이어 준비
@RequestMapping, @Get/Post/Patch…Mapping 들이 핸들러로 등록됨(기동 로그에 Mapped … 표시).


=========================================================================
요청 1건이 처리되는 전체 흐름

예) GET /api/testDB
HTTP 요청이 들어옴 (내장 톰캣).
HandlerMapping이 URL과 메서드를 매핑하여 TestController#list() 선택.
Controller는 Service 호출 → TestService#getAll().
Service는 Repository 호출 → TestRepositoryJdbc#findAll().
Repository는 JdbcTemplate로 SQL 실행 → ResultSet을 NameRow 리스트로 매핑.
리턴 체인: Repository → Service → Controller.
JSON 직렬화: @RestController 덕분에 Jackson이 반환객체(List<NameRow>)를 JSON으로 변환.
HTTP 응답 전송.



========================================
TestService, TestRepository, TestRepositoryJDBC, TestController 동작원리
HTTP 요청
   ↓
[Controller]  — 요청·응답 담당
   ↓
[Service]     — 비즈니스 로직 담당
   ↓
[Repository]  — DB 접근 담당 (인터페이스)
   ↓
[RepositoryJdbc] — SQL 실행 (구현체)
   ↓
DB(SQLite)