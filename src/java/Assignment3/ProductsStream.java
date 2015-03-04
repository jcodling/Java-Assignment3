package Assignment3;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Jeff Codling - c0471944
 */
@Path("products")
public class ProductsStream {

    @GET
    @Produces("application/json")
    public Response getAll() {
        return Response.ok(getResults("SELECT * FROM products"), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("{productId}")
    @Produces("application/json")
    public Response getById(@PathParam("productId") int productId) {
        return Response.ok(getResults("SELECT * FROM products WHERE productId=?", String.valueOf(productId)),
                MediaType.APPLICATION_JSON).build();
    }
    
    @POST
    @Consumes("application/json")
    public Response doInsert(String str) {
        JsonReader reader = Json.createReader(new StringReader(str));
        JsonObject json = reader.readObject();

        try (Connection conn = Credentials.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO products (name,description,quantity)"+
                            " VALUES ('"+
                            json.getString("name")+"','"+
                            json.getString("description")+"',"+
                            String.valueOf(json.getInt("quantity"))+")",
                    Statement.RETURN_GENERATED_KEYS);
            
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if(rs.next()) {
                return Response.ok("http://localhost:8080/CPD-4414-Assignment03/stream/"+
                        rs.getInt(1),
                        MediaType.TEXT_HTML).build();
            }
        } catch (SQLException ex) {
            Logger.getLogger(ProductsStream.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Response.status(500).build();
    }

    private String getResults(String query, String... params) {
        StringWriter out = new StringWriter();
        JsonGeneratorFactory factory = Json.createGeneratorFactory(null);

        try (Connection conn = Credentials.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(query);
            for (int i = 1; i <= params.length; i++) {
                pstmt.setString(i, params[i - 1]);
            }
            ResultSet rs = pstmt.executeQuery();
            JsonGenerator gen = factory.createGenerator(out);
            gen.writeStartArray();
            while (rs.next()) {
                gen.writeStartObject()
                        .write("productId", rs.getInt("productId"))
                        .write("name", rs.getString("name"))
                        .write("description", rs.getString("description"))
                        .write("quantity", rs.getInt("quantity"))
                        .writeEnd();
            }
            gen.writeEnd();
            gen.close();
        } catch (SQLException ex) {
            Logger.getLogger(ProductsStream.class.getName()).log(Level.SEVERE, null, ex);
        }
        return out.toString();
    }

}
