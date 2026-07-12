import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class PostgreSQLConnection implements IDatabaseConnection {

    private final String url;
    private final String user;
    private final String password;

    public PostgreSQLConnection() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("db.properties")) {
            props.load(in);
            this.url = props.getProperty("db.url");
            this.user = props.getProperty("db.user");
            this.password = props.getProperty("db.password");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
