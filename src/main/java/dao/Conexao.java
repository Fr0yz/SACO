package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexao {
    private static final String URL = "jdbc:mysql://localhost:3306/pi_athur";
    private static final String USER = "root";
    private static final String PASSWORD = "iarc1001";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Retorna o nome do driver JDBC (MySQL, PostgreSQL, etc.) */
    public static String getDriverName() {
        try (Connection conn = getConnection()) {
            return conn.getMetaData().getDriverName();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter nome do driver JDBC", e);
        }
    }
}
