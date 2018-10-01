package no.ntnu.tollefsen.chat.domain;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static no.ntnu.tollefsen.chat.domain.Conversation.FIND_BY_ID_AND_USERID;
import static no.ntnu.tollefsen.chat.domain.Conversation.FIND_BY_USER;
import static no.ntnu.tollefsen.chat.domain.Conversation.FIND_BY_USER_AND_DATE;


/**
 *
 * @author mikael
 */
@Entity
@Data @EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NamedQuery(name = FIND_BY_USER,
            query = "select distinct c from Conversation c, User u " +
                    "where u.userid = :userid and (c.owner = u or u member of c.recipients) " +
                    "order by c.created desc")
@NamedQuery(name = FIND_BY_USER_AND_DATE,
            query = "select distinct c from Conversation c, User u inner join c.messages m " +
                    "where u.userid = :userid and (c.owner = u or u member of c.recipients) " +
                    "and m.created >= :date")
@NamedQuery(name = FIND_BY_ID_AND_USERID,
            query = "select distinct c from Conversation c, User u " +
                    "where u.userid = :userid and c.id = :cid " +
                    "and (c.owner = u or u member of c.recipients)")
public class Conversation extends AbstractDomain {
    public static final String FIND_BY_USER = "Conversation.findByUser";
    public static final String FIND_BY_USER_AND_DATE = "Conversation.findByUserAndDate";
    public static final String FIND_BY_ID_AND_USERID = "Conversation.findByIdAndUserId";

    @Id @GeneratedValue
    Long id;

    @OneToMany(mappedBy = "conversation",cascade = CascadeType.ALL)
    List<Message> messages;

    @ManyToMany(cascade = {CascadeType.PERSIST})
    List<User> recipients;

    @ManyToOne(optional = false,cascade = CascadeType.PERSIST)
    User owner;
    
    protected Conversation() {
    }

    public Conversation(User owner) {
        this.owner = owner;
    }
    
    public Conversation(User owner, List<User> recipients) {
        this.owner = owner;
        this.recipients = recipients;
    }
        
    public List<Message> getMessages() {
        if(messages == null) {
            messages = new ArrayList<>();
        }
        
        return messages;
    }

    public void addMessage(Message message) {
       getMessages().add(message);
    }
}
