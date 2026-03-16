package com.server.board.controller;


import com.server.board.domain.dto.*;
import com.server.board.service.TestService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper; // json

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

//구글 OTP
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

@RestController
@RequestMapping("/api") // 프리픽스: /api/...
public class TestController {

    private final TestService service;

    public TestController(TestService service) {
        this.service = service;
    }

    @GetMapping("/testDB")
    public List<NameRow> list() {
        return service.getAll();
    }

    @PostMapping("/insertName")
    public String insert(@RequestBody CreateNameRequest req) {
        int n = service.add(req.name());
        return n + "행 삽입 완료: " + req.name();
    }

    @PostMapping("/viewPageData")
    public List<ViewPageData> viewPageData(@RequestBody Map<String, String> req) {
        List<ViewPageData> result = service.viewPageData(req.get("pagename"));

        //존재하지 않는 데이터이면 빈값형식을 만들어서 리턴
        if(result.isEmpty())
        {
            List<ViewPageData> empty = List.of(
                    new ViewPageData(0L, "", "", "")
            );
            return empty;
        }
        else
            return result;
    }
    @PostMapping("/viewPageDataAll")
    public List<ViewPageData> viewPageDataAll() {
        return service.viewPageDataAll();
    }


    @PatchMapping("/updateName")
    public String update(@RequestBody UpdateNameRequest req) {
        int n = service.rename(req.oldName(), req.newName());
        return n + "개의 행이 " + req.oldName() + " → " + req.newName() + " 로 업데이트되었습니다.";
    }

    @PostMapping("/insertPageData")
    public String insertPageData(@RequestBody Map<String, String> req, HttpSession session) {
        Object usertype = session.getAttribute("usertype");
        if (!"admin".equals(usertype)) {
            throw new IllegalArgumentException("not allowed user");
        }
        List<ViewPageData> result = service.viewPageData(req.get("pagename"));

        //존재하지 않는 데이터이면 빈값형식을 만들어서 리턴
        if(result.isEmpty())
        {
            int n = service.insertPageData(req.get("pagename"), req.get("content"), req.get("memo"));
            return "Success";
        }
        else
        {
            return "AlreadyExist";
        }


    }

    @PostMapping("/updatePageData")
    public String updatePageData(@RequestBody Map<String, String> req, HttpSession session) {
        Object usertype = session.getAttribute("usertype");
        if (!"admin".equals(usertype)) {
            throw new IllegalArgumentException("not allowed user");
        }
        int n = service.updatePageData(req.get("pagename"), req.get("content"), req.get("memo"));
        return n + "개의 행이 업데이트되었습니다.";
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> req, HttpSession session) {
        String id = req.get("id");
        String pw = req.get("pw");
        Map<String, String> result = new HashMap<>(); // 리턴하는 결과값
        int maxLoginFailcount = 5;

        //로그인 실패카운트 사전체크
        String failcountResult = service.viewLoginFailcount(id, maxLoginFailcount);
        if ("locked".equals(failcountResult)) {
            result.put("result","locked");
            result.put("id","fail");
            result.put("usertype","fail");
            result.put("LoginFailcount","-1");
            return result;
        }
        String result_isidexist = service.isidexist(id);
        if(result_isidexist.equals("false"))
        {
            result.put("result","id_not_exist");
            result.put("id",id);
            result.put("usertype","unknown");
            return result;
        }

        //로그인 프로세스 시작
        List<Login> LoginResult = service.login(id, pw);
        //디버깅용 메시지
        System.out.println(LoginResult);
        //아이디 패스워드 비교 실패. 로그인 실패
        if (LoginResult.isEmpty()) {
            System.out.println("logins.is Empty");
            //로그인 실패카운트 +1
            service.updateLoginFailcount(id, 1);
            int currentlogincount = service.getLoginFailcount(id);

            result.put("result","fail");
            result.put("id","fail");
            result.put("usertype","fail");
            result.put("LoginFailcount", String.valueOf(currentlogincount));

        }

        //구글OTP 사용여부 검사. DB에서 해당 계정 검색해 otp_enabed 값 확인
        //otp_enabled가 "false"이면 바로 로그인 성공. return loginSuccess(id, LoginResult, session);
        //otp_enabled가 "true"이면 otp 검사 로직 시작
            //otp_secret이 null이면 otp_create 리턴. otp 시크릿키값 생성해서 프론트로 리턴
            //otp_secret이 null이면 otp_verify 리턴.

        List<ViewOTPStatus>result_otpstatus = getOTPStatus(id);
        //result_otpstatus.getFirst().otp_enabled();
        //result_otpstatus.getFirst().otp_secret();

        //OTP 등록
        if(Objects.equals(result_otpstatus.getFirst().otp_enabled(), "true") && Objects.equals(result_otpstatus.getFirst().otp_secret(), ""))
        {
            Map<String, Object> otp_create_result = GoogleOTPCreate(id);
            String secretkey = (String)otp_create_result.get("otp_secret");
            String otp_qr = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL("MyApp", "none", (GoogleAuthenticatorKey) otp_create_result.get("otp_secretOrigin"));

            result.put("result","otp_create");
            result.put("id",id);
            result.put("usertype",LoginResult.getFirst().usertype());
            result.put("otp_sk", secretkey);
            result.put("otp_QR", otp_qr);

            return result;
        }
        //OTP 인증
        else if(Objects.equals(result_otpstatus.getFirst().otp_enabled(), "true") && !Objects.equals(result_otpstatus.getFirst().otp_secret(), ""))
        {
            result.put("result","otp_verify");
            result.put("id",id);
            result.put("usertype",LoginResult.getFirst().usertype());
            return result;
        }
        //OTP 필요없음
        else
        {
            return loginSuccess(id, LoginResult, session);
        }

        //로그인 성공
        //return loginSuccess(id, LoginResult, session);
    }
    private Map<String, String> loginSuccess(String id, List<Login> LoginResult, HttpSession session)
    {
        Map<String, String> result = new HashMap<>(); // 리턴하는 결과값

        //로그인 성공
        System.out.println("logins.is not Empty");
        //로그인 실패카운트 초기화
        service.updateLoginFailcount(id, 0);

        result.put("result","ok");
        result.put("id",LoginResult.getFirst().id());
        result.put("usertype",LoginResult.getFirst().usertype());
        result.put("LoginFailcount","0");

        //세션생성
        setSession(session, id, LoginResult.getFirst().usertype());
        System.out.println("세션ID=" + session.getId());
        System.out.println("userId=" + session.getAttribute("userId"));

        return result;
    }
    private Map<String, String> loginFail(int loginfailcount)
    {
        Map<String, String> result = new HashMap<>(); // 리턴하는 결과값
        result.put("result","fail");
        result.put("id","fail");
        result.put("usertype","fail");
        result.put("LoginFailcount", String.valueOf(loginfailcount));
        return result;
    }

