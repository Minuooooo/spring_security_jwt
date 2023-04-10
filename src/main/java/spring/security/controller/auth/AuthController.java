package spring.security.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.security.dto.member.MemberRequestDto;
import spring.security.dto.member.MemberResponseDto;
import spring.security.dto.sign.TokenDto;
import spring.security.dto.sign.TokenRequestDto;
import spring.security.dto.sign.TokenResponseDto;
import spring.security.response.Response;
import spring.security.service.auth.AuthService;

import static spring.security.response.Response.*;
import static spring.security.response.SuccessMessage.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Auth API Document")
public class AuthController {

    private final AuthService authService;

    @GetMapping
    public String hello() {
        return "Hello Docker!!!";
    }

    @PostMapping("/sign-up")
    @Operation(summary = "회원 가입", description = "회원 가입 API 입니다.")
    public Response<MemberResponseDto> signUp(@RequestBody MemberRequestDto memberRequestDto) {
        authService.signUp(memberRequestDto);
        return success(SIGN_UP_SUCCESS);
    }

    @PostMapping("/sign-in")
    @Operation(summary = "로그인", description = "로그인 API 입니다.")
    public Response<TokenResponseDto> signIn(@RequestBody MemberRequestDto memberRequestDto) {
        TokenDto tokenDto = authService.SignIn(memberRequestDto);
        TokenResponseDto tokenResponseDto = TokenResponseDto.builder()
                .accessToken(tokenDto.getAccessToken())
                .refreshToken(tokenDto.getRefreshToken())
                .build();
        return success(LOGIN_SUCCESS, tokenResponseDto);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "로그아웃 API 입니다.")
    public ResponseEntity<String> logout(@RequestBody TokenRequestDto tokenRequestDto) {
        authService.logout(tokenRequestDto);
        return ResponseEntity.ok("로그아웃 성공");
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "토큰 재발급 API 입니다.")
    public ResponseEntity<TokenDto> reissue(@RequestBody TokenRequestDto tokenRequestDto) {
        return ResponseEntity.ok(authService.reissue(tokenRequestDto));
    }
}
