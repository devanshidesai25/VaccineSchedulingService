package scheduler;
import java.util.UUID;
import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.List;


public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    private static String lowCase = "abcdefghijklmnopqrstuvwxyz";
    private static String upCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static String digits = "12345678910";

    private static String specialChar = "!@#?";

    public static void main(String[] args) throws SQLException {
        // printing greetings text
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }

    }

    private static boolean isStrongPassword(String password) {
        boolean hasUp = false;
        boolean hasLow = false;
        boolean hasSpec = false;
        boolean hasDigit = false;

        if (password.length() < 8) {
            System.out.println("Password is too short!");
            return false;
        }

        for (int i = 0; i < password.length(); i++) {
            String curr = String.valueOf(password.charAt(i));

            if (lowCase.contains(curr)) {
                hasLow = true;
            }
            if (upCase.contains(curr)) {
                hasUp = true;
            }
            if (specialChar.contains(curr)) {
                hasSpec = true;
            }
            if (digits.contains(curr)) {
                hasDigit = true;
            }
        }

        if (hasDigit && hasLow && hasSpec && hasUp) {
            return true;
        } else {
            return false;
        }
    }
    private static void createPatient(String[] tokens) {
        if (tokens.length != 3){
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        if (!isStrongPassword(password)){
            System.out.println("Password is weak, try again!");
            System.out.println("Here is the criteria for a strong password:");
            System.out.println("At least 8 characters long");
            System.out.println("A mixture of both uppercase and lowercase letters.");
            System.out.println("A mixture of letters and numbers.");
            System.out.println("Inclusion of at least one special character, from !, @, #, ?");
            return;

        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }

        if (!isStrongPassword(password)){
            System.out.println("Password is weak, try again!");
            System.out.println("Here is the criteria for a strong password:");
            System.out.println("At least 8 characters long");
            System.out.println("A mixture of both uppercase and lowercase letters.");
            System.out.println("A mixture of letters and numbers.");
            System.out.println("Inclusion of at least one special character, from !, @, #, ?");
            return;

        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) throws SQLException {
        //TODO: PART 2
        if (currentCaregiver == null && currentPatient == null){
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length !=2 ){
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.searchCaregiverSchedule(d);
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when searching for schedule");
            e.printStackTrace();
        }




    }

    private static void reserve(String[] tokens) throws SQLException {
        // TODO: Part 2
        if (currentPatient == null && currentCaregiver == null){
            System.out.println("Please login first!");
            return;
        }

        if (currentPatient == null){
            System.out.println("Please login as a patient!");
            return;
        }

        if (tokens.length !=3){
            System.out.println("Please try again!");
            return;
        }

        String date = tokens[1];
        String vaccine = tokens[2];

        if (!areDosesAvailable(vaccine)){
            System.out.println("Not enough available doses!");
        } else if (!isCaregiverAvailable(date)){
            System.out.println("No caregiver is available!");
        } else {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            String getCaregivers = "SELECT Availabilities.Username AS Caregiver " +
                    "FROM Availabilities " +
                    "WHERE Availabilities.Time = ? ";
            try {
                PreparedStatement statement = con.prepareStatement(getCaregivers);
                statement.setString(1, date);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    String caregiverName = resultSet.getString("Caregiver");
                    Vaccine reservedVaccine = null;
                    try {
                        reservedVaccine = new Vaccine.VaccineGetter(vaccine).get();
                        reservedVaccine.decreaseAvailableDoses(1);
                    } catch (SQLException e) {
                        System.out.println("Error occurred when adding doses");
                        e.printStackTrace();
                    }
                    String deleteAvailability = "DELETE FROM Availabilities WHERE Username = ?";

                    try{
                        PreparedStatement statement2 = con.prepareStatement(deleteAvailability);
                        statement2.setString(1, caregiverName);
                        statement2.executeUpdate();
                    } catch (SQLException e){
                        throw e;
                    }

                    String addAppointment = "INSERT INTO Reservations (AppointmentID, vname, pname, cname, rtime) VALUES (?, ?, ?, ?, ?)";

                    try {
                        PreparedStatement reservationStatement = con.prepareStatement(addAppointment);
                        String ID = UUID.randomUUID().toString();
                        reservationStatement.setString(1, ID);
                        reservationStatement.setString(2, vaccine);
                        reservationStatement.setString(3, currentPatient.getUsername()); // Assuming currentPatient has a getUsername() method
                        reservationStatement.setString(4, caregiverName);
                        reservationStatement.setString(5, date);
                        reservationStatement.executeUpdate();
                        System.out.println("Appointment created!");
                        System.out.println("Appointment ID: " + ID +  ", Caregiver: " + caregiverName);
                    } catch (SQLException e) {
                        System.out.println("Error occurred when adding reservation");
                        e.printStackTrace();
                    }

                }
            } catch (SQLException e) {
                throw e;
            } finally {
                cm.closeConnection();
            }
        }

    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) throws SQLException {
        if (currentCaregiver == null && currentPatient == null){
            System.out.println("Please login first!");
        }
        if (currentPatient != null){
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            String showAppointments = "SELECT AppointmentID, vname AS Vaccine, rtime AS Date, cname AS Caregiver " +
                                      "FROM Reservations " +
                                      "WHERE pname= ?";

            try {
                PreparedStatement statement = con.prepareStatement(showAppointments);
                statement.setString(1, currentPatient.getUsername());
                ResultSet resultSet = statement.executeQuery();
                if (!resultSet.next()){
                    System.out.println("No appointments scheduled!");
                } else {
                    String ID = resultSet.getString("AppointmentID");
                    String vaccine = resultSet.getString("Vaccine");
                    String date = resultSet.getString("Date");
                    String caregiver = resultSet.getString("Caregiver");
                    System.out.println(ID + ", " + vaccine + ", " + date + ", " + caregiver);
                    while (resultSet.next()) {
                        ID = resultSet.getString("AppointmentID");
                        vaccine = resultSet.getString("Vaccine");
                        date = resultSet.getString("Date");
                        caregiver = resultSet.getString("Caregiver");
                        System.out.println(ID + ", " + vaccine + ", " + date + ", " + caregiver);
                    }
                }
            } catch (SQLException e) {
                throw e;
            } finally {
                cm.closeConnection();
            }
        }

        if (currentCaregiver != null){
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            String showAppointments = "SELECT AppointmentID, vname AS Vaccine, rtime AS Date, pname AS Patient " +
                                      "FROM Reservations " +
                                       "WHERE cname= ?";

            try {
                PreparedStatement statement = con.prepareStatement(showAppointments);
                statement.setString(1, currentCaregiver.getUsername());
                ResultSet resultSet = statement.executeQuery();
                if (!resultSet.next()){
                    System.out.println("No appointments scheduled!");
                } else {
                    String ID = resultSet.getString("AppointmentID");
                    String vaccine = resultSet.getString("Vaccine");
                    String date = resultSet.getString("Date");
                    String patient = resultSet.getString("Patient");
                    System.out.println(ID + ", " + vaccine + ", " + date + ", " + patient);
                    while (resultSet.next()) {
                        ID = resultSet.getString("AppointmentID");
                        vaccine = resultSet.getString("Vaccine");
                        date = resultSet.getString("Date");
                        patient = resultSet.getString("Patient");
                        System.out.println(ID + ", " + vaccine + ", " + date + ", " + patient);
                    }
                }
            } catch (SQLException e) {
                throw e;
            } finally {
                cm.closeConnection();
            }
        }

    }

    private static void logout(String[] tokens) {
        if (currentCaregiver==null && currentPatient ==null){
            System.out.println("No user logged in!");
        } else {
            currentCaregiver = null;
            currentPatient = null;
            System.out.println("Logged out!");
        }
    }

    private static boolean areDosesAvailable(String vaccineName) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String findDoses = "SELECT Doses FROM Vaccines WHERE Name = ?";

        try {
            PreparedStatement statement = con.prepareStatement(findDoses);
            statement.setString(1, vaccineName);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int doses = resultSet.getInt("Doses");
                if (doses != 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            cm.closeConnection();
        }
        return false;
    }

    private static boolean isCaregiverAvailable(String d) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String findNumCaregiversAvailable = "SELECT COUNT(*) FROM Availabilities WHERE Time = ?";

        try {
            PreparedStatement statement = con.prepareStatement(findNumCaregiversAvailable);
            statement.setString(1, d);

            ResultSet resultSet = statement.executeQuery();

            // Check if there is at least one caregiver available at the specified time
            if (resultSet.next()) {
                int numCaregivers = resultSet.getInt(1);
                return numCaregivers > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking caregiver availability", e);
        } finally {
            cm.closeConnection();
        }
        return false;
    }


}
