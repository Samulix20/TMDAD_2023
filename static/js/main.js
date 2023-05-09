'use strict';

// Login page
const usernamePage = document.querySelector('#username-page');
const usernameForm = document.querySelector('#usernameForm');
const loginInfoElement = document.querySelector('#logininfo');

// Chat page
const chatPage = document.querySelector('#chat-page');

// Area for showing msgs
const inboxName = document.querySelector('#inboxName');
const messageArea = document.querySelector('#messageArea');
const messageInput = document.querySelector('#message');
const connectingElement = document.querySelector('#connecting');

// Form for starting chats
const chatStartForm = document.querySelector('#chatStartForm');

// Create group
const createGroupForm = document.querySelector('#createGroupForm');
// Add to group
const addToGroupForm = document.querySelector('#addGroupForm');

// Notification
const notificationText = document.querySelector('#notif');

// Cached files
const fileCache = new Map();

let stompClient = null;
let username = null;
let jwtToken = null;

const colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function loginError(e, info) {
    if(e) console.log(e);
    loginInfoElement.textContent = info
    loginInfoElement.style.color = 'red';
    loginInfoElement.style.backgroundColor = '#FFCCCB'
    loginInfoElement.classList.remove('hidden');
}

function login(event) {

    username = document.querySelector('#name').value.trim();
    let password = document.querySelector('#password').value.trim();
    event.preventDefault();

    if(username && password) {

        let jsonRequestBody = JSON.stringify({
            username: username,
            password: password,
        });

        let endpoint = null
        let errStr = null

        if (event.submitter.id == 'register') {
            endpoint = '/api/users/register'
            errStr = 'Register'
        } else {
            endpoint = '/api/users/login'
            errStr = 'Login'
        }
        
        fetch(endpoint, {
            method: 'POST',
            mode: 'cors',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
            },
            body: jsonRequestBody
        })
        .then(x => x.json())
        .then(data => {
            jwtToken = data.token;
            inboxName.innerHTML = username + "'s Inbox"
            usernamePage.classList.add('hidden');
            chatPage.classList.remove('hidden');
            const socket = new SockJS('/api/websocket');
            stompClient = Stomp.over(socket);
            var stompHeaders = {};
            stompHeaders['token'] = jwtToken;
            stompClient.connect(stompHeaders, onConnected, onError);
        }).catch (e => {
            loginError(e, errStr + " error");
        });
    } else {
        loginError(null, "Missing Fields");
    }

    usernameForm.reset();
}

async function onConnected() {
    // Subscribe to the user topic
    await stompClient.subscribe('/topic/chat/' + username, onMessageReceived);
    // Subscribe to the system personal notifications topic
    await stompClient.subscribe('/topic/system/notifications/' + username, onNotificationReceived);
    connectingElement.classList.add('hidden');
    stompClient.send("/app/chat.start", {}, "")
    stompClient.send("/app/chat.getGroups", {}, "")
}

function startChat(event) {
    event.preventDefault();

    let msgReceiver = document.querySelector('#userStart').value.trim();
    let msgContent = document.querySelector('#messageStart').value.trim();
    let msgAttch = document.querySelector('#fileAttch').files;
    let msgReceiverType = document.querySelector('#sendRcvType').value;
    let msg;

    if(msgReceiver && stompClient) {

        let tg = msgReceiverType === 'group';

        if(msgAttch.length > 0) {
            let attchUUID;
            for (var i = 0; i < msgAttch.length; i++) {
                // Add file type extension to directly download using the attchUUID
                attchUUID = crypto.randomUUID() + '.' + msgAttch[i].name.split('.').pop()
                fileCache.set(attchUUID, msgAttch[i]);
            }
            msg = {
                content: attchUUID,
                toGroup: tg,
                receiver: msgReceiver,
                type: 'ATTACHMENT'
            }
        } else if (msgContent) {
            msg = {
                content: msgContent,
                toGroup: tg,
                receiver: msgReceiver,
                type: 'CHAT'
            }
        } else {
            return;
        }

        stompClient.send("/app/chat.send", {}, JSON.stringify(msg));
        chatStartForm.reset();
    }
}

function createGroup(event) {
    event.preventDefault();

    let gn = document.querySelector('#groupName').value.trim();

    if(gn && stompClient) {
        let msg = {
            target: gn
        }
        stompClient.send("/app/chat.createGroup", {}, JSON.stringify(msg));
    }
    
    createGroupForm.reset();
}

function addUserToGroup(event) {
    event.preventDefault();

    let group = document.querySelector('#groupAdd').value.trim();
    let user = document.querySelector('#userAdd').value.trim();

    if(group && user && stompClient) {
        let msg = {
            target: group,
            name: user
        }
        stompClient.send("/app/chat.addToGroup", {}, JSON.stringify(msg));
    }
    
    addToGroupForm.reset();
}

function onError(error) {
    try {
        console.error('Error:', error.headers.message);
    } catch (e) {}
    
    connectingElement.textContent = 'No fue posible conectar con WebSocket';
    connectingElement.style.color = 'red';
}

function onNotificationReceived(payload) {
    let notification = JSON.parse(payload.body);
    notificationText.innerHTML = JSON.stringify(notification);

    if (notification.type == 'UPLOAD_FILE') {
        fetch(notification.url, {
            method: 'PUT',
            body: fileCache.get(notification.uuid)
        }).catch((e) => {
            console.log(e);
        });
        fileCache.delete(notification.uuid)
    }
}

async function downloadFile(name) {
    const image = await fetch("/api/files/" + name, {
        mode: "cors",
        headers: {'Authorization': 'Bearer ' + jwtToken}
    });
    const imageBlob = await image.blob();
    const imageURL = URL.createObjectURL(imageBlob);
    const anchor = document.createElement("a");
    anchor.href = imageURL;
    anchor.download = name;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(imageURL);
}

function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);

    const messageElement = document.createElement('li');

    if(message.type === 'JOIN') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' joined!';
    } else if (message.type === 'LEAVE') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' left!';
    } else {
        messageElement.classList.add('chat-message');

        const avatarElement = document.createElement('i');
        const avatarText = document.createTextNode(message.sender[0]);
        avatarElement.appendChild(avatarText);
        avatarElement.style['background-color'] = getAvatarColor(message.sender);

        messageElement.appendChild(avatarElement);

        const usernameElement = document.createElement('span');
        const usernameText = document.createTextNode(message.sender);
        usernameElement.appendChild(usernameText);
        messageElement.appendChild(usernameElement);
    }

    const textElement = document.createElement('p');

    if(message.type === 'ATTACHMENT') {
        var inputElement = document.createElement('button');
        inputElement.innerHTML = "Download File";
        inputElement.setAttribute("name", message.content);
        inputElement.addEventListener('click', function(event){
            downloadFile(event.target.getAttribute('name'));
        });
        textElement.appendChild(inputElement)

    } else {
        const messageText = document.createTextNode(message.content);
        textElement.appendChild(messageText);
    }
    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

function getAvatarColor(messageSender) {
    let hash = 0;
    for (let i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }

    const index = Math.abs(hash % colors.length);
    return colors[index];
}

usernameForm.addEventListener('submit', login, true);
chatStartForm.addEventListener('submit', startChat, true);
createGroupForm.addEventListener('submit', createGroup, true);
addToGroupForm.addEventListener('submit', addUserToGroup, true)
