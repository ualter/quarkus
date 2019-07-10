package ujr.quarkus.app;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

	public String greeting(String name) {
		return "Hello from Quarkus, " + name;
	}

}