package com.example.quant.model;

/**
 * K 线/指标周期枚举。
 */
public enum Period {
    M1("1m"),
    M5("5m"),
    M15("15m"),
    H1("1h"),
    H4("4h"),
    D1("1d");

    private final String code;

    Period(String code) {
        this.code = code;
    }

    /**
     * @return 周期代码（提供给前端下拉框）
     */
    public String code() {
        return code;
    }
}
