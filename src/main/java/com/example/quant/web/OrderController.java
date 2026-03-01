package com.example.quant.web;

import com.example.quant.model.OrderQuery;
import com.example.quant.order.OrderRecord;
import com.example.quant.order.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public List<OrderRecord> list(@RequestParam(required = false) String symbol,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) Long fromEpochSecond,
                                  @RequestParam(required = false) Long toEpochSecond,
                                  @RequestParam(required = false) Integer page,
                                  @RequestParam(required = false) Integer size) {
        return orderService.query(new OrderQuery(symbol, status, fromEpochSecond, toEpochSecond, page, size));
    }
}
