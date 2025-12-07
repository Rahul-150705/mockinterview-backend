package com.mockinterview.resume.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ResumeParserService {

    public String extractTextFromResume(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        
        if (fileName == null) {
            throw new IOException("File name is null");
        }

        String fileExtension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

        switch (fileExtension) {
            case ".pdf":
                return extractTextFromPDF(file.getInputStream());
            case ".doc":
            case ".docx":
                return extractTextFromWord(file.getInputStream());
            case ".txt":
                return new String(file.getBytes());
            default:
                throw new IOException("Unsupported file format: " + fileExtension);
        }
    }

    private String extractTextFromPDF(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            System.out.println("Extracted " + text.length() + " characters from PDF");
            return text.trim();
        }
    }

    private String extractTextFromWord(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            
            String text = extractor.getText();
            System.out.println("Extracted " + text.length() + " characters from Word document");
            return text.trim();
        }
    }

    public String summarizeResume(String resumeText) {
        // Truncate if too long (to fit in AI context)
        int maxLength = 3000; // characters
        if (resumeText.length() > maxLength) {
            System.out.println("Resume is long (" + resumeText.length() + " chars), truncating to " + maxLength);
            return resumeText.substring(0, maxLength) + "...";
        }
        return resumeText;
    }
}