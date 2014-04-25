

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class HomeServlet
 */
@WebServlet("/home")
public class HomeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public HomeServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("Home");
		System.out.println("Home");
		try {
			Class.forName("org.postgresql.Driver");
		} 
		catch (ClassNotFoundException cnfe) {
			System.out.println("Could not find the PostgreSQL JDBC driver !");
			//System.exit(1);
		}

		System.out.println("PostgreSQL JDBC Driver loaded.");

		Connection con=null;
		Statement st = null;
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
		
		try {
			st = con.createStatement();
			int nb = st.executeUpdate("insert into accesspoint(mac_addr) values('123456789123456789')");
		} catch (SQLException e) {
			System.out.println("Insertion failed.");
			e.printStackTrace();
		}
		
		try {
			st = con.createStatement();
			rs=st.executeQuery("select * from accesspoint");
			while(rs.next()) {
				int id = rs.getInt("id");
				String mac_addr = rs.getString("mac_addr");
				System.out.println("Access point id : "+id+ " and mac address : "+mac_addr);
				
			}
		}
		catch(SQLException e) {
			System.out.println("Query failed.");
		}

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
