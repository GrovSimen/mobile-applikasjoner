package no.ntnu.tollefsen.chat.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author mikael
 */
@Entity
@Data @AllArgsConstructor @EqualsAndHashCode(callSuper = false)
public class Configuration extends AbstractDomain {
    public static final String PHOTO_PATH = "photopath";
    
    @Id
    String id;
    
    @Column(nullable = false)
    String configuration;

    public Configuration() {
        super();
    }
}
