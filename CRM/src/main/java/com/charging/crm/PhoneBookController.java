import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Path("/phonebook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PhoneBookController {

    private final IPhoneBookService service;

    public PhoneBookController() {
        IDatabaseConnection db = new PostgreSQLConnection();
        IPhoneBookRepository repo = new PhoneBookRepository(db);
        this.service = new PhoneBookService(repo);
    }

    public PhoneBookController(IPhoneBookService service) {
        this.service = service;
    }

    @GET
    public Response getAllNumbers() {
        List<PhoneRecord> records = service.getAllNumbers();
        return Response.ok(records).build();
    }

    @POST
    public Response addNumber(Map<String, String> body) {
        try {
            String msisdn = body.get("msisdn");
            BigDecimal balance = new BigDecimal(body.get("balance"));
            service.addNumber(msisdn, balance);
            return Response.status(Response.Status.CREATED)
                           .entity(Map.of("message", "Number added successfully."))
                           .build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", e.getMessage()))
                           .build();
        }
    }

    @DELETE
    @Path("/{msisdn}")
    public Response deleteNumber(@PathParam("msisdn") String msisdn) {
        try {
            service.deleteNumber(msisdn);
            return Response.ok(Map.of("message", "Number deleted successfully.")).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", e.getMessage()))
                           .build();
        }
    }

    @PUT
    @Path("/{msisdn}/balance")
    public Response updateBalance(@PathParam("msisdn") String msisdn,
                                  Map<String, String> body) {
        try {
            BigDecimal newBalance = new BigDecimal(body.get("balance"));
            service.updateBalance(msisdn, newBalance);
            return Response.ok(Map.of("message", "Balance updated successfully.")).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", e.getMessage()))
                           .build();
        }
    }

    @GET
    @Path("/{msisdn}/balance")
    public Response getBalance(@PathParam("msisdn") String msisdn) {
        try {
            BigDecimal balance = service.getBalance(msisdn);
            return Response.ok(Map.of("msisdn", msisdn, "balance", balance.toString())).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", e.getMessage()))
                           .build();
        }
    }
}
