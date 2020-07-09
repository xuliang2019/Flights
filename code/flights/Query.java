package edu.uw.cs;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.xml.bind.*;

/**
 * Runs queries against a back-end database
 */
public class Query {
  // DB Connection
  private Connection conn;

  // Password hashing parameter constants
  private static final int HASH_STRENGTH = 65536;
  private static final int KEY_LENGTH = 128;

  // Canned queries
  private static final String CHECK_FLIGHT_CAPACITY = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement checkFlightCapacityStatement;

  private static final String GET_USER = "SELECT * FROM Users WHERE username = ? AND password = ?";
  private PreparedStatement getUserStatement;

  private static final String USER_INFO = "SELECT * FROM Users WHERE username = ?";
  private PreparedStatement userInfoStatement;

  // Users statements
  private static final String CREATE_USER = "INSERT INTO Users(username, password, balance) VALUES (?, ?, ?)";
  private PreparedStatement createUserStatement;

  private static final String GET_BALANCES = "SELECT balance FROM Users WHERE username = ?";
  private PreparedStatement getBalancesStatement;

  // Reservation statements
  private static final String GET_RESERVATIONS = "SELECT * FROM Reservations WHERE username = ? AND dayOfMonth = ?";
  private PreparedStatement getReservationsStatement;

  private static final String NEW_RESERVATIONS = "SELECT * FROM Reservations WHERE rid = ?";
  private PreparedStatement reservationsStatement;

  private static final String RESERVATIONS_TABLE = "SELECT * FROM Reservations WHERE username = ? ORDER BY rid ASC";
  private PreparedStatement reservationsTableStatement;

  private static final String RESERVATION = "INSERT INTO Reservations VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
  private PreparedStatement insertReservationStatement;

  //Capacity statements
  private static final String INSERT_CAPACITY = "INSERT INTO Capacities "
          + "SELECT F.fid, F.capacity "
          + "FROM Flights F "
          + "WHERE F.fid = ? "
          + "AND NOT EXISTS "
          + "(SELECT * FROM Capacities C WHERE C.fid = F.fid)";
  private PreparedStatement capacityStatement;

  private static final String GET_CAPACITY = "SELECT capacity FROM Capacities WHERE fid = ?";
  private PreparedStatement getCapacityStatement;

  private static final String UPDATE_CAPACITY= "UPDATE Capacities "
          + "SET capacity = ((SELECT capacity FROM Capacities WHERE fid = ?) - 1) "
          +"WHERE fid = ?";
  private PreparedStatement updateCapacityStatement;

  // Find day
  private static final String FIND_DAY = "SELECT day_of_month FROM Flights WHERE fid = ?";
  private PreparedStatement getDayStatement;

  // Update balance and paid
  private static final String UPDATE_BALANCE= "UPDATE Users SET balance = ? WHERE username = ?";
  private PreparedStatement updateBalanceStatement;

  private static final String UPDATE_PAID= "UPDATE Reservations SET paid = ? WHERE rid = ?";
  private PreparedStatement updatePaidStatement;

  // Get flight
  private static final String GET_FLIGHT = "SELECT * FROM Flights WHERE fid = ?";
  private PreparedStatement getFlightStatement;

  // Update paid and canceled
  private static final String CANCEL = "UPDATE Reservations SET paid = ? , canceled = ? WHERE rid = ?";
  private PreparedStatement cancelStatement;

  // Add capacity
  private static final String ADD = "UPDATE Capacities SET capacity = ? WHERE fid = ?";
  private PreparedStatement addCapacityStatement;

  // local variables
  private String login;
  private int rid = 1;

  // Search implementation
  private static final String DIRECT = "SELECT TOP (?) * FROM Flights "
          + "WHERE origin_city = ? AND dest_city = ? AND day_of_month =  ? "
          + "AND canceled <> 1 ORDER BY actual_time ASC, fid ASC";
  private PreparedStatement directStatement;

  private static final String INDIRECT = "SELECT TOP (?) * FROM Flights F, Flights F1 "
          + "WHERE F.origin_city = ? AND F.dest_city = F1.origin_city AND F1.dest_city = ? "
          + "AND F.day_of_month = ? AND F.day_of_month = F1.day_of_month "
          + "AND F.canceled <> 1 AND F1.canceled <> 1 "
          + "ORDER BY (F.actual_time + F1.actual_time), F.fid ASC;";
  private PreparedStatement indirectStatement;
  // itinerary
  private List<Itinerary> itinerary = new ArrayList<Itinerary>();

