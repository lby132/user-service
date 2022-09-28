package com.example.userservice.security;

import com.example.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.servlet.Filter;

@Configuration
@EnableWebSecurity
public class WebSecurity extends WebSecurityConfigurerAdapter {

//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http.csrf().disable();
//        http.authorizeRequests().antMatchers("/users/**").permitAll(); // /users로 들어온 모든 작업은 통과시킨다.
//
//        http.headers().frameOptions().disable();    // 프레임별로 구분된거 무시됨
//    }

    private UserService userService;
    private BCryptPasswordEncoder bCryptPasswordEncoder;    // 빈으로 등록한 패스워드를 암호화 해주는 BCryptPasswordEncoder를 주입한다.
    private Environment env;        // yml에 있는 설정들을 가져다가 쓸수 있다.

    @Autowired
    public WebSecurity(UserService userService, BCryptPasswordEncoder bCryptPasswordEncoder, Environment env) {
        this.userService = userService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.env = env;
    }

    @Override //HttpSecurity를 매개변수로 받는 configure메서드는 권한에 관련된 작업이다.
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.authorizeRequests().antMatchers("/actuator/**").permitAll();
        http.authorizeRequests().antMatchers("/error/**").permitAll()     //에러 요청을 처리해야해서 에러에 대한 부분만 통과
                .antMatchers("/**")         // 모든 작업에 통과시키지 않는다.
                .access("hasIpAddress('"+"192.168.35.30"+"')")  // 사용자는 무조건 아이피를 제한적으로 받는다.
                .and()
                .addFilter(getAuthenticationFilter());  // 여기에 통과된 데이터만 권한을 부여하고 작업을 진행을 한다.

        http.headers().frameOptions().disable();    // 프레임별로 구분된거 무시됨
    }

    private AuthenticationFilter getAuthenticationFilter() throws Exception {
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(authenticationManager(), userService, env);
        //authenticationFilter.setAuthenticationManager(authenticationManager());

        return authenticationFilter;
    }

    // select pwd from users where email = ?
    // db_pwd(encrypted) == input_pwd(encrypted)
    @Override//AuthenticationManagerBuilder를 매개변수로 받는 configure메서드는 인증에 관련된 작업이다.
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService).passwordEncoder(bCryptPasswordEncoder);
    }
}
