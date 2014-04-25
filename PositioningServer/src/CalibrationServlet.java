

import java.io.IOException;
import java.io.PrintWriter;
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
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		
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
		
		//register the location in the database
		try {
			st = con.prepareStatement("insert into location(x,y,mapid) values(?,?,?)");
			st.setInt(1,refPointX);
			st.setInt(2,refPointY);
			st.setInt(3,map_id);
			st.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Insert statement failed.");
			e.printStackTrace();
		}
	
		//get the mobile device mac address
		String userIpAddress = request.getRemoteAddr();
		//ARPPacket 
		//TODO voir jpcap
		
		//get the list of access points
		ArrayList<String> apMACAddresses = new ArrayList<String>();
		try {
			st = con.prepareStatement("select * into accesspoint");
			rs = st.executeQuery();
			while(rs.next()) {
				apMACAddresses.add(rs.getString("mac_addr"));
			}
		} catch (SQLException e) {
			System.out.println("Selectstatement failed.");
			e.printStackTrace();
		}
		
		
		//send request to each access point for RSSI measurements with the mobile device mac address as parameter
		
		
		
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