    private List<ViewOTPStatus> getOTPStatus(String id)
    {
        List<ViewOTPStatus> result_otpstatus = service.getGoogleOTPStatus(id);
        //result_otpstatus.getFirst().otp_enabled();
        //result_otpstatus.getFirst().otp_secret();
        return result_otpstatus;
    }

    private Map<String, Object> GoogleOTPCreate(String id)
    {
        Map<String, Object> result = new HashMap<>();
        final GoogleAuthenticator gAuth = new GoogleAuthenticator(); // 구글OTP
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();

        int result_setOtpSecret = service.setOtpSecret(id, secret);
        if(result_setOtpSecret == 1)
        {
            System.out.println("OTP Secret Key create Success" + result);
            result.put("otp_secret",secret);
            result.put("otp_secretOrigin",key);
        }
        else
        {
            System.out.println("OTP Secret Key create ERROR" + result);
            result.put("otp_secret","none");
            result.put("otp_secretOrigin",null);
            throw new IllegalArgumentException("OTP Secret Key create ERROR");
        }
        return result;
    }

    @PostMapping("/GoogleOTPLogin")
    public Map<String, String> GoogleOTPLogin(@RequestBody Map<String, String> req, HttpSession session)
    {
        String id = req.get("id");
        String usertype = req.get("usertype");
        String otpcode = req.get("otpcode");

        System.out.println("id, otpcode");
        System.out.println(id+"  "+otpcode);

        final GoogleAuthenticator gAuth = new GoogleAuthenticator(); // 구글OTP
        List<ViewOTPStatus> result_otpstatus = getOTPStatus(id);
        String otp_secret = result_otpstatus.getFirst().otp_secret();

        //otp 검증
        boolean authorizeresult = gAuth.authorize(otp_secret, Integer.parseInt(otpcode));

        if(authorizeresult)
        {
            System.out.println("otp authorize success ");
            //record Login(String id, String usertype)
            List<Login> LoginResult = List.of(
                    new Login(id, usertype)
            );
            return loginSuccess(id, LoginResult, session);
        }
        else
        {
            System.out.println("otp authorize fail ");
            //로그인 실패카운트 +1
            service.updateLoginFailcount(id, 1);
            int currentlogincount = service.getLoginFailcount(id);
            return loginFail(currentlogincount);
        }
    }

