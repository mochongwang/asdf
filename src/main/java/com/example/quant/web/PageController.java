package com.example.quant.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 简单演示页面控制器。
 */
@Controller
public class PageController {

    /**
     * 策略创建页面。
     *
     * @return 页面模板名
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
