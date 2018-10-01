package no.ntnu.tollefsen.chat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import no.ntnu.tollefsen.chat.domain.Conversation;
import no.ntnu.tollefsen.chat.domain.Group;
import no.ntnu.tollefsen.chat.domain.User;

/**
 *
 * @author mikael
 */
@Path("conversation")
@Stateless
@RolesAllowed({Group.USER})
public class ConversationService {
    @PersistenceContext
    EntityManager em;

    @Context
    SecurityContext sc;


    @GET
    public List<Conversation> getConversations() {
        return em.createNamedQuery(Conversation.FIND_BY_USER,Conversation.class)
                 .setParameter("userid", sc.getUserPrincipal().getName())
                 .getResultList();
    }

    @GET
    @Path("conversationfromdate")
    public List<Conversation> getUpdatedConversations(
                @QueryParam("from") String date) throws ParseException {
        Date from = new SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss").parse(date);
        return em.createNamedQuery(Conversation.FIND_BY_USER_AND_DATE,Conversation.class)
                .setParameter("userid", sc.getUserPrincipal().getName())
                .setParameter("date",from)
                .getResultList();
    }    
    
    
    @GET
    @Path("createconversation")
    public Conversation createConversation(
            @QueryParam("recipientids") List<String> recipientIds) {
        Conversation result = null;

        User owner = em.find(User.class, sc.getUserPrincipal().getName());
        List<User> recipients = findUsersByUserId(recipientIds);
        if(owner != null) {
            result = new Conversation(owner, recipients);
            em.persist(result);
        }
        
        return result;
    }

    @POST
    @Path("updaterecipients")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updaterecipients(
            @DefaultValue("-1") @FormParam("conversationid") Long conversationid,
            @FormParam("recipientids") List<String> recipientIds) {
        Conversation conversation = em.find(Conversation.class,conversationid);
        if(conversation != null) {
            List<User> recipients = findUsersByUserId(recipientIds);
            conversation.setRecipients(recipients);
            em.merge(conversation);
            return Response.ok().build();
        } else {
            return Response.notModified().build();
        }
    }
    

    private List<User> findUsersByUserId(List<String> recipientIds) {
        return recipientIds.size() > 0 ? em.createNamedQuery(User.FIND_USER_BY_IDS, User.class)
                .setParameter("ids",recipientIds)
                .getResultList() : new ArrayList<>();
    }
}