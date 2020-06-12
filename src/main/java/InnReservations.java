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
	    			System.out.println("Please enter the integer code of the reservation you would like to update:");
	    			int code = s.nextInt();
	    			while(!ir.getReservation(code)){
	    				System.out.println("There are no reservations with that reservation code. " +
								"Please enter a valid integer code:");
						code = s.nextInt();
					}
	    			ir.updateReservation(code);
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
	try (Connection conn = DriverManager.getConnection(JDBC_URL,
							   JDBC_USER,
							   JDBC_PASSWORD)) {
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

	    try (Statement stmt = conn.createStatement();
		 ResultSet rs = stmt.executeQuery(sql)) {

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

	}
    }

	private boolean getReservation(int code) throws SQLException {
		try (Connection conn = DriverManager.getConnection(JDBC_URL,
				JDBC_USER,
				JDBC_PASSWORD)) {
			// dont need to sanitize this because input is integer from nextInt
			String sql = "SELECT * FROM lab7_reservations WHERE Code=" + code;

			try (Statement stmt = conn.createStatement();
				 ResultSet rs = stmt.executeQuery(sql)) {

				if (!rs.isBeforeFirst() ) {
					return false;
				}

				System.out.println("This is your current reservation (output format: " +
						"CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids):");
				while (rs.next()) {
					String resCode = rs.getString("Code");
					String room = rs.getString("Room");
					String checkIn = rs.getString("CheckIn");
					String checkOut = rs.getString("CheckOut");
					float rate = rs.getFloat("Rate");
					String last = rs.getString("LastName");
					String first = rs.getString("FirstName");
					int adults = rs.getInt("Adults");
					int kids = rs.getInt("Kids");
					// Tried to handle this in SQL query using ifnull and it worked on labthreesixfive, but not for H2
					System.out.format("%s, %s, %s, %s, ($%.2f), %s, %s, %d, %d %n",
							resCode, room, checkIn, checkOut, rate, last, first, adults, kids);
				}
			}
		}

		return true;
	}

	private void updateReservation(int code) throws SQLException {
    	//FIXME NEED TO CHECK WHETHER END DATE IS LATER THAN BEGIN DATE
		try (Connection conn = DriverManager.getConnection(JDBC_URL,
				JDBC_USER,
				JDBC_PASSWORD)) {
			Scanner s = new Scanner(System.in);
			ArrayList<String> colsList = new ArrayList<String>();
			ArrayList<String> valsList = new ArrayList<String>();
			System.out.println("For each of the following values, please enter the new value; " +
					"(enter \"no change\" if you do not wish to change that particular value for the reservation):");
			System.out.print("First Name: ");
			String first = s.nextLine();
			if (!first.equals("no change")){
				colsList.add("FirstName");
				valsList.add(first);
			}
			System.out.print("Last Name: ");
			String last = s.nextLine();
			if (!last.equals("no change")){
				colsList.add("LastName");
				valsList.add(last);
			}
			System.out.print("Begin Date (YYYY-MM-DD): ");
			String begin = s.nextLine();
			if (!begin.equals("no change")){
				if (simpleDateParse(begin)) {
					if (checkBeginConflict(code, begin)){
						colsList.add("CheckIn");
						valsList.add(begin);
					} else {
						System.out.println("This begin date conflicts with another reservation in the database." +
								"The begin date for the reservation will not be updated.");
					}
				} else {
					System.out.println("The date entered was incorrectly formatted. " +
							"The begin date for the reservation will not be updated.");
				}
			}
			System.out.print("End Date (YYYY-MM-DD): ");
			String end = s.nextLine();
			if (!end.equals("no change")){
				if (simpleDateParse(end)) {
					if (checkEndConflict(code, end)){
						colsList.add("CheckOut");
						valsList.add(end);
					} else {
						System.out.println("This end date conflicts with another reservation in the database." +
								"The end date for the reservation will not be updated.");
					}
				} else {
					System.out.println("The date entered was incorrectly formatted. " +
							"The end date for the reservation will not be updated.");
				}
			}
			System.out.print("Number of Children: ");
			String kids = s.nextLine();
			if (!kids.equals("no change")){
				colsList.add("Kids");
				valsList.add(kids);
			}
			System.out.print("Number of Adults: ");
			String adults = s.nextLine();
			if (!adults.equals("no change")){
				colsList.add("Adults");
				valsList.add(adults);
			}

			if (colsList.size()>0){
				String sql = "UPDATE lab7_reservations SET ";
				for (int i = 0; i < colsList.size(); i++){
					sql = sql + colsList.get(i) + "=?, ";
				}
				sql = sql.substring(0, sql.length()-2) + " WHERE CODE=" + code;

				PreparedStatement pstmt = conn.prepareStatement(sql);
				for (int i = 0; i < valsList.size(); i++) {
					if (colsList.get(i).equals("Kids") || colsList.get(i).equals("Adults")) {
						pstmt.setInt(i + 1, Integer.parseInt(valsList.get(i)));
					} else {
						pstmt.setString(i + 1, valsList.get(i));
					}
				}

				pstmt.executeUpdate();
			}
		}
	}

	private boolean simpleDateParse(String date){
    	if (date.length() != 10){
    		return false;
		}
    	if (date.charAt(4) != '-' || date.charAt(7) != '-'){
    		return false;
		}
    	int x;
    	try {
    		x = Integer.parseInt(date.substring(0, 4));
    		if (x < 0){
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		try {
			x = Integer.parseInt(date.substring(5, 7));
			if (x < 1  || x > 12){
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		try {
			x = Integer.parseInt(date.substring(8, 10));
			if (x < 1 || x > 31){
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	private boolean checkBeginConflict(int code, String begin) throws SQLException {
		try (Connection conn = DriverManager.getConnection(JDBC_URL,
				JDBC_USER,
				JDBC_PASSWORD)) {
			// dont need to sanitize this because input is integer from nextInt
			String sql = "SELECT R2.CheckIn, R2.CheckOut\n" +
						 "FROM lab7_reservations AS R1 JOIN lab7_reservations AS R2 ON R1.Room=R2.Room\n" +
						 "   AND R1.Code=" + code + "\n" +
						 "WHERE (R2.CheckIn<=\'" + begin + "\' AND R2.CheckOut>\'" + begin + "\')\n" +
						 "	 OR (R2.CheckIn>=\'" + begin + "\' AND R2.CheckIn<R1.CheckOut);";


			try (Statement stmt = conn.createStatement();
				 ResultSet rs = stmt.executeQuery(sql)) {

				return !rs.isBeforeFirst();
			}
		}
	}

	private boolean checkEndConflict(int code, String end) throws SQLException {
		try (Connection conn = DriverManager.getConnection(JDBC_URL,
				JDBC_USER,
				JDBC_PASSWORD)) {
			// dont need to sanitize this because input is integer from nextInt
			String sql = "SELECT R2.CheckIn, R2.CheckOut\n" +
					"FROM lab7_reservations AS R1 JOIN lab7_reservations AS R2 ON R1.Room=R2.Room\n" +
					"   AND R1.Code=" + code + "\n" +
					"WHERE (R2.CheckIn<=\'" + end + "\' AND R2.CheckOut>\'" + end + "\')\n" +
					"	 OR (R2.CheckIn<=\'" + end + "\' AND R2.CheckIn>R1.CheckIn);";


			try (Statement stmt = conn.createStatement();
				 ResultSet rs = stmt.executeQuery(sql)) {

				return !rs.isBeforeFirst();
			}
		}
	}

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
                        "CODE int(11) PRIMARY KEY, " +
                        "Room char(5) REFERENCES lab7_rooms (RoomCode), " +
                        "CheckIn DATE, " +
                        "CheckOut DATE, " +
                        "Rate float, " +
                        "LastName varchar(15), " +
                        "FirstName varchar(15), " +
                        "Adults int(11) DEFAULT NULL, " +
                        "Kids int(11) DEFAULT NULL, " +
						"CHECK (CheckIn <= CheckOut))");
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
					"VALUES (10991, 'CAS', '2020-09-21', '2020-09-27', 175, 'TRACHSEL', 'DAMIEN', 1, 3)");
				stmt.execute("INSERT INTO lab7_reservations" +
					"(CODE, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) " +
					"VALUES (10992, 'CAS', '2080-09-21', '2080-09-27', 175, 'BIG', 'DATE', 1, 3)");
	    }
	}
    }
    

}

