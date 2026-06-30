package com.example.musicapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NgheNhac Music API — Backend cho ứng dụng nghe nhạc Android.
 *
 * API cung cấp danh sách bài hát mẫu (public domain) để streaming,
 * phục vụ demo đồ án. Chạy trên port 8080 mặc định.
 *
 * Cách chạy:
 *   cd backend
 *   mvn spring-boot:run
 *
 * Sau đó cập nhật local.properties của app Android:
 *   base.api.url=http://<IP_MÁY_TÍNH>:8080
 */
@SpringBootApplication
public class MusicApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MusicApiApplication.class, args);
    }
}
