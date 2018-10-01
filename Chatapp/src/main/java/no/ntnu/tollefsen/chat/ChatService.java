package no.ntnu.tollefsen.chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import net.coobird.thumbnailator.Thumbnails;
import no.ntnu.tollefsen.chat.domain.*;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;


/**
 *
 * @author mikael
 */
@Path("chat")
@Stateless
@DeclareRoles({Group.USER})
public class ChatService {
    @Inject
    ConfigurationService config;
    
    @Inject
    AuthService authService;    
    
    @Context
    SecurityContext sc;
        
    @PersistenceContext
    EntityManager em;
    
    @Resource(mappedName="java:app/jdbc/chat")
    DataSource dataSource;
    
    @GET
    @Path("users")
    @RolesAllowed({Group.USER})
    public List<User> getAllUsers() {
        return em.createNamedQuery(User.FIND_ALL_USERS,User.class).getResultList();
    }

    
    @GET
    @Path("messages")
    @RolesAllowed({Group.ADMIN})
    public List<Message> getMessages() {
        return em.createNamedQuery(Message.FIND_ALL_MESSAGES,Message.class).getResultList();
    }
    
    @GET
    @Path("messages/{conversationid}")
    @RolesAllowed({Group.USER})
    public List<Message> getMessages(@DefaultValue("-1") @PathParam("conversationid")Long conversationid) {
        return em.createNamedQuery(Message.FIND_MESSAGES_BY_USERID,Message.class)
            .setParameter("cid", conversationid)
            .setParameter("userid", sc.getUserPrincipal().getName())
            .getResultList();
    }

    
    private String getPhotoPath() {
        return ".";
    }

    /**
     * TODO: Check if user is part of receivers
     *
     * @param conversationid
     * @return
     */
    private Conversation getConversation(Long conversationid) {
        return em.createNamedQuery(Conversation.FIND_BY_ID_AND_USERID,Conversation.class)
                 .setParameter("cid", conversationid)
                 .setParameter("userid", sc.getUserPrincipal().getName())
                 .getSingleResult();
    }

    @POST
    @Path("send")    
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Group.USER})
    public Response sendMessage(@FormDataParam("conversationid")Long conversationid,
                                @FormDataParam("message")String text,
                                FormDataMultiPart multiPart) {
        Message message;
        try {
            User user = em.find(User.class,sc.getUserPrincipal().getName());
            Conversation conversation = getConversation(conversationid);
            message = new Message(text,user, conversation);
            
            List<FormDataBodyPart> images = multiPart.getFields("image");
            if(images != null) {
                for(FormDataBodyPart part : images) {
                    InputStream is = part.getEntityAs(InputStream.class);
                    ContentDisposition meta = part.getContentDisposition();            

                    String pid = UUID.randomUUID().toString();
                    Files.copy(is, Paths.get(getPhotoPath(),pid));

                    MediaObject photo = new MediaObject(pid, user,meta.getFileName(),meta.getSize(),meta.getType());
                    em.persist(photo);
                    message.addPhoto(photo);
                }
            }

            em.persist(message);
            em.merge(conversation);
        } catch (IOException ex) {
            Logger.getLogger(ChatService.class.getName()).log(Level.SEVERE, null, ex);
            return Response.serverError().build();
        }
        
        return Response.ok(message).build();
    }
    

    /**
     * Streams an image to the browser(the actual compressed pixels). The image
     * will be scaled to the appropriate with if the with parameter is provided.
     *
     * @param name the filename of the image
     * @param width the required scaled with of the image
     * 
     * @return the image in original format or in jpeg if scaled
     */
    @GET
    @Path("image/{name}")
    @Produces("image/jpeg")
    public Response getImage(@PathParam("name") String name, 
                             @QueryParam("width") int width) {
        if(em.find(MediaObject.class, name) != null) {
            StreamingOutput result = (OutputStream os) -> {
                java.nio.file.Path image = Paths.get(getPhotoPath(),name);
                if(width == 0) {
                    Files.copy(image, os);
                    os.flush();
                } else {
                    Thumbnails.of(image.toFile())
                              .size(width, width)
                              .outputFormat("jpeg")
                              .toOutputStream(os);
                }
            };

            // Ask the browser to cache the image for 24 hours
            CacheControl cc = new CacheControl();
            cc.setMaxAge(86400);
            cc.setPrivate(true);

            return Response.ok(result).cacheControl(cc).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }    
    
    
    // === Create testdata ===
    /**
     * 
     * @return 
     */
    public List<Conversation> createConversations() {
        List<Conversation> result = new ArrayList<>();
        
        List<User> users = new ArrayList<>();
        for(int i = 0; i < 10;i++) {
            User user = authService.createUser("user" + i, "user" + i,"user" + i,"user" + i);
            users.add(user);
        }
        
        users.forEach((user) -> {
            for(int i = 0; i < 5; i++) {
                Conversation c = new Conversation(user,getRandomUser(users));
                
                for(int j = 0; j < 5; j++ ) {
                    c.addMessage(
                        new Message("Text from user " + user.getFirstname() + " #" + j,user,c)
                    );
                }
                em.persist(c);
                result.add(c);
            }
        });
        
        return result;
    }
    
    private static List<User> getRandomUser(List<User> users) {
        List<User> result = new ArrayList<>(users);
        Collections.shuffle(users);
        return result.subList(0, 3);
    }    
}