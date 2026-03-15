package com.kludgeworks.mcp.sparql;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(AppDirsTestConfig.Config.class)
class ApplicationTests extends AppDirsTestConfig {

	@Test
	void contextLoads() {
	}

}
