package co.edu.uniquindio.oldbaker;

import co.edu.uniquindio.oldbaker.services.MailService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OldBakerApplication {
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(OldBakerApplication.class);
        app.run(args);
	}
}