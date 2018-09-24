const dateFormat = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}).\d{1,3}Z\[UTC\]$/;

function reviver(key, value) {
    if (typeof value === "string" && dateFormat.test(value)) {
        let match = dateFormat.exec(value);
        return new Date(match[1]);
    }
    
    return value;
}

class User {
    constructor(uid,firstname, lastname) {
        this.userid = uid;
        this.firstname = firstname;
        this.lastname = lastname;
    }
}

class Message {
    constructor(sender, text, created = new Date(), photos = null) {        
        this.sender = sender;
        this.text = text;
        this.created = new Date(created);
        this.photos = photos;
    }
}

class Conversation {
    constructor(from, recipients = new Set(), messages = [], id = -1) {
        this.owner = from;
        this.recipients = recipients;
        this.messages = messages;
        this.id = id;
    }
}

uid = new User('user1','Firstname', 'Lastname');


class Controller {
    constructor() {
        // Domain datastructures
        let jorgen = new User('jørgen', 'Jørgen');
        let mikael = new User('mikael','Mikael');
        let kari = new User('kari','Kari');
        this.contacts = new Set([jorgen,mikael,kari]);

        this.conversations = [
            new Conversation(uid,new Set(this.contacts),
                [new Message(uid, "Hei, hvordan går det her?", new Date()),
                 new Message(jorgen, "Hei, ja her går det bra:)", new Date())]),
            new Conversation(uid,new Set([kari]),
                [new Message(uid, "Dette er en test", new Date()),
                 new Message(kari, "Så bra det funker", new Date())])
        ];

        // The selected conversation
        this.selectedConversation = null;

        // Setup Conversation-View
        this.conversation_view = document.getElementById('conversations-view');
        this.conversation_view.style.display = 'block';
        this.conversationsList = document.getElementById('conversations');
        document.getElementById('addconversation').onclick = event => this.addConversation();

        // Setup Message View
        this.contactList  = document.getElementById('contact-list');
        this.message_view = document.getElementById('messages-view');
        this.messagesList = document.getElementById('messages');
        document.getElementById('message-back').onclick = event => this.showConversation();

        // Message edit and send
        this.fileInput = document.getElementById('img-file');
        this.fileInput.onchange = event => this.onImageChange();
        this.image = document.getElementById('image');

        this.textInput = document.getElementById('text');
        this.textInput.onkeydown = event => {
            if(event.code === 'Enter') {
                this.sendMessage();
            }
        };
        document.getElementById('add-img').onclick = event => this.fileInput.click();
        document.getElementById('send').onclick = event => this.sendMessage();


        // Load users
        fetch('api/chat/users')
            .then(response => response.json())
            .then(contacts => {
                this.contacts = new Map();
                for(let i = 0; i < contacts.length; i++) {
                    this.contacts.set(contacts[i].userid,contacts[i]);
                }

                // Initialize conversations                
                fetch('api/conversation/' + uid.userid)
                    .then(response => response.text())
                    .then(text => JSON.parse(text,reviver))
                    .then(conversations => {
                        for(let i = 0; i < conversations.length; i++) {
                            conversations[i].recipients = this.createContactSet(conversations[i].recipients);
                        }
                        return conversations;
                    })
                    .then(conversations => {
                        this.conversations = conversations;
                        for(let i = 0; i < this.conversations.length; i++) {
                            let conversation = this.conversations[i];
                            let li = this.createMessage(conversation.messages[0]);
                            li.onclick = event => this.openConversation(conversation);
                            this.conversationsList.appendChild(li);
                        }
                     })
                    .catch(error => console.error('Fetching conversations error:', error));
            }
        );
    }
        

    createContactSet(recipients) {
        let result = new Set();
        for(let i = 0; i < recipients.length; i++) {
            if(this.contacts.has(recipients[i].userid)) {
                result.add(this.contacts.get(recipients[i].userid));
            }
        }
        return result;
    }

    /**
     *
     */
    
    
    addConversation() {
        fetch('api/conversation/createconversation?userid=' + uid.userid)
            .then(response => response.text())
            .then(text => JSON.parse(text,reviver))
            .then(conversation => {
                conversation.recipients = new Set();
                this.selectedConversation = conversation;
                this.conversations.push(this.selectedConversation);
                this.openConversation(this.selectedConversation);
        });
    }
    


    /**
     *
     * @param conversation
     */
    openConversation(conversation) {
        this.selectedConversation = conversation;
        this.messagesList.innerHTML = '';
        for(let i = 0; i < this.selectedConversation.messages.length; i++) {
            let msg = this.selectedConversation.messages[i];
            this.messagesList.appendChild(this.createMessage(msg,true));
        }
        this.populateContacts(this.selectedConversation);
        this.showMessage();
    }


