package com.server.board.controller;

import com.server.board.config.JwtUtil;
import com.server.board.domain.dto.*;
import com.server.board.service.MainService;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtUtil jwtUtil;

    public LoginController(MainService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> req) {
        String id = req.get("id");
        String pw = req.get("pw");
        Map<String, String> result = new HashMap<>();
        int maxLoginFailcount = 5;

        String failcountResult = service.viewLoginFailcount(id, maxLoginFailcount);
        if ("locked".equals(failcountResult)) {
            result.put("result", "locked");
            result.put("id", "fail");
            result.put("usertype", "fail");
            result.put("LoginFailcount", "-1");
            return result;
        }

        String result_isidexist = service.isidexist(id);
        if (result_isidexist.equals("false")) {
            result.put("result", "id_not_exist");
            result.put("id", id);
            result.put("usertype", "unknown");
            return result;
        }

        List<Login> LoginResult = service.login(id, pw);
        System.out.println(LoginResult);

        if (LoginResult.isEmpty()) {
            System.out.println("logins.is Empty");
            service.updateLoginFailcount(id, 1);
            int currentlogincount = service.getLoginFailcount(id);

            result.put("result", "fail");
            result.put("id", "fail");
            result.put("usertype", "fail");
            result.put("LoginFailcount", String.valueOf(currentlogincount));
            return result;
        }

        List<ViewOTPStatus> result_otpstatus = getOTPStatus(id);

        if (Objects.equals(result_otpstatus.getFirst().otp_enabled(), "true")
                && Objects.equals(result_otpstatus.getFirst().otp_secret(), "")) {
            Map<String, Object> otp_create_result = GoogleOTPCreate(id);
            String secretkey = (String) otp_create_result.get("otp_secret");
            String otp_qr = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                    "MyApp", "none", (GoogleAuthenticatorKey) otp_create_result.get("otp_secretOrigin"));

            result.put("result", "otp_create");
            result.put("id", id);
            result.put("usertype", LoginResult.getFirst().usertype());
            result.put("otp_sk", secretkey);
            result.put("otp_QR", otp_qr);
            return result;
        } else if (Objects.equals(result_otpstatus.getFirst().otp_enabled(), "true")
                && !Objects.equals(result_otpstatus.getFirst().otp_secret(), "")) {
            result.put("result", "otp_verify");
            result.put("id", id);
            result.put("usertype", LoginResult.getFirst().usertype());
            return result;
        } else {
            return loginSuccess(id, LoginResult);
        }
    }

    private Map<String, String> loginSuccess(String id, List<Login> LoginResult) {
        Map<String, String> result = new HashMap<>();

        System.out.println("logins.is not Empty");
        service.updateLoginFailcount(id, 0);

        String usertype = LoginResult.getFirst().usertype();
        String token = jwtUtil.generateToken(id, usertype);

        result.put("result", "ok");
        result.put("id", LoginResult.getFirst().id());
        result.put("usertype", usertype);
        result.put("LoginFailcount", "0");
        result.put("token", token);

        System.out.println("JWT 발급 완료: userid=" + id);
        return result;
    }

    private Map<String, String> loginFail(int loginfailcount) {
        Map<String, String> result = new HashMap<>();
        result.put("result", "fail");
        result.put("id", "fail");
        result.put("usertype", "fail");
        result.put("LoginFailcount", String.valueOf(loginfailcount));
        return result;
    }

    private List<ViewOTPStatus> getOTPStatus(String id) {
        return service.getGoogleOTPStatus(id);
    }

    private Map<String, Object> GoogleOTPCreate(String id) {
        Map<String, Object> result = new HashMap<>();
        final GoogleAuthenticator gAuth = new GoogleAuthenticator();
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();

        int result_setOtpSecret = service.setOtpSecret(id, secret);
        if (result_setOtpSecret == 1) {
            System.out.println("OTP Secret Key create Success" + result);
            result.put("otp_secret", secret);
            result.put("otp_secretOrigin", key);
        } else {
            System.out.println("OTP Secret Key create ERROR" + result);
            result.put("otp_secret", "none");
            result.put("otp_secretOrigin", null);
            throw new IllegalArgumentException("OTP Secret Key create ERROR");
        }
        return result;
    }

    @PostMapping("/GoogleOTPLogin")
    public Map<String, String> GoogleOTPLogin(@RequestBody Map<String, String> req) {
        String id = req.get("id");
        String usertype = req.get("usertype");
        String otpcode = req.get("otpcode");

        System.out.println("id, otpcode");
        System.out.println(id + "  " + otpcode);

        final GoogleAuthenticator gAuth = new GoogleAuthenticator();
        List<ViewOTPStatus> result_otpstatus = getOTPStatus(id);
        String otp_secret = result_otpstatus.getFirst().otp_secret();

        boolean authorizeresult = gAuth.authorize(otp_secret, Integer.parseInt(otpcode));

        if (authorizeresult) {
            System.out.println("otp authorize success ");
            List<Login> LoginResult = List.of(new Login(id, usertype));
            return loginSuccess(id, LoginResult);
        } else {
            System.out.println("otp authorize fail ");
            service.updateLoginFailcount(id, 1);
            int currentlogincount = service.getLoginFailcount(id);
            return loginFail(currentlogincount);
        }
    }

    @PostMapping("/test_gooleotpreset")
    public String test_gooleotpreset() {
        System.out.println("Test : OTP Reset start");
        String id = "otptest";
        int result = service.test_gooleotpreset(id);
        System.out.println("Test : OTP Reset end : " + result);
        return "ok";
    }

    // JWT 유효성 검증 (기존 getsession 대체)
    @PostMapping("/verifytoken")
    public Map<String, Object> verifyToken(HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        Claims claims = (Claims) request.getAttribute("jwtClaims");

        if (claims != null) {
            res.put("userid", claims.getSubject());
            res.put("usertype", claims.get("usertype", String.class));
            res.put("haveSession", true);
        } else {
            res.put("userid", null);
            res.put("usertype", null);
            res.put("haveSession", false);
        }
        return res;
    }

    // JWT는 무상태(stateless)이므로 서버에서 삭제할 토큰이 없음. 쿠키 삭제는 프론트에서 처리.
    @PostMapping("/deleteLoginSession")
    public String deleteLoginSession() {
        return "ok";
    }
}
