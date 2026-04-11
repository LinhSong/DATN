package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.TestResult;

import java.io.File;
import java.util.List;

public class ResultWriter {

    public static void write(List<TestResult> results, String path) throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(path), results);
    }
}