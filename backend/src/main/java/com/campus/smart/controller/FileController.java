package com.campus.smart.controller;

import com.campus.smart.common.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传模块控制器
 */
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/file")
public class FileController {

    /**
     * 文件上传（模拟，返回一个假的URL）
     */
    @Operation(summary = "文件上传")
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }

        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        // 获取文件后缀
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 生成新文件名
        String newFileName = UUID.randomUUID().toString().replace("-", "") + suffix;

        // 模拟上传，返回假URL
        // 实际项目中这里应该上传到OSS、COS等对象存储
        String fileUrl = "https://smart-campus.oss-cn-hangzhou.aliyuncs.com/" + newFileName;

        Map<String, String> result = new HashMap<>();
        result.put("url", fileUrl);
        result.put("fileName", newFileName);
        result.put("originalName", originalFilename);

        return Result.success(result);
    }
}
