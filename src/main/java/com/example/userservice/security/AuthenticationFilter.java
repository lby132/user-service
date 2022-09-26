package com.example.userservice.security;

import com.example.userservice.dto.UserDto;
import com.example.userservice.service.UserService;
import com.example.userservice.vo.RequestLogin;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

@Slf4j
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private UserService userService;
    private Environment env;

    public AuthenticationFilter(AuthenticationManager authenticationManager, UserService userService, Environment env) {
        super.setAuthenticationManager(authenticationManager);
        this.userService = userService;
        this.env = env;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        try {
            RequestLogin creds = new ObjectMapper().readValue(request.getInputStream(), RequestLogin.class);

            // 사용자가 입력한 아이디와 패스워드를 UsernamePasswordAuthenticationToken으로 바꾼걸 AuthenticationManager(인증처리해주는 매니저)에 인증작업을 요청하면 아이디와 패스워드를 비교해준다.
            return getAuthenticationManager().authenticate(
                    //사용자가 입력한 아이디와 패스워드를 스프링 시큐리티에서 사용할 수 있는 형태로 변환하기 위해서 UsernamePasswordAuthenticationToken으로 바꿔 줘야한다.
                    new UsernamePasswordAuthenticationToken(creds.getEmail(), creds.getPassword(), new ArrayList<>())
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 로그인 성공시 자동으로 호출된다.
    // 로그인 성공시 토큰 생성 메서드
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {

        log.debug( ((User)authResult.getPrincipal()).getUsername() );   // 로그인한 사용자 이름 반환

        String username = ((User) authResult.getPrincipal()).getUsername();
        UserDto userDetails = userService.getUserDetailsByEmail(username);

        String token = Jwts.builder()       // jsonWebToken인 jjwt를 주입받으면 만들수있다.
                .setSubject(userDetails.getUserId())
                //setExpiration에는 토큰 만료기간을 정해줘야하는데 현재시간과 설정파일에 설정한 시간 token.expiration_time=8640000 을 더해줘서 토큰 만료일을 정한다.
                .setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(env.getProperty("token.expiration_time")))) // 문자열이므로 숫자로 변환했다.
                .signWith(SignatureAlgorithm.HS512, env.getProperty("token.secret"))    // 토큰을 생성할때 설정파일에서 설정한 token.secret=user_token 이 키를 가지고 생성한다.
                .compact();

        response.addHeader("token", token);     // 헤더에 토큰을 반환한다.
        response.addHeader("userId", userDetails.getUserId());      // 데이터베이스에서 가져온 유저 아이디

    }
}
