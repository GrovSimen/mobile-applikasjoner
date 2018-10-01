package no.ntnu.tollefsen.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.*;
import java.util.*;

import static no.ntnu.tollefsen.chat.domain.User.FIND_ALL_USERS;
import static no.ntnu.tollefsen.chat.domain.User.FIND_USER_BY_IDS;

/**
 * A user of the system. Bound to the authentication system
 *
 * @author mikael
 */
@Entity @Table(name = "CHATUSER")
@Data @EqualsAndHashCode(callSuper = false)
@AllArgsConstructor @NoArgsConstructor
@NamedQuery(name = FIND_ALL_USERS, query = "select u from User u")
@NamedQuery(name = FIND_USER_BY_IDS, query = "select u from User u where u.userid in :ids")
public class User extends AbstractDomain {
    public static final String FIND_USER_BY_IDS = "User.findUserByIds";
    public static final String FIND_ALL_USERS = "User.findAllUsers";

    public enum State {
        ACTIVE, INACTIVE
    }

    @Id
    String userid;

    @JsonbTransient
    String password;

    @Enumerated(EnumType.STRING)
    State currentState = State.ACTIVE;

    @JsonbTransient
    @ManyToMany(mappedBy = "users")
    Set<Group> groups;

    @JsonbProperty(nillable=true)
    String firstname;
    String middlename;
    
    @JsonbProperty(nillable=true)
    String lastname = "";
    String phonenumber;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_properties", joinColumns=@JoinColumn(name="userid"))
    @MapKeyColumn(name="property_key")
    @Column(name = "value")
    Map<String,String> properties = new HashMap<String, String>();

    public User(String userid) {
        this.userid = userid;
    }

    public User(String userid, String password) {
        this.userid = userid;
        this.password = password;
    }
    
    @PrePersist
    protected void onCreate() {
        if(password == null) {
            password = UUID.randomUUID().toString();
        }
    }
    
    public void addGroup(Group group) {
        getGroups().add(group);
        group.getUsers().add(this);
    }

    public Set<Group> getGroups() {
        if(groups == null) {
            groups = new HashSet<>();
        }
        return groups;
    }
    
    public String getFirstname() {
        return firstname != null ? firstname : "";
    }
    
    public String getLastname() {
        return lastname != null ? lastname : "";
    }
}
