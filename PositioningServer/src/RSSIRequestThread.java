import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class RSSIRequestThread extends Thread {

	private String mobileMacAddress;
	private String apIpAddress;
	private String rssiMeasurement;
	public String nbMeasures;

	public RSSIRequestThread(String name,String mobileMacAddress, String apIpAddress) {
		super(name);
		this.mobileMacAddress = mobileMacAddress;
		this.apIpAddress = apIpAddress;
		rssiMeasurement = null;
		nbMeasures = null;
	}

	public void run() {
		String url = "http://"+apIpAddress+"/";

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			// optional default is GET
			con.setRequestMethod("GET");

			//add mobile mac address as request parameter
			con.addRequestProperty("mac", mobileMacAddress);

			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'GET' request to URL : " + url);		
			System.out.println("Response Code : " + responseCode);


			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			String inputLine;
			String response="";

			while ((inputLine = in.readLine()) != null) {
				response+=inputLine;
			}
			in.close();
			
			
			
		}
		catch(Exception e){}
		
		

	}

	public String getRSSIMeasurement () {
		return this.rssiMeasurement;
	}
	
	public String getNBMeasurement() {
		return this.nbMeasures;
	}
}
