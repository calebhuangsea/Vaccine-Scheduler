package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.sql.Date;

/**
 * @author Caleb_
 * @title Appointments class allows users to add/cancel/show appointments
 * @description Appointment
 * @date 2021/11/23 10:11
 */
public class Appointments {

    private int id;//primary key and auto increment by 1
    private String patient;//patient name in appointment
    private String careGiver;//caregiver name in appointment
    private Date date;//date in appointment
    private String vaccine;//vaccine name in appointment

    /**
     * Construct an Appointment with patient caregiver usernames and date and vaccine name
     * use in show-appointments function
     * @param id
     * @param patient
     * @param careGiver
     * @param date
     * @param vaccine
     */
    public Appointments(int id, String patient, String careGiver, Date date, String vaccine) {
        this.id = id;
        this.patient = patient;
        this.careGiver = careGiver;
        this.date = date;
        this.vaccine = vaccine;
    }

    /**
     * non-parameter constructor for calling method
     */
    public Appointments() {}

    public String getPatient() {
        return patient;
    }

    public String getCareGiver() {
        return careGiver;
    }

    public Date getDate() {
        return date;
    }

    public String getVaccine() {
        return vaccine;
    }

    public int getID() {return id;}

    /**
     * get max id in appointments table, use to show user what his/her new appointment id is
     * @return max appointment id, which is the newest appointment id
     * @throws SQLException sql execute exception
     */
    public int getMaxID() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getMaxID = "SELECT MAX(id) FROM Appointments";
        try {
            PreparedStatement statement = con.prepareStatement(getMaxID);
            ResultSet resultSet = statement.executeQuery();

            int id = 0;
            if (resultSet.next()) {
                id = resultSet.getInt(1);
            }
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when getting Max ID!");
        } finally {
            cm.closeConnection();
        }
    }

    /**
     * Add a new appointment to the Appointment table
     * @param patient
     * @param careGiver
     * @param date
     * @param vaccine
     * @throws SQLException sql execution exception
     * @return return the new appointment id(max id)
     */
    public int addAppointment(String patient, String careGiver, Date date, String vaccine) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAppointment = "INSERT INTO Appointments VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment);
            statement.setDate(1, date);
            statement.setString(2, vaccine);
            statement.setString(3, patient);
            statement.setString(4, careGiver);
            statement.executeUpdate();
            return getMaxID();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when adding new appointment!");
        } finally {
            cm.closeConnection();
        }
    }

    /**
     * show all appointments for a given user
     * @param type determine if current user is a patient or caregiver
     * @param username username we want to check
     * @return return a list containing appointments objects
     * @throws SQLException sql execute exception
     */
    public List<Appointments> showAppointments(String type, String username) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        List<Appointments> appointments = new ArrayList<>();

        String findAppointment;
        if (type.equals("Patient")) {
            findAppointment = "SELECT * FROM Appointments WHERE PatientName = ?";
        } else {
            findAppointment = "SELECT * FROM Appointments WHERE CaregiverName = ?";
        }
        try {
            PreparedStatement statement = con.prepareStatement(findAppointment);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String caregiver = resultSet.getString("CaregiverName");
                String patient = resultSet.getString("PatientName");
                String vaccine = resultSet.getString("Vaccine");
                java.sql.Date date = resultSet.getDate("Date");
                appointments.add(new Appointments(id, patient, caregiver, date, vaccine));
            }
            return appointments;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when showing appointment for user!");
        } finally {
            cm.closeConnection();
        }
    }

    /**
     * get all information for given id appointment
     * @param id id of appointment we want to search
     * @return an appointments object if found, return null if not found
     * @throws SQLException sql execute exception
     */
    public Appointments getInfo(int id) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String select = "SELECT * FROM Appointments WHERE id = ?";
        try {
            PreparedStatement selectStatement = con.prepareStatement(select);
            selectStatement.setInt(1, id);
            ResultSet resultSet = selectStatement.executeQuery();

            Appointments app = null;

            while (resultSet.next()) {
                String caregiver = resultSet.getString("CaregiverName");
                String patient = resultSet.getString("PatientName");
                String vaccine = resultSet.getString("Vaccine");
                Date date = resultSet.getDate("Date");
                app = new Appointments(id, patient, caregiver, date, vaccine);
            }
            return app;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when getting appointment information!");
        } finally {
            cm.closeConnection();
        }
    }

    /**
     * cancel an appointment by id
     * @param id id of appointment we want to cancel
     * @throws SQLException sql execute exception
     */
    public void cancelAppointment(int id) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String cancel = "DELETE FROM Appointments WHERE id = ?";
        try {
            PreparedStatement cancelStatement = con.prepareStatement(cancel);
            cancelStatement.setInt(1, id);
            cancelStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when canceling appointment!");
        } finally {
            cm.closeConnection();
        }
    }

    /**
     * show all id in our appointments table, use to check if user's input id is valid
     * @return list containing ids
     * @throws SQLException sql execute exception
     */
    public List<Integer> showAllID() throws SQLException {//get a connection with database
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        List<Integer> list = new ArrayList<Integer>();
        try {
            PreparedStatement statement = con.prepareStatement("SELECT id FROM Appointments");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                list.add(resultSet.getInt("id"));
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when searching for all ID!");
        } finally {
            cm.closeConnection();
        }
    }

}
