package no.ntnu.tollefsen.chat.domain;

import java.util.ArrayList;
import java.util.List;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static no.ntnu.tollefsen.chat.domain.Message.FIND_ALL_MESSAGES;
import static no.ntnu.tollefsen.chat.domain.Message.FIND_MESSAGES_BY_USERID;

@Entity
@Data @EqualsAndHashCode(exclude = {"sender","conversation","photos"}, callSuper = false)
@AllArgsConstructor
@NamedQuery(name = FIND_MESSAGES_BY_USERID,
            query = "select m from Message m " +
                    "where m.conversation.id = :cid and " +
                    "(m.conversation.owner.userid = :userid or :userid member of m.conversation.recipients)")
@NamedQuery(name = FIND_ALL_MESSAGES,
            query = "select m from Message m")
public class Message extends AbstractDomain {
    public static final String FIND_ALL_MESSAGES = "Message.findAllUsers";
    public static final String FIND_MESSAGES_BY_USERID = "Message.findByUserId";

    @Id @GeneratedValue
    private Long id;

    String text;
    
    @ManyToOne(optional = false,cascade = CascadeType.PERSIST)
    User sender;

    @JsonbTransient
    @ManyToOne(optional = false,cascade = CascadeType.PERSIST)
    Conversation conversation;
    
    @JsonbTypeAdapter(MediaObjectAdapter.class)
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    List<MediaObject> photos;

    protected Message() {}
    
    public Message(String text, User sender, Conversation conversation) {
        this.text = text;
        this.sender = sender;
        this.conversation = conversation;
        this.conversation.getMessages().add(this);
    }
    
    public Long getId() {
        return id;
    }

    public void addPhoto(MediaObject photo) {
        if(this.photos == null) {
            this.photos = new ArrayList<>();
        }
        
        this.photos.add(photo);
    }    
}
