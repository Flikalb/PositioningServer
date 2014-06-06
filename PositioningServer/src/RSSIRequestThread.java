import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RSSIRequestThread extends Thread {

	private String mobileMacAddress;
	private String apIpAddress;
	private Double rssiMeasurement;
	public Integer nbMeasures;

	public RSSIRequestThread(String name,String mobileMacAddress, String apIpAddress) {
		super(name);
		this.mobileMacAddress = mobileMacAddress;
		this.apIpAddress = apIpAddress;
		rssiMeasurement = null;
		nbMeasures = null;
	}

	@SuppressWarnings("deprecation")
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
			
			this.stop();
			
			/*
			 * {”ap”: ”ap:ap:ap:ap:ap:ap”,”rssi”:[{”xx:xx:xx:xx:xx:xx”:”val”,”samples”:”nb”}]}
			 * {”ap”: ”ap:ap:ap:ap:ap:ap”,”rssi”:[]}
			 */
			
			if(response!="") {
				response=response.substring(33,response.length()-1);
				String[] extract = null;
				if(!response.equals("[]")) {
					extract = response.split("\"");
				}
				if(extract.length == 4) {
					this.rssiMeasurement = Double.parseDouble(extract[1]);
					this.nbMeasures = Integer.parseInt(extract[3]);
				}
			}
			
		}
		catch(Exception e){}
		
		

	}

	public Double getRSSIMeasurement () {
		return this.rssiMeasurement;
	}
	
	public Integer getNBMeasurement() {
		return this.nbMeasures;
	}
}
