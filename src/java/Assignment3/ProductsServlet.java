package Assignment3;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Jeff Codling - c0471944
 */
@WebServlet("/products")
public class ProductsServlet extends HttpServlet {

    /** 
     * Provides GET /products and GET /products?id=X
     * 
     * @param request - the request object
     * @param response - the response object
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Content-Type", "text/plain-text");
        try (PrintWriter out = response.getWriter()) {
            if (!request.getParameterNames().hasMoreElements()) {
                // There are no parameters at all
                out.println(getResults("SELECT * FROM products"));
            } else {
                // There are some parameters
                int id = Integer.parseInt(request.getParameter("id"));
                out.println(getResults("SELECT * FROM products WHERE productId = ?", String.valueOf(id)));
            }
        } catch (IOException ex) {
            Logger.getLogger(ProductsServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Provides POST /products?name=X&description=X&quantity=X
     * 
     * @param request - the request object
     * @param response - the response object
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Content-Type", "text/plain-text");
            try (PrintWriter out = response.getWriter()) {
            if (!request.getParameterNames().hasMoreElements()) {
                // There are no parameters
                response.setStatus(500);
                out.println("No parameters specified.");
            } else {
                // There are parameters
                Set<String> keySet = request.getParameterMap().keySet();
                if (keySet.contains("name") && keySet.contains("description") && keySet.contains("quantity")) {
                    // There are the correct parameters
                    // Insert item
                    int idOfLast = doUpdate("INSERT INTO products (name,description,quantity) VALUES ( ?, ?, ?)",
                            request.getParameter("name"),
                            request.getParameter("description"),
                            request.getParameter("quantity"));

//                    if(idOfLast != 0) {
//                        out.println("http://localhost:8080/CPD-4414-Assignment03/products/id="+idOfLast);
//                    } else {
//                        response.setStatus(500);
//                        out.println("Insert failed. ("+idOfLast+")");
//                    }
                    
                    idOfLast = getIdByName(request.getParameter("name"));
                    
                    if(idOfLast == 0) {
                        response.setStatus(500);
                        out.println("Insert Failed.");
                    } else {
                        out.println("http://localhost:8080/CPD-4414-Assignment03/products?id="+idOfLast);
                    }

                } else {
                    response.setStatus(500);
                    out.println("Required parameters missing. (name, description, quantity)");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ProductsServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getResults(String query, String... params) {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = Credentials.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(query);
            for (int i = 1; i <= params.length; i++) {
                pstmt.setString(i, params[i - 1]);
            }
            ResultSet rs = pstmt.executeQuery();
            sb.append("[ ");
            while (rs.next()) {
                sb.append(String.format("{ \"productId\" : \"%s\", ", rs.getInt("productId")));
                sb.append(String.format("\"name\" : \"%s\", ", rs.getString("name")));
                sb.append(String.format("\"description\" : \"%s\", ", rs.getString("description")));
                sb.append(String.format("\"quantity\" : \"%s\" }, ", rs.getInt("quantity")));
            }
            sb.deleteCharAt(sb.length() - 2);
            sb.append("]");
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(ProductsServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }

    private int doUpdate(String query, String... params) {
        int numChanges = 0;
        try (Connection conn = Credentials.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            for (int i = 1; i <= params.length; i++) {
                pstmt.setString(i, params[i - 1]);
            }
            numChanges = pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                numChanges = rs.getInt(1);
            }
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(ProductsServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        return numChanges;
    }
    
    private int getIdByName(String name) {
        int returnedId = 0;
        try (Connection conn = Credentials.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM products WHERE name='"+name+"'");
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()) {
                returnedId = rs.getInt("productId");
            }
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(ProductsServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        return returnedId;
    }
}
