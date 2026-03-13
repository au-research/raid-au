import java.net.HttpURLConnection;
import java.net.URL;

public class Status {
    public static void main(String[] args) throws Exception {
        System.err.println("Running health check...");

        if (args.length == 0) {
            System.err.println("No URL provided");
            System.exit(1);
        }

        URL url = new URL(args[0]);
        System.err.println("Checking: " + url);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();

        System.err.println("Response code: " + responseCode);

        if (responseCode != 200) {
            System.exit(1);
        }
    }
}