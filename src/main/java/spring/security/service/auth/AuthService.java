package spring.security.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import spring.security.domain.member.Authority;
import spring.security.domain.member.Member;
import spring.security.dto.member.MemberRequestDto;
import spring.security.dto.member.MemberResponseDto;
import spring.security.dto.sign.TokenDto;
import spring.security.dto.sign.TokenRequestDto;
import spring.security.jwt.TokenProvider;
import spring.security.repository.member.MemberRepository;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public void signUp(MemberRequestDto memberRequestDto) {

        if (memberRepository.existsByEmail(memberRequestDto.getEmail())) {
            throw new RuntimeException("이미 가입되어 있는 유저입니다.");
        }

        Member member = memberRequestDto.toMember(passwordEncoder);
        memberRepository.save(member);
    }

    @Transactional
    public TokenDto SignIn(MemberRequestDto memberRequestDto) {

        // 1. Login ID/PW 를 기반으로 AuthenticationToken 생성
        UsernamePasswordAuthenticationToken authenticationToken = memberRequestDto.toAuthentication();

        // 2. 실제로 검증 (사용자 비밀번호 체크) 이 이루어지는 부분
        // authentication 메서드가 실행이 될 때 CustomUserDetailsService 에서 만들었던 loadUserByUsername 메서드가 실행됨
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

        // 4. refreshToken Redis 저장 (expirationTime 설정을 통해 자동 삭제 처리)
        redisTemplate.opsForValue().
                set("RT: " + authentication.getName(), tokenDto.getRefreshToken(), tokenDto.getRefreshTokenExpirationTime(), TimeUnit.MILLISECONDS);

        // 5. 토큰 발급
        return tokenDto;
    }

    public void logout(TokenRequestDto tokenRequestDto) {
        // 1. accessToken 검증
        if (!tokenProvider.validateToken(tokenRequestDto.getAccessToken())) {
            throw new RuntimeException("잘못된 요청입니다.");
        }

        // 2. accessToken 에서 User email 을 가져옴
        Authentication authentication = tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());

        // 3. Redis 에서 해당 User email 로 저장된 refreshToken 이 있는지 여부를 확인 후 있을 경우 삭제
        if (redisTemplate.opsForValue().get("RT: " + authentication.getName()) != null) {
            // refreshToken 삭세
            redisTemplate.delete("RT: " + authentication.getName());
        }

        // 4. 해당 accessToken 유효시간 가지고 와서 BlackList 로 저장
        Long expiration = tokenProvider.getExpiration(tokenRequestDto.getAccessToken());
        redisTemplate.opsForValue()
                .set(tokenRequestDto.getAccessToken(), "logout", expiration, TimeUnit.MILLISECONDS);
    }

    @Transactional
    public TokenDto reissue(TokenRequestDto tokenRequestDto) {

        // 1. Refresh Token 검증
        if (!tokenProvider.validateToken(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("Refresh Token 이 유효하지 않습니다.");
        }

        // 2. Access Token 에서 User email 가져오기
        Authentication authentication = tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());

        // 3. Redis 에서 User email 을 기반으로 저장된 Refresh Token 값을 가져오기
        String refreshToken = (String) redisTemplate.opsForValue().get("RT: " + authentication.getName());

        // (추가) 로그아웃 되어 Redis 에 Refresh Token 이 존재하지 않는 경우 처리
        if (ObjectUtils.isEmpty(refreshToken)) {
            throw new RuntimeException("Refresh Token 정보가 일치하지 않습니다.");
        }

//        // 4. Refresh Token 일치하는지 검사
//        if (!refreshToken.getValue().equals(tokenRequestDto.getRefreshToken())) {
//            throw new RuntimeException("토큰의 유저 정보가 일치하지 않습니다.");
//        }

        // 5. 새로운 토큰 생성
        TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

        // 6. Refresh Token Redis 업데이트
        redisTemplate.opsForValue()
                .set("RT: " + authentication.getName(), tokenDto.getRefreshToken(), tokenDto.getRefreshTokenExpirationTime(), TimeUnit.MILLISECONDS);

        // 토큰 발급
        return tokenDto;
    }
}
