package dk.kb.netarchivesuite.solrwayback.service.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ServiceExceptionMapper implements ExceptionMapper<SolrWaybackServiceException> {
    @Override
    public Response toResponse(SolrWaybackServiceException exception) {
    
        Response.Status responseStatus = exception.getResponseStatus();
        String message = exception.getMessage();
        
        return (message != null)
                       ? Response.status(responseStatus)
                                 .entity(message)
                                 .type("text/plain")
                                 .build()
                       : Response.status(responseStatus).build();
    }
}
