package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.TestCase;

import java.io.File;
import java.util.List;

public class JsonReader {

    public static List<TestCase> readTestCases(String path) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(
                new File(path),
                mapper.getTypeFactory().constructCollectionType(List.class, TestCase.class)
        );
    }
}