  /**
   * Establishes a new application-to-database connection. Uses the
   * dbconn.properties configuration settings
   * 
   * @throws IOException
   * @throws SQLException
   */
  public void openConnection() throws IOException, SQLException {
    // Connect to the database with the provided connection configuration
    Properties configProps = new Properties();
    configProps.load(new FileInputStream("dbconn.properties"));
    String serverURL = configProps.getProperty("hw1.server_url");
    String dbName = configProps.getProperty("hw1.database_name");
    String adminName = configProps.getProperty("hw1.username");
    String password = configProps.getProperty("hw1.password");
    String connectionUrl = String.format("jdbc:sqlserver://%s:1433;databaseName=%s;user=%s;password=%s", serverURL,
        dbName, adminName, password);
    conn = DriverManager.getConnection(connectionUrl);

    // By default, automatically commit after each statement
    conn.setAutoCommit(true);

    // By default, set the transaction isolation level to serializable
    conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
  }

  /**
   * Closes the application-to-database connection
   */
  public void closeConnection() throws SQLException {
    conn.close();
  }


  // Itinerary class
  class Itinerary
  {
    public int fid1;
    public int fid2;
    public int capacity1;
    public int capacity2;
    public int cost;
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      Statement clear = conn.createStatement();
      clear.executeUpdate("DELETE FROM Users");
      clear.executeUpdate("DELETE FROM Capacities");
      clear.executeUpdate("DELETE FROM Reservations");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  public void prepareStatements() throws SQLException {
    checkFlightCapacityStatement = conn.prepareStatement(CHECK_FLIGHT_CAPACITY);
    getUserStatement = conn.prepareStatement(GET_USER);
    createUserStatement = conn.prepareStatement(CREATE_USER);
    directStatement = conn.prepareStatement(DIRECT);
    indirectStatement = conn.prepareStatement(INDIRECT);
    getReservationsStatement = conn.prepareStatement(GET_RESERVATIONS);
    capacityStatement = conn.prepareStatement(INSERT_CAPACITY);
    getCapacityStatement = conn.prepareStatement(GET_CAPACITY);
    updateCapacityStatement = conn.prepareStatement(UPDATE_CAPACITY);
    getDayStatement = conn.prepareStatement(FIND_DAY);
    insertReservationStatement = conn.prepareStatement(RESERVATION);
    reservationsStatement = conn.prepareStatement(NEW_RESERVATIONS);
    getBalancesStatement = conn.prepareStatement(GET_BALANCES);
    updateBalanceStatement = conn.prepareStatement(UPDATE_BALANCE);
    updatePaidStatement = conn.prepareStatement(UPDATE_PAID);
    reservationsTableStatement = conn.prepareStatement(RESERVATIONS_TABLE);
    getFlightStatement = conn.prepareStatement(GET_FLIGHT);
    cancelStatement = conn.prepareStatement(CANCEL);
    addCapacityStatement = conn.prepareStatement(ADD);
    userInfoStatement = conn.prepareStatement(USER_INFO);
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username user's username
   * @param password user's password
   *
   * @return If someone has already logged in, then return "User already logged
   *         in\n" For all other errors, return "Login failed\n". Otherwise,
   *         return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password) {
    if (this.login != null) {
      return "User already logged in\n";
    }
    try {
      getUserStatement.clearParameters();
      getUserStatement.setString(1, username);
      getUserStatement.setString(2, password);
      ResultSet result = getUserStatement.executeQuery();
      if (result.next()) {
        this.login = username;
        result.close();
        return "Logged in as " + this.login + "\n";
      }
      result.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return "Login failed\n";
  }

  /**
   * Implement the create user function.
   *
   * @param username   new user's username. User names are unique the system.
   * @param password   new user's password.
   * @param initAmount initial amount to deposit into the user's account, should
   *                   be >= 0 (failure otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n"
   *         if failed.
   */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    if (initAmount >= 0) {
      try {
        userInfoStatement.clearParameters();
        userInfoStatement.setString(1, username);
        ResultSet result = userInfoStatement.executeQuery();
        if (result.next()) {
          result.close();
          return "Failed to create user\n";
        }
        createUserStatement.clearParameters();
        createUserStatement.setString(1, username);
        createUserStatement.setString(2, password);
        createUserStatement.setInt(3, initAmount);
        createUserStatement.execute();
        return "Created user " + username + "\n";
      }
     catch (Exception e) {
       e.printStackTrace();
     }
    }
    return "Failed to create user\n";
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination
   * city, on the given day of the month. If {@code directFlight} is true, it only
   * searches for direct flights, otherwise is searches for direct flights and
   * flights with two "hops." Only searches for up to the number of itineraries
   * given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight        if true, then only search for direct flights,
   *                            otherwise include indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return
   *
   * @return If no itineraries were found, return "No flights match your
   *         selection\n". If an error occurs, then return "Failed to search\n".
   *
   *         Otherwise, the sorted itineraries printed in the following format:
   *
   *         Itinerary [itinerary number]: [number of flights] flight(s), [total
   *         flight time] minutes\n [first flight in itinerary]\n ... [last flight
   *         in itinerary]\n
   *
   *         Each flight should be printed using the same format as in the
   *         {@code Flight} class. Itinerary numbers in each search should always
   *         start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, boolean directFlight, int dayOfMonth,
      int numberOfItineraries) {
    // WARNING the below code is unsafe and only handles searches for direct flights
    // You can use the below code as a starting reference point or you can get rid
    // of it all and replace it with your own implementation.
    //
    StringBuffer sb = new StringBuffer();
    try {
      if (directFlight) {
        // direct itineraries
        directStatement.clearParameters();
        directStatement.setInt(1, numberOfItineraries);
        directStatement.setString(2, originCity);
        directStatement.setString(3, destinationCity);
        directStatement.setInt(4, dayOfMonth);
        ResultSet directResult = directStatement.executeQuery();
        int num = 0;
        while (directResult.next()) {
          int result_fid = directResult.getInt("fid");
          int result_dayOfMonth = directResult.getInt("day_of_month");
          String result_carrierId = directResult.getString("carrier_id");
          String result_flightNum = directResult.getString("flight_num");
          String result_originCity = directResult.getString("origin_city");
          String result_destCity = directResult.getString("dest_city");
          int result_time = directResult.getInt("actual_time");
          int result_capacity = directResult.getInt("capacity");
          int result_price = directResult.getInt("price");
          Itinerary it_list = new Itinerary();
          it_list.fid1 = result_fid;
          it_list.fid2 = -1;
          it_list.capacity1 = result_capacity;
          it_list.capacity2 = -1;
          it_list.cost = result_price;
          itinerary.add(it_list);
          sb.append("Itinerary " + num + ": 1 flight(s), " + result_time + " minutes\n");
          sb.append("ID: " + result_fid + " Day: " + result_dayOfMonth + " Carrier: " + result_carrierId
                  + " Number: " + result_flightNum + " Origin: " + result_originCity
                  + " Dest: " + result_destCity + " Duration: " + result_time
                  + " Capacity: " + result_capacity + " Price: " + result_price + "\n");
          num += 1;
        }
        directResult.close();
      }
      //output indirectflight
      // still need to work
      else {
        directStatement.clearParameters();
        directStatement.setInt(1, numberOfItineraries);
        directStatement.setString(2, originCity);
        directStatement.setString(3, destinationCity);
        directStatement.setInt(4, dayOfMonth);
        ResultSet directResult = directStatement.executeQuery();
        int direct = 0;
        while (directResult.next()) {
          direct += 1;
        }
        int remain = numberOfItineraries - direct;
        directStatement.clearParameters();
        directStatement.setInt(1, direct);
        directStatement.setString(2, originCity);
        directStatement.setString(3, destinationCity);
        directStatement.setInt(4, dayOfMonth);
        ResultSet directResult2 = directStatement.executeQuery();
        // indirect flight
        indirectStatement.clearParameters();
        indirectStatement.setInt(1, remain);
        indirectStatement.setString(2, originCity);
        indirectStatement.setString(3, destinationCity);
        indirectStatement.setInt(4, dayOfMonth);
        ResultSet indirectResult = indirectStatement.executeQuery();
        int result1 = 1;
        int result2 = 1;
        if (!directResult2.next()) {
          result1 = 0;
        }
        if (!indirectResult.next()) {
          result2 = 0;
        }
        int count = 0;
        while (numberOfItineraries > 0) {
          // time comparison
          int direct_time = 0;
          int indirect_time = 0;
          if (result1 == 1 && result2 == 1) {
            direct_time = directResult2.getInt("actual_time");
            indirect_time = indirectResult.getInt(15) + indirectResult.getInt(33);
          }
          else if (result1 == 0 && result2 == 1) {
            direct_time = 10000;
            indirect_time = indirectResult.getInt(15) + indirectResult.getInt(33);
          }
          else if (result1 == 1 && result2 == 0) {
            direct_time = directResult2.getInt("actual_time");
            indirect_time = 100000;
          }
          if (direct_time <= indirect_time) {
            int result_fid = directResult2.getInt("fid");
            int result_dayOfMonth = directResult2.getInt("day_of_month");
            String result_carrierId = directResult2.getString("carrier_id");
            String result_flightNum = directResult2.getString("flight_num");
            String result_originCity = directResult2.getString("origin_city");
            String result_destCity = directResult2.getString("dest_city");
            int result_time = directResult2.getInt("actual_time");
            int result_capacity = directResult2.getInt("capacity");
            int result_price = directResult2.getInt("price");
            Itinerary it_list = new Itinerary();
            it_list.fid1 = result_fid;
            it_list.fid2 = -1;
            it_list.capacity1 = result_capacity;
            it_list.capacity2 = -1;
            it_list.cost = result_price;
            itinerary.add(it_list);
            sb.append("Itinerary " + count + ": 1 flight(s), " + result_time + " minutes\n");
            sb.append("ID: " + result_fid + " Day: " + result_dayOfMonth + " Carrier: " + result_carrierId
                    + " Number: " + result_flightNum + " Origin: " + result_originCity
                    + " Dest: " + result_destCity + " Duration: " + result_time
                    + " Capacity: " + result_capacity + " Price: " + result_price + "\n");
            if (!directResult2.next()) {
              result1 = 0;
            }
            numberOfItineraries -= 1;
            count += 1;
          }
          else {
            int result_dayOfMonth = indirectResult.getInt("day_of_month");
            // first flight
            int fid1 = indirectResult.getInt(1);
            String carrierId1 = indirectResult.getString(5);
            String flightNum1 = indirectResult.getString(6);
            String originCity1 = indirectResult.getString(7);
            String destCity1 = indirectResult.getString(9);
            int time1 = indirectResult.getInt(15);
            int capacity1 = indirectResult.getInt(17);
            int price1 = indirectResult.getInt(18);

            // second flight
            int fid2 = indirectResult.getInt(19);
            String carrierId2 = indirectResult.getString(23);
            String flightNum2 = indirectResult.getString(24);
            String originCity2 = indirectResult.getString(25);
            String destCity2 = indirectResult.getString(27);
            int time2 = indirectResult.getInt(33);
            int capacity2 = indirectResult.getInt(35);
            int price2 = indirectResult.getInt(36);
            // append values
            Itinerary it_list = new Itinerary();
            it_list.fid1 = fid1;
            it_list.fid2 = fid2;
            it_list.capacity1 = capacity1;
            it_list.capacity2 = capacity2;
            it_list.cost = price1 + price2;
            itinerary.add(it_list);
            // append result
            sb.append("Itinerary " + count + ": 2 flight(s), " + (time1 + time2) + " minutes\n");
            sb.append("ID: " + fid1 + " Day: " + result_dayOfMonth + " Carrier: " + carrierId1
                    + " Number: " + flightNum1 + " Origin: " + originCity1 + " Dest: "
                    + destCity1 + " Duration: " + time1
                    + " Capacity: " + capacity1 + " Price: " + price1 + "\n");
            sb.append("ID: " + fid2 + " Day: " + result_dayOfMonth + " Carrier: " + carrierId2
                    + " Number: " + flightNum2 + " Origin: " + originCity2 + " Dest: "
                    + destCity2 + " Duration: " + time2
                    + " Capacity: " + capacity2 + " Price: " + price2 + "\n");
            if (!indirectResult.next()) {
              result2 = 0;
            }
            numberOfItineraries -= 1;
            count += 1;
          }
        }
        directResult2.close();
        indirectResult.close();
      }
      if (itinerary.size() == 0) {
        return "No flights match your selection\n";
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is
   *                    returned by search in the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations,
   *         not logged in\n". If try to book an itinerary with invalid ID, then
   *         return "No such itinerary {@code itineraryId}\n". If the user already
   *         has a reservation on the same day as the one that they are trying to
   *         book now, then return "You cannot book two flights in the same
   *         day\n". For all other errors, return "Booking failed\n".
   *
   *         And if booking succeeded, return "Booked flight(s), reservation ID:
   *         [reservationId]\n" where reservationId is a unique number in the
   *         reservation system that starts from 1 and increments by 1 each time a
   *         successful reservation is made by any user in the system.
   */
  public String transaction_book(int itineraryId) {
    if (this.login == null) {
      return "Cannot book reservations, not logged in\n";
    }
    if (itineraryId < 0 || itineraryId > itinerary.size() - 1) {
      return "No such itinerary " + itineraryId + "\n";
    }
    Itinerary it = itinerary.get(itineraryId);
    // judge whether booked two  flights on the same day
    try {
      getDayStatement.clearParameters();
      getDayStatement.setInt(1, it.fid1);
      ResultSet dayResult = getDayStatement.executeQuery();
      dayResult.next();
      int dayOfMonth = dayResult.getInt("day_of_month");
      dayResult.close();

      getReservationsStatement.clearParameters();
      getReservationsStatement.setString(1, this.login);
      getReservationsStatement.setInt(2, dayOfMonth);
      ResultSet reservation = getReservationsStatement.executeQuery();
      //reservation.next();
      if (reservation.next()) {
        return "You cannot book two flights in the same day\n";
      }
      reservation.close();

      capacityStatement.clearParameters();
      capacityStatement.setInt(1, it.fid1);
      capacityStatement.execute();
      // check the first flight capacity
      getCapacityStatement.clearParameters();
      getCapacityStatement.setInt(1, it.fid1);
      ResultSet getCapacity1 = getCapacityStatement.executeQuery();
      getCapacity1.next();
      int capacity1 = getCapacity1.getInt("capacity");
      getCapacity1.close();

      if (capacity1 == 0) {
        return "Booking failed\n";
      }
      // check the second flight capacity
      if (it.fid2 != -1) {
        getCapacityStatement.clearParameters();
        getCapacityStatement.setInt(1, it.fid2);
        ResultSet getCapacity2 = getCapacityStatement.executeQuery();
        getCapacity2.next();
        int capacity2 = getCapacity2.getInt("capacity");
        getCapacity2.close();
        if (capacity2 == 0) {
          return "Booking failed\n";
        }
      }
      // update the first flight capacity
      updateCapacityStatement.clearParameters();
      updateCapacityStatement.setInt(1, it.fid1);
      updateCapacityStatement.setInt(2, it.fid1);
      updateCapacityStatement.execute();
      // update the second flight capacity
      if (it.fid2 != -1) {
        updateCapacityStatement.clearParameters();
        updateCapacityStatement.setInt(1, it.fid2);
        updateCapacityStatement.setInt(2, it.fid2);
        updateCapacityStatement.execute();
      }
      // reservation table
      insertReservationStatement.clearParameters();
      insertReservationStatement.setInt(1, rid);
      insertReservationStatement.setInt(2, it.fid1);
      insertReservationStatement.setInt(3, it.fid2);
      insertReservationStatement.setInt(4, 0);
      insertReservationStatement.setInt(5, it.cost);
      insertReservationStatement.setString(6, this.login);
      insertReservationStatement.setInt(7, dayOfMonth);
      insertReservationStatement.setInt(8, 0);
      insertReservationStatement.execute();
      rid += 1;
      return "Booked flight(s), reservation ID: " + (rid-1) + "\n";
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
    return "Booking failed\n";
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n"
   *         If the reservation is not found / not under the logged in user's
   *         name, then return "Cannot find unpaid reservation [reservationId]
   *         under user: [username]\n" If the user does not have enough money in
   *         their account, then return "User has only [balance] in account but
   *         itinerary costs [cost]\n" For all other errors, return "Failed to pay
   *         for reservation [reservationId]\n"
   *
   *         If successful, return "Paid reservation: [reservationId] remaining
   *         balance: [balance]\n" where [balance] is the remaining balance in the
   *         user's account.
   */
  public String transaction_pay(int reservationId) {
    if (this.login == null) {
      return "Cannot pay, not logged in\n";
    }
    try {
      // get reservation
      reservationsStatement.clearParameters();
      reservationsStatement.setInt(1, reservationId);
      ResultSet resResult = reservationsStatement.executeQuery();
      if (!resResult.next()) {
        resResult.close();
        return "Cannot find unpaid reservation " + reservationId + " under user: " + this.login + "\n";
      }
      else {
        int paid = resResult.getInt("paid");
        if (paid == 1) {
          resResult.close();
          return "Cannot find unpaid reservation " + reservationId + " under user: " + this.login + "\n";
        }

        int ticket_price = resResult.getInt("price");
        resResult.close();
        // get user balance
        getBalancesStatement.clearParameters();
        getBalancesStatement.setString(1, this.login);
        ResultSet balanceResult = getBalancesStatement.executeQuery();
        balanceResult.next();
        int balance = balanceResult.getInt("balance");
        balanceResult.close();
        if (ticket_price > balance) {
          return "User has only " + balance + " in account but itinerary costs " + ticket_price +"\n";
        }
        // update balance and paid
        updatePaidStatement.clearParameters();
        updatePaidStatement.setInt(1, 1);
        updatePaidStatement.setInt(2, reservationId);
        updatePaidStatement.execute();

        updateBalanceStatement.clearParameters();
        updateBalanceStatement.setInt(1, balance-ticket_price);
        updateBalanceStatement.setString(2, this.login);
        updateBalanceStatement.execute();
        return "Paid reservation: " + reservationId + " remaining balance: " + (balance - ticket_price) + "\n";
      }
    } catch (SQLException e) {e.printStackTrace(); }
    return "Failed to pay for reservation " + reservationId + "\n";
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not
   *         logged in\n" If the user has no reservations, then return "No
   *         reservations found\n" For all other errors, return "Failed to
   *         retrieve reservations\n"
   *
   *         Otherwise return the reservations in the following format:
   *
   *         Reservation [reservation ID] paid: [true or false]:\n" [flight 1
   *         under the reservation] [flight 2 under the reservation] Reservation
   *         [reservation ID] paid: [true or false]:\n" [flight 1 under the
   *         reservation] [flight 2 under the reservation] ...
   *
   *         Each flight should be printed using the same format as in the
   *         {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations() {
    if (this.login == null) {
      return "Cannot view reservations, not logged in\n";
    }
    try {
      reservationsTableStatement.clearParameters();
      reservationsTableStatement.setString(1, this.login);
      ResultSet resResult = reservationsTableStatement.executeQuery();
      //ResultSet resResult = resResult2;
      //System.out.println(resResult);
      if (!resResult.isBeforeFirst()) {
        return "No reservations found\n";
      }
      else {
        StringBuffer sb = new StringBuffer();
        while (resResult.next()) {
          if (resResult.getInt("canceled") == 0) {
            int id = resResult.getInt("rid");
            int paid = resResult.getInt("paid");
            int fid1 = resResult.getInt("fid1");
            int fid2 = resResult.getInt("fid2");
            // get fid1 information
            getFlightStatement.clearParameters();
            getFlightStatement.setInt(1, fid1);
            ResultSet fid1Result = getFlightStatement.executeQuery();
            fid1Result.next();

            int dayOfMonth1 = fid1Result.getInt("day_of_month");
            String carrierId1 = fid1Result.getString("carrier_id");
            String flightNum1 = fid1Result.getString("flight_num");
            String originCity1 = fid1Result.getString("origin_city");
            String destCity1 = fid1Result.getString("dest_city");
            int time1 = fid1Result.getInt("actual_time");
            int capacity1 = fid1Result.getInt("capacity");
            int price1 = fid1Result.getInt("price");
            fid1Result.close();
            String temp;
            if (paid == 1) {
              temp = "true:";
            }
            else {
              temp = "false:";
            }
            sb.append("Reservation " + id + " paid: " + temp + "\n");
            sb.append("ID: " + fid1 + " Day: " + dayOfMonth1 + " Carrier: " + carrierId1
                    + " Number: " + flightNum1 + " Origin: " + originCity1
                    + " Dest: " + destCity1 + " Duration: " + time1
                    + " Capacity: " + capacity1 + " Price: " + price1 + "\n");
            // get fid2 information
            if (fid2 != -1) {
              getFlightStatement.clearParameters();
              getFlightStatement.setInt(1, fid2);
              ResultSet fid2Result = getFlightStatement.executeQuery();
              fid2Result.next();

              int dayOfMonth2 = fid2Result.getInt("day_of_month");
              String carrierId2 = fid2Result.getString("carrier_id");
              String flightNum2 = fid2Result.getString("flight_num");
              String originCity2 = fid2Result.getString("origin_city");
              String destCity2 = fid2Result.getString("dest_city");
              int time2 = fid2Result.getInt("actual_time");
              int capacity2 = fid2Result.getInt("capacity");
              int price2 = fid2Result.getInt("price");
              fid2Result.close();
              sb.append("ID: " + fid2 + " Day: " + dayOfMonth2 + " Carrier: " + carrierId2
                      + " Number: " + flightNum2 + " Origin: " + originCity2
                      + " Dest: " + destCity2 + " Duration: " + time2
                      + " Capacity: " + capacity2 + " Price: " + price2 + "\n");
            }
          } else {
            return "Failed to retrieve reservations\n";
          }
        }
        return sb.toString();
      }
    } catch (SQLException e) {e.printStackTrace(); }
    return "Failed to retrieve reservations\n";
  }

  /**
   * Implements the cancel operation.
   *
   * @param reservationId the reservation ID to cancel
   *
   * @return If no user has logged in, then return "Cannot cancel reservations,
   *         not logged in\n" For all other errors, return "Failed to cancel
   *         reservation [reservationId]\n"
   *
   *         If successful, return "Canceled reservation [reservationId]\n"
   *
   *         Even though a reservation has been canceled, its ID should not be
   *         reused by the system.
   */
  public String transaction_cancel(int reservationId) {
    if (this.login == null) {
      return "Cannot cancel reservations,not logged in\n";
    }
    try {
      reservationsStatement.clearParameters();
      reservationsStatement.setInt(1, reservationId);
      ResultSet resResult = reservationsStatement.executeQuery();
      if (!resResult.next()) {
        return "Failed to cancel reservation " + reservationId + "\n";
      }
      else if (resResult.getInt("canceled") == 1) {
        return "Failed to cancel reservation " + reservationId + "\n";
      }
      else {
        int price = resResult.getInt("price");
        int fid1 = resResult.getInt("fid1");
        int fid2 = resResult.getInt("fid2");
        resResult.close();

        // get current balance
        getBalancesStatement.clearParameters();
        getBalancesStatement.setString(1, this.login);
        ResultSet balanceResult = getBalancesStatement.executeQuery();
        balanceResult.next();
        int remainBalance = balanceResult.getInt("balance");
        balanceResult.close();
        // update balance
        updateBalanceStatement.clearParameters();
        updateBalanceStatement.setInt(1, price + remainBalance);
        updateBalanceStatement.setString(2, this.login);
        updateBalanceStatement.execute();

        // update reservation table
        cancelStatement.clearParameters();
        cancelStatement.setInt(1, 0);
        cancelStatement.setInt(2, 1);
        cancelStatement.setInt(3, reservationId);
        cancelStatement.execute();

        // get 1st flight current capacity
        getCapacityStatement.clearParameters();
        getCapacityStatement.setInt(1, fid1);
        ResultSet capacityResult1 = getCapacityStatement.executeQuery();
        capacityResult1.next();
        int remainCapacity1 = capacityResult1.getInt("capacity");
        capacityResult1.close();
        // update 1st flight capacity table
        addCapacityStatement.clearParameters();
        addCapacityStatement.setInt(1, remainCapacity1 + 1);
        addCapacityStatement.setInt(2, fid1);
        addCapacityStatement.execute();

        // get 2nd flight current capacity
        if (fid2 != -1) {
          getCapacityStatement.clearParameters();
          getCapacityStatement.setInt(1, fid2);
          ResultSet capacityResult2 = getCapacityStatement.executeQuery();
          capacityResult2.next();
          int remainCapacity2 = capacityResult2.getInt("capacity");
          capacityResult2.close();
          // update 2nd flight capacity
          addCapacityStatement.clearParameters();
          addCapacityStatement.setInt(1, remainCapacity2 + 1);
          addCapacityStatement.setInt(2, fid2);
          addCapacityStatement.execute();
        }
        return "Canceled reservation " + reservationId + "\n";
      }
    } catch (SQLException e) {e.printStackTrace(); }
    return "Failed to cancel reservation " + reservationId + "\n";
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    checkFlightCapacityStatement.clearParameters();
    checkFlightCapacityStatement.setInt(1, fid);
    ResultSet results = checkFlightCapacityStatement.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * A class to store flight information.
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: " + flightNum + " Origin: "
          + originCity + " Dest: " + destCity + " Duration: " + time + " Capacity: " + capacity + " Price: " + price;
    }
  }
}
