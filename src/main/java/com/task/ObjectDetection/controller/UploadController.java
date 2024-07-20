package com.task.ObjectDetection.controller;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/upload")
public class UploadController {
	private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

	private static final String CREDENTIALS_FILE_PATH = "Credentials/object-detection-429915-3d479335d183.json";

	@PostMapping("/image")
	public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
		Map<String, Object> response = new HashMap<>();
		try {
			// Load credentials from the JSON file
			GoogleCredentials credentials;
			try (FileInputStream credentialsStream = new FileInputStream(CREDENTIALS_FILE_PATH)) {
				credentials = GoogleCredentials.fromStream(credentialsStream);
			}

			// Initialize the Vision API client with credentials
			ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(credentials))
					.build();
			try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
				// Convert MultipartFile to ByteString
				ByteString imgBytes = ByteString.readFrom(file.getInputStream());

				// Image converted to Google cloud vision understandable format
				Image img = Image.newBuilder().setContent(imgBytes).build();

				// Request to the Vision API to detect and mark objects
				AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
						.addFeatures(Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION))
						.setImage(img)
						.build();

				// Add the request to a list of requests
				List<AnnotateImageRequest> requests = new ArrayList<>();
				requests.add(request);

				// Perform the request
				AnnotateImageResponse res = client.batchAnnotateImages(requests).getResponsesList().get(0);
				if (res.hasError()) {
					// If there's an error in the response, add it to the response map
					response.put("error", res.getError().getMessage());
					return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
				}

				// Extract detected objects and their confidence levels
				List<Map<String, Object>> objects = new ArrayList<>();
				for (LocalizedObjectAnnotation annotation : res.getLocalizedObjectAnnotationsList()) {
					Map<String, Object> object = new HashMap<>();
					object.put("name", annotation.getName());
					object.put("score", annotation.getScore());
					objects.add(object);
				}

				// Add the detected objects to the response map
				response.put("objects", objects);
				return new ResponseEntity<>(response, HttpStatus.OK);
			}
		} catch (IOException e) {
			// Handle any exceptions that occur during the image processing
			logger.error("Error processing request: ", e);
			response.put("error", "Error processing request: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
