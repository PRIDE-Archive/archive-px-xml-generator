package uk.ac.ebi.pride.archive.px;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import uk.ac.ebi.pride.archive.px.writer.SchemaOnePointFourStrategy;
import uk.ac.ebi.pride.archive.px.writer.SchemaOnePointThreeStrategy;
import uk.ac.ebi.pride.archive.px.writer.MessageWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @author Suresh Hewapathirana
 */
public class Util {

    /**
     * Sets the appropriate strategry based on the version
     * @param version
     * @return
     */
    public static MessageWriter getSchemaStrategy(String version){
        switch (version) {
            case "1.3.0":
                return new SchemaOnePointThreeStrategy(version);
            case "1.4.0":
                return new SchemaOnePointFourStrategy(version);
        }
        return null;
    }

    /**
     * This class helps to create MS Excel sheet to save error messages when we do bulk tests
     *
     * @param errorList List of errors being a Map, Key should be project accession and
     *                  the value should be the exact error
     * @param spreadsheetName name of the spreadsheet
     * @param outFile output name of the excel sheet
     */
    public static void saveInExcel(Map<String, String> errorList, String spreadsheetName, String outFile) {
        try {
            // Create a Workbook
            // new HSSFWorkbook() for generating `.xls` file
            Workbook  workbook = new XSSFWorkbook();
            // Create a Sheet
            Sheet sheet = workbook.createSheet(spreadsheetName);

            // Create rows and cells with data
            int rowNum = 1;
            for (Map.Entry<String, String> error : errorList.entrySet())
            {
                Row row = sheet.createRow(rowNum++);

                // project accession
                row.createCell(0).setCellValue(error.getKey());
                // Error message
                row.createCell(1).setCellValue(error.getValue());
            }

            // Write the output to a file
            FileOutputStream fileOut = new FileOutputStream(outFile);
            workbook.write(fileOut);
            fileOut.close();

            // Closing the workbook
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
