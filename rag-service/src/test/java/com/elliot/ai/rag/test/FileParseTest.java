package com.elliot.ai.rag.test;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileParseTest {

    @Test
    public void readWord() throws Exception {
        try (InputStream inputStream = Files.newInputStream(Paths.get("/Users/elliotk/tmp/kk.docx"));
             XWPFDocument document = new XWPFDocument(inputStream);) {
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    System.out.println(paragraph);
                }
            }

        }
    }

    @Test
    public void readPdf() throws Exception {
        Path path = Paths.get("/Users/elliotk/tmp/复习.pdf");
        try(PDDocument document = Loader.loadPDF(path.toFile(), IOUtils.createTempFileOnlyStreamCache())) {
            int pageCount = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                System.out.println(text);
            }

        }
    }
}
