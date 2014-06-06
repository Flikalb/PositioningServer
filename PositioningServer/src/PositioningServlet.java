import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class PositioningServlet
 */
@WebServlet("/positioning")
public class PositioningServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public PositioningServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@SuppressWarnings("deprecation")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub

		response.setContentType("text/html");

		Runtime rt;
		Process pr;
		BufferedReader input;
		String line;

		//open the database connection
		try {
			Class.forName("org.postgresql.Driver");
		} 
		catch (ClassNotFoundException cnfe) {
			System.out.println("Could not find the PostgreSQL JDBC driver !");
			//System.exit(1);
		}

		System.out.println("PostgreSQL JDBC Driver loaded.");

		Connection con=null;
		PreparedStatement st = null;
		ResultSet rs = null;

		String url = "jdbc:postgresql://localhost/lo53";
		String user = "lo53";
		String password = "lo53";

		try {
			con = DriverManager.getConnection(url, user, password);
		}
		catch (SQLException e) {
			System.out.println("Connection failed !");
			e.printStackTrace();
		}

		if(con != null) {
			System.out.println("Connected to the database.");
		}
		else {
			System.out.println("Failed to make connection.");
		}

		//get the mobile device mac address
		String mobileIpAddress = request.getRemoteAddr();
		System.out.println(mobileIpAddress);
		String mobileMacAddress = null;
		rt = Runtime.getRuntime();
		pr = rt.exec("cat /proc/net/arp");
		input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

		line=null;
		input.readLine();
		while((line=input.readLine()) != null) {
			String[] arpEntry = line.split(" +");
			if(arpEntry[0].equals(mobileIpAddress)) {
				mobileMacAddress = arpEntry[3];
				break;
			}
		}


		//get the list of access points

		ArrayList<String> apMacAddresses = new ArrayList<String>();
		try {
			st = con.prepareStatement("select * into accesspoint");
			rs = st.executeQuery();
			while(rs.next()) {
				apMacAddresses.add(rs.getString("mac_addr"));
			}
		} catch (SQLException e) {
			System.out.println("Selectstatement failed.");
			e.printStackTrace();
		}


		//get ip adresses for each access points
		rt = Runtime.getRuntime();
		pr = rt.exec("cat /proc/net/arp");
		input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		ArrayList<String> apIpAddresses = new ArrayList<String>();
		line=null;
		input.readLine();
		while((line=input.readLine()) != null) {
			String[] arpEntry = line.split(" +");
			for(String s : apMacAddresses) {
				if(arpEntry[3].equals(s)) {
					apIpAddresses.add(arpEntry[0]);
					break;
				}
			}

		}

		//send request to each access point for RSSI measurements with the mobile device mac address as parameter
		RSSIRequestThread[] threads = new RSSIRequestThread[apMacAddresses.size()];
		for(int i=0;i<apIpAddresses.size();i++) {
			threads[i] = new RSSIRequestThread("AP"+(i+1),mobileMacAddress,apIpAddresses.get(i));
			threads[i].start();
		}

		try {
			this.wait(500);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}

		//delete everything from temp rssi
		try {
			st = con.prepareStatement("delete from temprssi");
			st.executeQuery();
		} catch (SQLException e) {
			System.out.println("Delete statement failed.");
			e.printStackTrace();
		}

		double avg_rssi = 0;
		int positioningRssiCount =0;
		for(int i=0;i<threads.length;i++) {

			if(threads[i].isAlive())
				threads[i].stop();

			if(threads[i].getRSSIMeasurement()!=null && threads[i].getNBMeasurement()!=null && threads[i].getNBMeasurement()>=5 ) {
				avg_rssi +=threads[i].getRSSIMeasurement();
				positioningRssiCount++;
			}

			if(positioningRssiCount==0) {
				response.getOutputStream().println("{\"positioning\":\"try_again\"}");
				return;
			}
		}
		avg_rssi/=positioningRssiCount;

		//get the closest rssi of the database
		double distance = Double.POSITIVE_INFINITY;
		int locId=-1;
		try {
			st = con.prepareStatement("select avg_val,loc_id into rssi");
			rs = st.executeQuery();
			while(rs.next()) {
				double tmpRssi=rs.getDouble("avg_val");
				int tmpLocId = rs.getInt("loc_id");
				double tmpDistance = Math.sqrt((avg_rssi-tmpRssi)*(avg_rssi-tmpRssi));
				if(tmpDistance<distance) {
					distance = tmpDistance;
					locId=tmpLocId;
				}
			}
		} catch (SQLException e) {
			System.out.println("Selectstatement failed.");
			e.printStackTrace();
		}

		//get location corresponding to closestRssi
		double x=-1.0,y=-1.0;
		int map_id=-1;

		try {
			st = con.prepareStatement("select x,y,map_id into location where id like ?");
			st.setInt(1,locId);
			rs = st.executeQuery();
			if(rs.next()) {
				x=rs.getDouble("x");
				y=rs.getDouble("y");
				map_id=rs.getInt("map_id");
			}
		} catch (SQLException e) {
			System.out.println("Select statement failed.");
			e.printStackTrace();
		}

		response.getOutputStream().println("{\"positioning\":\"ok\",\"x\":\""+x+"\",\"y\":\""+y+"\",\"map_id\":\""+map_id+"\"}");

		//close the connection to the database
		if(con!=null) {
			try {
				con.close();
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
