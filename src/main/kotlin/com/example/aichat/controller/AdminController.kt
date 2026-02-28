package com.example.aichat.controller

import com.example.aichat.dto.AdminDailyMetricsResponse
import com.example.aichat.service.AdminService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val adminService: AdminService,
) {
    @GetMapping("/metrics/daily")
    fun getDailyMetrics(): AdminDailyMetricsResponse {
        return adminService.getDailyMetrics()
    }

    @GetMapping(
        value = ["/reports/daily-chats.csv"],
        produces = ["text/csv"],
    )
    fun downloadDailyChatsCsv(): ResponseEntity<ByteArray> {
        val report = adminService.generateDailyChatsCsv()
        val body = UTF8_BOM + report.content.toByteArray(StandardCharsets.UTF_8)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${report.fileName}\"")
            .contentType(MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(body)
    }

    companion object {
        private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }
}
