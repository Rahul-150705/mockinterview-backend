package com.mockinterview.pdf.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.mockinterview.interview.entity.Answer;
import com.mockinterview.interview.entity.Interview;
import com.mockinterview.interview.entity.Question;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(99, 102, 241); // Primary color from your design
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(243, 244, 246);
    private static final DeviceRgb DARK_GRAY = new DeviceRgb(107, 114, 128);

    public byte[] generateInterviewPdf(Interview interview) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Add header
            addHeader(document, interview);
            
            // Add interview details
            addInterviewDetails(document, interview);
            
            // Add questions and answers
            addQuestionsAndAnswers(document, interview);
            
            // Add summary
            addSummary(document, interview);
            
            // Add footer
            addFooter(document);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document document, Interview interview) {
        // Title
        Paragraph title = new Paragraph("Interview Report")
                .setFontSize(28)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(title);

        // Subtitle
        Paragraph subtitle = new Paragraph(interview.getJobTitle())
                .setFontSize(18)
                .setFontColor(DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(subtitle);
    }

    private void addInterviewDetails(Document document, Interview interview) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        // Create details table
        Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Add rows
        addDetailRow(detailsTable, "Job Title:", interview.getJobTitle());
        addDetailRow(detailsTable, "Started At:", interview.getStartedAt().format(formatter));
        if (interview.getFinishedAt() != null) {
            addDetailRow(detailsTable, "Finished At:", interview.getFinishedAt().format(formatter));
        }
        addDetailRow(detailsTable, "Total Questions:", String.valueOf(interview.getQuestions().size()));
        addDetailRow(detailsTable, "Candidate:", interview.getUser().getName());

        document.add(detailsTable);
    }

    private void addDetailRow(Table table, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setBold())
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(null)
                .setPadding(8));
        
        table.addCell(new Cell()
                .add(new Paragraph(value))
                .setBorder(null)
                .setPadding(8));
    }

    private void addQuestionsAndAnswers(Document document, Interview interview) {
        document.add(new Paragraph("Questions & Answers")
                .setFontSize(20)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(20)
                .setMarginBottom(15));

        int questionNumber = 1;
        for (Question question : interview.getQuestions()) {
            // Question header
            Paragraph questionHeader = new Paragraph("Question " + questionNumber)
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(PRIMARY_COLOR)
                    .setMarginTop(15);
            document.add(questionHeader);

            // Question text
            Paragraph questionText = new Paragraph(question.getQuestionText())
                    .setFontSize(12)
                    .setMarginLeft(10)
                    .setMarginBottom(10);
            document.add(questionText);

            // Answer section
            if (question.getAnswers() != null && !question.getAnswers().isEmpty()) {
                Answer answer = question.getAnswers().get(0); // Get first answer

                // User's answer
                document.add(new Paragraph("Your Answer:")
                        .setFontSize(11)
                        .setBold()
                        .setMarginLeft(10));
                
                document.add(new Paragraph(answer.getUserAnswer())
                        .setFontSize(11)
                        .setMarginLeft(20)
                        .setMarginBottom(10)
                        .setBackgroundColor(LIGHT_GRAY)
                        .setPadding(10));

                // AI Feedback
                document.add(new Paragraph("AI Feedback:")
                        .setFontSize(11)
                        .setBold()
                        .setMarginLeft(10));
                
                document.add(new Paragraph(answer.getAiFeedback())
                        .setFontSize(11)
                        .setMarginLeft(20)
                        .setMarginBottom(10)
                        .setPadding(10));

                // Score
                DeviceRgb scoreColor = getScoreColor(answer.getScore());
                document.add(new Paragraph("Score: " + String.format("%.0f", answer.getScore()) + "/100")
                        .setFontSize(12)
                        .setBold()
                        .setFontColor(scoreColor)
                        .setMarginLeft(10)
                        .setMarginBottom(15));
            } else {
                document.add(new Paragraph("Not answered")
                        .setFontSize(11)
                        .setItalic()
                        .setFontColor(DARK_GRAY)
                        .setMarginLeft(20)
                        .setMarginBottom(15));
            }

            questionNumber++;
        }
    }

    private void addSummary(Document document, Interview interview) {
        document.add(new Paragraph("Summary")
                .setFontSize(20)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(30)
                .setMarginBottom(15));

        // Calculate overall score
        double totalScore = 0;
        int answeredQuestions = 0;
        
        for (Question question : interview.getQuestions()) {
            if (question.getAnswers() != null && !question.getAnswers().isEmpty()) {
                totalScore += question.getAnswers().get(0).getScore();
                answeredQuestions++;
            }
        }

        double overallScore = answeredQuestions > 0 ? totalScore / answeredQuestions : 0;

        // Summary table
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        summaryTable.addCell(new Cell()
                .add(new Paragraph("Questions Answered").setBold())
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(null)
                .setPadding(10));
        
        summaryTable.addCell(new Cell()
                .add(new Paragraph(answeredQuestions + " / " + interview.getQuestions().size()))
                .setBorder(null)
                .setPadding(10));

        summaryTable.addCell(new Cell()
                .add(new Paragraph("Overall Score").setBold())
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(null)
                .setPadding(10));
        
        summaryTable.addCell(new Cell()
                .add(new Paragraph(String.format("%.0f/100", overallScore))
                        .setFontColor(getScoreColor(overallScore)))
                .setBorder(null)
                .setPadding(10));

        document.add(summaryTable);
    }

    private void addFooter(Document document) {
        document.add(new Paragraph("\nGenerated by InterviewAI")
                .setFontSize(10)
                .setFontColor(DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30));
    }

    private DeviceRgb getScoreColor(double score) {
        if (score >= 80) {
            return new DeviceRgb(34, 197, 94); // Success green
        } else if (score >= 60) {
            return new DeviceRgb(251, 146, 60); // Warning orange
        } else {
            return new DeviceRgb(239, 68, 68); // Destructive red
        }
    }
}