package com.elliot.ai.rag.service;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.config.StorageProperties;
import com.elliot.ai.rag.dto.StoredFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class LocalFilesStorageService {

    private final Path rootDirectory;
    private final Path parsedDirectory;

    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("txt", "md", "pdf", "docx");


    public LocalFilesStorageService(StorageProperties storageProperties) {
        this.rootDirectory = Paths.get(storageProperties.getRootDirectory())
                .toAbsolutePath().normalize();
        this.parsedDirectory = Paths.get(storageProperties.getParsedDirectory())
                .toAbsolutePath().normalize();
    }


    public void delete(String relativePath) {
        deleteUnderRoot(rootDirectory, relativePath);
    }

    public void deleteParsed(String relativePath) {
        deleteUnderRoot(parsedDirectory, relativePath);
    }

    private void deleteUnderRoot(Path root, String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return;
        }
        Path targetPath = root.resolve(relativePath).normalize();
        if (!targetPath.startsWith(root)) {
            log.warn("拒绝删除存储根目录之外的文件：{}", relativePath);
            return;
        }
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            // 文档记录删除不应因文件已被手工移除或暂时无法清理而失败。
            log.warn("删除存储文件失败：{}", targetPath, e);
        }
    }


    public StoredFile store(MultipartFile file) {
        validate(file);
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getExtension(originalName);
        String storedName = UUID.randomUUID() + "." + extension;

        LocalDate today = LocalDate.now();

        Path relativePath = Paths.get(
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                storedName
        );
        Path target = rootDirectory.resolve(relativePath).normalize();

        // 防止后续修改存储名生成规则时意外引入目录穿越问题。
        if (!target.startsWith(rootDirectory)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "非法文件名");
        }

        try {
            Files.createDirectories(target.getParent());

            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            long sizeBytes;
            try (InputStream inputStream = file.getInputStream();
                 DigestInputStream digestInputStream =
                         new DigestInputStream(inputStream, messageDigest)) {
                sizeBytes = Files.copy(digestInputStream, target);
            }

            return new StoredFile(
                    originalName,
                    storedName,
                    relativePath.toString(),
                    file.getContentType(),
                    extension,
                    sizeBytes,
                    HexFormat.of().formatHex(messageDigest.digest())
            );
        } catch (IOException | NoSuchAlgorithmException exception) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // 保留最初的存储异常。
            }
            throw new BusinessException(ResultCode.FAIL, "文件存储失败");
        }
    }


    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    ResultCode.PARAM_ERROR,
                    "上传文件不能为空"
            );
        }

        String filename = file.getOriginalFilename();

        if (!StringUtils.hasText(filename)) {
            throw new BusinessException(
                    ResultCode.PARAM_ERROR,
                    "文件名不能为空"
            );
        }

        String extension = getExtension(filename);

        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(
                    ResultCode.PARAM_ERROR,
                    "当前只支持 TXT/Markdown/PDF/DOCX 文件"
            );
        }

    }

    private String getExtension(String filename) {
        String extension =
                StringUtils.getFilenameExtension(filename);

        if (!StringUtils.hasText(extension)) {
            throw new BusinessException(
                    ResultCode.PARAM_ERROR,
                    "文件缺少扩展名"
            );
        }
        return extension.toLowerCase(Locale.ROOT);
    }

}
