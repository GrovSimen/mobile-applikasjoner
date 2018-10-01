package no.ntnu.tollefsen.chat;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.security.DeclareRoles;
import javax.enterprise.context.ApplicationScoped;
import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;
import javax.security.enterprise.identitystore.PasswordHash;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import no.ntnu.tollefsen.chat.domain.Group;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 *
 * @author mikael
 */
@ApplicationScoped
@ApplicationPath("api")
@DeclareRoles({Group.ADMIN,Group.USER})
@DatabaseIdentityStoreDefinition(
    dataSourceLookup="java:app/jdbc/chat", 
    callerQuery="select password from chatuser where userid = ?",
    groupsQuery="select groups_name from chatusergroup_chatuser where users_userid  = ?",
    hashAlgorithm = PasswordHash.class
)
@BasicAuthenticationMechanismDefinition(realmName = "chatrealm")
public class JAXRSConfiguration extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        
        // Register the HTTP multipart support with JAX-RS
        resources.add(MultiPartFeature.class);        
        addRestResourceClasses(resources);
        return resources;
    }

    /**
     * Do not modify addRestResourceClasses() method.
     * It is automatically populated with
     * all resources defined in the project.
     * If required, comment out calling this method in getClasses().
     */
    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(no.ntnu.tollefsen.chat.AuthService.class);
        resources.add(no.ntnu.tollefsen.chat.ChatService.class);
        resources.add(no.ntnu.tollefsen.chat.ConfigurationService.class);
        resources.add(no.ntnu.tollefsen.chat.ConversationService.class);
    }
}
