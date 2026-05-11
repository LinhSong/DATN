package utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.TestCase;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.*;

public class ExcelReader {

    public static List<TestCase> readTestCases(String path) throws Exception {

        List<TestCase> testCases = new ArrayList<>();

        FileInputStream fis = new FileInputStream(path);

        Workbook workbook = new XSSFWorkbook(fis);

        Sheet sheet = workbook.getSheetAt(0);

        Iterator<Row> rows = sheet.iterator();

        // HEADER
        Row headerRow = rows.next();

        Map<String, Integer> columns = new HashMap<>();

        for (Cell cell : headerRow) {

            columns.put(
                    cell.getStringCellValue().trim(),
                    cell.getColumnIndex()
            );
        }

        ObjectMapper mapper = new ObjectMapper();

        // DATA ROWS
        while (rows.hasNext()) {

            Row row = rows.next();

            TestCase tc = new TestCase();

            tc.id = getString(row, columns, "id");

            tc.method = getString(row, columns, "method");

            tc.endpoint = getString(row, columns, "endpoint");

            tc.description = getString(row, columns, "description");

            // expected_status
            String expected = getString(
                    row,
                    columns,
                    "expected_status"
            );

            tc.expectedStatus = expected.isEmpty()
                    ? 0
                    : Integer.parseInt(expected);

            // headers
            String headersJson = getString(
                    row,
                    columns,
                    "headers"
            );

            if (!headersJson.isEmpty()) {

                tc.headers = mapper.readValue(
                        headersJson,
                        new TypeReference<Map<String, String>>() {}
                );
            }

            // body
            String bodyJson = getString(
                    row,
                    columns,
                    "body"
            );

            if (!bodyJson.isEmpty()) {

                tc.body = mapper.readValue(
                        bodyJson,
                        Object.class
                );
            }

            // pathParams
            if (columns.containsKey("pathParams")) {

                String pathParamJson = getString(
                        row,
                        columns,
                        "pathParams"
                );

                if (!pathParamJson.isEmpty()) {

                    tc.pathParams = mapper.readValue(
                            pathParamJson,
                            new TypeReference<Map<String, Object>>() {}
                    );
                }
            }

            testCases.add(tc);
        }

        workbook.close();
        fis.close();

        return testCases;
    }

    private static String getString(
            Row row,
            Map<String, Integer> columns,
            String columnName
    ) {

        Integer index = columns.get(columnName);

        if (index == null) {
            return "";
        }

        Cell cell = row.getCell(index);

        if (cell == null) {
            return "";
        }

        DataFormatter formatter = new DataFormatter();

        return formatter.formatCellValue(cell).trim();
    }
}