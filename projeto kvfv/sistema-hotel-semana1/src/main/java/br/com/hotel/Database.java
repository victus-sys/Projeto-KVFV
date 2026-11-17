package br.com.hotel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import br.com.hotel.domain.PerfilAcesso;
import br.com.hotel.domain.Usuario;
import br.com.hotel.security.PasswordHasher;

public final class Database {

    private static final String URL = "jdbc:sqlite:data/hotel.db";

    private Database() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initialize() {
        try {
            java.nio.file.Files.createDirectories(
                    java.nio.file.Paths.get("data")
            );

            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {

                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS usuario (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nome TEXT NOT NULL," +
                    "login TEXT NOT NULL UNIQUE," +
                    "senha TEXT NOT NULL," +
                    "perfil TEXT NOT NULL" +
                    ")"
                );

                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS hospede (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nome TEXT NOT NULL," +
                    "documento TEXT," +
                    "telefone TEXT," +
                    "email TEXT," +
                    "ativo INTEGER NOT NULL DEFAULT 1" +
                    ")"
                );
                adicionarColunaSeAusente(statement, "hospede", "ativo", "INTEGER NOT NULL DEFAULT 1");

                boolean hasUsuarioId = false;
                try (java.sql.ResultSet columns = statement.executeQuery("PRAGMA table_info(hospede)")) {
                    while (columns.next()) {
                        if ("usuario_id".equals(columns.getString("name"))) hasUsuarioId = true;
                    }
                }
                if (!hasUsuarioId) {
                    statement.executeUpdate("ALTER TABLE hospede ADD COLUMN usuario_id INTEGER REFERENCES usuario(id)");
                }
                statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_hospede_usuario ON hospede(usuario_id) WHERE usuario_id IS NOT NULL");

                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS quarto (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "numero TEXT NOT NULL UNIQUE," +
                    "tipo TEXT NOT NULL," +
                    "status TEXT NOT NULL" +
                    ")"
                );

                criarAdministradorInicial(connection);

                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS reserva (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "hospede_id INTEGER NOT NULL," +
                    "quarto_id INTEGER NOT NULL," +
                    "data_entrada TEXT NOT NULL," +
                    "data_saida TEXT NOT NULL," +
                    "status TEXT NOT NULL," +
                    "FOREIGN KEY(hospede_id) REFERENCES hospede(id)," +
                    "FOREIGN KEY(quarto_id) REFERENCES quarto(id)" +
                    ")"
                );
                adicionarColunaSeAusente(statement, "quarto", "valor_diaria", "REAL NOT NULL DEFAULT 0");
                adicionarColunaSeAusente(statement, "quarto", "capacidade", "INTEGER NOT NULL DEFAULT 1");
                adicionarColunaSeAusente(statement, "quarto", "descricao", "TEXT");
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_reserva_quarto_periodo ON reserva(quarto_id,data_entrada,data_saida)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS hospedagem (id INTEGER PRIMARY KEY AUTOINCREMENT, reserva_id INTEGER NOT NULL UNIQUE, checkin_em TEXT NOT NULL, checkout_em TEXT, FOREIGN KEY(reserva_id) REFERENCES reserva(id))");
                adicionarColunaSeAusente(statement, "hospedagem", "valor_total", "REAL NOT NULL DEFAULT 0");
            }

            System.out.println("Banco SQLite inicializado com sucesso.");

        } catch (Exception e) {
            System.err.println("Erro ao inicializar o banco: " + e.getMessage());
        }
    }

    private static void criarAdministradorInicial(Connection connection) throws SQLException {
        String login = System.getenv().getOrDefault("HOTEL_ADMIN_USERNAME", "admin@hotel.local")
                .trim().toLowerCase(java.util.Locale.ROOT);
        String senha = System.getenv().getOrDefault("HOTEL_ADMIN_PASSWORD", "admin123");
        try (var consulta = connection.prepareStatement("SELECT 1 FROM usuario WHERE login = ?")) {
            consulta.setString(1, login);
            try (var resultado = consulta.executeQuery()) {
                if (resultado.next()) return;
            }
        }
        try (var inserir = connection.prepareStatement(
                "INSERT INTO usuario (nome, login, senha, perfil) VALUES (?, ?, ?, 'ADMIN')")) {
            inserir.setString(1, "Administrador");
            inserir.setString(2, login);
            inserir.setString(3, PasswordHasher.hash(senha));
            inserir.executeUpdate();
        }
    }

    private static void adicionarColunaSeAusente(Statement statement, String tabela, String coluna, String definicao) throws SQLException {
        boolean existe = false;
        try (var resultado = statement.executeQuery("PRAGMA table_info(" + tabela + ")")) {
            while (resultado.next()) if (coluna.equals(resultado.getString("name"))) existe = true;
        }
        if (!existe) statement.executeUpdate("ALTER TABLE " + tabela + " ADD COLUMN " + coluna + " " + definicao);
    }

