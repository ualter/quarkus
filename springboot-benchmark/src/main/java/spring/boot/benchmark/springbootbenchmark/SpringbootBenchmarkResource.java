package spring.boot.benchmark.springbootbenchmark;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(path = "/hello")
public class SpringbootBenchmarkResource {

	@GetMapping(path = "/greeting/{name}", produces = MediaType.TEXT_PLAIN_VALUE)
	public String greeting(@PathVariable(value = "name") String name) {
		return "Hello from SpringBoot, " + name;
	}

}
