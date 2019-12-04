package ujr.combat.quarkus;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

@Path("/echo")
public class EchoResource {

    @GET
    @Path("/{word}")
    @Produces(MediaType.APPLICATION_JSON)
    public Echo echo(@PathParam String word) {
    	return new Echo(UUID.randomUUID().toString(),word, extractIP());
    }

	private String extractIP() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			return "Ops... " + e.getMessage();
		}
	}
    
}