package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.*;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregivers currentCaregivers = null;
    private static Patients currentPatients = null;
    private static final String TOKENMISMATCH = "Please check your operation information and try again!";
    private static final String VALIDDATE = "Please enter a valid date!";
    private static final String PASSWORDINVALID = "Your password is not strong enough!!!Please read requirement and Try again!!\n" +
                                                    "Your password should contain the followings:\n" +
                                                    "1.At least 8 characters\n" +
                                                    "2.Mixture of Uppercase and Lowercase letters and numbers\n" +
                                                    "3.Include at lease one of these special characters(!, @, #, ?)";

    public static void main(String[] args) throws IOException {
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        //keep running until user enter quit
        while (true) {
            showCommands();
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.err.println("System reading error. Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.err.println("Please don't input empty spaces only!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient") || operation.equals("1")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver") || operation.equals("2")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient") || operation.equals("3")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver") || operation.equals("4")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule") || operation.equals("5")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve") || operation.equals("6")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability") || operation.equals("7")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel") || operation.equals("8")) {
                cancel(tokens);
            } else if (operation.equals("add_doses") || operation.equals("9")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments") || operation.equals("10")) {
                showAppointments(tokens);
            } else if (operation.equals("logout") || operation.equals("11")) {
                logout(tokens);
            } else if (operation.equals("quit") || operation.equals("12")) {
                System.out.println("Good Bye!");
                r.close();
                return;
            } else {
                System.err.println("Invalid operation name! \nPlease read through the above opeartions list and type again!");
            }
        }
    }

    /**
     * create_patient <username> <password>
     * check 1: If a user has logged in
     * check 2: If input tokens length is exactly 3
     * check 3: If username is taken
     * check 4: If password is strong enough
     * add qualified patient to our Patient table
     * login as new account
     * @param tokens command, username and password
     * print error message if create patient failed
     */
    private static void createPatient(String[] tokens) {
        //check 1 and check 2
        if (!basicCreateCheck(tokens)) {
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 3: if username is taken
        if (usernameExists(username, "Patient")) {
            System.err.println("Username taken, try again!");
            return;
        }
        // check 4: if password is strong enough
        if (!checkStrongPassword(password)) {
            return;
        }
        //generate salt & hash using password
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            //create and save our new patient
            currentPatients = new Patients.PatientBuilder(username, salt, hash).build();
            currentPatients.saveToDB();
            System.out.println(" *** Congratulation, " + username + "! Account created successfully *** ");
            System.out.println("Patient logged in as:" + username);
        } catch (SQLException e) {
            //print error message if sql execution error
            System.err.println(e.getMessage());
        }
    }

    /**
     * create_caregiver <username> <password>
     * check 1: if a user has logged in
     * check 2: if input tokens length is exactly 3
     * check 3: if username is taken
     * check 4: if password is strong enough
     * add qualified caregiver to our Caregiver table
     * login as new account
     * @param tokens command, username and password
     * print error message if create caregiver failed
     */
    private static void createCaregiver(String[] tokens) {
        //check 1 and check 2
        if (!basicCreateCheck(tokens)) {
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 3: if username is taken
        if (usernameExists(username, "Caregiver")) {
            System.err.println("Username taken, try again!");
            return;
        }
        // check 4: if password is strong enough
        if (!checkStrongPassword(password)) {
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            //create and save a new caregiver
            currentCaregivers = new Caregivers.CaregiverBuilder(username, salt, hash).build();
            currentCaregivers.saveToDB();
            System.out.println(" *** Account created successfully *** ");
            System.out.println("Caregiver logged in as:" + username);
        } catch (SQLException e) {
            //print error message if sql execution error
            System.err.println(e.getMessage());
        }
    }

    /**
     * login_patient <username> <password>
     * check 1: If a patient or caregiver has logged-in
     * check 2: If input tokens length is exactly 3
     * check 3: If patient username and password can match a patient account
     * print error message if password and account can't match
     * login as a patient if given information match an account in our Patient table
     * @param tokens command, username and password
     * print error message if get method failed
     */
    private static void loginPatient(String[] tokens) {
        //check 1 and check 2
        if (!basicLoginCheck(tokens)) {
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        Patients patients = null;
        try {
            //get a patient object using given password
            patients = new Patients.PatientGetter(username, password).get();
        } catch (SQLException e) {
            //print error message if getting patient failed
            System.err.println(e.getMessage());
        }
        //check3: if patient username and password can match a patient account
        if (patients == null) {//get() return nothing, username and password doesn't match
            System.err.println("Cannot find user, please try again! Make sure you enter correct information!");
        } else {//return a patients object and login
            System.out.println("Patient logged in as:" + username);
            currentPatients = patients;
        }
    }

    /**
     * login_caregiver <username> <password>
     * check 1: If a patient or caregiver has logged-in
     * check 2: If input tokens length is exactly 3
     * check 3: If patient username and password can match a caregiver account
     * print error message if password and account can't match
     * login as a caregiver if given information match an account in our Caregiver table
     * @param tokens command, username and password
     * print error message if get method failed
     */
    private static void loginCaregiver(String[] tokens) {
        //check 1 and check 2
        if (!basicLoginCheck(tokens)) {
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        Caregivers caregivers = null;
        try {
            //get a caregiver object using given username and password
            caregivers = new Caregivers.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            //print error message if get method failed
            System.err.println(e.getMessage());
        }
        //check 3: If patient username and password can match a caregiver account
        if (caregivers == null) {//return nothing, username and password doesn't match
            System.err.println("Cannot find user, Please try again! Make sure you enter correct information!");
        } else {//return a caregivers object and login
            System.out.println("Caregiver logged in as: " + username);
            currentCaregivers = caregivers;
        }
    }

    /**
     * search_caregiver_schedule <date>
     * check 1: if a patient or caregiver has logged-in
     * check 2: if input tokens length is exactly 2
     * check 3: if input date is valid
     * Output the username for the caregivers that are available for the date,
     * along with the number of available doses left for each vaccine.
     * @param tokens command, date
     * IllegalArgumentException if input date is invalid
     * SQLException if Error when searching schedules
     */
    private static void searchCaregiverSchedule(String[] tokens) {
        //check 1: if a patient or caregiver has logged-in
        if (currentPatients == null && currentCaregivers == null) {
            System.err.println("Please login your account first!");
            return;
        }
        //check 2: if input tokens length is exactly 2
        if (tokens.length != 2) {
            System.err.println(TOKENMISMATCH);
            return;
        }

        try {
            //check 3: if input date is valid with try catch
            Date date = Date.valueOf(tokens[1]);
            //get all available caregivers on this date
            List<Availabilities> availabilities = new Availabilities().getAvailabilities(date);
            if (availabilities.size() == 0) {//if no result
                System.err.println("Oops! No Caregiver is available on this day!");
            } else {//if yes result
                System.out.println("These caregivers are available on this day:");
                //show caregivers and date information
                for (Availabilities availability : availabilities) {
                    System.out.println("Caregiver name:" + availability.getUsername() + ", on Date:" + availability.getTime());
                }
                //get all vaccines information (name, numbers)
                List<Vaccines> allVaccines = new Vaccines().getAllVaccines();
                if (allVaccines.size() == 0) {// if no result
                    System.err.println("Oops! No vaccines are on the market now!");
                } else {// if yes result
                    System.out.println();
                    System.out.println("These are available doses of vaccines!");
                    for (Vaccines allVaccine : allVaccines) {
                        System.out.println(allVaccine);
                    }
                }
            }
        } catch (SQLException e) {
            //print error message if sql execution error
            System.err.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            //print error message if date is invalid
            System.err.println(VALIDDATE);
        }

    }

    /**
     * reserve <date> <vaccine>
     * check 1: If a patient has logged-in
     * check 2: If input tokens length is exactly 3
     * check 3: If a patient already has an appointment
     * check 4: If any caregivers are available that day
     * check 5: If input vaccine exist
     * check 6: If input vaccine stock is enough
     * randomly assigned a caregiver for the reservation on that date.
     * add a new reserve to the Appointment table
     * remove caregiver on Availabilities table that date
     * minus doses for selected vaccine for one
     * @param tokens command, date and vaccine
     * IllegalArgument Exception if input date is invalid
     * SQLException if error when reserving
     */
    private static void reserve(String[] tokens){
        //check 1: if a patient has logged-in
        if (currentPatients == null) {
            System.err.println("please login as a patient first!");
            return;
        }
        //check 2: if input tokens length is exactly 3
        if (tokens.length != 3) {
            System.err.println(TOKENMISMATCH);
            return;
        }
        String date = tokens[1];
        String vaccine = tokens[2];

        try {
            Appointments appointments = new Appointments();
            // check 3: If a patient already has an appointment
            List<Appointments> patient = appointments.showAppointments("Patient", currentPatients.getUsername());
            if (patient.size() != 0) {//get appointment for a patient
                System.err.println("One patient can only have one appointment at most!");
                return;
            }
            // check if given date is valid with try catch
            Date d = Date.valueOf(date);
            //get all available caregivers and vaccines information
            List<Availabilities> availabilities = new Availabilities().getAvailabilities(d);
            Vaccines vaccines1 = new Vaccines.VaccineGetter(vaccine).get();
            // check 4: If any caregivers are available that day
            if (availabilities.size() == 0) {
                System.err.println("Sorry, no caregiver is available today, please check the schedule and reserve again!");
                return;
            } else if (vaccines1 == null) {//check 5: If input vaccine exist
                System.err.println("Sorry, the vaccine you choose is not applicable, please choose other vaccines!");
                return;
            } else if (vaccines1.getAvailableDoses() == 0) {//check 6: If input vaccine stock is enough
                System.err.println("Sorry, the vaccine you choose is running out of storage, please choose other vaccines!");
            } else {
                //randomly assign a caregiver
                Random random = new Random();
                int index = random.nextInt(availabilities.size());
                String caregiver = availabilities.get(index).getUsername();
                //remove one dose for selected vaccine
                vaccines1.decreaseAvailableDoses(1);
                //remove caregiver on that day on availabilities
                new Availabilities().removeCaregiver(caregiver, d);
                //add an appointment to the table and return its id
                int id = new Appointments().addAppointment(currentPatients.getUsername(), caregiver, d, vaccine);
                System.out.println("Your appointment id is:" + id + ", Your assigned caregiver is:" +
                        caregiver + ", Your selected vaccine is:" + tokens[2]);
            }
        } catch (SQLException e) {
            //print error message if sql execution error
            System.err.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            //print error message if input an invalid date
            System.err.println(VALIDDATE);
        }

    }

    /**
     * uploadAvailability <date>
     * check 1: If a caregiver has logged-in
     * check 2: If input tokens length is exactly 2
     * check 3: If caregiver has uploaded on this date before(both reserve and appointment)
     * @param tokens command date
     * IllegalArgumentException if input invalid date
     * SQLException if error occurred when uploading availability
     */
    private static void uploadAvailability(String[] tokens) {
        // check 1: if a caregiver has logged-in
        if (currentCaregivers == null) {
            System.err.println("Please login as a caregiver first!");
            return;
        }
        // check 2: if input tokens length is exactly 2
        if (tokens.length != 2) {
            System.err.println(TOKENMISMATCH);
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            //check 3: if caregiver has uploaded on this date before(appointment and availabilities)
            List<Availabilities> availabilities = new Availabilities().getAvailabilities(d);
            //get appointment information for this caregiver
            List<Appointments> appointments = new Appointments().showAppointments("Caregiver", currentCaregivers.getUsername());
            //check if current caregiver has uploaded to availabilities table
            for (Availabilities availability : availabilities) {
                if (availability.getUsername().equals(currentCaregivers.getUsername())) {
                    System.err.println("You have uploaded yourself on this date before!");
                    return;
                }
            }
            //check if current caregiver has an appointment on this day
            for (Appointments appointment : appointments) {
                if (appointment.getDate().equals(d)) {
                    System.err.println("You have uploaded yourself on this date before!");
                    return;
                }
            }
            //upload to the table if it pass all checks and print message
            currentCaregivers.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            //print error message if input invalid date
            System.err.println(VALIDDATE);
        } catch (SQLException e) {
            //print error message if sql execution error
            System.err.println(e.getMessage());
        }
    }

    /**
     * cancel <appointment_id>
     * check 1: If a caregiver or patient has logged-in
     * check 2: If input tokens length is exactly 2
     * check 3: If the given appointment_id is a number(regex)
     * check 4: If the given appointment_id is in our appointment table
     * check 5: If current user is qualified to cancel this appointment
     * cancel appointment with given appointment id
     * delete this appointment from Appointment table
     * add caregiver back to availability table
     * add vaccine number back to table
     * @param tokens command and appointment_id
     * SQLException if error occurred when canceling appointment
     */
    private static void cancel(String[] tokens) {
        //check 1: if a patient or caregiver has logged in
        if (currentCaregivers == null && currentPatients == null) {
            System.err.println("Please login your account first!");
            return;
        }
        //check 2: if input tokens length is exactly 2
        if (tokens.length != 2) {
            System.err.println(TOKENMISMATCH);
            return;
        }
        //check 4: if the given appointment_id is in our appointment table
        try {
            Appointments app = new Appointments();
            //check 3: if the given appointment_id is a number with try catch
            int id = Integer.parseInt(tokens[1]);
            List<Integer> integers = app.showAllID();
            if (!integers.contains(id)) {
                System.err.println("Please make sure you enter a valid id!");
                return;
            }

            String type;
            String username;
            //get if a patient or caregiver is operating cancel and get its information
            if (currentPatients != null) {
                type = "Patient";
                username = currentPatients.getUsername();
            } else {
                type = "Caregiver";
                username = currentCaregivers.getUsername();
            }
            //check 5: If current user is qualified to cancel this appointment
            List<Appointments> appointments = app.showAppointments(type, username);
            //get all id for this user and see if input id is contained in his/her id list
            List<Integer> ids = new ArrayList<Integer>();
            for (Appointments appointment : appointments) {
                ids.add(appointment.getID());
            }
            if (!ids.contains(id)) {
                System.err.println("Please make sure the appointment id belongs to you!");
                return;
            } else {
                //get information before delete the row because we need to upload back to availabilities table
                Appointments appointments1 = app.getInfo(id);
                //cancel the appointment
                app.cancelAppointment(id);
                // upload back to availabilities table
                new Availabilities().upLoadAvailability(appointments1.getCareGiver(), appointments1.getDate());
                Vaccines vaccines = new Vaccines.VaccineGetter(appointments1.getVaccine()).get();
                vaccines.increaseAvailableDoses(1);
                System.out.println("You have successfully delete this appointment!");
            }
        } catch (SQLException e) {
            //print error message if sql execution failed
            System.err.println(e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Please make sure you enter an numeric id or a valid date!");
        }
    }

    /**
     * add_doses <vaccine> <number>
     * check 1: If a caregiver has logged-in
     * check 2: If input tokens length is exactly 3
     * check 3: If the number is a number//not character(also not negative)
     * add doses for given vaccine name, create it if it doesn't exist
     * @param tokens command, vaccine name and number
     */
    private static void addDoses(String[] tokens) {
        // check 1: if a caregiver has logged-in
        if (currentCaregivers == null) {
            System.err.println("Please login as a caregiver first!");
            return;
        }
        // check 2: if input tokens length is exactly 3
        if (tokens.length != 3) {
            System.err.println(TOKENMISMATCH);
            return;
        }
        try {
            // check 3: if input number is a number with try catch
            int doses = Integer.parseInt(tokens[2]);
            String vaccineName = tokens[1];
            //get the vaccine object
            Vaccines vaccines = new Vaccines.VaccineGetter(vaccineName).get();

            if (vaccines == null) {//create new vaccine if we can't find the vaccine
                vaccines = new Vaccines.VaccineBuilder(vaccineName, 0).build();
                vaccines.saveToDB();
            }
            vaccines.increaseAvailableDoses(doses);// increase stock numbers
            System.out.println("Doses updated!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Please make sure you input a valid number!");
        }
    }

    /**
     * show_appointments
     * check 1: if patient or caregiver has logged-in
     * show all appointments for current user, doesn't matter what user type in after command
     * doesn't show patient for patient, doesn't show caregiver for caregiver
     * @param tokens command
     */
    private static void showAppointments(String[] tokens) {
        //check 1: if a patient or caregiver has logged-in
        if (currentPatients == null && currentCaregivers == null) {
            System.err.println("Please login your account before checking your appointments!");
            return;
        }
        String type;
        String username;
        try {
            //check if a caregiver or patient is operating show appointment and get information
            if (currentCaregivers != null) {
                type = "Caregiver";
                username = currentCaregivers.getUsername();
            } else {
                type = "Patient";
                username = currentPatients.getUsername();
            }
            //get appointment information for current user
            List<Appointments> appointments = new Appointments().showAppointments(type, username);
            if (appointments.size() == 0) {
                System.err.println("You don't have any upcoming appointments!");
            } else {
                System.out.println("You have following appointments:");
            }
            //check current user type and print different messages
            if (currentCaregivers != null) {
                for (Appointments appointment : appointments) {
                    System.out.println("Appointment ID:" + appointment.getID() + "-----Date:" + appointment.getDate() + "-----Vaccine Name:" + appointment.getVaccine()
                            + "-----Patient Name:" + appointment.getPatient());
                }
            } else {
                for (Appointments appointment : appointments) {
                    System.out.println("Appointment ID:" + appointment.getID() + "-----Date:" + appointment.getDate() + "-----Vaccine Name:" + appointment.getVaccine()
                            + "-----Caregiver Name:" + appointment.getCareGiver());
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * logout
     * check 1: If a patient or caregiver has logged-in
     * logout current user account, doesn't matter what user type in after the command
     * @param tokens command
     */
    private static void logout(String[] tokens) {
        // check 1: if a patient or caregiver has logged-in
        if (currentPatients == null && currentCaregivers == null) {
            System.err.println("Please login your account before logout!");
            return;
        }
        if (currentCaregivers != null) {
            currentCaregivers = null;
        } else {
            currentPatients = null;
        }
        System.out.println("You have successfully logged out!");
    }


    /**
     * check whether the given username is unique in our Caregiver/ Patient table
     * @param name the username we need to check
     * @param type determine whether we are checking in caregiver or patient table
     * @return boolean for username unique or not
     */
    private static boolean usernameExists(String name, String type) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername;
        if (type.equals("Patient")) {
            selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        } else {
            selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        }
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.err.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    /**
     * @param password the password we need to check
     * @return return whether the password satifies strong password rules
     * At least 8 characters.
     * A mixture of both uppercase and lowercase letters.
     * A mixture of letters and numbers.
     * Inclusion of at least one special character, from “!”, “@”, “#”, “?”.
     */
    private static boolean checkStrongPassword(String password) {
        String regex = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@,#,!,?]).{8,}$";
        if (!password.matches(regex)) {
            System.err.println(PASSWORDINVALID);
            return false;
        }
        return true;
    }

    /**
     * show greeting messages
     */
    private static void showCommands() {
        // printing greetings text
        System.out.println();
        System.out.println("*** Please enter one of the following commands or command number ***");
        System.out.println("> hint: enter yyyy-mm-dd for date!");
        System.out.println("> hint: our username is case ignored, try that out in create and login function!");
        System.out.println("> (1)create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> (2)create_caregiver <username> <password>");
        System.out.println("> (3)login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> (4)login_caregiver <username> <password>");
        System.out.println("> (5)search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> (6)reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> (7)upload_availability <date>");
        System.out.println("> (8)cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> (9)add_doses <vaccine> <number>");
        System.out.println("> (10)show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> (11)logout");  // TODO: implement logout (Part 2)
        System.out.println("> (12)quit");
        System.out.println();
    }

    private static boolean basicLoginCheck(String[] tokens) {
        //check 1: if a patient or caregiver has logged-in
        if (currentCaregivers != null || currentPatients != null) {
            System.err.println("Already logged-in, please logout current account and try again!");
            return false;
        }
        // check 2: if input tokens length is exactly 3
        if (tokens.length != 3) {
            System.err.println(TOKENMISMATCH);
            return false;
        }
        return true;
    }

    private static boolean basicCreateCheck(String[] tokens) {
        // check 1: if a user has logged in
        if (currentCaregivers != null || currentPatients != null) {
            System.err.println("Please logout current account before creating a new account!");
            return false;
        }
        // check 2: if input tokens length is exactly 3
        if (tokens.length != 3) {
            System.err.println(TOKENMISMATCH);
            return false;
        }
        return true;
    }
}
