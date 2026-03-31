package com.docflow.infra.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 將前端 SPA 路由轉發至入口頁，避免重新整理時發生 404。
 */
@Controller
public class SpaForwardController {

    /**
     * 轉發根路徑與 SPA 路由至 `index.html`。
     *
     * @return 轉發結果字串
     */
    @GetMapping({"/", "/login", "/app", "/app/**"})
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
