const dateFormat = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}).\d{1,3}Z\[UTC\]$/;

function reviver(key, value) {
    if (typeof value === "string" && dateFormat.test(value)) {
        let match = dateFormat.exec(value);
        return new Date(match[1]);
    }
    
    return value;
}

function sortUsers(a,b) {
    if(a.firstname < b.firstname) return -1;
    if(a.firstname > b.firstname) return 1;
    return 0;
}

class User {
    constructor(uid,firstname = '', lastname = '') {
        this.userid = uid;
        this.firstname = firstname;
        this.lastname = lastname;
    }
}

class Message {
    constructor(sender, text = '', created = new Date(), photos = null) {        
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


class NetworkService {    
    constructor() {
        this.token = '';
    }

    login(userid, password) {
        this.token = 'Basic ' + btoa(userid + ":" + password);
        let headers = new Headers();        
        headers.append('Authorization', this.token);

        return fetch('api/auth/login', {
            method: 'GET',
            headers: headers
        }).then(response => response.json())
          .then(response => {
            this.user = response;
            return response;
        });
    }

    getUser() {
        return this.user;
    }

    updateRecipients(conversation) {
        let formData = new URLSearchParams();                        
        formData.append('conversationid',conversation.id);
        
        for(let recipient of conversation.recipients) {
            formData.append('recipientids',recipient.userid);
        }
            
        let headers = new Headers();        
        headers.append('Authorization', this.token);            
        fetch('api/conversation/updaterecipients', {
            method: 'POST',
            body: formData,
            headers: headers
        });
    }
    
    sendMessage(conversationid,text,filelist) {
        // POST message to server
        let formData = new FormData();
        formData.append('conversationid',conversationid);
        formData.append('message',text);

        if(filelist) {
            for(let i = 0; i < filelist.length; i++) {
                formData.append('image', filelist[i]);
            }
        }
        
        let headers = new Headers();        
        headers.append('Authorization', this.token);            
        fetch('api/chat/send', {
            method: "POST",
            body: formData,
            headers: headers
        })
        .then(response => response.json())
        .catch(error => console.error('Error:', error))
        .then(msg => console.log("Message: " + JSON.stringify(msg)));        
    }
    

    addConversation(updateUI) {
        let headers = new Headers();        
        headers.append('Authorization', this.token);            
        fetch('api/conversation/createconversation',{
            method: 'GET',
            headers: headers
        }).then(response => response.text())
          .then(text => JSON.parse(text,reviver))
          .then(updateUI);
    }    
    
    getContacts(updateUI = contacts => {}) {
        let headers = new Headers();        
        headers.append('Authorization', this.token);        
        fetch('api/chat/users', {
            method: 'GET',
            cache: "no-store",
            headers: headers
        }).then(response => response.json())
            .then(result => {
                let contacts = new Map();
                for(let i = 0; i < result.length; i++) {
                    contacts.set(result[i].userid,result[i]);
                }
                return contacts;
            })
            .then(contacts => updateUI(contacts))
            .catch(error => console.error('Fetching contacts error:', error));
    }
    
    getConversations(contacts, updateUI = contacts => {}) {
        // Initialize conversations
        let headers = new Headers();
        headers.append('Authorization', this.token);
        fetch('api/conversation',{
            method: 'GET',
            cache: "no-store",
            headers: headers
        }).then(response => response.text())
         .then(text => JSON.parse(text,reviver))
         .then(conversations => {
            for(let i = 0; i < conversations.length; i++) {
                conversations[i].recipients = this.createContactSet(contacts, conversations[i].recipients);
            }
            return conversations;
         })
         .then(conversations => updateUI(conversations))
         .catch(error => console.error('Fetching conversations error:', error));
    }
    
    createContactSet(contacts,recipients) {
        recipients.sort(sortUsers);
        let result = new Set();
        for(let i = 0; i < recipients.length; i++) {
            if(contacts.has(recipients[i].userid)) {
                result.add(contacts.get(recipients[i].userid));
            }
        }
        return result;
    }    
}





class Controller {
    constructor() {
        // Handle all networkcalls
        this.service = new NetworkService();
        
        // Domain datastructures
        /*let nils = new User('nils', 'Nils');
        let gunnar = new User('gunnar','Gunnar');
        let eva = new User('eva','Eva');
        let petra = new User('petra','Petra');
        
        this.contacts = new Map();
        this.contacts.set(nils.userid,nils);
        this.contacts.set(gunnar.userid,gunnar);
        this.contacts.set(eva.userid,eva);
        this.contacts.set(petra.userid,petra);

        let uid = this.service.getUser().userid;
        this.conversations = [
            new Conversation(uid.userid,new Set(this.contacts.values()),
                [new Message(uid.userid, "Text 1", new Date()),
                 new Message(nils, "Text 2", new Date())]),
            new Conversation(uid.userid,new Set([nils,gunnar]),
                [new Message(uid.userid, "Text 3", new Date()),
                 new Message(gunnar, "Text 4", new Date())])
        ];*/

        // The selected conversation
        this.selectedConversation = null;

        // Setup Login-View
        this.login_view = document.getElementById('login-view');
        this.login_view.style.display = 'block';
        document.getElementById('login').onclick = event => this.login();
        document.querySelector('.login-panel').onkeydown = event => {
            if(event.code === 'Enter' && document.activeElement !== document.getElementById('login')) {
                this.login();
            }
        };

        // Setup Conversation-View
        this.conversation_view = document.getElementById('conversations-view');
        this.conversation_view.style.display = 'none';
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
        this.thumbnails = document.getElementById('thumbnails');

        this.textInput = document.getElementById('text');
        this.textInput.onkeydown = event => {
            if(event.code === 'Enter') {
                this.sendMessage();
            }
        };
        document.getElementById('add-img').onclick = event => this.fileInput.click();
        document.getElementById('send').onclick = event => this.sendMessage();
    }


    /**
     * 
     * @returns {undefined}
     */
    login() {
        let userid = document.getElementById('login-userid').value;
        let password = document.getElementById('login-password').value;
        this.service.login(userid,password).then(user => {
            // Load users
            this.service.getContacts(contacts => {
                this.contacts = contacts;

                // Load conversations
                this.service.getConversations(contacts, conversations => {
                    this.conversations = conversations;
                    for (let i = 0; i < this.conversations.length; i++) {
                        let conversation = this.conversations[i];
                        let li = this.createMessage(conversation.messages[0]);
                        li.onclick = event => this.openConversation(conversation);
                        this.conversationsList.appendChild(li);
                    }
                    this.showConversation();
                });

            });
        });
    }


    /**
     *
     */
    addConversation() {
        this.service.addConversation(conversation => {
            conversation.recipients = new Set();
            this.selectedConversation = conversation;
            this.conversations.unshift(this.selectedConversation);
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
        this.messagesList.scrollTop = this.messagesList.scrollHeight;
    }


    /**
     *
     * @param conversation
     */
    populateContacts(conversation) {
        let to = document.getElementById('to');
        to.innerHTML = '';
        let owner = document.createElement('span');
        owner.innerText = conversation.owner.firstname + " " + conversation.owner.lastname;
        to.appendChild(owner);
        for(let contact of conversation.recipients) {
            let tag = document.createElement('span');
            tag.innerText = contact.firstname + " " + contact.lastname;
            to.appendChild(tag);
        }

        // Initialize contacts
        this.contactList.innerHTML = '';
        let names = Array.from(this.contacts.values());
        names.sort(sortUsers);
        for(let contact of names) {
            let cb = document.createElement('input');
            cb.type = 'checkbox';
            
            if(contact.userid === conversation.owner.userid){
                cb.checked = true;
                cb.disabled = true;
            } else {
                cb.checked = this.hasContact(contact, conversation.recipients);
            }
            
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
        this.service.updateRecipients(this.selectedConversation);
    }


    /**
     *
     */
    showConversation() {
        this.login_view.style.display = 'none';
        this.conversation_view.style.display = 'block';
        this.message_view.style.display = 'none';

        this.clearTempImage();
        this.conversationsList.innerHTML = '';
        for(let i = 0; i < this.conversations.length; i++) {
            let first = this.conversations[i].messages[0];
            if(first === undefined) {
                first = new Message(this.service.getUser());
            }
            let li = this.createMessage(first);
            li.onclick = event => this.openConversation(this.conversations[i]);
            this.conversationsList.appendChild(li);
        }
    }

    showMessage() {
        this.login_view.style.display = 'none';
        this.conversation_view.style.display = 'none';
        this.message_view.style.display = 'flex';
        this.clearTempImage();
        this.textInput.focus();
    }
    
    sendMessage() {
        if(this.textInput.value.length > 0 || this.hasImage()) {
            this.service.sendMessage(this.selectedConversation.id,this.textInput.value,this.fileInput.files);

            /* Add to messagelist */
            let msg = new Message(this.service.getUser(), this.textInput.value);
            this.selectedConversation.messages.push(msg);
            this.messagesList.appendChild(this.createMessage(msg,true));
            this.messagesList.scrollTop = this.messagesList.scrollHeight;
            this.textInput.value = '';
        }
    }

    onImageChange() {
        if(this.hasImage()) {
            for(let file of this.fileInput.files) {
                let obj = new FileReader();
                obj.onload = data => {
                    let img = document.createElement('img');
                    img.src = data.target.result;
                    this.thumbnails.appendChild(img);
                };
                obj.readAsDataURL(file);
            }
        }
    }

    hasImage() {
        return this.fileInput.files && this.fileInput.files[0];
    }

    clearTempImage() {
        this.fileInput.value = '';
        this.thumbnails.innerHTML = '';
    }


    /**
     * Create a new message element
     *
     * li
     *   message-container
     *      icon
     *      message-content
     *          header
     *          text
     *          images
     *   date
     *
     * @param msg the message
     * @param rightify right-justify message
     * @returns {HTMLLIElement}
     */
    createMessage(msg, rightify = false) {
        let user = this.service.getUser();
        if(!msg) {
            msg = new Message(user);
        }

        let isRight = msg.sender.userid === user.userid && rightify;
        let images = document.createElement('div');
        images.setAttribute('class','message-images');
        if(rightify) {
            if(msg.photos && msg.photos.length > 0) {
                for(let photo of msg.photos) {
                    let image = document.createElement('img');
                    image.src = 'api/chat/image/' + photo;
                    images.appendChild(image);
                }
            } else if(this.hasImage()) {
                for(let file of this.fileInput.files) {
                    let reader = new FileReader();
                    reader.onload = data => {
                        let image = document.createElement('img');
                        image.src = data.target.result;
                        images.appendChild(image);
                    };
                    reader.readAsDataURL(file);
                }
                this.clearTempImage();
            }
        }

        let messageContent = document.createElement('div');
        messageContent.setAttribute('class','message-content');
        messageContent.innerHTML = `
            <div class="message-header">${msg.sender.firstname}</div>
            <div class="text">${msg.text}</div>
        `;
        messageContent.appendChild(images);

        let messageContainer = document.createElement('div');
        messageContainer.setAttribute('class','message-container');
        messageContainer.innerHTML = `
             <img id="account-img" src="https://cdn.iconscout.com/public/images/icon/premium/png-512/account-avatar-man-client-person-profile-user-32aab4c67b296cea-512x512.png">
        `;
        messageContainer.appendChild(messageContent);

        let date = document.createElement('div');
        date.setAttribute('class', 'date');
        date.innerHTML = `
            ${msg.created.toLocaleDateString('nb-NO')} 
            ${msg.created.toLocaleTimeString('nb-NO')}
        `;

        let li = document.createElement('li');
        li.setAttribute('class', isRight ? 'message message-right' : 'message message-left');
        li.appendChild(messageContainer);
        li.appendChild(date);

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