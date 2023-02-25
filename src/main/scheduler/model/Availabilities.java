package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.sql.Date;

/**
 * @author Caleb_
 * @title Availabilities
 * @description
 * @date 2021/11/23 10:44
 */
public class Availabilities {

    private Date Time;
    private String Username;

    public Availabilities(Date time, String username) {
        Time = time;
        Username = username;
    }

    public Availabilities() {
    }

    public Date getTime() {
        return Time;
    }

    public String getUsername() {
        return Username;
    }

    /**
     * get all available caregiver in a given date
     * @param date date we wanna get information from
     * @return a list containing available caregivers
     * @throws SQLException sql execution exception
     */
    public List<Availabilities> getAvailabilities(Date date)  throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        List<Availabilities> availabilities = new ArrayList<Availabilities>();
        String findAvailabilites = "SELECT * FROM Availabilities WHERE Time = ?";
        try {
            PreparedStatement statement = con.prepareStatement(findAvailabilites);
            statement.setDate(1, date);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String username = resultSet.getString("Username");
                Date time = resultSet.getDate("Time");
                availabilities.add(new Availabilities(time, username));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when getting caregiver's availabilities");
        } finally {
            cm.closeConnection();
        }
        return availabilities;
    }

    /**
     * remove a caregiver from the availability table on one day
     * @param name caregiver name we wanna remove
     * @param date date we wanna remove on
     * @throws SQLException sql execution exception
     */
    public void removeCaregiver(String name, Date date) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String removeCaregiver = "DELETE FROM Availabilities WHERE Username = ? AND TIME = ?";
        try {
            PreparedStatement statement = con.prepareStatement(removeCaregiver);
            statement.setString(1, name);
            statement.setDate(2, date);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when removing caregiver from Availabilities!");
        } finally {
            cm.closeConnection();
        }
    }

    /**
     * upload a caregiver and date to our availabilities table
     * @param username caregiver's name
     * @param date date
     * @throws SQLException sql execution exception
     */
    public void upLoadAvailability(String username, Date date) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAvailability = "INSERT INTO Availabilities VALUES (? , ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAvailability);
            statement.setDate(1, date);
            statement.setString(2, username);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when uploading Availability!");
        } finally {
            cm.closeConnection();
        }
    }
}
