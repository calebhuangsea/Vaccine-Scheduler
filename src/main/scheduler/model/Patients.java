package scheduler.model;

import scheduler.db.ConnectionManager;
import scheduler.util.Util;

import java.sql.*;
import java.util.Arrays;

public class Patients {

    private final String username;
    private final byte[] salt;
    private final byte[] hash;

    /**
     * Construct Patient using PatientBuilder
     * @param builder a PatientBuilder object Patient
     */
    public Patients(PatientBuilder builder) {
        this.username = builder.username;
        this.salt = builder.salt;
        this.hash = builder.hash;
    }

    /**
     * Construct Patient using PatientGetter
     * @param getter a PatientGetter object Patient
     */
    public Patients(PatientGetter getter) {
        this.username = getter.username;
        this.salt = getter.salt;
        this.hash = getter.hash;
    }

    //getters
    public String getUsername() {
        return username;
    }

    /**
     * save information of this current patient into patient table
     * @throws SQLException sql execution exception
     */
    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addPatient = "INSERT INTO Patients VALUES (? , ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addPatient);
            statement.setString(1, this.username);
            statement.setBytes(2, this.salt);
            statement.setBytes(3, this.hash);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when creating new Patient! Failed!");
        } finally {
            cm.closeConnection();
        }
    }

    public static class PatientBuilder {
        private final String username;
        private final byte[] salt;
        private final byte[] hash;

        /**
         * Construct a PatientBuilder using username and password(salt, hash)
         * @param username username
         * @param salt salt generated randomly
         * @param hash hash generated randomly by password and salt
         */
        public PatientBuilder(String username, byte[] salt, byte[] hash) {
            this.username = username;
            this.salt = salt;
            this.hash = hash;
        }

        /**
         * get current new patient
         * @return return the patient with PatientBuilder
         */
        public Patients build() {
            return new Patients(this);
        }
    }

    /**
     * The PatientGetter is used when trying to login
     */
    public static class PatientGetter {
        private final String username;
        private final String password;
        private byte[] salt;
        private byte[] hash;

        //construct with only username and password
        public PatientGetter(String username, String password) {
            this.username = username;
            this.password = password;
        }


        /**
         * Verify whether given username and password match one of user in our Patient table
         * @return return the patient object that match information in database
         * @throws SQLException sql execution exception
         */
        public Patients get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getPatient = "SELECT Salt, Hash FROM Patients WHERE Username = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getPatient);
                statement.setString(1, this.username);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    byte[] salt = resultSet.getBytes("Salt");
                    // we need to call Util.trim() to get rid of the paddings,
                    // try to remove the use of Util.trim() and you'll see :)
                    byte[] hash = Util.trim(resultSet.getBytes("Hash"));
                    // check if the password matches
                    byte[] calculatedHash = Util.generateHash(password, salt);
                    if (!Arrays.equals(hash, calculatedHash)) {
                        //return nothing if hash mismatch
                        return null;
                    } else {
                        //return a new patient, which is logged-in patient
                        this.salt = salt;
                        this.hash = hash;
                        return new Patients(this);
                    }
                }
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                throw new SQLException("Error occurred when getting patient's information!");
            } finally {
                cm.closeConnection();
            }
        }
    }
}
