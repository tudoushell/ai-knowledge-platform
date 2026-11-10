package com.elliot.ai.identity.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    @Test
    public void test() {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(12);
        String qwe123 = bCryptPasswordEncoder.encode("qwe123");
        System.out.println(qwe123);
    }
}
