package com.imstargg.core.admin.application

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Service
@Profile("local")
class LocalImageStorageService : ImageStorageService {
    private val log = LoggerFactory.getLogger(javaClass)
    private val storageDir: Path = Path.of(System.getProperty("java.io.tmpdir"), "imstargg-images")

    init {
        Files.createDirectories(storageDir)
        log.info("로컬 이미지 저장 경로: {}", storageDir)
    }

    override fun upload(
        key: String,
        inputStream: InputStream,
        contentType: String,
    ): String {
        val targetPath = storageDir.resolve(key)
        Files.createDirectories(targetPath.parent)
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
        return key
    }

    override fun delete(key: String) {
        val targetPath = storageDir.resolve(key)
        Files.deleteIfExists(targetPath)
    }
}
