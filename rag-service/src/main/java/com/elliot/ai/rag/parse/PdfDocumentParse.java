package com.elliot.ai.rag.parse;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.config.StorageProperties;
import com.elliot.ai.rag.service.StructuredParsedStorageService;
import com.elliot.ai.rag.service.impl.StructuredParsedStorageServiceImpl;
import com.elliot.ai.rag.storage.parsed.ParsedArtifact;
import com.elliot.ai.rag.storage.parsed.ParsedBlock;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class PdfDocumentParse extends AbstractDocumentParse {

    private final StructuredParsedStorageService structuredParsedStorageService;

    public PdfDocumentParse(StorageProperties properties, StructuredParsedStorageService structuredParsedStorageService) {
        super(properties);
        this.structuredParsedStorageService = structuredParsedStorageService;
    }

    @Override
    public boolean isSupport(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    @Override
    protected String parsedFileExtension() {
        return ".jsonl";
    }

    @Override
    protected ParsedArtifact parseFile(Path sourcePath, Path targetPath, String parsedRelationPath) throws IOException {

        try (
                PDDocument pdfDocument = Loader.loadPDF(sourcePath.toFile(), IOUtils.createTempFileOnlyStreamCache());
                StructuredParsedStorageServiceImpl.StructuredParseWriter writer =
                        structuredParsedStorageService.createWriter(targetPath)
        ) {
            if (pdfDocument.isEncrypted()) {
                throw new BusinessException(
                        ResultCode.FAIL,
                        "暂不支持加密PDF"
                );
            }
            int pageCount = pdfDocument.getNumberOfPages();
            int sequence = 0;
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String text = normalize(stripper.getText(pdfDocument));
                if (text.isBlank()) {
                    continue;
                }
                writer.write(new ParsedBlock(
                        sequence++,
                        "PAGE",
                        page,
                        null,
                        text
                ));
            }
            if (writer.getBlockCount() == 0) {
                throw new BusinessException(
                        ResultCode.FAIL,
                        "PDF 中没有提取到可用文本，可能是扫描版 PDF"
                );
            }
            return writer.complete(parsedRelationPath, pageCount);
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

    }
}
