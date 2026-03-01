package com.example.quant.web;

import com.example.quant.order.OrderRecord;
import com.example.quant.order.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单查询接口。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 查询全部订单。
     *
     * @return 订单列表
     */
    @GetMapping
    public List<OrderRecord> list() {
        return orderService.listOrders();
    }
}
