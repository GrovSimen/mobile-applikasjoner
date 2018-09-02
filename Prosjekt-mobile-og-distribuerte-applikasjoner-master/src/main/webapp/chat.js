

var     conversationNumber = 1,
        conversationName = 'Conversation',
        conversationList = $("#conversationlist"),
        addConversation = document.getElementById('newConv'),
        sendMessage = document.getElementById("sendmessage"),
        messageList1 = $("#messagelist1"),
        messageList2 = document.getElementById("messagelist2"),
        messageArea = document.getElementById("messagecontent");


function getMessageContent() {
    messageContent = document.getElementById("messagecontent").value;
    return messageContent;
}

function sendTheMessage() {
    var messageContent = getMessageContent();
    var e = document.createElement("LI");
    e.className = "chat-message";
    e.textContent = messageContent;
    
    messageList2.append(e);
    
    clearContents(document.getElementById("messagecontent"));
    
}

function clearContents(element) {
    element.value = '';
}

addConversation.addEventListener('click', function makeConversation() {
    var conversationEl = $("<li class='conversation'>"
            + "<button id="
            + conversationName + conversationNumber
            + ">"
            + conversationName + " " +conversationNumber
            + "</button>"
            +'</li>');
    
    conversationList.append(conversationEl);
    
    conversationNumber++;
    
});


sendMessage.addEventListener('click', sendTheMessage());

messageArea.addEventListener('keydown', function (event) {
    if((event.keyCode || event.charCode) !== 13) return true;
    sendTheMessage();
    return false;
  });
