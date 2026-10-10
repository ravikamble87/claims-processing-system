package com.claims;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Arrays;

@SpringBootApplication
public class ClaimsProcessingSystemApplication {

    /**
     * No positional arguments → web server (GUI + REST API).
     * Positional arguments (input.csv output.csv) → batch mode without starting the web server.
     */
    public static void main(String[] args) {
        boolean batchMode = Arrays.stream(args).anyMatch(arg -> !arg.startsWith("--"));
        new SpringApplicationBuilder(ClaimsProcessingSystemApplication.class)
                .web(batchMode ? WebApplicationType.NONE : WebApplicationType.SERVLET)
                .run(args);
    }
}
