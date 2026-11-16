package com.chuseok22.jjalpick.admin.application.controller;

import com.chuseok22.logging.annotation.LogMonitoring;
import io.swagger.v3.oas.annotations.Hidden;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class AdminDashboardController {

    @LogMonitoring
    @GetMapping("/admin")
    public String showDashboard(Principal principal, Model model) {
        model.addAttribute("adminUsername", principal.getName());
        return "admin/dashboard";
    }

}
