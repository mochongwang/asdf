package com.example.quant.web;

import com.example.quant.model.LeverageInfo;
import com.example.quant.service.LeverageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 杠杆信息接口。
 */
@RestController
@RequestMapping("/api/leverage")
public class LeverageController {

    private final LeverageService leverageService;

    public LeverageController(LeverageService leverageService) {
        this.leverageService = leverageService;
    }

    @GetMapping
    public List<LeverageInfo> list() {
        return leverageService.list();
    }
}