    @PostMapping("/test_gooleotpreset")
    public String test_gooleotpreset()
    {
        System.out.println("Test : OTP Reset start");
        String id = "otptest";//req.get("id");
        int result = service.test_gooleotpreset(id);
        System.out.println("Test : OTP Reset end : "+ result);
        return "ok";
    }

    //세션 관리 로직들
    //세션 생성
    public void setSession(HttpSession session, String userid, String usertype) {
        session.setAttribute("userid", userid);
        session.setAttribute("usertype", usertype);

        System.out.println("setSession 세션ID=" + userid);
    }

    //세션 유무 조회
    @PostMapping("/getsession")
    public Map<String, Object> getSession(HttpSession session) {
        Map<String, Object> res = new HashMap<>();

        Object userid = session.getAttribute("userid");
        Object usertype = session.getAttribute("usertype");

        res.put("userid", userid);
        res.put("usertype", usertype);
        res.put("haveSession", userid != null);

        System.out.println("GET 세션ID=" + session.getId());
        System.out.println("GET userid=" + userid);
        System.out.println("GET usertype=" + usertype);

        return res;
    }

    //세션 삭제
    @PostMapping("/deleteLoginSession")
    public String deleteLoginSession(HttpSession session) {
        session.invalidate();
        return "세션 삭제 완료";
    }


    //이미지 업로드
    @PostMapping(
                value = "/uploadMultimedia",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, Object> uploadMultimedia(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "meta", required = false) String metaJson,
            HttpSession session
    )
    {
        Object usertype = session.getAttribute("usertype");
        if (!"admin".equals(usertype)) {
            System.out.println("usertype = " + usertype);
            throw new IllegalArgumentException("not allowed user");
        }
        Map<String, Object> response = new HashMap<>();

        try {
            // 1) 현재 작업 디렉토리 (프로젝트 루트일 가능성이 큼)
            Path rootPath = Paths.get("").toAbsolutePath();
            System.out.println("rootPath = " + rootPath);

            // 2) ./multimedia 절대 경로 만들기
            Path multimediaDir = rootPath.resolve("multimedia");
            Files.createDirectories(multimediaDir); // 없으면 생성

            System.out.println("multimediaDir = " + multimediaDir);

            // 3) 파일명/확장자
            String originalName = file.getOriginalFilename();
            String ext = Objects.requireNonNull(originalName)
                    .substring(originalName.lastIndexOf("."))
                    .toLowerCase();

            if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") && !ext.equals(".pdf")) {
                response.put("ok", false);
                response.put("message", "jpg/png/pdf만 업로드 가능합니다.");
                return response;
            }

            String saveName = UUID.randomUUID() + ext;
            Path savePath = multimediaDir.resolve(saveName);
            System.out.println("savePath = " + savePath);

            // 4) 임시파일 → 우리가 지정한 절대 경로로 이동
            file.transferTo(savePath.toFile());

            // 5) DB처리
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> meta = mapper.readValue(metaJson, Map.class);
            String username = meta.get("username");
            String description = meta.get("description");
            System.out.println("metaJson username = " + username);
            System.out.println("metaJson description = " + description);

            int n = service.addMultimedia(username,description,saveName,ext);

            // 6) response 처리
            response.put("ok", true);
            response.put("filename", saveName);
            response.put("path", savePath.toString());
            response.put("meta", metaJson);
            return response;

        }
        catch (Exception e) {
            e.printStackTrace();
            response.put("ok", false);
            response.put("message", e.toString());
            return response;
        }
    }

    @PostMapping("/viewMediaData")
    public List<ViewMediaData> viewMediaData() {
        return service.viewMediaData();
    }

    @PostMapping("/ViewPageContenttasksummary")
    public List<ViewPageContenttasksummary> ViewPageContenttasksummary() {
        return service.ViewPageContenttasksummary();
    }

    @PostMapping("/UpdatePageContenttasksummary")
    public String UpdatePageContenttasksummary(
            @RequestBody UpdatePageContentTaskSummaryRequest req,
            HttpSession session
    ) {
        Object usertype = session.getAttribute("usertype");
        if (!"admin".equals(usertype)) {
            throw new IllegalArgumentException("not allowed user");
        }

        if (req == null) {
            throw new IllegalArgumentException("request body is null");
        }

        var yearGroups = req.getYearGroups();
        var projects = req.getProjects();

        if (yearGroups == null) yearGroups = List.of();
        if (projects == null) projects = List.of();

        return service.UpdatePageContenttasksummary(yearGroups, projects);
    }



}


