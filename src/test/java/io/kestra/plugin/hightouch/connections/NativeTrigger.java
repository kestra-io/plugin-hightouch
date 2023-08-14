import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NativeTrigger  {

    public static void main(String[] args) {
        triggerSync();
    }

    public static void triggerSync() {
        String urlString = "https://api.hightouch.com/api/v1/syncs/1127166/trigger";
        String authorizationHeader = "Bearer WRONG_TOKEN"; // Replace with your actual token
        String requestBody = "{}";

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", authorizationHeader);
            connection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(requestBody);
            wr.flush();
            wr.close();

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                System.out.println("Response: " + response.toString());
            } else {
                // In this case we should end up with 401 error as we provide wrong token
                // Should return : Request failed with response code: 401
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    System.out.println("As expected we have 401 status code");
                }
                else {
                    System.out.println("Request failed with response code: " + responseCode);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
