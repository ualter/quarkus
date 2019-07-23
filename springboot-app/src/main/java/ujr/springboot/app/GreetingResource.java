package ujr.springboot.app;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(path = "/hello")
public class GreetingResource {

	@GetMapping(path = "/greeting/{name}", produces = MediaType.TEXT_PLAIN_VALUE)
	public String greeting(@PathVariable(value = "name") String name) {
		String ip = "";
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			ip = "Ops... " + e.getMessage();
		}
		return "Hello from SpringBoot, " + name  + "\n\n*Server_IP --> " + ip;
	}

}
