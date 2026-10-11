package com.campus.smart.controller;

import com.campus.smart.common.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 高德地图代理
 * 小程序端 request 域名白名单不含 restapi.amap.com，
 * 由后端转发高德请求规避域名校验（后端服务器无此限制）
 */
@Tag(name = "地图服务")
@RestController
@RequestMapping("/api/amap")
public class AmapProxyController {

    @Value("${amap.key:}")
    private String amapKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Operation(summary = "逆地理编码（坐标转地址）")
    @GetMapping("/regeo")
    public Result<String> regeo(@RequestParam String location) {
        if (amapKey == null || amapKey.isEmpty()) {
            return Result.error("地图服务未配置");
        }
        try {
            String url = "https://restapi.amap.com/v3/geocode/regeo?key=" + amapKey
                    + "&location=" + location + "&extensions=base";
            String resp = restTemplate.getForObject(url, String.class);
            return Result.success(resp);
        } catch (Exception e) {
            return Result.error("地址解析失败");
        }
    }
}