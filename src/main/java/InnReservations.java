import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.Map;
import java.util.Scanner;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

/*
Introductory JDBC examples based loosely on the BAKERY dataset from CSC 365 labs.
 */
public class InnReservations {

    private final String JDBC_URL = "jdbc:h2:~/csc365_lab7";
    private final String JDBC_USER = "";
    private final String JDBC_PASSWORD = "";
    
    public static void main(String[] args) {
	try {
	    InnReservations ir = new InnReservations();
            ir.initDb();
	    Scanner s = new Scanner(System.in);
	    int cur = 0;
	    while (cur != 6) {
	    	System.out.println();
	    	System.out.println("Select an option:");
	    	System.out.println("1: Rooms and Rates");
	    	System.out.println("2: Reservations");
	    	System.out.println("3: Reservation Change");
	    	System.out.println("4: Reservation Cancellation");
	    	System.out.println("5: Revenue Summary");
	    	System.out.println("6: Quit program\n");
	    	System.out.print("Enter option number: ");
	    	cur = s.nextInt();

	    	switch (cur) {
	    		case 1:
	    			ir.outputRooms();
	    			break;
	    		case 2:
	    			break;
	    		case 3:
	    			break;
	    		case 4:
	    			break;
	    		case 5:
	    			break;
	    		case 6:
	    			System.out.println("Quit successful");
	    	}
	    }
	} catch (SQLException e) {
	    System.err.println("SQLException: " + e.getMessage());
	}
    }

