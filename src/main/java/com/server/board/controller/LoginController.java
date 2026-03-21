package com.server.board.controller;

import com.server.board.domain.dto.*;
import com.server.board.service.MainService;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class LoginController {
    private final MainService service;

    public LoginController(MainService service) {
        this.service = service;
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
            return result;
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
}
