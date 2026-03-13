package com.kludgeworks.mcp.sparql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThat;

class RdfServiceTest {

    final String endpoint = "https://publications.europa.eu/webapi/rdf/sparql";
    private static final int PAGE_SIZE = 300;

    private RdfService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
        service = new RdfService(new RdfBackendClient(RestClient.create()), objectMapper);
    }

    @Test
    void getWorkUri() throws IOException {
        String query = """
                PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>
                PREFIX cmr: <http://publications.europa.eu/ontology/cdm/cmr#>
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                
                SELECT DISTINCT ?work ?p ?o
                WHERE {
                  # First get our work
                  ?work cdm:resource_legal_id_celex "32015R1998"^^xsd:string .
                }
                """;

        RdfService.PagedResult response = objectMapper.readValue(
            service.select(endpoint, query, PAGE_SIZE, 0),
            RdfService.PagedResult.class
        );
        assertEnvelope(response, PAGE_SIZE, 0);
        assertThat(response.result()).contains("\"results\"");
    }

    @Test
    void getExpressionsFromWorkItem() throws IOException {
        String query = """
                PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>
                PREFIX cmr: <http://publications.europa.eu/ontology/cdm/cmr#>
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                
                SELECT DISTINCT ?expression ?p ?o
                WHERE {
                  # First get our work
                  ?work cdm:resource_legal_id_celex "32015R1998"^^xsd:string .
                
                  # Find expressions that reference this work and are in English
                  ?expression cdm:expression_belongs_to_work ?work ;
                              cmr:lang "eng"^^xsd:language ;
                             ?p ?o .
                }
                """;

        RdfService.PagedResult response = objectMapper.readValue(
            service.select(endpoint, query, PAGE_SIZE, 0),
            RdfService.PagedResult.class
        );
        assertEnvelope(response, PAGE_SIZE, 0);
        assertThat(response.result()).contains("\"results\"");
    }

    @Test
    void getExpression() throws IOException {
        String query = """
                PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>
                PREFIX cmr: <http://publications.europa.eu/ontology/cdm/cmr#>
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                
                SELECT DISTINCT ?expression ?p ?o
                WHERE {
                  # First get our work
                  ?work cdm:resource_legal_id_celex "32015R1998"^^xsd:string .
                
                  # Find expressions that reference this work and are in English
                  ?expression cdm:expression_belongs_to_work ?work ;
                              cmr:lang "eng"^^xsd:language ;
                             ?p ?o .
                }
                """;

        RdfService.PagedResult response = objectMapper.readValue(
            service.select(endpoint, query, PAGE_SIZE, PAGE_SIZE),
            RdfService.PagedResult.class
        );
        assertEnvelope(response, PAGE_SIZE, PAGE_SIZE);
        assertThat(response.pagination().pageIndex()).isEqualTo(1);
    }

    @Test
    void describe() throws IOException {
        String query = """
                    DESCRIBE <http://publications.europa.eu/resource/cellar/c38f361d-8a99-11e5-b8b7-01aa75ed71a1>
                """;
        RdfService.PagedResult response = objectMapper.readValue(
            service.describe(endpoint, query, PAGE_SIZE, 0),
            RdfService.PagedResult.class
        );
        assertEnvelope(response, PAGE_SIZE, 0);
        assertThat(response.result()).contains("http://");
    }

    private static void assertEnvelope(RdfService.PagedResult response, int pageSize, int offset) {
        assertThat(response.pagination().pageSizeChars()).isEqualTo(pageSize);
        assertThat(response.pagination().offset()).isEqualTo(offset);
        assertThat(response.pagination().totalPages()).isGreaterThanOrEqualTo(1);
        assertThat(response.pagination().returnedChars()).isLessThanOrEqualTo(pageSize);
        assertThat(response.result()).isNotBlank();
    }
}
