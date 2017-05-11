package sanya.project.imagesaver.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

public class FaceDetectResource {
	public static void main(String[] args) {
		HttpClient httpClient = new DefaultHttpClient();

		try {
			URIBuilder uriBuilder = new URIBuilder(
					"https://southeastasia.api.cognitive.microsoft.com/face/v1.0/detect");

			uriBuilder.setParameter("returnFaceId", "true");
			uriBuilder.setParameter("returnFaceLandmarks", "true");
			uriBuilder.setParameter("returnFaceAttributes", "age,gender");

			URI uri = uriBuilder.build();
			HttpPost request = new HttpPost(uri);

			// Request headers. Replace the example key below with your valid
			// subscription key.
			request.setHeader("Content-Type", "application/json");
			request.setHeader("Ocp-Apim-Subscription-Key", "74a2ea436c3d4f999182b3414419ae36");

			// Request body. Replace the example URL below with the URL of the
			// image you want to analyze.
			String imageUrl = args[0];
			StringEntity reqEntity = new StringEntity(
					"{\'url\':\'" + imageUrl + "'}");
			request.setEntity(reqEntity);

			HttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();

			BufferedReader rd = new BufferedReader(new InputStreamReader(entity.getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			String jsonResult = result.toString();
			JSONObject object = new JSONObject(jsonResult.substring(1,jsonResult.length() -1));
			JSONObject attributes = object.getJSONObject("faceAttributes");
			String age = attributes.getString("age");
			String gender = attributes.getString("gender");
			
			BasicDBObject faceAttributes = new BasicDBObject();
			faceAttributes.put("imageUrl", imageUrl);
			faceAttributes.put("age", age);
			faceAttributes.put("gender", gender);
			
			
			DB db = getMongoConnection(args[1], args[2], args[3], args[4], args[5], args[6]);
			db.getCollection("image").save(faceAttributes);
			System.out.println("Successfully saved image atttributes");

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private static DB getMongoConnection(String host, String port, String database, String auth, String username,
			String password) {
		MongoClient mongoClient = null;
		try {
			if (auth.equalsIgnoreCase("true")) {
				MongoCredential mongoCredential = MongoCredential.createScramSha1Credential(username, database,
						password.toCharArray());
				mongoClient = new MongoClient(new ServerAddress(host, Integer.parseInt(port)),
						Arrays.asList(mongoCredential));
			} else {
				mongoClient = new MongoClient(host, Integer.parseInt(port));
			}

		} catch (NumberFormatException e) {
			System.out.println("Could not connect to mongo : " + e);
		} catch (Exception e) {
			System.out.println("Could not connect to mongo : " + e);
		}

		DB db = mongoClient.getDB(database);
		return db;
	}

}