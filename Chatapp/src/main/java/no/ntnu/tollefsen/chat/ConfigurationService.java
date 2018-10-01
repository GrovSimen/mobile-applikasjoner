package no.ntnu.tollefsen.chat;

import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import no.ntnu.tollefsen.chat.domain.Configuration;
import no.ntnu.tollefsen.chat.domain.Group;

/**
 *
 * @author mikael
 */
@Path("config")
@Stateless
@RolesAllowed({Group.ADMIN})
public class ConfigurationService {
    @PersistenceContext
    EntityManager em;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Configuration> getAll() {
        return em.createQuery("Select c from Configuration c").getResultList();
    }
    
    @GET
    @Path("{key}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getValue(@PathParam("key")String key) {
        Configuration result = em.find(Configuration.class, key);
        return result != null ? result.getConfiguration(): "";
    }

    @PUT
    @Path("update")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response setValue(@QueryParam("key")String key, @QueryParam("value")String value) {
        Configuration config = em.find(Configuration.class, key);
        if(config != null) {
            config.setConfiguration(value);
            em.merge(config);
        } else {
            em.persist(new Configuration(key,value));
        }
        
        return Response.accepted().build();
    }
}