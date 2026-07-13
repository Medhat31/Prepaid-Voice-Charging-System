import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PhoneBookRepository implements IPhoneBookRepository {

    private final IDatabaseConnection db;

    public PhoneBookRepository(IDatabaseConnection db) {
        this.db = db;
    }

    @Override
    public void addNumber(String msisdn, BigDecimal initialBalance) {
        String sql = "INSERT INTO user_balance (msisdn, balance) VALUES (?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msisdn);
            stmt.setBigDecimal(2, initialBalance);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add number: " + msisdn, e);
        }
    }

    @Override
    public void deleteNumber(String msisdn) {
        String sql = "DELETE FROM user_balance WHERE msisdn = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msisdn);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete number: " + msisdn, e);
        }
    }

    @Override
    public void updateBalance(String msisdn, BigDecimal newBalance) {
        String sql = "UPDATE user_balance SET balance = ? WHERE msisdn = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newBalance);
            stmt.setString(2, msisdn);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update balance for: " + msisdn, e);
        }
    }

    @Override
    public List<PhoneRecord> getAllNumbers() {
        String sql = "SELECT msisdn, balance FROM user_balance ORDER BY msisdn";
        List<PhoneRecord> records = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                records.add(new PhoneRecord(
                    rs.getString("msisdn"),
                    rs.getBigDecimal("balance")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve phone numbers", e);
        }
        return records;
    }

    @Override
    public boolean exists(String msisdn) {
        String sql = "SELECT 1 FROM user_balance WHERE msisdn = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msisdn);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check existence of: " + msisdn, e);
        }
    }

    @Override
    public BigDecimal getBalance(String msisdn) {
        String sql = "SELECT balance FROM user_balance WHERE msisdn = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msisdn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("balance");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get balance for: " + msisdn, e);
        }
    }
}
