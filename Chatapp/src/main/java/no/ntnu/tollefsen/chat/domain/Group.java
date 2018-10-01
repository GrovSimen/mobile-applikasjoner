package no.ntnu.tollefsen.chat.domain;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;


/**
 *
 * @author mikael
 */
@Entity @Table(name = "CHATUSERGROUP")
@Data @AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(exclude = "users",callSuper = false)
@ToString(exclude = "users")
public class Group extends AbstractDomain {
    public static final String USER = "user";
    public static final String ADMIN = "admin";
    public static final String[] GROUPS = {USER, ADMIN};

    @Id
    String name;

    @ManyToMany()
    Set<User> users;

    public Group(String name) {
        this.name = name;
    }
}
