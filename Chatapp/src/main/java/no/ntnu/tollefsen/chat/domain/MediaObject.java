package no.ntnu.tollefsen.chat.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author mikael
 */
/**
 *
 * @author mikael
 */
@Entity
@Data @EqualsAndHashCode(callSuper = false)
public class MediaObject extends AbstractDomain {
    @Id
    String id;
    
    String name;
    
    long filesize;
    String mimeType;
    
    @JoinColumn(nullable = false)
    @ManyToOne
    User owner;

    protected MediaObject() {
        super();
    }
    
    public MediaObject(String id, User owner) {
        this();
        this.id = id;
        this.owner = owner;
    }

    public MediaObject(String id, User owner, String name, long filesize, String mimetype) {
        this();
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.filesize = filesize;
        this.mimeType = mimetype;
    }
}