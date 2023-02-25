package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Vaccines {

    private final String vaccineName;
    private int availableDoses;

    private Vaccines(VaccineBuilder builder) {
        this.vaccineName = builder.vaccineName;
        this.availableDoses = builder.availableDoses;
    }

    private Vaccines(VaccineGetter getter) {
        this.vaccineName = getter.vaccineName;
        this.availableDoses = getter.availableDoses;
    }

    public Vaccines() {
        this.vaccineName = null;
    }

    // Getters
    public String getVaccineName() {
        return vaccineName;
    }

    public int getAvailableDoses() {
        return availableDoses;
    }

    /**
     * save a new row to vaccine
     * @throws SQLException sql execution exception
     */
    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addDoses = "INSERT INTO Vaccines VALUES (?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addDoses);
            statement.setString(1, this.vaccineName);
            statement.setInt(2, this.availableDoses);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when creating a new Vaccine!");
        } finally {
            cm.closeConnection();
        }
    }

    /**
     * get all vaccines names and their numbers
     * @return a list containing vaccines information
     * @throws SQLException sql execution exception
     */
    public List<Vaccines> getAllVaccines() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String findAll = "SELECT * FROM Vaccines";
        List<Vaccines> vaccines = new ArrayList<Vaccines>();
        try {
            PreparedStatement statement = con.prepareStatement(findAll);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String name = resultSet.getString("Name");
                int doses = resultSet.getInt("Doses");
                vaccines.add(new Vaccines(new VaccineBuilder(name, doses)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when getting vaccines information!");
        } finally {
            cm.closeConnection();
        }
        return vaccines;
    }

    /**
     * increase stock for a given vaccine
     * @param num positive number
     * @throws SQLException sql execution exception
     * @throws IllegalArgumentException if input number is less than 0
     */
    public void increaseAvailableDoses(int num) throws SQLException {
        if (num <= 0) {
            throw new IllegalArgumentException();
        }
        this.availableDoses += num;

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String removeAvailability  = "UPDATE Vaccines SET Doses = ? WHERE name = ?;";
        try {
            PreparedStatement statement = con.prepareStatement(removeAvailability);
            statement.setInt(1, this.availableDoses);
            statement.setString(2, this.vaccineName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when increasing vaccine doses!");
        } finally {
            cm.closeConnection();
        }
    }

    /**
     * decrease stock for a given vaccine
     * @param num number of doses we want to decrease
     * @throws IllegalArgumentException if not enough doses left to decrease
     * @throws SQLException sql execution exception
     */
    public void decreaseAvailableDoses(int num) throws SQLException {
        if (this.availableDoses - num < 0) {
            throw new IllegalArgumentException("Not enough available doses!");
        }
        this.availableDoses -= num;
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String removeAvailability  = "UPDATE Vaccines SET Doses = ? WHERE name = ?;";
        try {
            PreparedStatement statement = con.prepareStatement(removeAvailability);
            statement.setInt(1, this.availableDoses);
            statement.setString(2, this.vaccineName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when decreasing vaccine doses!");
        } finally {
            cm.closeConnection();
        }
    }

    @Override
    public String toString() {
        return "Vaccine{" +
                "vaccineName='" + vaccineName + '\'' +
                ", availableDoses=" + availableDoses +
                '}';
    }

    /**
     * Vaccine Builder to add new vaccines
     */
    public static class VaccineBuilder {
        private final String vaccineName;
        private int availableDoses;

        public VaccineBuilder(String vaccineName, int availableDoses) {
            this.vaccineName = vaccineName;
            this.availableDoses = availableDoses;
        }

        public Vaccines build() throws SQLException {
            return new Vaccines(this);
        }
    }

    /**
     * Vaccine getters to get information for a vaccine
     */
    public static class VaccineGetter {
        private final String vaccineName;
        private int availableDoses;

        public VaccineGetter(String vaccineName) {
            this.vaccineName = vaccineName;
        }

        /**
         * get information for a given vaccine name
         * return null if can't find
         * @return a Vaccine with name and stock
         * @throws SQLException sql execution exception
         */
        public Vaccines get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getVaccine = "SELECT Name, Doses FROM Vaccines WHERE Name = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getVaccine);
                statement.setString(1, this.vaccineName);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    this.availableDoses = resultSet.getInt("Doses");
                    return new Vaccines(this);
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }
}

