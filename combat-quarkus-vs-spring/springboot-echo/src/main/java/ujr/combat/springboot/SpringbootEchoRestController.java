package ujr.combat.springboot;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(path = "/echo")
@RestController
public class SpringbootEchoRestController {
	
	
	@GetMapping(path = "/{word}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Echo echo(@PathVariable String word) {
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
