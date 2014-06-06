

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class CalibrationServlet
 */
@WebServlet("/calibration")
public class CalibrationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CalibrationServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@SuppressWarnings("deprecation")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

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

		//reception of the mobile device request parameters
		int refPointX = Integer.parseInt(request.getParameter("x"));
		int refPointY = Integer.parseInt(request.getParameter("y"));
		int map_id = Integer.parseInt(request.getParameter("map_id"));

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
		
		int nbTempRssi = 0;
		
		//delete everything from temp rssi
		try {
			st = con.prepareStatement("delete from temprssi");
			st.executeQuery();
		} catch (SQLException e) {
			System.out.println("Delete statement failed.");
			e.printStackTrace();
		}
		
		for(int i=0;i<threads.length;i++) {
			if(threads[i].isAlive())
				threads[i].stop();
			if(threads[i].getRSSIMeasurement()!=null && threads[i].getNBMeasurement()!=null && threads[i].getNBMeasurement()>=5 ) {
				//get the id of the corresponding AP
				int apId = -1;
				try {
					st = con.prepareStatement("select id from accesspoint where mac_addr like ?");
					st.setString(1,apMacAddresses.get(i));
					rs=st.executeQuery();
					apId=rs.getInt("id");
					nbTempRssi++;
				} catch (SQLException e) {
					System.out.println("Select statement failed.");
					e.printStackTrace();
				}

				//insert the temp rssi measurement
				try {
					st = con.prepareStatement("insert into temprssi(ap_id,client_mac,avg_val) values(?,?,?)");
					st.setInt(1,apId);
					st.setString(2,mobileMacAddress);
					st.setDouble(3,threads[i].getRSSIMeasurement());
					st.executeUpdate();
				} catch (SQLException e) {
					System.out.println("Insert statement failed.");
					e.printStackTrace();
				}
			}
		}

		
		
		
		if(nbTempRssi==apMacAddresses.size()) {
			//register the location in the database if enough rssi value per access point
			int locId=-1;
			try {
				st = con.prepareStatement("insert into location(x,y,mapid) values(?,?,?)",Statement.RETURN_GENERATED_KEYS);
				st.setInt(1,refPointX);
				st.setInt(2,refPointY);
				st.setInt(3,map_id);
				st.executeUpdate();
				rs=st.getGeneratedKeys();
				if (rs != null && rs.next()) {
				    locId = rs.getInt(1);
				}
			} catch (SQLException e) {
				System.out.println("Insert statement failed.");
				e.printStackTrace();
			}
			
			for(int i=0; i<apMacAddresses.size();i++){
				
				//get the id of the corresponding AP
				int apId = -1;
				try {
					st = con.prepareStatement("select id from accesspoint where mac_addr like ?");
					st.setString(1,apMacAddresses.get(i));
					rs=st.executeQuery();
					apId=rs.getInt("id");
				} catch (SQLException e) {
					System.out.println("Select statement failed.");
					e.printStackTrace();
				}
				
				//get the corresponding rssi measurement
				double rssi = -1;
				try {
					st = con.prepareStatement("select avg_val from temprssi where ap_id like ?");
					st.setInt(1,apId);
					rs=st.executeQuery();
					rssi=rs.getDouble("avg_val");
				} catch (SQLException e) {
					System.out.println("Select statement failed.");
					e.printStackTrace();
				}
				
				//insert the rssi mapped value
				try {
					st = con.prepareStatement("insert into rssi(id_loc,id_ap,avg_val,std_dev) values(?,?,?,?)");
					st.setInt(1,locId);
					st.setInt(2,apId);
					st.setDouble(3,rssi);
					st.setDouble(4,0.0);
					st.executeUpdate();
				} catch (SQLException e) {
					System.out.println("Insert statement failed.");
					e.printStackTrace();
				}
				
			}
			
			//notify the mobile device of the calibration success
			response.getOutputStream().println("{\"calibration\":\"ok\"}");
		}
		else {
			// notify to the mobile device that the calibration failed
			response.getOutputStream().println("{\"calibration\":\"try_again\"}");
		}

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
