package com.chuseok22.jjalpick.admin.application.controller;

import com.chuseok22.logging.annotation.LogMonitoring;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class AdminLoginController {

    @LogMonitoring
    @GetMapping("/admin/login")
    public String showLoginPage() {
        return "admin/login";
    }

}