    /**
     *
     * @param conversation
     */
    populateContacts(conversation) {
        let to = document.getElementById('to');
        to.innerHTML = '';
        for(let contact of conversation.recipients) {
            let tag = document.createElement('span');
            tag.innerText = contact.firstname + " " + contact.lastname;
            to.appendChild(tag);
        }

        // Initialize contacts
        this.contactList.innerHTML = '';
        for(let contact of this.contacts.values()) {
            let cb = document.createElement('input');
            cb.type = 'checkbox';
            cb.checked = this.hasContact(contact, conversation.recipients);
            cb.id = `c${contact.userid}`;
            cb.onclick = event => {
                cb.checked ?  conversation.recipients.add(contact) : conversation.recipients.delete(contact);
                this.updateToField(conversation);
            };

            let label = document.createElement('label');
            label.setAttribute('for', `c${contact.userid}`);
            label.innerText = contact.firstname + " " + contact.lastname;

            let li = document.createElement('li');
            li.appendChild(cb);
            li.appendChild(label);

            this.contactList.appendChild(li);
        }
    }

    hasContact(contact, recipients) {
        for(let rcontact of recipients) {
            if(rcontact.userid === contact.userid) {                
                return true;
            }
        }
        
        return false;
    }


    /**
     *
     * @param conversation
     */
    updateToField(conversation) {
        let to = document.getElementById('to');
        to.innerHTML = '';
        for(let contact of conversation.recipients) {
            let tag = document.createElement('span');
            tag.innerHTML = `${contact.firstname} ${contact.lastname}`;
            to.appendChild(tag);
        }
    }


    /**
     *
     */
    showConversation() {
        this.conversation_view.style.display = 'block';
        this.message_view.style.display = 'none';

        this.clearTempImage();
        this.conversationsList.innerHTML = '';
        for(let i = 0; i < this.conversations.length; i++) {
            let first = this.conversations[i].messages[0];
            if(first === undefined) {
                first = new Message(uid,'',new Date());
            }
            let li = this.createMessage(first);
            li.onclick = event => this.openConversation(this.conversations[i]);
            this.conversationsList.appendChild(li);
        }
    }

    showMessage() {
        this.conversation_view.style.display = 'none';
        this.message_view.style.display = 'flex';
        this.clearTempImage();
        this.textInput.focus();
    }
    
    sendMessage() {
        if(this.textInput.value.length > 0 || this.hasImage()) {
            let msg = new Message(uid, this.textInput.value, new Date());

            // POST message to server
            let formData = new FormData();
            formData.append('userid',uid.userid);
            formData.append('conversationid',this.selectedConversation.id);
            formData.append('message',msg.text);
            /*formData.append('message', new Blob([JSON.stringify(msg)], {
                                type: "application/json"})
                            );*/
                            
            for(let i = 0; i < this.fileInput.files.length; i++) {
                formData.append('image', this.fileInput.files[i]);
            }

           fetch('api/chat/send', {
                method: "POST",
                body: formData
            })
            .then(response => response.json())
            .catch(error => console.error('Error:', error))
            .then(msg => console.log("Message: " + JSON.stringify(msg)));
            
           
            /* Add to messagelist */
            this.selectedConversation.messages.push(msg);
            this.messagesList.appendChild(this.createMessage(msg,true));
            this.messagesList.scrollTop = this.messagesList.scrollHeight;
            this.textInput.value = '';

        }
    }

    onImageChange() {
        if(this.hasImage()) {
            let file = this.fileInput.files[0];
            let obj = new FileReader();
            obj.onload = data => this.image.src = data.target.result;
            obj.readAsDataURL(file);
        }
    }

    hasImage() {
        return this.fileInput.files && this.fileInput.files[0];
    }

    clearTempImage() {
        this.fileInput.value = '';
        this.image.src = '';
    }

    createMessage(msg, rightify = false) {
        if(!msg) {
            msg = new Message();
            msg.sender = uid;
        }
        
        let isRight = msg.sender === uid && rightify;
        let classes = 'account-icon material-icons';
        if(isRight) {
            classes += ' account-icon-right';
        }

        let li = document.createElement('li');
        li.setAttribute('class', isRight ? 'message message-right' : 'message');
        li.data = msg;
        li.innerHTML = `
             <img id="account-img" src="https://cdn.iconscout.com/public/images/icon/premium/png-512/account-avatar-man-client-person-profile-user-32aab4c67b296cea-512x512.png">
             <div>
                <div>${msg.sender.firstname}</div>
                <div class="text">${msg.text}</div>
                <div class="date">
                    ${msg.created.toLocaleDateString('nb-NO')} ${msg.created.toLocaleTimeString('nb-NO')}
                </div>
            </div>`;

        // Add image if exist
        if(rightify && (msg.photo || this.hasImage())) {
            let image = document.createElement('img');
            if(msg.photo) {
                image.src = msg.photo;
            } else {
                let reader = new FileReader();
                reader.onload = data => {
                    msg.photo = data.target.result;
                    image.src = data.target.result;
                };
                reader.readAsDataURL(this.fileInput.files[0]);
                this.clearTempImage();
            }

            li.appendChild(image);
        }

        return li;
    }
}

/***
 * Setup application
 */
ctrl = null;
document.addEventListener('DOMContentLoaded', function() {
    this.ctrl = new Controller();
}, false);