    public static void salvarHospede(Integer id,String nome,String documento,String telefone,String email)throws SQLException{
        if(nome==null||nome.isBlank())throw new IllegalArgumentException("Informe o nome do hóspede.");
        try(Connection c=getConnection()){
            if(id==null){try(PreparedStatement p=c.prepareStatement("INSERT INTO hospede(nome,documento,telefone,email) VALUES(?,?,?,?)")){p.setString(1,nome.trim());p.setString(2,vazio(documento));p.setString(3,vazio(telefone));p.setString(4,vazio(email));p.executeUpdate();}return;}
            c.setAutoCommit(false);
            try{
                Integer usuarioId=null;try(PreparedStatement p=c.prepareStatement("SELECT usuario_id FROM hospede WHERE id=?")){p.setInt(1,id);try(var r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Hóspede não encontrado.");int value=r.getInt(1);if(!r.wasNull())usuarioId=value;}}
                if(usuarioId!=null){if(email==null||email.isBlank())throw new IllegalArgumentException("O e-mail de acesso é obrigatório para uma conta vinculada.");try(PreparedStatement p=c.prepareStatement("UPDATE usuario SET nome=?,login=? WHERE id=?")){p.setString(1,nome.trim());p.setString(2,email.trim().toLowerCase(java.util.Locale.ROOT));p.setInt(3,usuarioId);p.executeUpdate();}}
                try(PreparedStatement p=c.prepareStatement("UPDATE hospede SET nome=?,documento=?,telefone=?,email=? WHERE id=?")){p.setString(1,nome.trim());p.setString(2,vazio(documento));p.setString(3,vazio(telefone));p.setString(4,vazio(email));p.setInt(5,id);p.executeUpdate();}
                c.commit();
            }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;if(e instanceof IllegalArgumentException ia)throw ia;throw new SQLException(e);}
        }
    }

    public static int excluirHospede(int id)throws SQLException{
        try(Connection c=getConnection()){
            c.setAutoCommit(false);
            try{
                Integer usuarioId=null;try(PreparedStatement p=c.prepareStatement("SELECT usuario_id FROM hospede WHERE id=?")){p.setInt(1,id);try(var r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Hóspede não encontrado.");int value=r.getInt(1);if(!r.wasNull())usuarioId=value;}}
                try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM reserva WHERE hospede_id=? AND status='EM_HOSPEDAGEM' LIMIT 1")){p.setInt(1,id);try(var r=p.executeQuery()){if(r.next())throw new IllegalStateException("Não é possível excluir um hóspede que está hospedado agora.");}}
                int reservas;try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM reserva WHERE hospede_id=?")){p.setInt(1,id);try(var r=p.executeQuery()){r.next();reservas=r.getInt(1);}}
                try(PreparedStatement p=c.prepareStatement("DELETE FROM hospedagem WHERE reserva_id IN (SELECT id FROM reserva WHERE hospede_id=?)")){p.setInt(1,id);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("DELETE FROM reserva WHERE hospede_id=?")){p.setInt(1,id);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("DELETE FROM hospede WHERE id=?")){p.setInt(1,id);p.executeUpdate();}
                if(usuarioId!=null)try(PreparedStatement p=c.prepareStatement("DELETE FROM usuario WHERE id=? AND perfil='HOSPEDE'")){p.setInt(1,usuarioId);p.executeUpdate();}
                c.commit();return reservas;
            }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;if(e instanceof IllegalArgumentException ia)throw ia;if(e instanceof IllegalStateException ie)throw ie;throw new SQLException(e);}
        }
    }

    private static String vazio(String value){return value==null||value.isBlank()?null:value.trim();}

    public static Usuario autenticar(String login, String senha) throws SQLException {
        String sql = "SELECT id, nome, login, senha, perfil FROM usuario WHERE login = ?";
        try (Connection connection = getConnection();
             var consulta = connection.prepareStatement(sql)) {
            consulta.setString(1, login.trim().toLowerCase(java.util.Locale.ROOT));
            try (var resultado = consulta.executeQuery()) {
                if (!resultado.next() || !PasswordHasher.matches(senha, resultado.getString("senha"))) return null;
                return new Usuario(resultado.getInt("id"), resultado.getString("nome"),
                        resultado.getString("login"), resultado.getString("senha"),
                        PerfilAcesso.valueOf(resultado.getString("perfil")));
            }
        }
    }

    public static br.com.hotel.domain.Hospede buscarHospedeDoUsuario(int usuarioId) throws SQLException {
        String sql = "SELECT id, nome, documento, telefone, email FROM hospede WHERE usuario_id = ? AND ativo=1";
        try (Connection connection = getConnection(); var consulta = connection.prepareStatement(sql)) {
            consulta.setInt(1, usuarioId);
            try (var resultado = consulta.executeQuery()) {
                if (!resultado.next()) return null;
                return new br.com.hotel.domain.Hospede(resultado.getInt("id"), resultado.getString("nome"),
                        resultado.getString("documento"), resultado.getString("telefone"), resultado.getString("email"));
            }
        }
    }

    public static java.util.List<br.com.hotel.domain.Hospede> buscarHospedesInativos()throws SQLException{
        java.util.List<br.com.hotel.domain.Hospede> result=new java.util.ArrayList<>();
        try(Connection c=getConnection();PreparedStatement p=c.prepareStatement("SELECT id,nome,documento,telefone,email FROM hospede WHERE ativo=0 ORDER BY id DESC");var r=p.executeQuery()){
            while(r.next())result.add(new br.com.hotel.domain.Hospede(r.getInt("id"),r.getString("nome"),r.getString("documento"),r.getString("telefone"),r.getString("email")));
        }
        return result;
    }
}

