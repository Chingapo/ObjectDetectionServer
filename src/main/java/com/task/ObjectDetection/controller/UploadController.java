package com.task.ObjectDetection.controller;


import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
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


@RestController
@RequestMapping("/upload")
public class UploadController {
/*
 * Detects localized objects in the specified local image.
 *
 * @param filePath The path to the file to perform localized object detection on.
 * @throws Exception on errors while closing the client.
 * @throws IOException on Input/Output errors.
 */

	@PostMapping("/image")
	public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
		Map<String, Object> response = new HashMap<>();
		try {
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

			// Initialize the Vision API client and perform the request
			try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
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
			response.put("error", "Failed to process image");
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


//	public static void detectLocalizedObjects(String filePath) throws IOException {
//		List<AnnotateImageRequest> requests = new ArrayList<>();
//
//		ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
//
//		Image img = Image.newBuilder().setContent(imgBytes).build();
//		AnnotateImageRequest request =
//				AnnotateImageRequest.newBuilder()
//						.addFeatures(Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION))
//						.setImage(img)
//						.build();
//		requests.add(request);
//
//		// Initialize client that will be used to send requests. This client only needs to be created
//		// once, and can be reused for multiple requests. After completing all of your requests, call
//		// the "close" method on the client to safely clean up any remaining background resources.
//		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
//			// Perform the request
//			BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
//			List<AnnotateImageResponse> responses = response.getResponsesList();
//
//			// Display the results
//			for (AnnotateImageResponse res : responses) {
//				for (LocalizedObjectAnnotation entity : res.getLocalizedObjectAnnotationsList()) {
//					System.out.format("Object name: %s%n", entity.getName());
//					System.out.format("Confidence: %s%n", entity.getScore());
//					System.out.format("Normalized Vertices:%n");
//					entity
//							.getBoundingPoly()
//							.getNormalizedVerticesList()
//							.forEach(vertex -> System.out.format("- (%s, %s)%n", vertex.getX(), vertex.getY()));
//				}
//			}
//		}
//	}
}
