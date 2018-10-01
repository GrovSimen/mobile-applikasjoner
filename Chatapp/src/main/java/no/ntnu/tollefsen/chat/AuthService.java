package no.ntnu.tollefsen.chat;

import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.security.enterprise.identitystore.PasswordHash;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import no.ntnu.tollefsen.chat.domain.Group;
import no.ntnu.tollefsen.chat.domain.User;



/**
 * Authentication REST service used for login, logout and to register new users
 *
 * @Path("auth) makes this class into a JAX-RS REST service. "auth" specifies 
 * that the URL of this service would begin with "domainname/chat/api/auth"
 * depending on the domain, context path of project and the JAX-RS base configuration
 * @Produces(MediaType.APPLICATION_JSON) instructs JAX-RS that the default result 
 * of a method is to be marshalled as JSON
 * 
 * @Stateless makes this class into a transactional stateless EJB, which is a 
 * requirement of using the JPA EntityManager to communicate with the database.
 * 
 * @DeclareRoles({UserGroup.ADMIN,UserGroup.USER}) specifies the roles used in
 * this EJB.
 * 
 * @author mikael
 */
@Path("auth")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@DeclareRoles({Group.ADMIN,Group.USER})
public class AuthService {
    private static final String ANONYMOUS = "anonymous";    
    
    @Context
    SecurityContext sc;
    
    @Inject
    PasswordHash hasher;

    /** 
     * The application server will inject a EntityManager as a way to communicate 
     * with the database.
     */
    @PersistenceContext
    EntityManager em;


    /**
     * Get User from UserPrincipal
     *
     * @return
     */
    @RolesAllowed({Group.USER})
    public User getUser() {
        return em.find(User.class, sc.getUserPrincipal().getName());
    }
    
    /**
     *  This method is annotated as having a security constraint, 
     *  which makes the jdbc authentication module intercept the call and do
     *  a BASIC authentication. This method will not be called if the user is
     *  not authenticated.
     * 
     * @param sc - provides access to security related information
     * @param request - represents the HTTP request
     * @return A JSON objects containing the userid and authorization information. 
     *         This might leak more internal information then you want. Just for
     *         demonstration purposes.
     */
    @GET
    @Path("login")
    @RolesAllowed({Group.ADMIN,Group.USER})
    public Response login(@Context SecurityContext sc,
                          @Context HttpServletRequest request) {
        /* Creates a JSESSIONID cookie. This enables us to call other methods
           without resending the users credentials */
        request.getSession(true); 
        
        //System.out.printf("SC is %s\n",this.sc);
        
        return Response.ok(getUser()).build();
    }

    
    /**
     * Logs out the user from the Java EE authorization system. Manually removes
     * the JSESSIONID cookie.
     * 
     * @param request the HTTP request
     * @return an empty response
     */
    @GET
    @Path("logout")
    public Response logout(@Context HttpServletRequest request) {
        try {
            request.logout();
        } catch (ServletException ex) {
            Logger.getLogger(AuthService.class.getName()).log(Level.SEVERE, null, ex);
        }

        return Response.ok().cookie((NewCookie[])null).build();
    }

    
    public User getAnonymous() {
        return getOrCreate(ANONYMOUS);
    }
    

    /**
     * 
     * @param uid
     * @return 
     */
    public User getOrCreate(String uid) {
        User result = em.find(User.class, uid);
        if(result == null) {
            result = new User(uid);
            em.persist(result);
        }
        
        return result;
    }
    
    @GET
    @Path("setpwd")
    public Response updatePwd(@QueryParam("uid")String uid, @QueryParam("pwd")String pwd) {
        if(uid != null && pwd != null) {
            User u = em.find(User.class, uid);
            if(u != null) {
                u.setPassword(hasher.generate(pwd.toCharArray()));
                em.merge(u);
            }
            return Response.accepted().build();
        }   
        
        return Response.notModified().build();
    }
    

    /**
     * Does an insert into the CHATUSER and CHATUSERGROUP tables. It creates
     * a hash of the password before the user is stored in the database. 
     * The authentication system will read the CHATUSER table when doing an 
     * authentication.
     * 
     * @param uid
     * @param pwd
     * @param firstname
     * @param lastname
     * @return 
     */
    @GET @Path("create")
    //@RolesAllowed(UserGroup.ADMIN)
    public User createUser(@QueryParam("uid") String uid, @QueryParam("pwd") String pwd,
                           @QueryParam("firstname") String firstname,@QueryParam("lastname") String lastname) {
        // Creates a new ChatUser object with a hashed and encoded version of the password
        User result = new User(uid, hasher.generate(pwd.toCharArray()));
        result.setFirstname(firstname);
        result.setLastname(lastname);
            
        // Add default group to user
        result.addGroup(em.find(Group.class, Group.USER));

        // Inserts User into the database
        em.persist(result);            

        return result;
    }


    /**
     * Extract some security information about the user and encode it in a 
     * JsonObject to be returned to the user.
     * 
     * @param sc provides access to security related information
     * @param request the HTTP request
     * @return Security related information about the user
     */
    @GET @Path("status")
    public JsonObject getSecureRole(@Context SecurityContext sc,
                                    @Context HttpServletRequest request) {
        JsonArrayBuilder cookies = Json.createArrayBuilder();
        int length = request.getCookies() != null ? request.getCookies().length : 0;
        for(int i = 0; i < length; i++) {
            Cookie c = request.getCookies()[i];
            cookies.add(Json.createObjectBuilder()
               .add("name", c.getName())
               .add("value", c.getValue())
               .add("maxAge", c.getMaxAge())
               .add("secure", c.getSecure())
               .add("httpOnly", c.isHttpOnly())
               .add("version", c.getVersion())
            );
        }

        Principal user = request.getUserPrincipal();
        String authScheme = sc.getAuthenticationScheme() != null ? sc.getAuthenticationScheme() : "null";
        return Json.createObjectBuilder()
                .add("userid", user != null ? user.getName() : "not logged in")
                .add("authScheme", authScheme)
                .add("admin",  Boolean.toString(sc.isUserInRole("admin")))
                .add("user",  Boolean.toString(sc.isUserInRole("user")))
                .add("cookies",cookies)
                .build();
    }
}