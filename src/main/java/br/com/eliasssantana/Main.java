package br.com.eliasssantana;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Main implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Client s3Client = S3Client.builder().build();
    private final String BUCKET_NAME = "url-shortened-storage-area";
    private static final Logger logger2 = LogManager.getLogger(Main.class);

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {

        String pathParameters = (String) event.get("rawPath");

        LambdaLogger logger = context.getLogger();

        Map<String, Object> response = new HashMap<>();

        String shortUrlCode = pathParameters.replace("/","");

        logger.log(shortUrlCode);

        if (!(isShortUrlCodeValid(shortUrlCode))){
            throw new IllegalArgumentException("Invalid input: 'shortUrlCode' is required.");
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(shortUrlCode + ".json")
                .build();

        InputStream s3ObjectStream;

        try{
            s3ObjectStream = s3Client.getObject(request);
        } catch (Exception e){
            throw new RuntimeException("Error fetching data from S3: " + e.getMessage(), e);
        }

        UrlData urlData;

        try {
            urlData = objectMapper.readValue(s3ObjectStream, UrlData.class);
        } catch(Exception e){
            throw new RuntimeException("Error deserializing URL data: " + e.getMessage(), e);
        }

        long currentTimeInSeconds = System.currentTimeMillis() / 1000;

        if (expirationTimeExceeded(currentTimeInSeconds, urlData)){
            response.put("statusCode", 410);
            response.put("body", "URL has expired");
        }

        response.put("statusCode", 302);
        Map<String, String> headers = new HashMap<>();
        headers.put("Location", urlData.getOriginalUrl());
        response.put("headers", headers);

        return response;
    }

    private boolean expirationTimeExceeded(long currentTimeInSeconds, UrlData urlData) {
        return currentTimeInSeconds > urlData.getExpirationTime();
    }

    private boolean isShortUrlCodeValid(String shortUrlCode) {
        return !(shortUrlCode == null || shortUrlCode.isEmpty());
    }
}