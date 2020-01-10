package edu.uw.cs;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.sql.*;

public class FlightService {

  /**
   * Execute the specified command on the database query connection
   */
  public static String execute(Query q, String command) {
    String[] tokens = tokenize(command.trim());
    String response;

    // empty input
    if (tokens.length == 0) {
      response = "Please enter a command";
    }

    // login
    else if (tokens[0].equals("login")) {
      if (tokens.length == 3) {
        String username = tokens[1];
        String password = tokens[2];
        response = q.transaction_login(username, password);
      } else {
        response = "Error: Please provide a username and password";
      }
    }

    // create
    else if (tokens[0].equals("create")) {
      if (tokens.length == 4) {
        String username = tokens[1];
        String password = tokens[2];
        int initAmount = Integer.parseInt(tokens[3]);
        response = q.transaction_createCustomer(username, password, initAmount);
      } else {
        response = "Error: Please provide a username, password, and initial amount in the account";
      }
    }

    // search
    else if (tokens[0].equals("search")) {
      if (tokens.length == 6) {
        String originCity = tokens[1];
        String destinationCity = tokens[2];
        boolean direct = tokens[3].equals("1");
        try {
          int day = Integer.valueOf(tokens[4]);
          int count = Integer.valueOf(tokens[5]);
          response = q.transaction_search(originCity, destinationCity, direct, day, count);
        } catch (NumberFormatException e) {
          response = "Failed to parse integer";
        }
      } else {
        response = "Error: Please provide all search parameters <origin_city> <destination_city> <direct> <date> <nb itineraries>";
      }
    }

    // book
    else if (tokens[0].equals("book")) {
      if (tokens.length == 2) {
        int itinerary_id = Integer.parseInt(tokens[1]);
        response = q.transaction_book(itinerary_id);
      } else {
        response = "Error: Please provide an itinerary_id";
      }
    }

    // reservations
    else if (tokens[0].equals("reservations")) {
      response = q.transaction_reservations();
    }

    // pay
    else if (tokens[0].equals("pay")) {
      if (tokens.length == 2) {
        int reservation_id = Integer.parseInt(tokens[1]);
        response = q.transaction_pay(reservation_id);
      } else {
        response = "Error: Please provide a reservation_id";
      }
    }

    // cancel
    else if (tokens[0].equals("cancel")) {
      if (tokens.length == 2) {
        int reservation_id = Integer.parseInt(tokens[1]);
        response = q.transaction_cancel(reservation_id);
      } else {
        response = "Error: Please provide a reservation_id";
      }
    }

    // quit
    else if (tokens[0].equals("quit")) {
      response = "Goodbye\n";
    }

    // unknown command
    else {
      response = "Error: unrecognized command '" + tokens[0] + "'";
    }

    return response;
  }

  /**
   * Establishes an application-to-database connection and runs the Flights
   * application REPL
   * 
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, SQLException {
    /* prepare the database connection stuff */
    Query q = new Query();
    q.openConnection();
    q.prepareStatements();
    menu(q);
    q.closeConnection();
  }

  /**
   * REPL (Read-Execute-Print-Loop) for Flights application for the specified
   * application-to-database connection
   * 
   * @param q
   * @throws IOException
   */
  private static void menu(Query q) throws IOException {
    while (true) {
      // print the command options
      System.out.println();
      System.out.println(" *** Please enter one of the following commands *** ");
      System.out.println("> create <username> <password> <initial amount>");
      System.out.println("> login <username> <password>");
      System.out.println("> search <origin city> <destination city> <direct> <day of the month> <num itineraries>");
      System.out.println("> book <itinerary id>");
      System.out.println("> pay <reservation id>");
      System.out.println("> reservations");
      System.out.println("> cancel <reservation id>");
      System.out.println("> quit");

      // read an input command from the REPL
      BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
      System.out.print("> ");
      String command = r.readLine();

      // execute the given input command
      String response = execute(q, command);
      System.out.print(response);
      if (response.equals("Goodbye\n")) {
        break;
      }
    }
  }

  /**
   * Tokenize a string into a string array
   */
  private static String[] tokenize(String command) {
    String regex = "\"([^\"]*)\"|(\\S+)";
    Matcher m = Pattern.compile(regex).matcher(command);
    List<String> tokens = new ArrayList<>();
    while (m.find()) {
      if (m.group(1) != null)
        tokens.add(m.group(1));
      else
        tokens.add(m.group(2));
    }
    return tokens.toArray(new String[0]);
  }
}