    private void outputRooms() throws SQLException {
    	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    // Step 2: Construct SQL statement
		String sql = "WITH CurrOccupied AS (\n" +
				"	SELECT RoomCode, CheckOut AS NextAvail\n" +
				"	FROM lab7_rooms JOIN lab7_reservations ON Room=RoomCode\n" +
				"	WHERE CheckIn<=CURDATE() AND CheckOut>CURDATE()\n" +
				"),\n" +
				"NextRes AS (\n" +
				"    SELECT DISTINCT RoomCode, \n" +
				"        MIN(CheckIn) OVER (PARTITION BY RoomCode) AS NextReservation\n" +
				"    FROM lab7_rooms JOIN lab7_reservations ON RoomCode=Room\n" +
				"    WHERE CheckIn>CURDATE()\n" +
				")\n" +
				"SELECT R.RoomCode, R.RoomName, R.Beds, R.bedType, R.maxOcc, R.basePrice,\n" +
				"    R.decor, NextAvail, NextReservation\n" +
				"FROM (lab7_rooms AS R LEFT JOIN CurrOccupied AS CO ON R.RoomCode=CO.RoomCode)\n" +
				// just a normal join works in actual sql; need left join here for H2
				"    LEFT JOIN NextRes AS NR ON NR.RoomCode=R.RoomCode\n" +
				"ORDER BY RoomName;";

	    // Step 3: (omitted in this example) Start transaction

	    // Step 4: Send SQL statement to DBMS
	    try (Statement stmt = conn.createStatement();
		 ResultSet rs = stmt.executeQuery(sql)) {

		// Step 5: Receive results
	    System.out.println("1: Rooms and rates, output has the format:");
	    System.out.println("RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor, nextCheckIn, nextReservation:");
		while (rs.next()) {
		    String code = rs.getString("RoomCode");
		    String name = rs.getString("RoomName");
		    int beds = rs.getInt("Beds");
		    String bedType = rs.getString("bedType");
		    int maxOcc = rs.getInt("maxOcc");
		    float price = rs.getFloat("basePrice");
		    String decor = rs.getString("decor");
		    String nextAvail = rs.getString("NextAvail");
		    // Tried to handle this in SQL query using ifnull and it worked on labthreesixfive, but not for H2
		    if (nextAvail == null){
		    	nextAvail = "Today";
			}
		    String nextReservation = rs.getString("NextReservation");
			if (nextReservation == null){
				nextReservation = "None";
			}
		    System.out.format("%s, %s, %d, %s, %d, ($%.2f), %s, %s, %s %n",
					code, name, beds, bedType, maxOcc, price, decor, nextAvail, nextReservation);
		}
	    }

	    // Step 6: (omitted in this example) Commit or rollback transaction
	}
	// Step 7: Close connection (handled by try-with-resources syntax)

    }



    /*
    // Demo1 - Establish JDBC connection, execute DDL statement
    private void demo1() throws SQLException {

	// Step 0: Load JDBC Driver
	// No longer required as of JDBC 2.0  / Java 6
	try{
	    Class.forName("org.h2.Driver");
	    System.out.println("H2 JDBC Driver loaded");
	} catch (ClassNotFoundException ex) {
	    System.err.println("Unable to load JDBC Driver");
	    System.exit(-1);
	}

	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    // Step 2: Construct SQL statement
	    String sql = "ALTER TABLE hp_goods ADD COLUMN AvailUntil DATE";

	    // Step 3: (omitted in this example) Start transaction

	    try (Statement stmt = conn.createStatement()) {

		// Step 4: Send SQL statement to DBMS
		boolean exRes = stmt.execute(sql);
		
		// Step 5: Handle results
		System.out.format("Result from ALTER: %b %n", exRes);
	    }

	    // Step 6: (omitted in this example) Commit or rollback transaction
	}
	// Step 7: Close connection (handled by try-with-resources syntax)
    }
    

    // Demo2 - Establish JDBC connection, execute SELECT query, read & print result
    private void demo2() throws SQLException {

	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    // Step 2: Construct SQL statement
	    String sql = "SELECT * FROM hp_goods";

	    // Step 3: (omitted in this example) Start transaction

	    // Step 4: Send SQL statement to DBMS
	    try (Statement stmt = conn.createStatement();
		 ResultSet rs = stmt.executeQuery(sql)) {

		// Step 5: Receive results
		while (rs.next()) {
		    String flavor = rs.getString("Flavor");
		    String food = rs.getString("Food");
		    float price = rs.getFloat("Price");
		    System.out.format("%s %s ($%.2f) %n", flavor, food, price);
		}
	    }

	    // Step 6: (omitted in this example) Commit or rollback transaction
	}
	// Step 7: Close connection (handled by try-with-resources syntax)
    }


    // Demo3 - Establish JDBC connection, execute DML query (UPDATE)
    // -------------------------------------------
    // Never (ever) write database code like this!
    // -------------------------------------------
    private void demo3() throws SQLException {

        demo2();
        
	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    // Step 2: Construct SQL statement
	    Scanner scanner = new Scanner(System.in);
	    System.out.print("Enter a flavor: ");
	    String flavor = scanner.nextLine();
	    System.out.format("Until what date will %s be available (YYYY-MM-DD)? ", flavor);
	    String availUntilDate = scanner.nextLine();

	    // -------------------------------------------
	    // Never (ever) write database code like this!
	    // -------------------------------------------
	    String updateSql = "UPDATE hp_goods SET AvailUntil = '" + availUntilDate + "' " +
		               "WHERE Flavor = '" + flavor + "'";

	    // Step 3: (omitted in this example) Start transaction
	    
	    try (Statement stmt = conn.createStatement()) {
		
		// Step 4: Send SQL statement to DBMS
		int rowCount = stmt.executeUpdate(updateSql);
		
		// Step 5: Handle results
		System.out.format("Updated %d records for %s pastries%n", rowCount, flavor);		
	    }

	    // Step 6: (omitted in this example) Commit or rollback transaction
	    
	}
	// Step 7: Close connection (handled implcitly by try-with-resources syntax)

        demo2();
        
    }


    // Demo4 - Establish JDBC connection, execute DML query (UPDATE) using PreparedStatement / transaction    
    private void demo4() throws SQLException {

	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    // Step 2: Construct SQL statement
	    Scanner scanner = new Scanner(System.in);
	    System.out.print("Enter a flavor: ");
	    String flavor = scanner.nextLine();
	    System.out.format("Until what date will %s be available (YYYY-MM-DD)? ", flavor);
	    LocalDate availDt = LocalDate.parse(scanner.nextLine());
	    
	    String updateSql = "UPDATE hp_goods SET AvailUntil = ? WHERE Flavor = ?";

	    // Step 3: Start transaction
	    conn.setAutoCommit(false);
	    
	    try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
		
		// Step 4: Send SQL statement to DBMS
		pstmt.setDate(1, java.sql.Date.valueOf(availDt));
		pstmt.setString(2, flavor);
		int rowCount = pstmt.executeUpdate();
		
		// Step 5: Handle results
		System.out.format("Updated %d records for %s pastries%n", rowCount, flavor);

		// Step 6: Commit or rollback transaction
		conn.commit();
	    } catch (SQLException e) {
		conn.rollback();
	    }

	}
	// Step 7: Close connection (handled implcitly by try-with-resources syntax)
    }



    // Demo5 - Construct a query using PreparedStatement
    private void demo5() throws SQLException {

	// Step 1: Establish connection to RDBMS
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    Scanner scanner = new Scanner(System.in);
	    System.out.print("Find pastries with price <=: ");
	    Double price = Double.valueOf(scanner.nextLine());
	    System.out.print("Filter by flavor (or 'Any'): ");
	    String flavor = scanner.nextLine();

	    List<Object> params = new ArrayList<Object>();
	    params.add(price);
	    StringBuilder sb = new StringBuilder("SELECT * FROM hp_goods WHERE price <= ?");
	    if (!"any".equalsIgnoreCase(flavor)) {
		sb.append(" AND Flavor = ?");
		params.add(flavor);
	    }
	    
	    try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
		int i = 1;
		for (Object p : params) {
		    pstmt.setObject(i++, p);
		}

		try (ResultSet rs = pstmt.executeQuery()) {
		    System.out.println("Matching Pastries:");
		    int matchCount = 0;
		    while (rs.next()) {
			System.out.format("%s %s ($%.2f) %n", rs.getString("Flavor"), rs.getString("Food"), rs.getDouble("price"));
			matchCount++;
		    }
		    System.out.format("----------------------%nFound %d match%s %n", matchCount, matchCount == 1 ? "" : "es");
		}
	    }

	}
    }
    */

    private void initDb() throws SQLException {
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
	    try (Statement stmt = conn.createStatement()) {
	    		stmt.execute("DROP TABLE IF EXISTS lab7_reservations");
                stmt.execute("DROP TABLE IF EXISTS lab7_rooms");
                stmt.execute("CREATE TABLE lab7_rooms (" +
                        "RoomCode char(5) PRIMARY KEY, " +
                        "RoomName varchar(30) DEFAULT NULL, " +
                        "Beds int(11) DEFAULT NULL, " +
                        "bedType varchar(8) DEFAULT NULL, " +
                        "maxOcc int(11) DEFAULT NULL, " +
                        "basePrice float DEFAULT NULL, " +
                        "decor varchar(20) DEFAULT NULL)");
                stmt.execute("CREATE TABLE lab7_reservations (" +
                        "CODE int(11), " +
                        "Room char(5) REFERENCES lab7_rooms (RoomCode), " +
                        "CheckIn DATE, " +
                        "CheckOut DATE, " +
                        "Rate float, " +
                        "LastName varchar(15), " +
                        "FirstName varchar(15), " +
                        "Adults int(11), " +
                        "Kids int(11), " +
						"PRIMARY KEY (CODE, CheckIn, CheckOut))");
                stmt.execute("INSERT INTO lab7_rooms" +
                        "(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) " +
                        "VALUES ('AOB', 'Abscond or bolster', 2, 'Queen', 4, 175, 'traditional')");
                stmt.execute("INSERT INTO lab7_rooms" +
                        "(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) " +
                        "VALUES ('CAS', 'Convoke and sanguine', 2, 'King', 4, 175, 'traditional')");
                stmt.execute("INSERT INTO lab7_rooms" +
                        "(RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor) " +
                        "VALUES ('FNA', 'Frugal not apropos', 2, 'King', 4, 250, 'traditional')");
                stmt.execute("INSERT INTO lab7_reservations" +
                        "(CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) " +
                        "VALUES (10489, 'AOB', '2010-02-02', '2010-02-05', 218.75, 'CARISTO', 'MARKITA', 2, 1)");
                stmt.execute("INSERT INTO lab7_reservations" +
                        "(CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) " +
                        "VALUES (10990, 'CAS', '2010-09-21', '2010-09-27', 175, 'TRACHSEL', 'DAMIEN', 1, 3)");
                stmt.execute("INSERT INTO lab7_reservations" +
                        "(CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) " +
                        "VALUES (10984, 'AOB', '2010-12-28', '2011-01-01', 201.25, 'ZULLO', 'WILLY', 2, 1)");
				stmt.execute("INSERT INTO lab7_reservations" +
					"(CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) " +
					"VALUES (10990, 'CAS', '2020-09-21', '2020-09-27', 175, 'TRACHSEL', 'DAMIEN', 1, 3)");
	    }
	}
    }
    

}

